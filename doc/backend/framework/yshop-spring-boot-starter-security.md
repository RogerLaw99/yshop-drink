# yshop-spring-boot-starter-security

> 所属：yshop-framework；文档依据本仓库源码整理。

## 1. 模块定位

`yshop-spring-boot-starter-security` 是认证与授权框架 Starter，Maven 坐标 `co.yixiang.boot:yshop-spring-boot-starter-security`。它组合了两部分能力（与 `pom.xml` 描述一致）：

1. **security**：用户认证、Token 校验、权限校验，回答“谁可以做什么”。
2. **operatelog**：基于 `bizlog-sdk` 的操作日志，回答“谁在什么时间对什么做了什么”。

模块基于 Spring Security，但登录流程本身不使用 Spring Security 的表单登录/过滤器扩展；Token 校验委托给 system 模块的 `OAuth2TokenApi`。

## 2. 目录与包结构

根包：`co.yixiang.yshop.framework`

```text
co.yixiang.yshop.framework
├── operatelog
│   ├── config/YshopOperateLogConfiguration
│   └── core/service/LogRecordServiceImpl
└── security
    ├── config
    │   ├── AuthorizeRequestsCustomizer
    │   ├── SecurityProperties
    │   ├── YshopSecurityAutoConfiguration
    │   └── YshopWebSecurityConfigurerAdapter
    └── core
        ├── LoginUser
        ├── annotations/PreAuthenticated
        ├── aop/PreAuthenticatedAspect
        ├── context/TransmittableThreadLocalSecurityContextHolderStrategy
        ├── filter/TokenAuthenticationFilter
        ├── handler/{AccessDeniedHandlerImpl, AuthenticationEntryPointImpl}
        ├── service/{SecurityFrameworkService, SecurityFrameworkServiceImpl}
        └── util/SecurityFrameworkUtils
```

自动配置注册文件：`src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`。

## 3. 核心类与职责

| 类/接口 | 所在包 | 职责 |
|---|---|---|
| `YshopSecurityAutoConfiguration` | `framework.security.config` | 注册 `PreAuthenticatedAspect`、认证失败入口、403 处理器、`BCryptPasswordEncoder`、`TokenAuthenticationFilter`、名为 `ss` 的 `SecurityFrameworkService`，并把 Spring Security 上下文策略切换为 `TransmittableThreadLocalSecurityContextHolderStrategy`。 |
| `SecurityProperties` | `framework.security.config` | 配置前缀 `yshop.security`：Token Header/参数名、mock 开关与密钥、免登录 URL、密码加密强度。 |
| `YshopWebSecurityConfigurerAdapter` | `framework.security.config` | 构建 `SecurityFilterChain`：开启 CORS、关闭 CSRF、无状态 Session、关闭 frameOptions；配置免登录规则、项目自定义规则与兜底认证；把 Token Filter 加到 `UsernamePasswordAuthenticationFilter` 之前。`@EnableMethodSecurity(securedEnabled = true)` 开启方法级安全。 |
| `AuthorizeRequestsCustomizer` | `framework.security.config` | 供各业务模块自定义 URL 授权规则的抽象扩展点，提供 `buildAdminApi(url)`、`buildAppApi(url)`。 |
| `TokenAuthenticationFilter` | `framework.security.core.filter` | 从 Header 或参数读取 Token，调用 `OAuth2TokenApi.checkAccessToken(token)` 校验；校验通过后构建 `LoginUser` 并写入 Security 上下文；支持 mock 登录。 |
| `LoginUser` | `framework.security.core` | 登录用户信息：`id`、`userType`、`info`（昵称/部门 ID）、`tenantId`、`scopes`、`shopId`，以及不持久化的 `context` 临时缓存。 |
| `PreAuthenticated` / `PreAuthenticatedAspect` | `framework.security.core.annotations` / `aop` | `@PreAuthenticated` 表示“必须登录”；切面在未登录时抛出 `ServiceException(UNAUTHORIZED)`，区别于 `@PreAuthorize` 的“无权限”。 |
| `SecurityFrameworkService` / `SecurityFrameworkServiceImpl` | `framework.security.core.service` | Bean 名为 `ss`；提供 `hasPermission`、`hasAnyPermissions`、`hasRole`、`hasAnyRoles`、`hasScope`、`hasAnyScopes`。权限/角色通过 `PermissionApi` 查询，scope 直接读取 `LoginUser.scopes`。 |
| `SecurityFrameworkUtils` | `framework.security.core.util` | 获取/设置当前登录用户与用户 ID；从请求中提取 Token（Header 优先于参数，并去除 `Bearer ` 前缀）；提供昵称、部门 ID 读取。 |
| `AccessDeniedHandlerImpl` | `framework.security.core.handler` | 已登录但权限不足时返回 403 `CommonResult(FORBIDDEN)`。 |
| `AuthenticationEntryPointImpl` | `framework.security.core.handler` | 未登录访问受保护资源时返回 401 `CommonResult(UNAUTHORIZED)`。 |
| `TransmittableThreadLocalSecurityContextHolderStrategy` | `framework.security.core.context` | 基于 `TransmittableThreadLocal` 的 SecurityContext 策略，避免 `@Async` 等异步执行时上下文丢失。 |
| `YshopOperateLogConfiguration` | `framework.operatelog.config` | `@EnableLogRecord(tenant = "")`，注册 `@Primary` 的 `ILogRecordService`。 |
| `LogRecordServiceImpl` | `framework.operatelog.core.service` | 把 `bizlog-sdk` 的 `LogRecord` 补全用户、模块、请求信息后，调用 `OperateLogApi.createOperateLog(...)` 落库；查询方法不实现，直接抛出 `UnsupportedOperationException`。 |

## 4. 自动配置与生效方式

`AutoConfiguration.imports` 注册了 3 个类：

```text
co.yixiang.yshop.framework.security.config.YshopSecurityAutoConfiguration
co.yixiang.yshop.framework.security.config.YshopWebSecurityConfigurerAdapter
co.yixiang.yshop.framework.operatelog.config.YshopOperateLogConfiguration
```

生效要点：

- 两个 security 配置类都使用 `@AutoConfigureOrder(-1)`，目的是先于 Spring Security 自动配置。
- `YshopSecurityAutoConfiguration` 使用 `@EnableConfigurationProperties(SecurityProperties.class)`。
- `YshopWebSecurityConfigurerAdapter` 使用 `@EnableMethodSecurity(securedEnabled = true)`。
- 安全规则按以下顺序叠加：
  1. 静态资源 `/*.html`、`/*.css`、`/*.js` 允许匿名访问。
  2. 扫描所有标注 `jakarta.annotation.security.PermitAll` 的 Controller 方法，并按 HTTP Method 放行。
  3. `yshop.security.permit-all-urls` 列表放行。
  4. **整个 `/app-api/**` 前缀放行**。
  5. 各业务模块的 `AuthorizeRequestsCustomizer` 自定义规则。
  6. 其余请求 `anyRequest().authenticated()`。
- Token 过滤器被插入到 `UsernamePasswordAuthenticationFilter` 之前。
- `YshopOperateLogConfiguration` 通过 `@EnableLogRecord` 启用 `bizlog-sdk`，依赖 `OperateLogApi` Bean。

## 5. 关键配置项

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `yshop.security.token-header` | `Authorization` | Token 所在请求头 |
| `yshop.security.token-parameter` | `token` | Token 所在请求参数（用于 WebSocket 等无法设置 Header 的场景） |
| `yshop.security.mock-enable` | `false` | 是否启用 mock 登录；生产环境必须关闭 |
| `yshop.security.mock-secret` | `test` | mock Token 前缀 |
| `yshop.security.permit-all-urls` | 空列表 | 免登录 URL 列表 |
| `yshop.security.password-encoder-length` | `4` | `BCryptPasswordEncoder` 强度 |

说明：

- 本仓库 `application.yaml` 使用下划线写法 `yshop.security.permit-all_urls`，Spring Boot 宽松绑定会映射到 `permitAllUrls`。
- 本仓库 `application-local.yaml` 设置 `yshop.security.mock-enable: true`，因此本地可用 `test{userId}` 形式的 Token 模拟登录；生产环境应确保关闭。
- `@PermitAll` 的 URL 扫描依赖 `RequestMappingHandlerMapping`，因此只对已注册的 Controller 生效。

## 6. 使用方式 / 典型用法

管理端权限校验：

```java
@PreAuthorize("@ss.hasPermission('system:user:query')")
@GetMapping("/get")
public CommonResult<UserRespVO> getUser(@RequestParam("id") Long id) {
    return CommonResult.success(userService.getUser(id));
}
```

App 端“必须登录”：

```java
@PreAuthenticated
@GetMapping("/get")
public CommonResult<AppUserInfoRespVO> getUserInfo() {
    return CommonResult.success(memberUserService.getUserInfo());
}
```

获取当前登录用户：

```java
Long userId = SecurityFrameworkUtils.getLoginUserId();
LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
Integer userType = loginUser.getUserType();
Long shopId = loginUser.getShopId();
```

业务模块自定义放行规则：

```java
@Component
public class SecurityConfiguration extends AuthorizeRequestsCustomizer {
    @Override
    public void customize(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        registry.requestMatchers(buildAdminApi("/system/tenant/get-id-by-name")).permitAll();
    }
}
```

Token 传递：

```http
Authorization: Bearer <accessToken>
```

或（例如 WebSocket）：

```text
?token=<accessToken>
```

## 7. 依赖关系

直接依赖：

- `co.yixiang.boot:yshop-common`。
- `spring-boot-starter-aop`。
- `co.yixiang.boot:yshop-spring-boot-starter-web`。
- `spring-boot-configuration-processor`（`optional`）。
- `spring-boot-starter-security`。
- `com.google.guava:guava`。
- `io.github.mouzt:bizlog-sdk`（BOM 版本 3.0.6，排除其 `spring-boot-starter`）。
- `co.yixiang.boot:yshop-module-system-api`（`OAuth2TokenApi`、`PermissionApi`、`OperateLogApi`）。
- 传递依赖 `transmittable-thread-local`（来自 common）。

## 8. 注意事项

1. **`/app-api/**` 在安全过滤链中整体 `permitAll`**，因此 App 端接口不会被 Spring Security 拦截；需要登录的 App 接口必须显式加 `@PreAuthenticated`，否则匿名也可访问。
2. `@PreAuthorize("@ss.hasPermission(...)")` 依赖 system 模块的 `PermissionApi`；裁剪 system 模块时需替换 `ss` Bean。
3. Token 的合法性完全由 `OAuth2TokenApi.checkAccessToken` 决定，框架层只负责提取、校验用户类型并写入上下文。
4. Token 中携带的 `userType` 会与 URL 前缀推断出的用户类型比较：`/admin-api` 期望 ADMIN，`/app-api` 期望 MEMBER；不匹配会抛出 `AccessDeniedException("错误的用户类型")`。
5. mock 登录只要 Token 以 `mockSecret` 开头，剩余部分就会被解析为用户 ID，不校验密码；`application-local.yaml` 已开启，线上务必设置 `yshop.security.mock-enable=false`。
6. 安全上下文使用 `TransmittableThreadLocal` 策略，有利于异步线程传递，但异步任务仍需按 TTL 规范使用线程池。
7. 操作日志 `LogRecordServiceImpl` 只实现写入，`queryLog*` 方法会抛 `UnsupportedOperationException`；查询应使用 `OperateLogApi`。
8. `SecurityProperties.mockSecret` 带有默认值 `test`，即使未配置也能通过校验注解；生产环境应显式配置或关闭 mock。
9. 密码使用 `BCryptPasswordEncoder`，强度默认 4（源码注释说明“越高开销越大”）。
