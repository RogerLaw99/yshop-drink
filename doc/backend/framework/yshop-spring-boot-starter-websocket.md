# yshop-spring-boot-starter-websocket

> 所属：yshop-framework；文档依据本仓库源码整理。

## 1. 模块定位

本模块提供基于 Spring WebSocket 的**JSON 消息框架**，主要能力：

- 统一的 WebSocket 接入点与握手拦截；
- 基于消息 `type` 分发到业务 `WebSocketMessageListener`；
- `WebSocketSessionManager` 管理 Session（按 SessionId、用户类型、用户编号索引）；
- `WebSocketMessageSender` 支持按用户、用户类型、Session 单发或群发；
- 支持多种跨节点广播方式：`local`、`redis`、`rocketmq`、`kafka`、`rabbitmq`；
- 登录用户与租户信息通过 Session attributes 传递，广播时可按租户隔离。

### 在 yshop-server 中的依赖与启用状态

- `yshop-server/pom.xml` **没有直接声明**本模块。
- 通过 `yshop-module-infra-biz` 传递引入（`infra-biz/pom.xml` 声明了 `yshop-spring-boot-starter-websocket`）。
- 自动配置通过 `AutoConfiguration.imports` 注册。
- **运行时启用**：`application.yaml` 中

  ```yaml
  yshop:
    websocket:
      enable: true
      path: /infra/ws
      sender-type: local
  ```

  因此 `YshopWebSocketAutoConfiguration` 生效，接入路径为 `/infra/ws`，使用**单机本地发送器**。
- `infra` 模块已有真实使用：
  - `DemoWebSocketMessageListener`：监听 `demo-message-send`，演示单发/群发；
  - `WebSocketSenderApiImpl`：把 `WebSocketMessageSender` 封装为跨模块 API。
- Kafka / RocketMQ / RabbitMQ 的发送器源码存在，但对应依赖在本模块 pom 中均为 `optional=true`，且业务模块未显式引入；当前 `sender-type=local`，不会触发这些实现。

## 2. 目录与包结构

```text
yshop-spring-boot-starter-websocket/
├── pom.xml
└── src/main/
    ├── java/co/yixiang/yshop/framework/websocket/
    │   ├── config/
    │   │   ├── WebSocketProperties.java
    │   │   └── YshopWebSocketAutoConfiguration.java
    │   └── core/
    │       ├── handler/JsonWebSocketMessageHandler.java
    │       ├── listener/WebSocketMessageListener.java
    │       ├── message/JsonWebSocketMessage.java
    │       ├── security/
    │       │   ├── LoginUserHandshakeInterceptor.java
    │       │   └── WebSocketAuthorizeRequestsCustomizer.java
    │       ├── sender/
    │       │   ├── AbstractWebSocketMessageSender.java
    │       │   ├── WebSocketMessageSender.java
    │       │   ├── local/LocalWebSocketMessageSender.java
    │       │   ├── redis/{RedisWebSocketMessage,RedisWebSocketMessageConsumer,RedisWebSocketMessageSender}.java
    │       │   ├── kafka/{KafkaWebSocketMessage,KafkaWebSocketMessageConsumer,KafkaWebSocketMessageSender}.java
    │       │   ├── rabbitmq/{RabbitMQWebSocketMessage,RabbitMQWebSocketMessageConsumer,RabbitMQWebSocketMessageSender}.java
    │       │   └── rocketmq/{RocketMQWebSocketMessage,RocketMQWebSocketMessageConsumer,RocketMQWebSocketMessageSender}.java
    │       ├── session/
    │       │   ├── WebSocketSessionHandlerDecorator.java
    │       │   ├── WebSocketSessionManager.java
    │       │   └── WebSocketSessionManagerImpl.java
    │       └── util/WebSocketFrameworkUtils.java
    └── resources/META-INF/spring/
        └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

## 3. 核心类与职责

| 类/接口 | 所在包 | 职责 |
|---|---|---|
| `WebSocketProperties` | `...websocket.config` | 绑定 `yshop.websocket.path`、`yshop.websocket.sender-type`，并做非空校验 |
| `YshopWebSocketAutoConfiguration` | `...websocket.config` | 开启 WebSocket、注册接入点、握手拦截器、Session 管理器及各发送器 |
| `JsonWebSocketMessage` | `...websocket.core.message` | 消息帧：`type` + `content`（content 为 JSON 字符串） |
| `WebSocketMessageListener<T>` | `...websocket.core.listener` | 业务消息监听接口：`onMessage(session, message)` + `getType()` |
| `JsonWebSocketMessageHandler` | `...websocket.core.handler` | 解析 `type` 分发；支持 `ping` → `pong`；按 Session 租户执行 |
| `LoginUserHandshakeInterceptor` | `...websocket.core.security` | 握手时把当前 `LoginUser` 写入 Session attributes |
| `WebSocketAuthorizeRequestsCustomizer` | `...websocket.core.security` | 设计用于放行 WebSocket 路径（当前未注册为 Bean） |
| `WebSocketSessionHandlerDecorator` | `...websocket.core.session` | 连接建立/关闭时维护 Session；用并发装饰器包装 Session |
| `WebSocketSessionManager` | `...websocket.core.session` | Session 注册、移除、按 ID/用户类型/用户编号查询 |
| `WebSocketSessionManagerImpl` | `...websocket.core.session` | `ConcurrentHashMap` + `CopyOnWriteArrayList` 实现；按租户过滤广播列表 |
| `WebSocketFrameworkUtils` | `...websocket.core.util` | 在 Session attributes 中读写 `LoginUser`、用户 ID、用户类型、租户 ID |
| `WebSocketMessageSender` | `...websocket.core.sender` | 发送接口：按用户、用户类型、Session 发送，含 `sendObject` 便捷方法 |
| `AbstractWebSocketMessageSender` | `...websocket.core.sender` | 查找目标 Session、序列化 JSON、逐 Session 发送 |
| `LocalWebSocketMessageSender` | `...websocket.core.sender.local` | 单机发送（仅本 JVM 内的 Session） |
| `RedisWebSocketMessageSender` / `Consumer` | `...websocket.core.sender.redis` | 通过 Redis Pub/Sub 广播到各节点 |
| `RocketMQWebSocketMessageSender` / `Consumer` | `...websocket.core.sender.rocketmq` | 通过 RocketMQ 广播模式广播 |
| `KafkaWebSocketMessageSender` / `Consumer` | `...websocket.core.sender.kafka` | 通过 Kafka 每实例独立消费组广播 |
| `RabbitMQWebSocketMessageSender` / `Consumer` | `...websocket.core.sender.rabbitmq` | 通过 RabbitMQ Topic Exchange + 临时队列广播 |

## 4. 自动配置与生效方式

`AutoConfiguration.imports`：

```text
co.yixiang.yshop.framework.websocket.config.YshopWebSocketAutoConfiguration
```

类级注解与关键装配：

```java
@AutoConfiguration(before = YshopRedisMQConsumerAutoConfiguration.class)
@EnableWebSocket
@ConditionalOnProperty(prefix = "yshop.websocket", value = "enable", matchIfMissing = true)
@EnableConfigurationProperties(WebSocketProperties.class)
public class YshopWebSocketAutoConfiguration { ... }
```

1. `webSocketConfigurer(...)`：把 `webSocketHandler` 注册到 `yshop.websocket.path`，添加所有 `HandshakeInterceptor`，并 `setAllowedOriginPatterns("*")` 允许跨域。
2. `handshakeInterceptor()`：注册 `LoginUserHandshakeInterceptor`。
3. `webSocketHandler(...)`：
   - `JsonWebSocketMessageHandler` 负责 JSON 消息分发；
   - 外层 `WebSocketSessionHandlerDecorator` 负责连接生命周期与并发包装。
4. `webSocketSessionManager()`：`WebSocketSessionManagerImpl`。
5. 发送器使用嵌套 `@Configuration` + `@ConditionalOnProperty(prefix="yshop.websocket", name="sender-type", havingValue=...)`：
   - `local`（`matchIfMissing = true`）
   - `redis`
   - `rocketmq`
   - `rabbitmq`
   - `kafka`

> 注意：五个嵌套配置都写了 `matchIfMissing = true`。如果完全没有配置 `yshop.websocket.sender-type`，则**五个配置都会匹配**。本仓库显式配置为 `local`，因此只装配本地发送器。

`before = YshopRedisMQConsumerAutoConfiguration.class` 是为了保证使用 Redis 发送器时，`RedisWebSocketMessageConsumer` 先于 Redis 监听容器创建。

## 5. 关键配置项

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `yshop.websocket.enable` | `true`（`matchIfMissing = true`） | 是否启用 WebSocket 自动配置；不是 `WebSocketProperties` 字段 |
| `yshop.websocket.path` | `/ws` | WebSocket 连接路径；本仓库配置为 `/infra/ws` |
| `yshop.websocket.sender-type` | `local` | 发送器类型：`local` / `redis` / `rocketmq` / `kafka` / `rabbitmq`；本仓库为 `local` |
| `yshop.websocket.sender-rocketmq.topic` | 无 | RocketMQ 广播 Topic（通过 `@Value` 读取） |
| `yshop.websocket.sender-rocketmq.consumer-group` | 无 | RocketMQ 消费组（通过 `@Value` 读取） |
| `yshop.websocket.sender-rabbitmq.exchange` | 无 | RabbitMQ Topic Exchange（通过 `@Value` 读取） |
| `yshop.websocket.sender-rabbitmq.queue` | 无 | RabbitMQ 队列名前缀（通过 `@Value` 读取，实际追加 UUID） |
| `yshop.websocket.sender-kafka.topic` | 无 | Kafka Topic（通过 `@Value` 读取） |
| `yshop.websocket.sender-kafka.consumer-group` | 无 | Kafka 消费组前缀（通过 `@Value` 读取，实际追加 UUID） |

说明：`WebSocketProperties` 只声明了 `path` 与 `senderType` 两个字段；`sender-*` 下的 broker 参数并非绑定字段，而是由各嵌套配置通过 `@Value` 直接读取。

本仓库 `application.yaml` 中的实际值：

```yaml
yshop:
  websocket:
    enable: true
    path: /infra/ws
    sender-type: local
    sender-rocketmq:
      topic: ${spring.application.name}-websocket
      consumer-group: ${spring.application.name}-websocket-consumer
    sender-rabbitmq:
      exchange: ${spring.application.name}-websocket-exchange
      queue: ${spring.application.name}-websocket-queue
    sender-kafka:
      topic: ${spring.application.name}-websocket
      consumer-group: ${spring.application.name}-websocket-consumer
```

## 6. 使用方式 / 典型用法

### 6.1 客户端连接

前端通过查询参数携带 token：

```text
ws://127.0.0.1:48081/infra/ws?token=<accessToken>
```

`TokenAuthenticationFilter` 完成认证后，`LoginUserHandshakeInterceptor` 从安全上下文取出 `LoginUser` 写入 Session attributes。

### 6.2 消息帧格式

```json
{
  "type": "demo-message-send",
  "content": "{\"toUserId\":1,\"text\":\"hello\"}"
}
```

- `type` 用于路由到 `WebSocketMessageListener.getType()`；
- `content` 是业务消息的 JSON 字符串，由监听器泛型类型反序列化。

### 6.3 编写监听器

```java
@Component
public class DemoWebSocketMessageListener implements WebSocketMessageListener<DemoSendMessage> {

    @Resource
    private WebSocketMessageSender webSocketMessageSender;

    @Override
    public void onMessage(WebSocketSession session, DemoSendMessage message) {
        Long fromUserId = WebSocketFrameworkUtils.getLoginUserId(session);
        webSocketMessageSender.sendObject(UserTypeEnum.ADMIN.getValue(), message.getToUserId(),
                "demo-message-receive",
                new DemoReceiveMessage().setFromUserId(fromUserId).setText(message.getText()));
    }

    @Override
    public String getType() {
        return "demo-message-send";
    }
}
```

### 6.4 主动发送

```java
// 发给指定用户
webSocketMessageSender.send(userType, userId, messageType, jsonContent);
webSocketMessageSender.sendObject(userType, userId, messageType, objectContent);

// 发给某类用户（如全部管理员）
webSocketMessageSender.send(userType, messageType, jsonContent);

// 发给指定 Session
webSocketMessageSender.send(sessionId, messageType, jsonContent);
```

### 6.5 心跳

客户端发送文本 `ping`（长度 4），服务端直接回复 `pong`，不会进入业务监听器。

## 7. 依赖关系

模块自身依赖：

- `yshop-common`
- `yshop-spring-boot-starter-security`（`provided`，用于 `LoginUser` 与握手认证）
- `spring-boot-starter-websocket`
- `yshop-spring-boot-starter-mq`
- `spring-kafka`、`spring-rabbit`、`rocketmq-spring-boot-starter`（均 `optional=true`）
- `yshop-spring-boot-starter-biz-tenant`（`provided`，广播时按租户过滤）

上游使用者：

- `yshop-module-infra-biz` 直接依赖本模块；
- `yshop-server` 通过 `infra-biz` 传递引入；
- 其它业务模块通过 `infra` 的 `WebSocketSenderApi` 间接使用，无需直接依赖本模块。

## 8. 注意事项

1. **`local` 发送器仅适合单机**。多实例部署时必须改用 `redis`、`rocketmq`、`kafka` 或 `rabbitmq`，并显式引入对应依赖；当前仓库业务模块没有引入这些可选依赖。
2. **`WebSocketAuthorizeRequestsCustomizer` 未被注册为 Bean**：全仓库只有类定义，没有 `@Bean`/`@Component` 注册，因此不会自动把 `yshop.websocket.path` 加入 Security 放行列表。握手请求仍会经过安全过滤器；携带有效 token 时可正常通过。
3. 五个发送器嵌套配置的 `matchIfMissing` 都为 `true`，省略 `sender-type` 会导致多个发送器同时装配。应始终显式配置 `sender-type`。
4. `WebSocketSessionHandlerDecorator` 使用 `ConcurrentWebSocketSessionDecorator` 包装 Session，发送时间上限 5 秒、缓冲上限 100 KB；超过限制可能抛发送异常。
5. `WebSocketSessionManagerImpl.getSessionList(userType)` 会按 `TenantContextHolder` 过滤租户；`getSessionList(userType, userId)` **不做租户过滤**，按用户编号发送时需调用方自行保证用户编号不跨租户。
6. `JsonWebSocketMessageHandler` 捕获所有处理异常并只记录日志，不会向客户端返回错误帧；业务需自行记录与兜底。
7. 消息处理时通过 `TenantUtils.execute(tenantId, ...)` 设置 Session 所属租户；若 Session 未关联登录用户，`tenantId` 为 `null`。
8. 各广播实现的消费组/队列策略不同：
   - Redis：Pub/Sub 广播；
   - RocketMQ：`MessageModel.BROADCASTING`；
   - Kafka：消费组追加随机 UUID，确保每实例都收到；
   - RabbitMQ：队列名追加随机 UUID 且 `autoDelete=true`。
9. `KafkaWebSocketMessageConsumer` 中误用了 `@RabbitHandler`，字段名也被命名为 `rabbitMQWebSocketMessageSender`（实际类型是 `KafkaWebSocketMessageSender`）。这是复制粘贴遗留，不影响编译和 Kafka 监听，但阅读时需注意。
10. 握手时通过 `?token=` 传递凭证，token 可能出现在访问日志中；生产环境应对网关/访问日志做脱敏。
11. 本模块 `provided` 依赖 security 与 tenant，直接使用本模块的独立应用需自行保证这两个组件在运行类路径中存在（本仓库由 `infra-biz` 传递满足）。
