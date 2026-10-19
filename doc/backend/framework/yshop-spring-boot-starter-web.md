# yshop-spring-boot-starter-web

> 所属：yshop-framework；文档依据本仓库源码整理。

## 1. 模块定位

`yshop-spring-boot-starter-web` 是后端 Web 层的框架 Starter，Maven 坐标 `co.yixiang.boot:yshop-spring-boot-starter-web`。它基于 Spring Boot 3 + Spring MVC，统一封装：

- 管理端/用户端 API 的统一路径前缀（`/admin-api`、`/app-api`）。
- 全局异常处理与统一响应结果记录。
- API 访问日志、错误日志的采集入口。
- Jackson 的 Long / 时间类型序列化规则。
- Knife4j / Springdoc OpenAPI 文档。
- XSS 过滤。
- 参数脱敏注解与处理器。
- CORS、请求体缓存、演示模式等 Web Filter。
- 启动 Banner 提示。

模块 `pom.xml` 描述为“Web 框架，全局异常、API 日志、脱敏、错误码等”。

## 2. 目录与包结构

根包：`co.yixiang.yshop.framework`

```text
co.yixiang.yshop.framework
├── apilog
│   ├── config/YshopApiLogAutoConfiguration
│   └── core
│       ├── annotation/ApiAccessLog
│       ├── enums/OperateTypeEnum
│       ├── filter/ApiAccessLogFilter
│       ├── interceptor/ApiAccessLogInterceptor
│       └── service/{ApiAccessLogFrameworkService, ApiErrorLogFrameworkService, ...Impl}
├── banner
│   ├── config/YshopBannerAutoConfiguration
│   └── core/BannerApplicationRunner
├── desensitize/core
│   ├── base/{annotation/DesensitizeBy, handler/DesensitizationHandler, serializer/StringDesensitizeSerializer}
│   ├── regex/{annotation/EmailDesensitize, annotation/RegexDesensitize, handler/...}
│   └── slider/{annotation/..., handler/...}
├── jackson
│   ├── config/YshopJacksonAutoConfiguration
│   └── core/databind/{NumberSerializer, TimestampLocalDateTimeSerializer, TimestampLocalDateTimeDeserializer}
├── swagger/config
│   ├── SwaggerProperties
│   └── YshopSwaggerAutoConfiguration
├── web
│   ├── config/{WebProperties, YshopWebAutoConfiguration}
│   └── core
│       ├── filter/{ApiRequestFilter, CacheRequestBodyFilter, CacheRequestBodyWrapper, DemoFilter}
│       ├── handler/{GlobalExceptionHandler, GlobalResponseBodyHandler}
│       └── util/WebFrameworkUtils
└── xss
    ├── config/{XssProperties, YshopXssAutoConfiguration}
    └── core/{clean/JsoupXssCleaner, clean/XssCleaner, filter/XssFilter, filter/XssRequestWrapper, json/XssStringJsonDeserializer}
```

自动配置注册文件：`src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`。

## 3. 核心类与职责

| 类/接口 | 所在包 | 职责 |
|---|---|---|
| `YshopWebAutoConfiguration` | `framework.web.config` | 实现 `WebMvcConfigurer`；按 Controller 包名添加 `/admin-api`、`/app-api` 前缀；注册 `GlobalExceptionHandler`、`GlobalResponseBodyHandler`、`WebFrameworkUtils`、`CorsFilter`、`CacheRequestBodyFilter`、条件化的 `DemoFilter`、`RestTemplate`。 |
| `WebProperties` | `framework.web.config` | 配置前缀 `yshop.web`，包含 `appApi`、`adminApi`、`adminUi`；`Api` 内含 `prefix` 与 `controller` 匹配模式。 |
| `GlobalExceptionHandler` | `framework.web.core.handler` | `@RestControllerAdvice`；把参数缺失/类型错误/校验失败/404/405/`ServiceException`/`AccessDeniedException`/未知异常转换为 `CommonResult`；未知异常会写入 API 错误日志。 |
| `GlobalResponseBodyHandler` | `framework.web.core.handler` | `ResponseBodyAdvice`，仅拦截返回类型为 `CommonResult` 的方法，把结果放入 request attribute，供访问日志使用；不会改变 Controller 返回结构。 |
| `WebFrameworkUtils` | `framework.web.core.util` | 读取 `tenant-id`、`terminal` Header；读写当前登录用户 ID/类型；根据 URL 前缀推断用户类型；获取当前请求。 |
| `ApiRequestFilter` | `framework.web.core.filter` | 抽象过滤器，只对 `/admin-api`、`/app-api` 开头的请求生效。 |
| `CacheRequestBodyFilter` / `CacheRequestBodyWrapper` | `framework.web.core.filter` | 仅对 JSON 请求缓存 body，使请求体可重复读取。 |
| `DemoFilter` | `framework.web.core.filter` | 演示模式下拦截已登录用户的 POST/PUT/DELETE，直接返回 901 `DEMO_DENY`。 |
| `YshopApiLogAutoConfiguration` | `framework.apilog.config` | 在 `YshopWebAutoConfiguration` 之后初始化；注册访问日志/错误日志框架 Service，注册 `ApiAccessLogFilter`，并添加 `ApiAccessLogInterceptor`。 |
| `ApiAccessLog` | `framework.apilog.core.annotation` | 方法级访问日志注解，支持 `enable`、`requestEnable`、`responseEnable`、`sanitizeKeys`、`operateModule`、`operateName`、`operateType`。 |
| `OperateTypeEnum` | `framework.apilog.core.enums` | 操作类型：GET(1)、CREATE(2)、UPDATE(3)、DELETE(4)、EXPORT(5)、IMPORT(6)、OTHER(0)。 |
| `ApiAccessLogFilter` | `framework.apilog.core.filter` | 记录请求 URL、方法、用户、耗时、结果码、请求参数、可选响应体；默认脱敏 `password`、`token`、`accessToken`、`refreshToken`，通过 `ApiAccessLogFrameworkService` 异步落库。 |
| `ApiAccessLogInterceptor` | `framework.apilog.core.interceptor` | 非 `prod` 环境打印请求开始/结束与耗时日志；同时把 `HandlerMethod` 放入 request attribute。 |
| `ApiAccessLogFrameworkService(Impl)` / `ApiErrorLogFrameworkService(Impl)` | `framework.apilog.core.service` | 分别委托 `ApiAccessLogApi`、`ApiErrorLogApi` 写入日志，方法标注 `@Async`。 |
| `YshopJacksonAutoConfiguration` | `framework.jackson.config` | 向容器中的 `ObjectMapper` 注册：Long/Long.TYPE 序列化为字符串、LocalDate/LocalTime 默认格式、LocalDateTime 使用时间戳（Long）序列化/反序列化；并用第一个 `ObjectMapper` 初始化 `JsonUtils`。 |
| `YshopSwaggerAutoConfiguration` | `framework.swagger.config` | 条件：类路径存在 `OpenAPI`，且 `springdoc.api-docs.enabled` 为 `true`（缺省为 true）；创建 `OpenAPI`、安全方案（Authorization Header）、`all` 分组，并为每个接口追加 `tenant-id` 与 `Authorization` 参数。 |
| `SwaggerProperties` | `framework.swagger.config` | 配置前缀 `yshop.swagger`：`title`、`description`、`author`、`version`、`url`、`email`、`license`、`licenseUrl`，均为 `@NotEmpty`。 |
| `YshopXssAutoConfiguration` | `framework.xss.config` | 条件：`yshop.xss.enable=true`（缺省为 true）；注册 `JsoupXssCleaner`、Jackson 字符串反序列化过滤器、`XssFilter`。 |
| `XssProperties` | `framework.xss.config` | 配置前缀 `yshop.xss`：`enable` 默认 `true`，`excludeUrls` 默认为空列表。 |
| `XssFilter` / `XssRequestWrapper` | `framework.xss.core.filter` | 对 parameter、attribute、header、queryString 做 XSS 清理；按 `excludeUrls` 跳过。 |
| `XssStringJsonDeserializer` | `framework.xss.core.json` | 在 JSON 反序列化时清理字符串；同样支持 `excludeUrls`。 |
| `JsoupXssCleaner` / `XssCleaner` | `framework.xss.core.clean` | 基于 Jsoup `Safelist.relaxed()`，额外允许所有标签的 `style`/`class`、`a` 的 `target`、`img` 的 `data:` 协议。 |
| `YshopBannerAutoConfiguration` / `BannerApplicationRunner` | `framework.banner` | 启动成功后异步打印“项目启动成功”、接口文档与开发文档地址。 |
| `DesensitizeBy` 及 `@MobileDesensitize`、`@IdCardDesensitize`、`@PasswordDesensitize` 等 | `framework.desensitize.core` | 基于 Jackson 注解的字段脱敏；`@DesensitizeBy` 是元注解，指定 `DesensitizationHandler` 与 `StringDesensitizeSerializer`。该能力未注册自动配置类，通过字段注解直接生效。 |

## 4. 自动配置与生效方式

`AutoConfiguration.imports` 注册了 6 个自动配置类：

```text
co.yixiang.yshop.framework.apilog.config.YshopApiLogAutoConfiguration
co.yixiang.yshop.framework.jackson.config.YshopJacksonAutoConfiguration
co.yixiang.yshop.framework.swagger.config.YshopSwaggerAutoConfiguration
co.yixiang.yshop.framework.web.config.YshopWebAutoConfiguration
co.yixiang.yshop.framework.xss.config.YshopXssAutoConfiguration
co.yixiang.yshop.framework.banner.config.YshopBannerAutoConfiguration
```

生效要点：

- `YshopWebAutoConfiguration` 使用 `@EnableConfigurationProperties(WebProperties.class)`，并通过 `configurePathMatch` 为 `@RestController` 所在的 `**.controller.admin.**` / `**.controller.app.**` 包添加前缀。
- `YshopApiLogAutoConfiguration` 标注 `@AutoConfiguration(after = YshopWebAutoConfiguration.class)`；日志实现依赖业务模块提供的 `ApiAccessLogApi`、`ApiErrorLogApi` Bean。
- `YshopSwaggerAutoConfiguration` 依赖 `springdoc.api-docs.enabled`，可通过配置为 `false` 禁用。
- `YshopXssAutoConfiguration` 依赖 `yshop.xss.enable`，`matchIfMissing = true`。
- `YshopJacksonAutoConfiguration` 无额外条件，直接向容器内所有 `ObjectMapper` 注册模块。
- `YshopBannerAutoConfiguration` 无额外条件，启动时注册 `ApplicationRunner`。
- 脱敏组件不在 `AutoConfiguration.imports` 中，属于“注解 + Jackson 序列化器”式能力，不需要额外开关。
- `@Async` 异步写日志需要项目开启异步支持；本仓库由 `yshop-spring-boot-starter-job` 的 `YshopAsyncAutoConfiguration` 提供 `@EnableAsync`。

## 5. 关键配置项

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `yshop.web.app-api.prefix` | `/app-api` | 用户端 API 前缀 |
| `yshop.web.app-api.controller` | `**.controller.app.**` | 用户端 Controller 包匹配规则 |
| `yshop.web.admin-api.prefix` | `/admin-api` | 管理端 API 前缀 |
| `yshop.web.admin-api.controller` | `**.controller.admin.**` | 管理端 Controller 包匹配规则 |
| `yshop.web.admin-ui.url` | 无默认值（`@NotNull`） | 管理后台 UI 地址；本仓库 `application.yaml` 配置为 `http://dashboard.yshop.yixiang.co` |
| `yshop.xss.enable` | `true` | 是否启用 XSS 过滤 |
| `yshop.xss.exclude-urls` | 空列表 | 排除 XSS 过滤的 URL（支持路径匹配） |
| `yshop.access-log.enable` | `true`（`matchIfMissing=true`） | 是否启用 API 访问日志 Filter |
| `yshop.demo` | 未配置时为 `false` | 为 `true` 时启用演示模式 `DemoFilter` |
| `springdoc.api-docs.enabled` | `true`（`matchIfMissing=true`） | 是否启用 Springdoc/OpenAPI |
| `yshop.swagger.title` | 无默认值 | 文档标题，`@NotEmpty` |
| `yshop.swagger.description` | 无默认值 | 文档描述，`@NotEmpty` |
| `yshop.swagger.author` | 无默认值 | 文档作者，`@NotEmpty` |
| `yshop.swagger.version` | 无默认值 | 文档版本，`@NotEmpty` |
| `yshop.swagger.url` | 无默认值 | 作者/项目 URL，`@NotEmpty` |
| `yshop.swagger.email` | 无默认值 | 联系邮箱，`@NotEmpty` |
| `yshop.swagger.license` | 无默认值 | License 名称，`@NotEmpty` |
| `yshop.swagger.license-url` | 无默认值 | License URL，`@NotEmpty` |
| `spring.application.name` | 无默认值 | `GlobalExceptionHandler`、`ApiAccessLogFilter` 构造时使用 |

本仓库 `yshop-server/src/main/resources/application.yaml` 中实际配置了 `yshop.web.admin-ui.url`、`yshop.security.permit-all_urls`、`yshop.swagger.*`、`springdoc.*`、`knife4j.*`；`application-local.yaml` 中配置了 `yshop.xss.enable=false`、`yshop.access-log.enable=false`、`yshop.demo=false`。

## 6. 使用方式 / 典型用法

Controller 放在约定的包下，不需要在 `@RequestMapping` 中重复写 `/admin-api` 或 `/app-api`：

```java
@RestController
@RequestMapping("/system/user") // 实际暴露为 /admin-api/system/user
public class UserController {
    @GetMapping("/get")
    public CommonResult<UserRespVO> getUser(@RequestParam("id") Long id) {
        return CommonResult.success(userService.getUser(id));
    }
}
```

记录导出接口的访问日志：

```java
@GetMapping("/export")
@ApiAccessLog(operateType = OperateTypeEnum.EXPORT)
public void export(HttpServletResponse response) throws IOException {
    ExcelUtils.write(response, "用户数据.xls", "数据", UserRespVO.class, list);
}
```

获取当前请求信息：

```java
Long userId = WebFrameworkUtils.getLoginUserId();
Integer userType = WebFrameworkUtils.getLoginUserType();
Long tenantId = WebFrameworkUtils.getTenantId(request);
```

字段脱敏：

```java
public class UserAddressBaseVO {
    @MobileDesensitize
    private String mobile;
}
```

JSON 序列化约定（由本模块自动注册）：

- `Long` / `long` 序列化为字符串，避免前端 JS 精度丢失。
- `LocalDateTime` 默认序列化为 Long 时间戳。
- `LocalDate` / `LocalTime` 使用 Jackson JSR-310 默认格式。

## 7. 依赖关系

直接依赖：

- `co.yixiang.boot:yshop-common`。
- `org.springframework.boot:spring-boot-starter-web`。
- `spring-boot-configuration-processor`（`optional`，生成配置元数据）。
- `com.github.xiaoymin:knife4j-openapi3-jakarta-spring-boot-starter`（BOM 版本 4.3.0）。
- `org.springdoc:springdoc-openapi-starter-webmvc-api`（BOM 版本 2.2.0）。
- `org.springframework.security:spring-security-core`（`provided`，`GlobalExceptionHandler` 处理 `AccessDeniedException`）。
- `co.yixiang.boot:yshop-module-infra-api`（访问日志/错误日志 DTO 与 API）。
- `co.yixiang.boot:yshop-module-system-api`（`pom.xml` 声明，注释为“进行错误码的记录”；当前 `src/main/java` 中未发现直接 import）。
- `org.jsoup:jsoup`（BOM 版本 1.17.2）。
- 测试：`spring-boot-starter-test`、`mockito-inline`。

依赖方向：`yshop-common` → 本模块；本模块又是 `security`、`mybatis` 的上游依赖（`mybatis` 中以 `provided` 方式引用）。

## 8. 注意事项

1. Controller 的包名必须匹配 `**.controller.admin.**` 或 `**.controller.app.**`，否则不会自动添加 API 前缀。
2. `GlobalResponseBodyHandler` **不会**自动把任意返回值包装成 `CommonResult`，Controller 必须自行返回 `CommonResult`。
3. 访问日志默认开启，但本仓库 `application-local.yaml` 显式设置 `yshop.access-log.enable=false`；生产环境需按需开启。
4. `ApiAccessLogFrameworkServiceImpl` / `ApiErrorLogFrameworkServiceImpl` 使用 `@Async`，依赖项目中存在 `@EnableAsync`（本仓库由 job starter 提供）。
5. 访问日志依赖 infra 模块的 `ApiAccessLogApi` / `ApiErrorLogApi`；如果裁剪掉 infra 模块，需要相应排除或替换这些 Bean。
6. 本仓库 `local` Profile 默认关闭 XSS（`yshop.xss.enable=false`），与代码中的默认值 `true` 不同。
7. `yshop.demo=true` 时，已登录用户的 POST/PUT/DELETE 会被直接拒绝（错误码 901）。
8. Swagger 会为所有接口追加 `tenant-id` 与 `Authorization` 请求头参数，这是文档展示行为，不改变实际接口。
9. 脱敏组件没有自动配置类，且当前业务代码中使用较少（例如 `UserAddressBaseVO` 的 `@MobileDesensitize`）。
10. `yshop-module-system-api` 在 `pom.xml` 中被声明，但当前模块源码中未直接使用；如做依赖裁剪需确认是否仍有传递依赖需求。
