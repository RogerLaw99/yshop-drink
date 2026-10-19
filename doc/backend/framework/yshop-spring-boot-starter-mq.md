# yshop-spring-boot-starter-mq

> 所属：yshop-framework；文档依据本仓库源码整理。

## 1. 模块定位

本模块是消息队列的框架封装，当前**实际实现的是 Redis MQ**，提供两种消费模型：

- **Redis Pub/Sub**：广播消费，所有在线订阅者都会收到消息；
- **Redis Stream**：集群消费，按消费者组分摊消息，支持未 ACK 消息的定时重投。

同时提供：

- `RedisMQTemplate`：统一发送入口，支持发送前/后拦截器；
- `AbstractRedisMessage` / `AbstractRedisStreamMessage` / `AbstractRedisChannelMessage`：消息基类，自带 header；
- `RedisMessageInterceptor`：发送与消费前后的插件机制（多租户即通过它透传租户编号）；
- `RedisPendingMessageResendJob`：扫描 pending 消息并重新投递；
- RabbitMQ 的 JSON 消息转换器（仅在 RabbitMQ 类存在时生效）。

模块描述中提到支持 RocketMQ、RabbitMQ、Kafka，但本模块源码中**没有**这三者的生产者/消费者实现；Kafka/RocketMQ 的 MQ 集成位于 `biz-tenant` 与 `websocket` 模块。

### 在 yshop-server 中的依赖与启用状态

- `yshop-server/pom.xml` **没有直接声明**本模块。
- 通过 `yshop-module-infra-biz`、`yshop-module-system-biz`、`yshop-module-message-biz`、`yshop-module-pay-biz`、`yshop-module-score-biz`、`yshop-module-member-biz`、`yshop-module-express-biz`、`yshop-module-mp-biz`、`yshop-spring-boot-starter-websocket` 等传递引入。
- 三个自动配置类均通过 `AutoConfiguration.imports` 注册。
- **Redis MQ 在运行时启用且被实际使用**：
  - `pay` 模块 `PayNoticeProducer` 发送 `PayNoticeMessage`（Stream key 被重写为 `order.pay.notice`）；
  - `order` 模块 `PayNoticeConsumer` 消费该消息；
  - `message` 模块 `WeixinNoticeProducer` / `WeixinNoticeConsumer` 处理微信通知消息。
- **RabbitMQ 配置未生效**：`YshopRabbitMQAutoConfiguration` 使用 `@ConditionalOnClass(name = "org.springframework.amqp.rabbit.core.RabbitTemplate")`，而 `spring-rabbit` 在本仓库多个 pom 中均为 `optional=true`，业务模块没有显式引入，运行类路径通常不存在该类。因此只注册 Redis MQ，不会创建 RabbitMQ 消息转换器。
- Kafka / RocketMQ 同理：仅在可选依赖存在时才可能被 `biz-tenant` / `websocket` 使用。

## 2. 目录与包结构

```text
yshop-spring-boot-starter-mq/
├── pom.xml
└── src/main/
    ├── java/co/yixiang/yshop/framework/mq/
    │   ├── rabbitmq/config/YshopRabbitMQAutoConfiguration.java
    │   └── redis/
    │       ├── config/
    │       │   ├── YshopRedisMQProducerAutoConfiguration.java
    │       │   └── YshopRedisMQConsumerAutoConfiguration.java
    │       └── core/
    │           ├── RedisMQTemplate.java
    │           ├── interceptor/RedisMessageInterceptor.java
    │           ├── job/RedisPendingMessageResendJob.java
    │           ├── message/AbstractRedisMessage.java
    │           ├── pubsub/
    │           │   ├── AbstractRedisChannelMessage.java
    │           │   └── AbstractRedisChannelMessageListener.java
    │           └── stream/
    │               ├── AbstractRedisStreamMessage.java
    │               └── AbstractRedisStreamMessageListener.java
    └── resources/META-INF/spring/
        └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

## 3. 核心类与职责

| 类/接口 | 所在包 | 职责 |
|---|---|---|
| `YshopRedisMQProducerAutoConfiguration` | `...mq.redis.config` | 创建 `RedisMQTemplate`，并注入所有 `RedisMessageInterceptor` |
| `YshopRedisMQConsumerAutoConfiguration` | `...mq.redis.config` | 注册 Pub/Sub 容器、Stream 容器、pending 消息重投任务；`@EnableScheduling` |
| `YshopRabbitMQAutoConfiguration` | `...mq.rabbitmq.config` | 存在 `RabbitTemplate` 时注册 `Jackson2JsonMessageConverter` |
| `RedisMQTemplate` | `...mq.redis.core` | 发送 Pub/Sub 与 Stream 消息；按正序/倒序执行拦截器 |
| `RedisMessageInterceptor` | `...mq.redis.core.interceptor` | 发送前/后、消费前/后的扩展点（默认空实现） |
| `RedisPendingMessageResendJob` | `...mq.redis.core.job` | 每分钟扫描超时未 ACK 的 Stream 消息并重新投递 |
| `AbstractRedisMessage` | `...mq.redis.core.message` | 消息基类，维护 `headers` |
| `AbstractRedisChannelMessage` | `...mq.redis.core.pubsub` | Pub/Sub 消息基类；`getChannel()` 默认返回类简名 |
| `AbstractRedisChannelMessageListener` | `...mq.redis.core.pubsub` | Pub/Sub 监听器基类，按泛型反序列化并调用 `onMessage(T)` |
| `AbstractRedisStreamMessage` | `...mq.redis.core.stream` | Stream 消息基类；`getStreamKey()` 默认返回类简名 |
| `AbstractRedisStreamMessageListener` | `...mq.redis.core.stream` | Stream 监听器基类；消费成功后 ACK，使用 `spring.application.name` 作为消费组 |

## 4. 自动配置与生效方式

`AutoConfiguration.imports`：

```text
co.yixiang.yshop.framework.mq.redis.config.YshopRedisMQProducerAutoConfiguration
co.yixiang.yshop.framework.mq.redis.config.YshopRedisMQConsumerAutoConfiguration
co.yixiang.yshop.framework.mq.rabbitmq.config.YshopRabbitMQAutoConfiguration
```

### 4.1 Producer

- `@AutoConfiguration(after = YshopRedisAutoConfiguration.class)`；
- 注入 `StringRedisTemplate` 与 `List<RedisMessageInterceptor>`，创建 `RedisMQTemplate` 并逐个 `addInterceptor`。

### 4.2 Consumer

- `@AutoConfiguration(after = YshopRedisAutoConfiguration.class)`、`@EnableScheduling`；
- `redisMessageListenerContainer`：仅当存在 `AbstractRedisChannelMessageListener` Bean 时创建，按每个 listener 的 `getChannel()` 注册订阅；
- `redisStreamMessageListenerContainer`：仅当存在 `AbstractRedisStreamMessageListener` Bean 时创建，`initMethod=start`、`destroyMethod=stop`：
  - `batchSize=10`，目标类型统一为 `String`；
  - 消费组由 listener 的 `group` 决定（默认 `spring.application.name`）；
  - 消费者名称为 `IP@PID`；
  - `autoAcknowledge(false)`，由监听器消费成功后手动 ACK；
  - 注册前尝试 `createGroup`，已存在时忽略异常；
  - 启动时校验 Redis 主版本号必须 `>= 5`。
- `redisPendingMessageResendJob`：仅在存在 Stream listener 时创建，依赖 `RedissonClient`。

### 4.3 RabbitMQ

- 类级 `@ConditionalOnClass(name = "org.springframework.amqp.rabbit.core.RabbitTemplate")`；
- 仅注册 `MessageConverter`（`Jackson2JsonMessageConverter`），不包含发送/消费模板。

## 5. 关键配置项

本模块**没有定义 `@ConfigurationProperties`**，没有 `yshop.mq.*` 专属配置项。运行时依赖以下 Spring/Redis 标准配置：

| 配置项 | 说明 |
|---|---|
| `spring.application.name` | Stream 默认消费组名；`RedisPendingMessageResendJob` 也以它作为 groupName |
| `spring.data.redis.host` / `port` / `database` / `password` | Redis 连接配置（由 redis starter 使用） |
| `spring.rabbitmq.*` | RabbitMQ 连接配置；仅当 RabbitMQ 依赖存在时有意义 |
| `spring.kafka.*` | Kafka 连接配置；本模块不消费，仅供其它模块的可选集成使用 |
| `rocketmq.name-server` | RocketMQ 配置；本模块不消费 |

消息侧没有“开关”配置：只要 Redis 可用且存在对应监听器 Bean，容器就会启动。

## 6. 使用方式 / 典型用法

### 6.1 定义 Stream 消息

```java
@Data
public class PayNoticeMessage extends AbstractRedisStreamMessage {
    private String orderId;
    private String payType;

    @Override
    public String getStreamKey() {
        return "order.pay.notice"; // 不重写时默认使用类简名
    }
}
```

### 6.2 发送消息

```java
@Component
public class PayNoticeProducer {
    @Resource
    private RedisMQTemplate redisMQTemplate;

    public void sendPayNoticeMessage(String orderId, String payType) {
        redisMQTemplate.send(new PayNoticeMessage().setOrderId(orderId).setPayType(payType));
    }
}
```

### 6.3 消费消息（Stream，集群消费）

```java
@Component
public class PayNoticeConsumer extends AbstractRedisStreamMessageListener<PayNoticeMessage> {
    @Override
    public void onMessage(PayNoticeMessage message) {
        // 处理业务；方法正常返回后框架自动 ACK
    }
}
```

### 6.4 广播消费（Pub/Sub）

```java
public class DemoChannelMessage extends AbstractRedisChannelMessage { }

public class DemoChannelListener extends AbstractRedisChannelMessageListener<DemoChannelMessage> {
    @Override
    public void onMessage(DemoChannelMessage message) { /* ... */ }
}
```

### 6.5 自定义拦截器

```java
@Component
public class MyRedisMessageInterceptor implements RedisMessageInterceptor {
    @Override
    public void sendMessageBefore(AbstractRedisMessage message) {
        message.addHeader("x-trace-id", "...");
    }
}
```

## 7. 依赖关系

模块自身依赖：

- `yshop-spring-boot-starter-redis`
- `spring-kafka`（`optional=true`）
- `spring-rabbit`（`optional=true`）
- `rocketmq-spring-boot-starter`（`optional=true`）

上游使用者：

- `yshop-module-infra-biz`、`system-biz`、`message-biz`、`pay-biz`（含 `pay-api`）、`score-biz`（含 `score-api`）、`member-biz`、`express-biz`、`mp-biz`；
- `yshop-spring-boot-starter-websocket` 直接依赖它实现 Redis 广播发送；
- `yshop-spring-boot-starter-biz-tenant` 以 `optional` 方式依赖它；
- `yshop-server` 通过业务模块传递引入。

## 8. 注意事项

1. **Redis 版本要求 ≥ 5.0**：Stream 容器启动时通过 `INFO redis_version` 校验，低版本会抛 `IllegalStateException`。
2. Stream 监听器**必须声明泛型**（如 `extends AbstractRedisStreamMessageListener<PayNoticeMessage>`），否则构造时通过 `TypeUtil.getTypeArgument` 取不到消息类型并抛出 `IllegalStateException`。
3. Stream key 默认是消息类简名；本仓库 `PayNoticeMessage` 重写为 `order.pay.notice`。修改类名会改变默认 key，生产环境建议显式重写。
4. 消费组默认取 `spring.application.name`（`yshop-server`）；同一应用的所有 Stream listener 共用该组名，但按不同 streamKey 分别消费。
5. `onMessage` 正常返回后才 ACK；抛异常时不会 ACK，消息留在 pending 队列，由 `RedisPendingMessageResendJob` 在超过 5 分钟后重投。重投任务每分钟第 35 秒执行，并用 Redisson 锁 `redis:pending:msg:lock` 防止多节点重复执行。
6. `RedisMQTemplate.send(...)` 的发送前拦截器按正序执行，发送后拦截器按倒序执行；消费侧同样为正序 before、倒序 after。
7. Pub/Sub 为广播语义，不保证可靠投递；需要可靠消费应使用 Stream。
8. 本模块的 RabbitMQ 自动配置只提供 JSON `MessageConverter`，不提供生产者/消费者模板；`spring-rabbit` 是 optional 依赖，未显式引入时该配置不会生效。
9. 虽然 pom 声明了 Kafka/RocketMQ 可选依赖，但本模块没有对应源码；不要据此认为它们已集成。Kafka/RocketMQ 的实际使用点在 `biz-tenant`（Header 透传）与 `websocket`（多节点广播）。
10. `RedisPendingMessageResendJob` 依赖 `RedissonClient`，而 Redisson 由 `yshop-spring-boot-starter-redis` 提供；缺少 Redis 会导致应用启动失败（项目整体约束）。
