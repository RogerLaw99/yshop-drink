# yshop-common

> 所属：yshop-framework；文档依据本仓库源码整理。

## 1. 模块定位

`yshop-common` 是整个后端框架层的基础依赖模块，Maven 坐标为 `co.yixiang.boot:yshop-common`（版本由父工程 `${revision}` 统一管理，当前为 `3.0.0`）。它不是一个 Spring Boot Starter，不包含自动配置类，也不包含 `META-INF/spring` 注册文件；它只提供被其余 starter 和业务模块复用的基础 POJO、枚举、异常体系、校验注解与通用工具类。

模块 `pom.xml` 的描述为“定义基础 pojo 类、枚举、工具类等等”。

主要内容包括：

- 统一响应与分页模型：`CommonResult`、`PageParam`、`PageResult`、`SortablePageParam`、`SortingField`。
- 统一错误码与异常：`ErrorCode`、`ServiceException`、`ServerException`、`ServiceExceptionUtil`、`GlobalErrorCodeConstants`、`ServiceErrorCodeRange`。
- 通用枚举、常量与工具类。
- Bean Validation 扩展注解：`@InEnum`、`@Mobile`、`@Telephone`。
- Jackson 序列化扩展：`BigDecimalSerializer`、`DoubleSerializer`。

## 2. 目录与包结构

根包：`co.yixiang.yshop.framework.common`

```text
co.yixiang.yshop.framework.common
├── constant
│   ├── ShopConstants
│   └── SystemConfigConstants
├── core
│   ├── IntArrayValuable
│   └── KeyValue
├── enums
│   ├── CommonStatusEnum
│   ├── DateIntervalEnum
│   ├── DocumentEnum
│   ├── OrderInfoEnum
│   ├── OrderTypeEnum
│   ├── PayIdEnum
│   ├── ShopCommonEnum
│   ├── TerminalEnum
│   ├── UserTypeEnum
│   └── WebFilterOrderEnum
├── exception
│   ├── ErrorCode
│   ├── ServerException
│   ├── ServiceException
│   ├── enums/GlobalErrorCodeConstants
│   ├── enums/ServiceErrorCodeRange
│   └── util/ServiceExceptionUtil
├── params/QueryParam
├── pojo
│   ├── CommonResult
│   ├── PageParam
│   ├── PageResult
│   ├── SortablePageParam
│   └── SortingField
├── serializer
│   ├── BigDecimalSerializer
│   └── DoubleSerializer
├── util
│   ├── cache/CacheUtils
│   ├── collection/{ArrayUtils, CollectionUtils, MapUtils, SetUtils}
│   ├── date/{DateUtils, LocalDateTimeUtils}
│   ├── http/HttpUtils
│   ├── io/{FileUtils, IoUtils}
│   ├── json/JsonUtils
│   ├── monitor/TracerUtils
│   ├── number/{MoneyUtils, NumberUtils}
│   ├── object/{BeanUtils, ObjectUtils, PageUtils}
│   ├── servlet/ServletUtils
│   ├── spring/{SpringExpressionUtils, SpringUtils}
│   ├── string/StrUtils
│   ├── validation/ValidationUtils
│   └── yshop/LocationUtils
└── validation
    ├── InEnum / InEnumValidator / InEnumCollectionValidator
    ├── Mobile / MobileValidator
    └── Telephone / TelephoneValidator
```

`src/main` 下没有 `resources` 目录，也没有 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 或 `spring.factories`。

## 3. 核心类与职责

| 类/接口 | 所在包 | 职责 |
|---|---|---|
| `CommonResult<T>` | `common.pojo` | 统一返回体，字段为 `code`、`data`、`msg`；提供 `success(data)`、`error(...)`、`isSuccess()`、`isError()`、`checkError()`、`getCheckedData()` 等方法。成功码来自 `GlobalErrorCodeConstants.SUCCESS`（0）。 |
| `PageParam` | `common.pojo` | 分页入参，默认 `pageNo=1`、`pageSize=10`；`pageSize` 校验范围为 1~100；常量 `PAGE_SIZE_NONE=-1` 表示不分页。 |
| `PageResult<T>` | `common.pojo` | 分页返回，包含 `list` 与 `total`，提供 `empty()`、`empty(total)`。 |
| `SortablePageParam` | `common.pojo` | 继承 `PageParam`，增加 `sortingFields` 排序字段列表。 |
| `SortingField` | `common.pojo` | 排序字段 DTO，包含 `field`、`order`；常量 `ORDER_ASC="asc"`、`ORDER_DESC="desc"`。 |
| `QueryParam` | `common.params` | 旧式查询参数抽象类，包含 `page=1`、`limit=10`、`keyword`。 |
| `ErrorCode` | `common.exception` | 不可变错误码对象，包含 `code`、`msg`。 |
| `ServiceException` | `common.exception` | 业务异常，携带 `code` 与 `message`。 |
| `ServerException` | `common.exception` | 服务端异常，同样携带 `code` 与 `message`。 |
| `ServiceExceptionUtil` | `common.exception.util` | 创建 `ServiceException`，支持用 `{}` 占位符格式化错误消息；`invalidParamException(...)` 使用 400 错误码。 |
| `GlobalErrorCodeConstants` | `common.exception.enums` | 全局错误码：成功 0；400/401/403/404/405/423/429；500/501/502；900 重复请求、901 演示模式、999 未知错误。 |
| `ServiceErrorCodeRange` | `common.exception.enums` | 仅以注释形式约定各业务模块的错误码区间（如 infra、system、member、pay 等），不做实际校验。 |
| `IntArrayValuable` | `common.core` | 可输出 `int[]` 的枚举接口，供 `@InEnum` 校验使用。 |
| `KeyValue<K,V>` | `common.core` | 通用键值对。 |
| `WebFilterOrderEnum` | `common.enums` | 约定各 Web Filter 的顺序：`CORS_FILTER=Integer.MIN_VALUE`、`REQUEST_BODY_CACHE_FILTER=Integer.MIN_VALUE+500`、`API_ACCESS_LOG_FILTER=-103`、`XSS_FILTER=-102`、`DEMO_FILTER=Integer.MAX_VALUE` 等。 |
| `TerminalEnum` | `common.enums` | 终端：UNKNOWN(0)、WECHAT_MINI_PROGRAM(10)、WECHAT_WAP(11)、H5(20)、APP(31)。 |
| `UserTypeEnum` | `common.enums` | 用户类型：MEMBER(1，会员)、ADMIN(2，管理员)。 |
| `CommonStatusEnum` | `common.enums` | 通用状态：ENABLE(0)、DISABLE(1)，提供 `isEnable`、`isDisable`。 |
| `DateIntervalEnum` | `common.enums` | 时间间隔：DAY(1)、WEEK(2)、MONTH(3)、QUARTER(4)、YEAR(5)。 |
| `OrderInfoEnum`、`OrderTypeEnum`、`PayIdEnum`、`ShopCommonEnum`、`DocumentEnum` | `common.enums` | 商城业务相关枚举（订单状态/支付状态/退款状态/支付渠道、工作台类型、支付方式、通用开关值、文档地址）。 |
| `ShopConstants` | `common.constant` | 商城常量：订单超时时间、Redis key、微信支付/公众号/小程序服务名、短信长度与缓存时间等。 |
| `SystemConfigConstants` | `common.constant` | 系统配置项 key：地址、文件存储、积分、分销、包邮、微信/小程序/支付配置等。 |
| `JsonUtils` | `common.util.json` | 基于 Jackson 的 JSON 工具；静态 `ObjectMapper` 可通过 `init(ObjectMapper)` 注入 Spring 容器中的实例；提供序列化、解析对象/数组/树等方法。 |
| `CacheUtils` | `common.util.cache` | 基于 Guava 构建 `LoadingCache`；`buildAsyncReloadingCache` 异步刷新，`buildCache` 同步刷新。 |
| `DateUtils` | `common.util.date` | `Date` 与 `LocalDateTime` 互转、时间构造、过期判断、最大时间、今天/昨天判断。 |
| `LocalDateTimeUtils` | `common.util.date` | `LocalDateTime` 解析、加减、区间判断、月/季/年边界、时间范围拆分与格式化。 |
| `ServletUtils` | `common.util.servlet` | 写 JSON 响应、获取 UA/客户端 IP、判断 JSON 请求、读取 body、参数 Map、写附件。 |
| `HttpUtils` | `common.util.http` | 替换 URL 查询参数、拼接 URL、解析 Basic Authorization。 |
| `FileUtils` / `IoUtils` | `common.util.io` | 临时文件创建、按内容生成路径（SHA-256）；UTF-8 流读取。 |
| `TracerUtils` | `common.util.monitor` | 读取 SkyWalking `TraceContext.traceId()`。 |
| `MoneyUtils` | `common.util.number` | 金额百分比计算、分转元、金额乘法等（金额单位为分）。 |
| `NumberUtils` | `common.util.number` | 字符串转 Long/Integer、经纬度距离计算、可空 BigDecimal 乘法。 |
| `BeanUtils` | `common.util.object` | 基于 Hutool `BeanUtil` 的 Bean/List/PageResult 转换。 |
| `ObjectUtils` | `common.util.object` | 忽略 `id` 克隆对象、取最大值、取第一个非 null 值、`equalsAny`。 |
| `PageUtils` | `common.util.object` | 计算分页 offset、基于 Lambda 构建排序字段、设置默认排序。 |
| `SpringUtils` | `common.util.spring` | 继承 Hutool `SpringUtil`，提供 `isProd()`。 |
| `SpringExpressionUtils` | `common.util.spring` | 从 AOP `JoinPoint` 解析 SpEL 表达式。 |
| `StrUtils` | `common.util.string` | 字符串截断、前缀匹配、拆分 Long/Integer、按行移除、中文排序。 |
| `ValidationUtils` | `common.util.validation` | 手机号/URL/XML NCName 正则校验，以及编程式 Bean Validation。 |
| `LocationUtils` | `common.util.yshop` | 根据经纬度计算两点距离（千米，保留两位）。 |
| `ArrayUtils` / `CollectionUtils` / `MapUtils` / `SetUtils` | `common.util.collection` | 数组、集合、Map、Set 的转换、过滤、分组、去重、diff 等工具方法。 |
| `BigDecimalSerializer` / `DoubleSerializer` | `common.serializer` | 将 `BigDecimal` / `Double` 序列化为保留两位小数的字符串。 |
| `@InEnum` + 校验器 | `common.validation` | 校验整数或整数集合是否属于指定 `IntArrayValuable` 枚举的范围；空值默认通过。 |
| `@Mobile` + `MobileValidator` | `common.validation` | 校验中国大陆手机号；空值默认通过。 |
| `@Telephone` + `TelephoneValidator` | `common.validation` | 使用 Hutool `PhoneUtil.isTel/isPhone` 校验电话或手机号；空值默认通过。 |

## 4. 自动配置与生效方式

`yshop-common` **不是自动配置模块**：

- 没有 `@AutoConfiguration` / `@Configuration` 类。
- 没有 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`。
- 没有 `META-INF/spring.factories`。
- 没有 `@ConfigurationProperties` 配置类。

使用方式为普通 Maven 依赖引入后直接调用其中的类。部分工具类依赖 Spring 容器或外部 Bean，需要由其他 starter 完成初始化，例如：

- `JsonUtils.init(ObjectMapper)` 由 `yshop-spring-boot-starter-web` 的 `YshopJacksonAutoConfiguration` 调用。
- `SpringUtils` 依赖 Hutool `SpringUtil` 的 Spring 上下文注入能力。
- `TracerUtils` 依赖 SkyWalking `apm-toolkit-trace`。

## 5. 关键配置项

本模块自身没有 `@ConfigurationProperties`，因此没有模块专属配置项。代码中出现、且会被其他模块读取的固定常量包括：

| 常量 | 值 | 说明 |
|---|---|---|
| `GlobalErrorCodeConstants.SUCCESS` | `0` | 成功错误码 |
| `PageParam.PAGE_SIZE_NONE` | `-1` | `pageSize=-1` 表示不分页 |
| `WebFilterOrderEnum.CORS_FILTER` | `Integer.MIN_VALUE` | 跨域过滤器顺序 |
| `WebFilterOrderEnum.REQUEST_BODY_CACHE_FILTER` | `Integer.MIN_VALUE + 500` | 请求体缓存过滤器顺序 |
| `WebFilterOrderEnum.API_ACCESS_LOG_FILTER` | `-103` | 访问日志过滤器顺序 |
| `WebFilterOrderEnum.XSS_FILTER` | `-102` | XSS 过滤器顺序 |
| `WebFilterOrderEnum.DEMO_FILTER` | `Integer.MAX_VALUE` | 演示模式过滤器顺序 |
| `DateUtils.TIME_ZONE_DEFAULT` | `GMT+8` | 默认时区常量 |

## 6. 使用方式 / 典型用法

统一返回：

```java
return CommonResult.success(data);
return CommonResult.error(GlobalErrorCodeConstants.BAD_REQUEST);
return CommonResult.error(errorCode.getCode(), "自定义错误");
```

抛出业务异常（源码中的真实 API）：

```java
throw ServiceExceptionUtil.exception(ErrorCodeConstants.USER_NOT_FOUND);
throw ServiceExceptionUtil.exception(ErrorCodeConstants.USER_PASSWORD_ERROR, password);
throw ServiceExceptionUtil.invalidParamException("参数({}) 不正确", id);
```

参数校验：

```java
public class UserSaveReqVO {
    @Mobile
    private String mobile;

    @InEnum(UserTypeEnum.class)
    private Integer userType;

    @Telephone
    private String telephone;
}
```

分页：

```java
PageParam pageParam = new PageParam();
pageParam.setPageNo(1);
pageParam.setPageSize(10);
PageResult<UserDO> page = userMapper.selectPage(pageParam, wrapper);
```

## 7. 依赖关系

`pom.xml` 中的直接依赖：

- Spring：`spring-core`、`spring-expression`、`spring-aop`（均 `provided`）、`aspectjweaver`（`provided`）、`spring-web`（`provided`）、`jakarta.servlet-api`（`provided`）。
- 文档：`springdoc-openapi-starter-webmvc-api`（`provided`，`PageParam` 使用 `@Schema`）。
- 监控：`apm-toolkit-trace`（SkyWalking）。
- 工具：`lombok`、`mapstruct`、`mapstruct-jdk8`、`mapstruct-processor`、`guava`（`provided`）、`jackson-databind/core/jsr310`（`provided`）、`slf4j-api`（`provided`）、`jakarta.validation-api`（`provided`）、`hutool-all`、`transmittable-thread-local`、`easy-trans-anno`。
- 配置元数据：`spring-boot-configuration-processor`（`optional`）。
- 测试：`spring-boot-starter-test`（`test`）。

由于大量依赖为 `provided`，运行期需要由使用方（通常是 Web Starter 或其他业务模块）提供对应实现，否则相关工具类会在类加载或调用时失败。

## 8. 注意事项

1. 该模块不是 starter，引入它不会注册任何 Bean 或自动配置。
2. `provided` 依赖较多，单独把它作为普通库使用时，需自行补齐 Spring Web、Jackson、Servlet API、Guava、Validation 等运行期依赖。
3. `ServiceException` / `ServerException` 是 `final` 类，不能被继承。
4. `ServiceErrorCodeRange` 只是注释约定，没有运行时校验；新增业务模块时仍需人工避免错误码区间冲突。
5. `OrderInfoEnum`、`ShopCommonEnum` 等商城枚举与旧版业务逻辑耦合较深，部分枚举值命名重复前缀（如 `STATUS_*`、`PAY_STATUS_*`），使用时注意区分。
6. 项目实际使用 `CommonResult`、`PageParam`、`ServiceExceptionUtil`、`@Mobile`、`@InEnum` 等非常广泛；`QueryParam` 属于旧式分页参数，当前主流代码使用 `PageParam`。
