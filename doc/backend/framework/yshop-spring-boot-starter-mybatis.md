# yshop-spring-boot-starter-mybatis

> 所属：yshop-framework；文档依据本仓库源码整理。

## 1. 模块定位

`yshop-spring-boot-starter-mybatis` 是持久层框架 Starter，Maven 坐标 `co.yixiang.boot:yshop-spring-boot-starter-mybatis`。`pom.xml` 描述为“数据库连接池、多数据源、事务、MyBatis 拓展”。

它在 MyBatis-Plus、Druid、dynamic-datasource、MyBatis-Plus-Join、Easy-Trans 之上提供统一封装：

- 数据源与事务自动配置。
- Mapper 扫描、分页插件、字段自动填充。
- 通用 DO 基类 `BaseDO`、通用 Mapper `BaseMapperX`。
- 带 `*IfPresent` 条件的查询包装器。
- 多种 TypeHandler（逗号分隔列表、JSON Long Set、AES 加密字段）。
- 主键策略自动适配、Quartz JobStore Driver 自动设置。
- VO 数据翻译（Easy-Trans）工具。

该模块同时通过 `spring.factories` 注册了 `EnvironmentPostProcessor`，在环境准备阶段运行。

## 2. 目录与包结构

根包：`co.yixiang.yshop.framework`

```text
co.yixiang.yshop.framework
├── datasource
│   ├── config/YshopDataSourceAutoConfiguration
│   └── core
│       ├── enums/DataSourceEnum
│       └── filter/DruidAdRemoveFilter
├── mybatis
│   ├── config
│   │   ├── IdTypeEnvironmentPostProcessor
│   │   └── YshopMybatisAutoConfiguration
│   └── core
│       ├── dataobject/BaseDO
│       ├── enums/SqlConstants
│       ├── handler/DefaultDBFieldHandler
│       ├── mapper/BaseMapperX
│       ├── query/{QueryWrapperX, LambdaQueryWrapperX, MPJLambdaWrapperX}
│       ├── type/{EncryptTypeHandler, IntegerListTypeHandler, JsonLongSetTypeHandler,
│       │         LongListTypeHandler, StringListTypeHandler}
│       └── util/{JdbcUtils, MyBatisUtils}
└── translate
    ├── config/YshopTranslateAutoConfiguration
    └── core/TranslateUtils
```

注册文件：

- `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- `src/main/resources/META-INF/spring.factories`

## 3. 核心类与职责

| 类/接口 | 所在包 | 职责 |
|---|---|---|
| `YshopDataSourceAutoConfiguration` | `framework.datasource.config` | `@EnableTransactionManagement(proxyTargetClass = true)` 开启事务；启用 Druid 配置属性；当 `spring.datasource.druid.stat-view-servlet.enabled=true` 时注册 `DruidAdRemoveFilter` 去除 Druid 监控页广告。 |
| `DruidAdRemoveFilter` | `framework.datasource.core.filter` | 拦截 Druid `common.js`，用正则移除页面底部广告。 |
| `DataSourceEnum` | `framework.datasource.core.enums` | 定义多数据源名称常量 `MASTER="master"`、`SLAVE="slave"`，对应 `@Master`/`@Slave`/`@DS` 用法。 |
| `YshopMybatisAutoConfiguration` | `framework.mybatis.config` | `@AutoConfiguration(before = MybatisPlusAutoConfiguration.class)`；`@MapperScan` 扫描 `${yshop.info.base-package}` 下带 `@Mapper` 的接口；注册分页拦截器、`MetaObjectHandler`；当 `id-type=INPUT` 时按数据库类型注册 `IKeyGenerator`。 |
| `IdTypeEnvironmentPostProcessor` | `framework.mybatis.config` | 在 Spring 环境准备阶段读取主数据源 URL，推断 `DbType`；初始化 `SqlConstants.DB_TYPE`；把 `IdType.NONE` 自动改为 `INPUT`（Oracle/PostgreSQL/Kingbase/DB2/H2）或 `AUTO`（MySQL 等）；同时按需设置 Quartz `driverDelegateClass`。 |
| `BaseDO` | `framework.mybatis.core.dataobject` | 通用 DO 基类：`createTime`、`updateTime`、`creator`、`updater`、逻辑删除 `deleted`；实现 Easy-Trans `TransPojo`，并忽略 `transMap`。 |
| `DefaultDBFieldHandler` | `framework.mybatis.core.handler` | MyBatis-Plus `MetaObjectHandler`：插入时填充创建/更新时间、创建人/更新人；更新时填充更新时间、更新人。创建人/更新人取 `WebFrameworkUtils.getLoginUserId()`。 |
| `BaseMapperX<T>` | `framework.mybatis.core.mapper` | 继承 `MPJBaseMapper<T>`；提供 `selectPage(PageParam/SortablePageParam, ...)`、`selectJoinPage(...)`、`selectOne(...)`、`selectList(...)`、`selectCount(...)`、`insertBatch(...)`、`updateBatch(...)`、`insertOrUpdate(...)`、`delete(...)` 等便捷方法。 |
| `QueryWrapperX<T>` | `framework.mybatis.core.query` | 字符串列名版查询包装器，增加 `likeIfPresent`、`inIfPresent`、`eqIfPresent`、`neIfPresent`、`gtIfPresent`、`geIfPresent`、`ltIfPresent`、`leIfPresent`、`betweenIfPresent` 与 `limitN(n)`。 |
| `LambdaQueryWrapperX<T>` | `framework.mybatis.core.query` | Lambda 列引用版查询包装器，提供与 `QueryWrapperX` 对应的 `*IfPresent` 方法，并重写部分方法以保持链式返回类型。 |
| `MPJLambdaWrapperX<T>` | `framework.mybatis.core.query` | MyBatis-Plus-Join 的 Lambda 联表包装器，增加 `*IfPresent`，并重写 `selectAll`、`selectAs`、`selectCount`、`selectSum`、`selectMax`、`selectMin`、`selectAvg`、`selectLen` 等保持链式调用。 |
| `StringListTypeHandler` | `framework.mybatis.core.type` | `List<String>` ↔ 逗号分隔的 VARCHAR。 |
| `IntegerListTypeHandler` | `framework.mybatis.core.type` | `List<Integer>` ↔ 逗号分隔的 VARCHAR。 |
| `LongListTypeHandler` | `framework.mybatis.core.type` | `List<Long>` ↔ 逗号分隔的 VARCHAR。 |
| `JsonLongSetTypeHandler` | `framework.mybatis.core.type` | `Set<Long>` ↔ JSON 字符串，避免 Jackson 将小数值反序列化成 Integer。 |
| `EncryptTypeHandler` | `framework.mybatis.core.type` | 基于 Hutool AES 的字段加解密，密钥读取 `mybatis-plus.encryptor.password`。 |
| `MyBatisUtils` | `framework.mybatis.core.util` | 构建 MyBatis-Plus `Page`（含排序）、向 `MybatisPlusInterceptor` 添加内部拦截器、解析带转义符的表名、构建 `Column`。 |
| `JdbcUtils` | `framework.mybatis.core.util` | 判断 JDBC 连接是否可用；根据 URL 推断 MyBatis-Plus `DbType`。 |
| `SqlConstants` | `framework.mybatis.core.enums` | 保存当前数据库类型 `DB_TYPE`，由 `IdTypeEnvironmentPostProcessor.init` 初始化。 |
| `YshopTranslateAutoConfiguration` | `framework.translate.config` | 把容器中的 `TransService` 注入 `TranslateUtils`。 |
| `TranslateUtils` | `framework.translate.core` | 对 `List<? extends VO>` 手动触发 Easy-Trans 批量翻译；适用于无法使用 `@TransMethodResult` 的场景。 |

## 4. 自动配置与生效方式

`AutoConfiguration.imports` 注册：

```text
co.yixiang.yshop.framework.datasource.config.YshopDataSourceAutoConfiguration
co.yixiang.yshop.framework.mybatis.config.YshopMybatisAutoConfiguration
co.yixiang.yshop.framework.translate.config.YshopTranslateAutoConfiguration
```

`spring.factories` 注册：

```properties
org.springframework.boot.env.EnvironmentPostProcessor=\
  co.yixiang.yshop.framework.mybatis.config.IdTypeEnvironmentPostProcessor
```

关键条件与顺序：

- `YshopMybatisAutoConfiguration` 在 `MybatisPlusAutoConfiguration` 之前生效，避免 `@MapperScan` 扫描不到 Mapper 时打印 warn。
- `@MapperScan` 的 `annotationClass = Mapper.class`，因此 Mapper 接口需要标注 `@Mapper`。
- `@MapperScan` 的 `lazyInitialization = "${mybatis.lazy-initialization:false}"`，单元测试可设为 `true`。
- `IKeyGenerator` 仅在 `mybatis-plus.global-config.db-config.id-type=INPUT` 时注册；支持的数据库包括 PostgreSQL、Oracle/Oracle 12C、H2、Kingbase、DM。
- `IdTypeEnvironmentPostProcessor` 只在能读取到 `spring.datasource.dynamic.primary` 及其对应 `url` 时生效；它会把 `id-type=NONE` 自动改写。
- `YshopTranslateAutoConfiguration` 依赖 Easy-Trans 的 `TransService`，并由 `easy-trans.is-enable-global` 等业务配置控制全局翻译行为。

## 5. 关键配置项

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `mybatis-plus.global-config.db-config.id-type` | `NONE`（本仓库显式配置） | `NONE` 时由 `IdTypeEnvironmentPostProcessor` 自动适配；也可显式配置 `AUTO`/`INPUT`/`ASSIGN_ID` |
| `mybatis.lazy-initialization` | `false` | Mapper 是否懒加载，单元测试中设为 `true` |
| `mybatis-plus.encryptor.password` | 无默认值 | `EncryptTypeHandler` 的 AES 密钥；使用加密字段时必须配置 |
| `spring.datasource.dynamic.primary` | 无默认值 | 多数据源主库名称，`IdTypeEnvironmentPostProcessor` 读取 |
| `spring.datasource.dynamic.datasource.<name>.url` | 无默认值 | 用于推断数据库类型 |
| `spring.datasource.druid.stat-view-servlet.enabled` | 未配置 | 为 `true` 时启用 Druid 广告清理 Filter |
| `mybatis-plus.configuration.map-underscore-to-camel-case` | MyBatis-Plus 默认值 | 本仓库显式配置为 `true` |
| `mybatis-plus.global-config.db-config.logic-delete-value` | MyBatis-Plus 默认值 | 本仓库显式配置为 `1` |
| `mybatis-plus.global-config.db-config.logic-not-delete-value` | MyBatis-Plus 默认值 | 本仓库显式配置为 `0` |
| `easy-trans.is-enable-global` | 未配置 | 本仓库配置为 `true`，启用全局 VO 翻译 |

补充：`IdTypeEnvironmentPostProcessor` 还会读取 `spring.quartz.properties.org.quartz.jobStore.driverDelegateClass`，若为空则按数据库类型设置 PostgreSQL/Oracle/SQL Server 对应的 Driver。

## 6. 使用方式 / 典型用法

定义 DO：

```java
@TableName(value = "yshop_store_shop", autoResultMap = true)
@KeySequence("yshop_store_shop_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class StoreShopDO extends BaseDO {
    @TableId
    private Long id;

    private String name;

    @TableField(typeHandler = StringListTypeHandler.class)
    private List<String> images;
}
```

定义 Mapper：

```java
@Mapper
public interface StoreShopMapper extends BaseMapperX<StoreShopDO> {
    default PageResult<StoreShopDO> selectPage(StoreShopPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<StoreShopDO>()
                .likeIfPresent(StoreShopDO::getName, reqVO.getName())
                .eqIfPresent(StoreShopDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(StoreShopDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(StoreShopDO::getId));
    }
}
```

分页查询：

```java
PageResult<UserDO> page = userMapper.selectPage(pageParam, wrapper);
```

联表分页：

```java
PageResult<UserRespVO> page = userMapper.selectJoinPage(
        pageParam, UserRespVO.class,
        new MPJLambdaWrapperX<UserDO>()
                .selectAll(UserDO.class)
                .leftJoin(DeptDO.class, DeptDO::getId, UserDO::getDeptId)
                .selectAs(DeptDO::getName, UserRespVO::getDeptName));
```

字段自动填充由 `DefaultDBFieldHandler` 完成，DO 继承 `BaseDO` 即可获得 `createTime/updateTime/creator/updater/deleted`。

## 7. 依赖关系

直接依赖：

- `co.yixiang.boot:yshop-common`。
- `co.yixiang.boot:yshop-spring-boot-starter-web`（`provided`，仅 `OncePerRequestFilter`/`WebFrameworkUtils` 使用）。
- JDBC 驱动：`mysql-connector-j`；`ojdbc8`、`postgresql`、`mssql-jdbc`、`DmJdbcDriver18` 均为 `optional`。
- `com.alibaba:druid-spring-boot-3-starter`（BOM 版本 1.2.21）。
- `com.baomidou:mybatis-plus-spring-boot3-starter`（BOM 版本 3.5.5）。
- `com.baomidou:dynamic-datasource-spring-boot3-starter`（BOM 版本 4.3.0，排除 undertow）。
- `com.github.yulichang:mybatis-plus-join-boot-starter`（BOM 版本 1.4.10）。
- `com.fhs-opensource:easy-trans-spring-boot-starter`、`easy-trans-mybatis-plus-extend`（BOM 版本 2.2.11）。

## 8. 注意事项

1. 主键策略依赖 `spring.datasource.dynamic.primary` 与对应 URL；如果项目不使用 dynamic-datasource 的这套配置结构，`IdTypeEnvironmentPostProcessor` 不会生效，需要显式配置 `id-type`。
2. `@MapperScan` 只扫描带 `@Mapper` 注解的接口，新增 Mapper 时不要遗漏。
3. `BaseDO` 只包含通用审计字段；租户字段 `tenantId` 在 `yshop-spring-boot-starter-biz-tenant` 的 `TenantBaseDO` 中，而本仓库多租户当前未启用。
4. 使用 `@TableField(typeHandler = ...)` 的 DO 需要配合 `@TableName(autoResultMap = true)`，否则查询结果可能无法正确反序列化。
5. `EncryptTypeHandler` 的密钥 `mybatis-plus.encryptor.password` 在本仓库 `application.yaml` 中已配置；更换密钥会导致历史密文无法解密。
6. `JsonLongSetTypeHandler` 用于规避 Jackson 将 JSON 数字反序列化为 Integer 的问题，适合 ID 集合字段。
7. 多数据源能力已引入并配置 master/slave，但当前 `src/main/java` 中未发现 `@DS`、`@Master`、`@Slave` 的实际使用，属于“已具备但未使用”的能力。
8. `QueryWrapperX.limitN(n)` 依赖 `SqlConstants.DB_TYPE`，在未初始化数据库类型时会断言失败；跨多种数据库使用时需注意其 SQL 方言差异。
9. `insertBatch` 在 SQL Server 下会退化为逐条 `insert`，避免批量插入后获取 ID 报错。
10. `YshopTranslateAutoConfiguration` 依赖 Easy-Trans；若关闭 Easy-Trans 或移除相关依赖，需要同时排除该自动配置。
