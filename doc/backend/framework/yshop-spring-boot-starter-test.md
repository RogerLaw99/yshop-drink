# yshop-spring-boot-starter-test

> 所属：yshop-framework；文档依据本仓库源码整理。

## 1. 模块定位

`yshop-spring-boot-starter-test` 是测试支撑模块，Maven 坐标 `co.yixiang.boot:yshop-spring-boot-starter-test`。`pom.xml` 描述为“测试组件，用于单元测试、集成测试”。

它不参与生产运行，提供：

- 纯 Mockito 单元测试基类。
- 基于 H2 内存数据库的单元测试基类。
- 基于内嵌 Redis（jedis-mock）的单元测试基类。
- H2 + 内嵌 Redis 的组合基类。
- 测试用 SQL 初始化配置、内嵌 Redis 启动配置。
- 断言工具 `AssertUtils`、随机数据生成工具 `RandomUtils`（基于 Podam）。

注意：本模块**不是**通过 Spring Boot 自动配置生效的 Starter；其配置类由各测试基类通过 `@Import` 显式引入。

## 2. 目录与包结构

根包：`co.yixiang.yshop.framework.test`

```text
co.yixiang.yshop.framework.test
├── config
│   ├── RedisTestConfiguration
│   └── SqlInitializationTestConfiguration
└── core
    ├── ut
    │   ├── BaseDbAndRedisUnitTest
    │   ├── BaseDbUnitTest
    │   ├── BaseMockitoUnitTest
    │   └── BaseRedisUnitTest
    └── util
        ├── AssertUtils
        └── RandomUtils
```

`src/main/resources` 下没有 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`，也没有 `spring.factories`。SQL 脚本与 `application-unit-test.yaml` 由**使用该模块的业务模块**在自己的 `src/test/resources` 中提供。

## 3. 核心类与职责

| 类/接口 | 所在包 | 职责 |
|---|---|---|
| `BaseMockitoUnitTest` | `framework.test.core.ut` | 纯 Mockito 单元测试基类；`@ExtendWith(MockitoExtension.class)`。 |
| `BaseDbUnitTest` | `framework.test.core.ut` | 基于 H2 内存数据库的单元测试基类；`@SpringBootTest(webEnvironment = NONE)`；`@ActiveProfiles("unit-test")`；每个测试方法结束后执行 `/sql/clean.sql`。 |
| `BaseRedisUnitTest` | `framework.test.core.ut` | 基于内嵌 Redis 的单元测试基类；引入 Redis 测试配置、Spring Redis 自动配置、业务 Redis 配置与 Redisson 自动配置。 |
| `BaseDbAndRedisUnitTest` | `framework.test.core.ut` | H2 + 内嵌 Redis 组合基类；在 `BaseDbUnitTest` 的数据库配置基础上追加 Redis 配置；同样在每个测试后执行 `/sql/clean.sql`。 |
| `SqlInitializationTestConfiguration` | `framework.test.config` | 复制 Spring Boot `DataSourceInitializationConfiguration` 的逻辑，解决单元测试开启 `spring.main.lazy-initialization=true` 时 SQL 初始化不执行的问题；读取 `SqlInitializationProperties` 并注册 `DataSourceScriptDatabaseInitializer`。 |
| `RedisTestConfiguration` | `framework.test.config` | 使用 `com.github.fppt.jedismock.RedisServer` 在 `spring.data.redis.port` 指定的端口启动内嵌 Redis；启动异常被忽略，以兼容多个测试容器导致的端口占用。 |
| `AssertUtils` | `framework.test.core.util` | `assertPojoEquals` / `isPojoEquals`：按字段反射比较两个对象，可忽略指定字段；`assertServiceException`：断言抛出的 `ServiceException` 错误码与格式化后的消息。 |
| `RandomUtils` | `framework.test.core.util` | 基于 Podam 的随机数据生成：`randomString`、`randomLongId`、`randomInteger`、`randomDate`、`randomLocalDateTime`、`randomShort`、`randomSet`、`randomCommonStatus`、`randomEmail`、`randomURL`、`randomPojo`、`randomPojoList`。 |

### 测试基类引入的配置

`BaseDbUnitTest.Application` 导入：

```text
YshopDataSourceAutoConfiguration
DataSourceAutoConfiguration
DataSourceTransactionManagerAutoConfiguration
DruidDataSourceAutoConfigure
SqlInitializationTestConfiguration
YshopMybatisAutoConfiguration
MybatisPlusAutoConfiguration
MybatisPlusJoinAutoConfiguration
```

`BaseRedisUnitTest.Application` 导入：

```text
RedisTestConfiguration
RedisAutoConfiguration
YshopRedisAutoConfiguration
RedissonAutoConfiguration
```

`BaseDbAndRedisUnitTest.Application` 导入：

```text
YshopDataSourceAutoConfiguration
DataSourceAutoConfiguration
DataSourceTransactionManagerAutoConfiguration
DruidDataSourceAutoConfigure
SqlInitializationTestConfiguration
YshopMybatisAutoConfiguration
MybatisPlusAutoConfiguration
RedisTestConfiguration
YshopRedisAutoConfiguration
RedisAutoConfiguration
RedissonAutoConfiguration
```

其中 `BaseDbAndRedisUnitTest` **没有**导入 `MybatisPlusJoinAutoConfiguration`，而 `BaseDbUnitTest` 导入了；编写联表查询测试时需留意这一差异。

## 4. 自动配置与生效方式

本模块没有自动配置注册文件，也不应被生产代码依赖。生效方式为：

- 测试类继承上述基类，基类上的 `@SpringBootTest`、`@ActiveProfiles("unit-test")`、`@Sql` 生效。
- 基类内部类 `Application` 通过 `@Import` 显式装配所需的自动配置类。
- 使用方需要在 `src/test/resources` 提供：
  - `application-unit-test.yaml`；
  - `/sql/create_tables.sql`（建表）；
  - `/sql/clean.sql`（测试后清理）。

本仓库中 `yshop-module-system-biz` 与 `yshop-module-member-biz` 提供了上述测试资源；业务模块的 `pom.xml` 通常以 `test` scope 引入本模块，例如：

```xml
<dependency>
    <groupId>co.yixiang.boot</groupId>
    <artifactId>yshop-spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

## 5. 关键配置项

本模块自身没有 `@ConfigurationProperties`，没有专属配置前缀。测试运行依赖使用方提供的 `unit-test` Profile 配置。本仓库实际使用的配置项如下（来自各业务模块的 `src/test/resources/application-unit-test.yaml`）：

| 配置项 | 实际值 | 说明 |
|---|---|---|
| `spring.main.lazy-initialization` | `true` | 开启懒加载，加快测试启动 |
| `spring.main.banner-mode` | `off` | 关闭 Banner |
| `spring.datasource.url` | `jdbc:h2:mem:testdb;MODE=MYSQL;DATABASE_TO_UPPER=false;NON_KEYWORDS=value;` | H2 内存库，MySQL 兼容模式 |
| `spring.datasource.driver-class-name` | `org.h2.Driver` | H2 驱动 |
| `spring.datasource.username` | `sa` | H2 用户名 |
| `spring.datasource.password` | 空 | H2 密码 |
| `spring.datasource.druid.async-init` | `true` | 异步初始化连接池 |
| `spring.datasource.druid.initial-size` | `1` | 初始连接数 |
| `spring.sql.init.schema-locations` | `classpath:/sql/create_tables.sql` | 建表脚本 |
| `spring.data.redis.host` | `127.0.0.1` | 内嵌 Redis 地址 |
| `spring.data.redis.port` | `16379` | 内嵌 Redis 端口 |
| `spring.data.redis.database` | `0` | Redis 数据库索引 |
| `mybatis.lazy-initialization` | `true` | Mapper 懒加载，加速测试 |
| `yshop.info.base-package` | `co.yixiang.yshop.module` | 测试场景下的 Mapper 扫描根包 |

## 6. 使用方式 / 典型用法

纯 Mockito 单元测试：

```java
public class UserServiceTest extends BaseMockitoUnitTest {
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;
}
```

H2 数据库单元测试（本仓库真实用法）：

```java
@Import(DictDataServiceImpl.class)
public class DictDataServiceImplTest extends BaseDbUnitTest {

    @Resource
    private DictDataServiceImpl dictDataService;

    @Resource
    private DictDataMapper dictDataMapper;

    @MockBean
    private DictTypeService dictTypeService;

    @Test
    public void testGetDictDataList() {
        DictDataDO data = randomDictDataDO()
                .setDictType("yshop")
                .setStatus(CommonStatusEnum.ENABLE.getStatus());
        dictDataMapper.insert(data);

        List<DictDataDO> result = dictDataService.getDictDataList(
                CommonStatusEnum.ENABLE.getStatus(), "yshop");
        assertEquals(1, result.size());
    }
}
```

H2 + Redis 单元测试：

```java
public class CacheServiceTest extends BaseDbAndRedisUnitTest {
    // 可直接注入 Mapper、RedisTemplate 等
}
```

断言工具：

```java
assertPojoEquals(expected, actual, "id", "createTime");
assertServiceException(() -> userService.deleteUser(999L), USER_NOT_FOUND);
```

随机数据：

```java
UserDO user = randomPojo(UserDO.class, o -> o.setStatus(CommonStatusEnum.ENABLE.getStatus()));
List<UserDO> users = randomPojoList(UserDO.class);
```

`RandomUtils` 的 Podam 约定：

- `String` 生成长度 10 的随机字符串。
- 字段名以 `type`、`status`、`category`、`scope`、`result` 结尾时，生成 0~127 的 `Integer`。
- 字段名为 `status` 时，从 `CommonStatusEnum` 中随机取值。
- 字段名为 `deleted` 时固定为 `false`。
- `LocalDateTime` 使用 30 天内的随机日期，并将纳秒置零，避免 MySQL/H2 精度问题。

## 7. 依赖关系

直接依赖：

- `co.yixiang.boot:yshop-common`。
- `co.yixiang.boot:yshop-spring-boot-starter-mybatis`。
- `co.yixiang.boot:yshop-spring-boot-starter-redis`。
- `org.mockito:mockito-inline`（BOM 版本 5.2.0）。
- `org.springframework.boot:spring-boot-starter-test`。
- `com.h2database:h2`（测试用内存数据库）。
- `com.github.fppt:jedis-mock`（BOM 版本 1.0.13，内嵌 Redis）。
- `uk.co.jemos.podam:podam`（BOM 版本 8.0.1.RELEASE，随机生成 POJO）。

## 8. 注意事项

1. 本模块没有自动配置注册文件，不能被“引入即生效”；必须继承测试基类或手动 `@Import` 配置类。
2. `BaseDbUnitTest` / `BaseDbAndRedisUnitTest` 依赖使用方提供 `/sql/clean.sql` 与 `application-unit-test.yaml`；缺失会导致测试启动或清理失败。
3. `BaseDbAndRedisUnitTest` 未导入 `MybatisPlusJoinAutoConfiguration`，而 `BaseDbUnitTest` 导入了；涉及 MPJ 联表查询的测试应根据需要选择基类或自行补充 `@Import`。
4. `RedisTestConfiguration` 使用 `spring.data.redis.port`（本仓库为 16379）启动 jedis-mock；启动异常被静默忽略，多个测试容器并行时可能出现端口占用但不会立即报错。
5. `SqlInitializationTestConfiguration` 的存在是为了兼容 `spring.main.lazy-initialization=true`；如果使用 Spring Boot 原生 SQL 初始化，在懒加载模式下可能不执行。
6. `BaseMockitoUnitTest` 只启用 Mockito，不启动 Spring 容器，适合 Service 纯逻辑测试。
7. `AssertUtils.assertPojoEquals` 只遍历 `expected` 的字段；`actual` 存在但 `expected` 没有的字段不会被比较，`expected` 有而 `actual` 没有的字段会被忽略。
8. `RandomUtils.randomPojo` 依赖 Podam 的默认构造与 setter；不可变对象或缺少无参构造的类可能无法生成。
9. 本模块应始终以 `test` scope 引入，避免测试依赖（H2、jedis-mock、Podam、Mockito）进入生产包。
