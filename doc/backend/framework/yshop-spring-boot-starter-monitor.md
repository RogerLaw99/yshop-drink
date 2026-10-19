# yshop-spring-boot-starter-monitor

> 所属：yshop-framework；文档依据本仓库源码整理。

## 1. 模块定位

本模块是**服务监控与链路追踪**的聚合 starter，包含三部分：

1. **链路追踪**：基于 SkyWalking / OpenTracing 依赖，提供响应头 `trace-id` 输出、`@BizTrace` 业务标签注解及切面实现（切面当前未装配，见下文）。
2. **指标采集**：基于 Micrometer + Prometheus，为所有指标添加 `application=<spring.application.name>` 公共标签，并通过 Actuator 暴露指标端点。
3. **Spring Boot Admin 客户端**：引入 `spring-boot-admin-starter-client`，可将应用注册到 Admin Server（Server 端在 `infra` 模块中）。

### 在 yshop-server 中的依赖与启用状态

- `yshop-server/pom.xml` **没有直接声明**本模块。
- 通过 `yshop-module-infra-biz` 传递引入（`infra-biz/pom.xml` 声明了 `yshop-spring-boot-starter-monitor`）。
- 自动配置类通过 `AutoConfiguration.imports` 注册，默认生效：
  - `YshopTracerAutoConfiguration` 注册 `TraceFilter`；
  - `YshopMetricsAutoConfiguration` 注册 `MeterRegistryCustomizer`。
- **Spring Boot Admin 在 `local` 环境被关闭**：`application-local.yaml` 的 `spring.autoconfigure.exclude` 排除了：
  - `de.codecentric.boot.admin.server.config.AdminServerAutoConfiguration`
  - `de.codecentric.boot.admin.server.ui.config.AdminServerUiAutoConfiguration`
  - `de.codecentric.boot.admin.client.config.SpringBootAdminClientAutoConfiguration`
- `dev` 环境未排除，且存在 `spring.boot.admin.client.url` 与 `spring.boot.admin.context-path` 配置；`infra-biz` 还引入了 `spring-boot-admin-starter-server`，可在 `/admin` 提供 Admin Server。
- Actuator 端点已开放：`management.endpoints.web.exposure.include: '*'`，Prometheus 指标可从 `/actuator/prometheus` 获取。
- 当前没有业务代码使用 `@BizTrace`；且自动配置中 `BizTraceAspect` 的 Bean 注册代码被注释，因此该注解**目前不会产生 Span**。

## 2. 目录与包结构

```text
yshop-spring-boot-starter-monitor/
├── pom.xml
└── src/main/
    ├── java/co/yixiang/yshop/framework/tracer/
    │   ├── config/
    │   │   ├── TracerProperties.java
    │   │   ├── YshopTracerAutoConfiguration.java
    │   │   └── YshopMetricsAutoConfiguration.java
    │   └── core/
    │       ├── annotation/BizTrace.java
    │       ├── aop/BizTraceAspect.java
    │       ├── filter/TraceFilter.java
    │       └── util/TracerFrameworkUtils.java
    └── resources/META-INF/spring/
        └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

## 3. 核心类与职责

| 类/接口 | 所在包 | 职责 |
|---|---|---|
| `TracerProperties` | `...tracer.config` | 绑定 `yshop.tracer`，当前为空配置类 |
| `YshopTracerAutoConfiguration` | `...tracer.config` | 条件装配链路追踪；注册 `TraceFilter`；`BizTraceAspect`/`Tracer` 注册代码被注释 |
| `YshopMetricsAutoConfiguration` | `...tracer.config` | 为 `MeterRegistry` 添加 `application` 公共标签 |
| `BizTrace` | `...tracer.core.annotation` | 方法级注解：声明业务编号 `id`、业务类型 `type`、操作名 `operationName`，常量 `biz.id` / `biz.type` |
| `BizTraceAspect` | `...tracer.core.aop` | 为 `@BizTrace` 方法创建 Span 并写入业务标签（当前无 Bean，未生效） |
| `TraceFilter` | `...tracer.core.filter` | 每个请求向响应头写入 `trace-id` |
| `TracerFrameworkUtils` | `...tracer.core.util` | 将异常记录到 OpenTracing Span（`Tags.ERROR`、异常栈等） |

## 4. 自动配置与生效方式

`AutoConfiguration.imports`：

```text
co.yixiang.yshop.framework.tracer.config.YshopTracerAutoConfiguration
co.yixiang.yshop.framework.tracer.config.YshopMetricsAutoConfiguration
```

### 4.1 Tracer

```java
@AutoConfiguration
@ConditionalOnClass({BizTraceAspect.class})
@EnableConfigurationProperties(TracerProperties.class)
@ConditionalOnProperty(prefix = "yshop.tracer", value = "enable", matchIfMissing = true)
public class YshopTracerAutoConfiguration { ... }
```

- 条件默认满足，自动配置生效。
- 实际只注册一个 Bean：`FilterRegistrationBean<TraceFilter>`，order = `WebFilterOrderEnum.TRACE_FILTER`（`Integer.MIN_VALUE + 1`）。
- 源码中被注释的部分包括 `BizTraceAspect`、`SkywalkingTracer` 与 `GlobalTracer.register(...)`，注释说明原因是 opentracing 版本与阿里云短信 SDK 存在冲突。

### 4.2 Metrics

```java
@AutoConfiguration
@ConditionalOnClass({MeterRegistryCustomizer.class})
@ConditionalOnProperty(prefix = "yshop.metrics", value = "enable", matchIfMissing = true)
public class YshopMetricsAutoConfiguration { ... }
```

- 注册 `MeterRegistryCustomizer<MeterRegistry>`，执行 `registry.config().commonTags("application", applicationName)`。
- `applicationName` 来自 `@Value("${spring.application.name}")`，本仓库为 `yshop-server`。

## 5. 关键配置项

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `yshop.tracer.enable` | `true`（`matchIfMissing = true`） | 是否启用链路追踪自动配置（当前仅影响 `TraceFilter`） |
| `yshop.tracer` | 无 | `TracerProperties` 的前缀，当前类中没有字段 |
| `yshop.metrics.enable` | `true`（`matchIfMissing = true`） | 是否启用 Micrometer 公共标签配置 |
| `spring.application.name` | `yshop-server` | 指标 `application` 标签取值，同时用于 Admin 实例名 |
| `management.endpoints.web.base-path` | `/actuator` | Actuator 基础路径（仓库显式配置） |
| `management.endpoints.web.exposure.include` | `*`（仓库配置） | 开放的端点集合 |
| `spring.boot.admin.client.url` | `http://127.0.0.1:${server.port}/${spring.boot.admin.context-path}` | Admin Server 注册地址（仓库配置） |
| `spring.boot.admin.client.instance.service-host-type` | `IP` | 注册实例使用 IP |
| `spring.boot.admin.context-path` | `/admin` | Admin Server 上下文路径 |

Prometheus 抓取路径由 `micrometer-registry-prometheus` + Actuator 提供，本仓库对应 `/actuator/prometheus`。

## 6. 使用方式 / 典型用法

### 6.1 获取链路编号

客户端调用任意接口后，响应头会包含：

```text
trace-id: <SkyWalking TraceId>
```

实现位于 `TraceFilter`：

```java
response.addHeader("trace-id", TracerUtils.getTraceId());
```

`TracerUtils.getTraceId()` 直接返回 `org.apache.skywalking.apm.toolkit.trace.TraceContext.traceId()`；未挂载 SkyWalking Agent 时通常为空字符串。

### 6.2 业务链路标签（设计用法）

```java
@BizTrace(type = "#order.type", id = "#order.id", operationName = "创建订单")
public void createOrder(Order order) { /* ... */ }
```

设计上由 `BizTraceAspect` 解析 SpEL 并写入 `biz.type` / `biz.id` 标签。要使标签在 SkyWalking UI 中可检索，还需要在 OAP Server 配置 `SW_SEARCHABLE_TAG_KEYS` 中增加这两个 key。**注意：本仓库未注册该切面 Bean，当前不会生效。**

### 6.3 指标与 Admin

- Prometheus 配置抓取 `http://<host>:48081/actuator/prometheus`。
- Admin Server 地址为 `http://<host>:48081/admin`（`local` 环境对应自动配置被排除）。

## 7. 依赖关系

模块自身依赖：

- `yshop-common`
- `spring-boot-starter-aop`
- `spring-web`、`jakarta.servlet-api`（均 `provided`，仅 `TraceFilter` 使用）
- `io.opentracing:opentracing-util`
- `org.apache.skywalking:apm-toolkit-trace`
- `org.apache.skywalking:apm-toolkit-logback-1.x`
- `org.apache.skywalking:apm-toolkit-opentracing`
- `io.micrometer:micrometer-registry-prometheus`
- `de.codecentric:spring-boot-admin-starter-client`

上游使用者：

- `yshop-module-infra-biz` 直接依赖本模块，并额外引入 `spring-boot-admin-starter-server`；
- `yshop-server` 通过 `infra-biz` 传递获得。

## 8. 注意事项

1. **`@BizTrace` 当前是“死代码”**：`BizTraceAspect` 与 `Tracer` 的 Bean 注册在 `YshopTracerAutoConfiguration` 中被整体注释，业务方法加注解不会创建 Span。
2. `TracerProperties` 是空类，除条件开关 `yshop.tracer.enable` 外没有任何可配置字段。
3. `TraceFilter` 只负责响应头输出，不生成 traceId；真正的 traceId 来自 SkyWalking Agent。无 Agent 时 header 可能为空。
4. `local` 环境排除了 Spring Boot Admin 的 Server、Server UI 与 Client 自动配置；若要在本地查看 Admin，需要移除这些 exclude。
5. `dev` 环境中 Admin Client 指向 `http://127.0.0.1:${server.port}/admin`，与同机的 Admin Server 对应；多实例部署时应改为独立 Admin Server 地址。
6. `management.endpoints.web.exposure.include: '*'` 会暴露所有 Actuator 端点，生产环境应结合安全白名单收敛。
7. 源码注释提示 opentracing 与阿里云短信 SDK 存在版本冲突，这是 `BizTraceAspect` 未启用的现实原因；启用前需先解决依赖冲突。
8. `spring-boot-admin-starter-server` 不在本模块，而在 `infra-biz`；只引入 monitor 模块不会得到 Admin Server。
