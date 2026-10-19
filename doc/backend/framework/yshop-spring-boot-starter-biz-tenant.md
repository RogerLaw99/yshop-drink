# yshop-spring-boot-starter-biz-tenant

> 所属：yshop-framework；文档依据本仓库源码整理。

## 1. 模块定位

本模块提供 SaaS **多租户**能力，覆盖以下层面：

- **DB**：基于 MyBatis-Plus `TenantLineInnerInterceptor` 自动为 SQL 追加 `tenant_id` 条件；
- **Web**：从请求头 `tenant-id` 解析租户编号放入上下文，并校验租户合法性与用户越权；
- **Redis Cache**：缓存 key 自动追加 `:租户编号`；
- **MQ**：Redis / Kafka / RabbitMQ / RocketMQ 消息发送与消费时透传租户编号；
- **Job**：`@TenantJob` 标记的定时任务按租户逐个执行；
- **AOP/工具**：`@TenantIgnore`、`TenantUtils.execute(...)` / `executeIgnore(...)` 控制租户上下文。

### 在 yshop-server 中的依赖与启用状态

- `yshop-server/pom.xml` **没有直接声明**本模块。
- 通过 `yshop-module-system-biz`、`yshop-module-infra-biz`、`yshop-module-member-biz`、`yshop-module-message-biz`、`yshop-module-pay-biz`、`yshop-module-mp-biz`、`yshop-module-score-biz`、`yshop-module-express-biz`、`yshop-module-store-biz` 等大量业务模块传递引入。
- **当前运行时被关闭**：`yshop-server/src/main/resources/application.yaml` 中配置了

  ```yaml
  yshop:
    tenant:
      enable: false
  ```

  `YshopTenantAutoConfiguration` 使用 `@ConditionalOnProperty(prefix = "yshop.tenant", value = "enable", matchIfMissing = true)`，因此该自动配置类整体不生效：不注册租户 SQL 拦截器、Web 过滤器、Security 过滤器、Redis CacheManager、MQ/Job 切面等。
- 注意“代码依赖仍然存在”：多个 DO 继续继承 `TenantBaseDO`，`TenantUtils.executeIgnore(...)`、`@TenantIgnore` 等调用仍能编译和执行；但由于 DB 拦截器未注册，这些调用不会产生实际的租户过滤效果。

## 2. 目录与包结构

```text
yshop-spring-boot-starter-biz-tenant/
├── pom.xml
└── src/main/
    ├── java/
    │   ├── co/yixiang/yshop/framework/tenant/
    │   │   ├── config/
    │   │   │   ├── TenantProperties.java
    │   │   │   └── YshopTenantAutoConfiguration.java
    │   │   └── core/
    │   │       ├── aop/            # @TenantIgnore + 切面
    │   │       ├── context/        # TenantContextHolder
    │   │       ├── db/             # TenantBaseDO、TenantDatabaseInterceptor
    │   │       ├── job/            # @TenantJob + 切面
    │   │       ├── mq/
    │   │       │   ├── kafka/      # ProducerInterceptor + EnvironmentPostProcessor
    │   │       │   ├── rabbitmq/   # 消息头后处理器 + 初始化器
    │   │       │   ├── redis/      # Redis 消息拦截器
    │   │       │   └── rocketmq/   # 发送/消费 Hook + 初始化器
    │   │       ├── redis/          # TenantRedisCacheManager
    │   │       ├── security/       # TenantSecurityWebFilter
    │   │       ├── service/        # TenantFrameworkService(Impl)
    │   │       ├── util/           # TenantUtils
    │   │       └── web/            # TenantContextWebFilter
    │   └── org/springframework/messaging/handler/invocation/
    │       └── InvocableHandlerMethod.java   # 对 Spring 类的定制副本
    └── resources/META-INF/
        ├── spring.factories
        └── spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

## 3. 核心类与职责

| 类/接口 | 所在包 | 职责 |
|---|---|---|
| `TenantProperties` | `...tenant.config` | 绑定 `yshop.tenant.*`：是否启用、忽略 URL、忽略表 |
| `YshopTenantAutoConfiguration` | `...tenant.config` | 统一装配 DB/Web/Security/MQ/Job/Redis 组件 |
| `TenantIgnore` | `...tenant.core.aop` | 方法级“忽略租户”注解（仅 DB 场景有意义） |
| `TenantIgnoreAspect` | `...tenant.core.aop` | 在方法执行期间将忽略标记设为 `true`，结束后恢复 |
| `TenantContextHolder` | `...tenant.core.context` | 基于 `TransmittableThreadLocal` 保存 `tenantId` 与 `ignore` |
| `TenantBaseDO` | `...tenant.core.db` | 继承 `BaseDO`，追加 `tenantId` 字段 |
| `TenantDatabaseInterceptor` | `...tenant.core.db` | MyBatis-Plus `TenantLineHandler`：提供租户值与忽略表判断 |
| `TenantJob` | `...tenant.core.job` | 标记需要按租户逐个执行的 Job |
| `TenantJobAspect` | `...tenant.core.job` | 获取全部租户，并行逐个执行并汇总异常 |
| `TenantKafkaEnvironmentPostProcessor` | `...tenant.core.mq.kafka` | 向 Kafka Producer 追加租户拦截器配置 |
| `TenantKafkaProducerInterceptor` | `...tenant.core.mq.kafka` | 发送时把 `tenant-id` 写入 Kafka Header |
| `TenantRabbitMQInitializer` | `...tenant.core.mq.rabbitmq` | 为所有 `RabbitTemplate` 添加消息后处理器 |
| `TenantRabbitMQMessagePostProcessor` | `...tenant.core.mq.rabbitmq` | 发送时把 `tenant-id` 写入 RabbitMQ Header |
| `TenantRedisMessageInterceptor` | `...tenant.core.mq.redis` | Redis 消息发送/消费前后透传与清理租户上下文 |
| `TenantRocketMQInitializer` | `...tenant.core.mq.rocketmq` | 为 RocketMQ Producer/Consumer 注册 Hook |
| `TenantRocketMQSendMessageHook` / `TenantRocketMQConsumeMessageHook` | `...tenant.core.mq.rocketmq` | RocketMQ 消息属性透传租户编号 |
| `TenantRedisCacheManager` | `...tenant.core.redis` | `getCache(name)` 时拼接 `name:tenantId` |
| `TenantSecurityWebFilter` | `...tenant.core.security` | 校验登录用户租户、租户编号是否缺失、租户是否合法 |
| `TenantFrameworkService` / `TenantFrameworkServiceImpl` | `...tenant.core.service` | 获取全部租户、校验租户；带 1 分钟 Guava 缓存 |
| `TenantUtils` | `...tenant.core.util` | 指定租户执行、忽略租户执行、添加租户 Header |
| `TenantContextWebFilter` | `...tenant.core.web` | 从请求头读取 `tenant-id`，请求结束后清理上下文 |
| `InvocableHandlerMethod` | `org.springframework.messaging...` | Spring 类的定制副本：消费消息前从 Header 取租户并执行 |

## 4. 自动配置与生效方式

`AutoConfiguration.imports`：

```text
co.yixiang.yshop.framework.tenant.config.YshopTenantAutoConfiguration
```

`spring.factories`：

```properties
org.springframework.boot.env.EnvironmentPostProcessor=\
  co.yixiang.yshop.framework.tenant.core.mq.kafka.TenantKafkaEnvironmentPostProcessor
```

`YshopTenantAutoConfiguration` 的装配内容：

1. 类级条件：`@ConditionalOnProperty(prefix = "yshop.tenant", value = "enable", matchIfMissing = true)`；`@EnableConfigurationProperties(TenantProperties.class)`。
2. `TenantFrameworkService`：注入 `TenantApi`。
3. AOP：`TenantIgnoreAspect`。
4. DB：`TenantLineInnerInterceptor` + `TenantDatabaseInterceptor`，通过 `MyBatisUtils.addInterceptor(interceptor, inner, 0)` 插入首位。
5. Web：
   - `TenantContextWebFilter`，order = `WebFilterOrderEnum.TENANT_CONTEXT_FILTER`（`-104`）；
   - `TenantSecurityWebFilter`，order = `WebFilterOrderEnum.TENANT_SECURITY_FILTER`（`-99`，位于 Spring Security 过滤器之后）。
6. MQ：
   - 始终注册 `TenantRedisMessageInterceptor`；
   - `TenantRabbitMQInitializer` 仅在 `RabbitTemplate` 类存在时注册；
   - `TenantRocketMQInitializer` 仅在 `RocketMQTemplate` 类存在时注册；
   - Kafka 通过 `EnvironmentPostProcessor` 追加 Producer 拦截器。
7. Job：`TenantJobAspect`。
8. Redis：`@Primary` 的 `RedisCacheManager`（实际类型 `TenantRedisCacheManager`）。

> 由于本仓库 `yshop.tenant.enable=false`，上述 Bean 均不会被创建。

## 5. 关键配置项

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `yshop.tenant.enable` | `true`（`TenantProperties.ENABLE_DEFAULT`） | 是否启用多租户；本仓库显式配置为 `false` |
| `yshop.tenant.ignore-urls` | 空集合 | 无需携带 `tenant-id` 的请求路径，支持 Ant 风格；未携带时自动设置“忽略租户” |
| `yshop.tenant.ignore-tables` | 空集合 | 不追加租户条件的表名；匹配时同时兼容大小写，`DUAL` 恒定忽略 |

相关但非本模块定义的配置：

| 配置项 | 说明 |
|---|---|
| 请求头 `tenant-id` | `WebFrameworkUtils.HEADER_TENANT_ID = "tenant-id"`，由 `TenantContextWebFilter` 解析 |
| `spring.application.name` | 本模块未直接使用；MQ 消费侧租户透传依赖各 MQ 组件的 Header 机制 |

本仓库 `application.yaml` 中 `yshop.tenant.ignore-tables` 已列出 `system_tenant`、`system_menu`、`infra_job`、`yshop_store_order_*` 等约 30 张表；`ignore-urls` 列出验证码、支付回调、公众号回调、积木报表等路径。

## 6. 使用方式 / 典型用法

### 6.1 DO 继承租户基类

```java
@TableName("system_dept")
public class DeptDO extends TenantBaseDO {
    // tenantId 由父类提供
}
```

### 6.2 指定租户执行 / 忽略租户执行

```java
// 以指定租户身份执行（会强制 ignore=false，结束后恢复原上下文）
TenantUtils.execute(tenantId, () -> tenantService.initTenantData(tenantId));

// 忽略租户过滤执行
TenantUtils.executeIgnore(() -> jobMapper.deleteExpiredLogs());

// 方法级忽略
@TenantIgnore
public void cleanAllTenantLogs() { /* ... */ }
```

### 6.3 多租户定时任务

```java
@TenantJob
public String execute(String param) {
    // 外层切面会为每个租户分别执行一次
    return "ok";
}
```

### 6.4 广播消息时按租户过滤

`WebSocketSessionManagerImpl.getSessionList(userType)` 会读取 `TenantContextHolder.getTenantId()` 并与 Session 所属租户比对，避免跨租户广播。

## 7. 依赖关系

模块自身依赖：

- `yshop-common`
- `yshop-spring-boot-starter-security`
- `yshop-spring-boot-starter-mybatis`
- `yshop-spring-boot-starter-redis`
- `yshop-spring-boot-starter-job`
- `yshop-spring-boot-starter-mq`（`optional=true`）
- `spring-kafka`、`spring-rabbit`、`rocketmq-spring-boot-starter`（均 `optional=true`）
- `guava`（`TenantFrameworkServiceImpl` 缓存）
- `yshop-spring-boot-starter-test`（test scope）

上游使用者：`yshop-module-system-biz`、`infra-biz`、`member-biz`、`message-biz`、`pay-biz`、`mp-biz`、`score-biz`、`express-biz`、`store-biz` 等；`yshop-spring-boot-starter-websocket` 以 `provided` 方式依赖它。`yshop-server` 通过业务模块传递引入。

## 8. 注意事项

1. **本仓库实际未启用**（`yshop.tenant.enable=false`）。排查“为什么没有自动加 `tenant_id`”时，首先检查该配置。
2. 启用后，默认**所有表**都会追加租户条件，未列入 `ignore-tables` 的表必须有 `tenant_id` 字段，否则 SQL 会报错。
3. 请求头名为 `tenant-id`。登录用户未携带时，`TenantSecurityWebFilter` 会回填登录用户自身的租户编号；显式携带但与管理用户租户不一致时按越权处理并返回 403。
4. 忽略 URL 未携带租户编号时，过滤器会设置 `TenantContextHolder.setIgnore(true)`；普通 URL 未携带则直接返回 400。
5. `TenantDatabaseInterceptor.getTenantId()` 调用 `getRequiredTenantId()`，上下文为空会抛 `NullPointerException`；异步线程需要借助 `TransmittableThreadLocal` 正确传递。
6. `TenantRedisCacheManager` 仅在 `ignore=false` 且租户编号非空时追加 `:<tenantId>`，同名缓存可能因租户不同而隔离。
7. `TenantJobAspect` 使用 `parallelStream` 并行执行各租户，并要求 Job 幂等；任一租户失败会汇总到返回 JSON，但成功租户在重试时会再次执行。
8. 模块内包含一个与 Spring 同包同名的 `org.springframework.messaging.handler.invocation.InvocableHandlerMethod` 定制副本。它依赖类路径加载顺序覆盖框架类，属于侵入式改造；升级 Spring 版本时需重点核对，避免行为差异或类冲突。
9. Kafka 租户拦截器通过 `spring.factories` 的 `EnvironmentPostProcessor` 注册；未引入 Kafka 时捕获 `NoClassDefFoundError` 跳过。
10. `TenantUtils.execute(...)` 会强制 `ignore=false`，即使外层处于忽略状态；执行结束后恢复原值。
