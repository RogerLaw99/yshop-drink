# yshop-spring-boot-starter-biz-data-permission

> 所属：yshop-framework；文档依据本仓库源码整理。

## 1. 模块定位

本模块提供**数据权限**能力：在 MyBatis-Plus 的 SQL 执行阶段，通过 JSqlParser 解析并重写 `SELECT / UPDATE / DELETE` 语句，自动拼接部门、用户维度的过滤条件，从而实现“不同用户只能看到自己权限范围内的数据”。

核心设计：

- 数据权限规则由 `DataPermissionRule` 抽象，规则自行声明“对哪些表生效”以及“生成什么 WHERE 条件”。
- `@DataPermission` 注解可标注在类或方法上，用于整体开关、只启用某些规则或排除某些规则。
- 默认提供基于部门的 `DeptDataPermissionRule`，通过 `system` 模块的 `PermissionApi` 获取当前登录管理员的部门数据权限。
- 即使方法上没有 `@DataPermission` 注解，数据权限默认也是**开启**的，需要显式 `enable = false` 才关闭。

### 在 yshop-server 中的依赖与启用状态

- `yshop-server/pom.xml` **没有直接声明**本模块。
- 通过 `yshop-module-system-biz` 传递依赖引入（该 pom 中声明了 `yshop-spring-boot-starter-biz-data-permission`）。
- 自动配置类通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册，启动时生效。
- `yshop-module-system-biz` 中的 `DataPermissionConfiguration` 注册了 `DeptDataPermissionRuleCustomizer`，且 `PermissionApi` 实现存在，因此部门数据权限规则在运行时确实被创建。
- 业务代码中已使用 `@DataPermission`（如 `UserProfileController`、`DeptServiceImpl`、`PermissionServiceImpl`、`SmsSendServiceImpl`）和 `DataPermissionUtils.executeIgnore(...)`（`AdminUserServiceImpl`）。

## 2. 目录与包结构

```text
yshop-spring-boot-starter-biz-data-permission/
├── pom.xml
└── src/main/
    ├── java/co/yixiang/yshop/framework/datapermission/
    │   ├── config/
    │   │   ├── YshopDataPermissionAutoConfiguration.java
    │   │   └── YshopDeptDataPermissionAutoConfiguration.java
    │   └── core/
    │       ├── annotation/DataPermission.java
    │       ├── aop/
    │       │   ├── DataPermissionAnnotationAdvisor.java
    │       │   ├── DataPermissionAnnotationInterceptor.java
    │       │   └── DataPermissionContextHolder.java
    │       ├── db/DataPermissionDatabaseInterceptor.java
    │       ├── rule/
    │       │   ├── DataPermissionRule.java
    │       │   ├── DataPermissionRuleFactory.java
    │       │   ├── DataPermissionRuleFactoryImpl.java
    │       │   └── dept/
    │       │       ├── DeptDataPermissionRule.java
    │       │       ├── DeptDataPermissionRuleCustomizer.java
    │       │       └── package-info.java
    │       └── util/DataPermissionUtils.java
    └── resources/META-INF/spring/
        └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

## 3. 核心类与职责

| 类/接口 | 所在包 | 职责 |
|---|---|---|
| `YshopDataPermissionAutoConfiguration` | `...datapermission.config` | 注册规则工厂、SQL 拦截器、注解 Advisor |
| `YshopDeptDataPermissionAutoConfiguration` | `...datapermission.config` | 条件装配基于部门的 `DeptDataPermissionRule` |
| `DataPermission` | `...datapermission.core.annotation` | 类/方法级数据权限开关及规则包含、排除配置 |
| `DataPermissionAnnotationAdvisor` | `...datapermission.core.aop` | 匹配类或方法上的 `@DataPermission` 注解 |
| `DataPermissionAnnotationInterceptor` | `...datapermission.core.aop` | 方法执行前将注解压栈，执行后出栈；带方法级缓存 |
| `DataPermissionContextHolder` | `...datapermission.core.aop` | 基于 `TransmittableThreadLocal` 的注解栈，支持嵌套调用与线程传递 |
| `DataPermissionDatabaseInterceptor` | `...datapermission.core.db` | 基于 MyBatis-Plus `InnerInterceptor` + JSqlParser 重写 SQL |
| `DataPermissionRule` | `...datapermission.core.rule` | 数据权限规则接口：表名集合 + 条件表达式 |
| `DataPermissionRuleFactory` | `...datapermission.core.rule` | 规则容器接口 |
| `DataPermissionRuleFactoryImpl` | `...datapermission.core.rule` | 按当前 `@DataPermission` 配置过滤规则 |
| `DeptDataPermissionRule` | `...datapermission.core.rule.dept` | 部门/用户维度的数据权限规则实现 |
| `DeptDataPermissionRuleCustomizer` | `...datapermission.core.rule.dept` | 业务模块用于登记“表 → 部门/用户字段”的函数式接口 |
| `DataPermissionUtils` | `...datapermission.core.util` | 以 `enable = false` 临时忽略数据权限执行逻辑 |

## 4. 自动配置与生效方式

`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 内容：

```text
co.yixiang.yshop.framework.datapermission.config.YshopDataPermissionAutoConfiguration
co.yixiang.yshop.framework.datapermission.config.YshopDeptDataPermissionAutoConfiguration
```

装配过程：

1. `YshopDataPermissionAutoConfiguration` 无条件生效：
   - `DataPermissionRuleFactory`：注入容器中所有 `DataPermissionRule`。
   - `DataPermissionDatabaseInterceptor`：通过 `MyBatisUtils.addInterceptor(interceptor, inner, 0)` 插入到 `MybatisPlusInterceptor` 的**第 0 位**，保证位于分页插件之前。
   - `DataPermissionAnnotationAdvisor`：同时匹配类级和方法级 `@DataPermission`。
2. `YshopDeptDataPermissionAutoConfiguration` 需同时满足：
   - `@ConditionalOnClass(LoginUser.class)`：存在 security 组件；
   - `@ConditionalOnBean({PermissionApi.class, DeptDataPermissionRuleCustomizer.class})`：存在权限 API 与至少一个表配置定制器。
3. SQL 重写时，`DataPermissionDatabaseInterceptor` 从 `DataPermissionRuleFactory` 取得当前 Mapper 生效的规则；规则为空、或 `@DataPermission(enable = false)` 时不重写。
4. 对于已判定“无需重写”的 `MappedStatement`，`MappedStatementCache` 会缓存结果以减少 SQL 解析开销。

## 5. 关键配置项

本模块**没有定义 `@ConfigurationProperties`**，不存在 `yshop.data-permission.*` 之类的专属配置项。启用/关闭与规则选择完全由注解和容器中的 Bean 决定。

`@DataPermission` 的注解参数（不是配置文件项）：

| 参数 | 默认值 | 说明 |
|---|---|---|
| `enable` | `true` | 是否启用数据权限；`false` 时该规则范围内不重写 SQL |
| `includeRules` | `{}` | 只启用指定规则类，优先级高于 `excludeRules` |
| `excludeRules` | `{}` | 排除指定规则类 |

`DeptDataPermissionRule` 的表字段登记不是配置文件，而是通过 `addDeptColumn(...)` / `addUserColumn(...)` 在 Java 配置中完成。

## 6. 使用方式 / 典型用法

### 6.1 关闭某个方法的数据权限

```java
@DataPermission(enable = false) // 避免只查看自己时查询不到部门
public List<DeptRespVO> getDeptList() {
    // ...
}
```

### 6.2 只启用或排除某些规则

```java
@DataPermission(includeRules = DeptDataPermissionRule.class)
public void queryWithDeptRule() { /* ... */ }

@DataPermission(excludeRules = DeptDataPermissionRule.class)
public void queryWithoutDeptRule() { /* ... */ }
```

### 6.3 业务模块登记部门/用户字段

`yshop-module-system-biz` 中的真实配置：

```java
@Bean
public DeptDataPermissionRuleCustomizer sysDeptDataPermissionRuleCustomizer() {
    return rule -> {
        rule.addDeptColumn(AdminUserDO.class);       // system_users.dept_id
        rule.addDeptColumn(DeptDO.class, "id");      // system_dept.id
        rule.addUserColumn(AdminUserDO.class, "id"); // system_users.id
    };
}
```

### 6.4 代码中临时忽略数据权限

```java
List<AdminUserDO> users = DataPermissionUtils.executeIgnore(() -> userMapper.selectList(...));
```

### 6.5 自定义规则

实现 `DataPermissionRule` 的两个方法，并注册为 Spring Bean：

```java
public class MyRule implements DataPermissionRule {
    @Override
    public Set<String> getTableNames() { return Set.of("biz_order"); }

    @Override
    public Expression getExpression(String tableName, Alias tableAlias) {
        return new EqualsTo(MyBatisUtils.buildColumn(tableName, tableAlias, "owner_id"),
                new LongValue(SecurityFrameworkUtils.getLoginUserId()));
    }
}
```

## 7. 依赖关系

模块自身依赖：

- `yshop-common`
- `yshop-spring-boot-starter-security`（`optional=true`，仅 `DeptDataPermissionRule` 需要）
- `yshop-spring-boot-starter-mybatis`
- `yshop-module-system-api`（获取 `PermissionApi` 与 `DeptDataPermissionRespDTO`）
- `yshop-spring-boot-starter-test`（test scope）

上游使用者：

- `yshop-module-system-biz` 直接依赖本模块；
- `yshop-server` 通过 `yshop-module-system-biz` 传递获得，因此运行时启用。

## 8. 注意事项

1. **默认开启**：没有 `@DataPermission` 注解时规则仍然生效；若某接口不应受数据权限影响，必须显式关闭。
2. 只有登录用户且 `userType == UserTypeEnum.ADMIN` 时，`DeptDataPermissionRule` 才生成条件；未登录或非管理员直接返回 `null`，不拼接条件。
3. 部门条件与用户条件同时存在时采用 `OR`：`(dept_id IN (...) OR user_id = ?)`；若既无部门又不可看自己，返回 `WHERE null = null` 以保证查不到数据。
4. 规则只对 `getTableNames()` 返回的表生效。当前仓库实际登记的是 `system_users` 与 `system_dept`，不是全库自动生效。
5. 重写发生在 SQL 层，复杂 SQL（子查询、JOIN、UNION、WITH 等）由 `DataPermissionDatabaseInterceptor` 递归处理；MyBatis-Plus 升级时需要复核其与 `TenantLineInnerInterceptor` 的同步逻辑（源码注释已提示）。
6. 拦截器插入位置为索引 0；若同时启用多租户，多租户拦截器也插入索引 0，最终先后顺序取决于 Bean 创建顺序，排查 SQL 时应同时关注两者。
7. `DataPermissionRuleFactoryImpl.getDataPermissionRule(mappedStatementId)` 当前**未使用** `mappedStatementId`，规则过滤只依赖注解上下文。
8. 业务代码中若使用 `@DataPermission(enable = false)` 调用 `PermissionApi`，可避免“获取数据权限时又触发数据权限”的递归问题（`PermissionServiceImpl` 即如此处理）。
