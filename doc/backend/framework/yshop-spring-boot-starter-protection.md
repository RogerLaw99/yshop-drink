# yshop-spring-boot-starter-protection

> 所属：yshop-framework；文档依据本仓库源码整理。

## 1. 模块定位

本模块是“服务保障”组件集合，包含三个子能力：

1. **幂等（Idempotent）**：通过 `@Idempotent` + Redis `SET NX` 防止同一请求在短时间内重复执行。
2. **限流（RateLimiter）**：通过 `@RateLimiter` + Redisson `RRateLimiter` 做分布式限流。
3. **分布式锁（Lock4j）**：对 Lock4j 的失败策略做定制，获取锁失败时统一抛出业务异常；仅在 Lock4j 类存在时生效。

### 在 yshop-server 中的依赖与启用状态

- 本模块是 `yshop-server/pom.xml` 中**唯一被直接声明的框架 starter**：

  ```xml
  <dependency>
      <groupId>co.yixiang.boot</groupId>
      <artifactId>yshop-spring-boot-starter-protection</artifactId>
  </dependency>
  ```

- 三个自动配置类均通过 `AutoConfiguration.imports` 注册：
  - 幂等与限流默认生效（依赖 Redis / Redisson）；
  - **Lock4j 未生效**：`com.baomidou:lock4j-redisson-spring-boot-starter` 在本模块 pom 中为 `optional=true`，全仓库没有其它模块显式引入，运行类路径通常不存在 `com.baomidou.lock.annotation.Lock4j`，因此 `YshopLock4jConfiguration` 的条件不满足。
- 当前业务代码中**没有发现** `@Idempotent`、`@RateLimiter`、`@Lock4j` 的实际使用；模块已装配，但能力处于“可用未使用”状态。
- `application-local.yaml` / `application-dev.yaml` 中仍保留了 `lock4j.acquire-timeout`、`lock4j.expire` 配置，但由于依赖缺失，这些配置不会被 Lock4j 使用。

## 2. 目录与包结构

```text
yshop-spring-boot-starter-protection/
├── pom.xml
└── src/main/
    ├── java/co/yixiang/yshop/framework/
    │   ├── idempotent/
    │   │   ├── config/YshopIdempotentConfiguration.java
    │   │   └── core/
    │   │       ├── annotation/Idempotent.java
    │   │       ├── aop/IdempotentAspect.java
    │   │       ├── keyresolver/
    │   │       │   ├── IdempotentKeyResolver.java
    │   │       │   └── impl/
    │   │       │       ├── DefaultIdempotentKeyResolver.java
    │   │       │       ├── ExpressionIdempotentKeyResolver.java
    │   │       │       └── UserIdempotentKeyResolver.java
    │   │       └── redis/IdempotentRedisDAO.java
    │   ├── lock4j/
    │   │   ├── config/YshopLock4jConfiguration.java
    │   │   └── core/
    │   │       ├── DefaultLockFailureStrategy.java
    │   │       └── Lock4jRedisKeyConstants.java
    │   └── ratelimiter/
    │       ├── config/YshopRateLimiterConfiguration.java
    │       └── core/
    │           ├── annotation/RateLimiter.java
    │           ├── aop/RateLimiterAspect.java
    │           ├── keyresolver/
    │           │   ├── RateLimiterKeyResolver.java
    │           │   └── impl/
    │           │       ├── ClientIpRateLimiterKeyResolver.java
    │           │       ├── DefaultRateLimiterKeyResolver.java
    │           │       ├── ExpressionRateLimiterKeyResolver.java
    │           │       ├── ServerNodeRateLimiterKeyResolver.java
    │           │       └── UserRateLimiterKeyResolver.java
    │           └── redis/RateLimiterRedisDAO.java
    └── resources/META-INF/spring/
        └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

## 3. 核心类与职责

| 类/接口 | 所在包 | 职责 |
|---|---|---|
| `YshopIdempotentConfiguration` | `...idempotent.config` | 注册幂等切面、Redis DAO 与三个 Key 解析器 |
| `Idempotent` | `...idempotent.core.annotation` | 幂等注解：超时、提示语、Key 解析器、SpEL 参数、异常时删除 Key |
| `IdempotentAspect` | `...idempotent.core.aop` | 拦截 `@Idempotent`：加锁失败抛重复请求异常，成功执行，异常时可选删 Key |
| `IdempotentKeyResolver` | `...idempotent.core.keyresolver` | 幂等 Key 解析接口 |
| `DefaultIdempotentKeyResolver` | `...idempotent.core.keyresolver.impl` | 方法签名 + 参数，MD5 压缩（全局级别） |
| `UserIdempotentKeyResolver` | `...idempotent.core.keyresolver.impl` | 方法 + 参数 + `userId` + `userType`（用户级别） |
| `ExpressionIdempotentKeyResolver` | `...idempotent.core.keyresolver.impl` | 解析 `keyArg` 指定的 SpEL 表达式 |
| `IdempotentRedisDAO` | `...idempotent.core.redis` | `idempotent:%s` 键上的 `setIfAbsent` / `delete` |
| `YshopRateLimiterConfiguration` | `...ratelimiter.config` | 注册限流切面、Redis DAO 与五个 Key 解析器 |
| `RateLimiter` | `...ratelimiter.core.annotation` | 限流注解：时间窗口、次数、提示语、Key 解析器、SpEL 参数 |
| `RateLimiterAspect` | `...ratelimiter.core.aop` | 方法执行前尝试获取令牌，失败抛 `TOO_MANY_REQUESTS` |
| `RateLimiterKeyResolver` | `...ratelimiter.core.keyresolver` | 限流 Key 解析接口 |
| `DefaultRateLimiterKeyResolver` | `...ratelimiter.core.keyresolver.impl` | 方法 + 参数（全局级别） |
| `UserRateLimiterKeyResolver` | `...ratelimiter.core.keyresolver.impl` | 方法 + 参数 + 用户信息 |
| `ClientIpRateLimiterKeyResolver` | `...ratelimiter.core.keyresolver.impl` | 方法 + 参数 + 客户端 IP |
| `ServerNodeRateLimiterKeyResolver` | `...ratelimiter.core.keyresolver.impl` | 方法 + 参数 + 本机 `IP@PID` |
| `ExpressionRateLimiterKeyResolver` | `...ratelimiter.core.keyresolver.impl` | 解析 `keyArg` 指定的 SpEL 表达式 |
| `RateLimiterRedisDAO` | `...ratelimiter.core.redis` | `rate_limiter:%s` 键上的 Redisson `RRateLimiter` 操作 |
| `YshopLock4jConfiguration` | `...lock4j.config` | 存在 Lock4j 类时注册失败策略 |
| `DefaultLockFailureStrategy` | `...lock4j.core` | 获取锁失败时抛出 `ServiceException(GlobalErrorCodeConstants.LOCKED)` |
| `Lock4jRedisKeyConstants` | `...lock4j.core` | 约定 Redis key 前缀 `lock4j:%s` |

## 4. 自动配置与生效方式

`AutoConfiguration.imports`：

```text
co.yixiang.yshop.framework.idempotent.config.YshopIdempotentConfiguration
co.yixiang.yshop.framework.lock4j.config.YshopLock4jConfiguration
co.yixiang.yshop.framework.ratelimiter.config.YshopRateLimiterConfiguration
```

- `YshopIdempotentConfiguration`
  - `@AutoConfiguration(after = YshopRedisAutoConfiguration.class)`；
  - `idempotentAspect(List<IdempotentKeyResolver>, IdempotentRedisDAO)`；
  - `idempotentRedisDAO(StringRedisTemplate)`；
  - 默认、用户、SpEL 三个 `IdempotentKeyResolver` Bean。
- `YshopRateLimiterConfiguration`
  - `@AutoConfiguration(after = YshopRedisAutoConfiguration.class)`；
  - `rateLimiterAspect(List<RateLimiterKeyResolver>, RateLimiterRedisDAO)`；
  - `rateLimiterRedisDAO(RedissonClient)`；
  - 默认、用户、客户端 IP、服务节点、SpEL 五个 `RateLimiterKeyResolver` Bean。
- `YshopLock4jConfiguration`
  - `@AutoConfiguration(before = LockAutoConfiguration.class)`；
  - `@ConditionalOnClass(name = "com.baomidou.lock.annotation.Lock4j")`；
  - 注册 `DefaultLockFailureStrategy`。
  - 由于本仓库未引入 Lock4j starter，该配置不会加载。

## 5. 关键配置项

本模块**没有定义自己的 `@ConfigurationProperties`**。可用参数分两类。

### 5.1 注解参数

| 注解 | 参数 | 默认值 | 说明 |
|---|---|---|---|
| `@Idempotent` | `timeout` | `1` | 幂等 Key 的过期时间数值 |
| `@Idempotent` | `timeUnit` | `SECONDS` | 过期时间单位 |
| `@Idempotent` | `message` | `重复请求，请稍后重试` | 重复请求提示 |
| `@Idempotent` | `keyResolver` | `DefaultIdempotentKeyResolver.class` | Key 解析器 |
| `@Idempotent` | `keyArg` | `""` | SpEL 表达式（配合 `ExpressionIdempotentKeyResolver`） |
| `@Idempotent` | `deleteKeyWhenException` | `true` | 业务异常时删除 Key，允许下次重试 |
| `@RateLimiter` | `time` | `1` | 限流窗口数值 |
| `@RateLimiter` | `timeUnit` | `SECONDS` | 时间单位 |
| `@RateLimiter` | `count` | `100` | 窗口内允许的次数 |
| `@RateLimiter` | `message` | `""` | 为空时使用 `TOO_MANY_REQUESTS` 默认提示 |
| `@RateLimiter` | `keyResolver` | `DefaultRateLimiterKeyResolver.class` | Key 解析器 |
| `@RateLimiter` | `keyArg` | `""` | SpEL 表达式 |

### 5.2 外部依赖配置

| 配置项 | 本仓库取值 | 说明 |
|---|---|---|
| `lock4j.acquire-timeout` | `3000` | Lock4j 获取锁超时（毫秒）；当前依赖未引入，不生效 |
| `lock4j.expire` | `30000` | Lock4j 锁过期时间（毫秒）；当前依赖未引入，不生效 |
| `spring.data.redis.*` | `127.0.0.1:6379`（local） | 幂等与限流共用的 Redis 连接 |

## 6. 使用方式 / 典型用法

### 6.1 幂等

```java
@Idempotent(timeout = 5, timeUnit = TimeUnit.SECONDS)
public void createOrder(OrderCreateReqVO reqVO) { /* ... */ }

@Idempotent(keyResolver = UserIdempotentKeyResolver.class)
public void submitScore() { /* 同一用户短时间只能提交一次 */ }

@Idempotent(keyResolver = ExpressionIdempotentKeyResolver.class,
            keyArg = "#reqVO.orderId")
public void pay(OrderPayReqVO reqVO) { /* 同一订单号幂等 */ }
```

### 6.2 限流

```java
@RateLimiter(time = 1, count = 10)
public CommonResult<?> sendSms(String mobile) { /* 每秒最多 10 次 */ }

@RateLimiter(keyResolver = ClientIpRateLimiterKeyResolver.class, count = 30)
public CommonResult<?> login(LoginReqVO reqVO) { /* 按客户端 IP 限流 */ }
```

### 6.3 分布式锁（需先引入 Lock4j）

```java
@Lock4j(keys = "#orderId", acquireTimeout = 3000, expire = 30000)
public void handleOrder(String orderId) { /* ... */ }
```

获取锁失败时，`DefaultLockFailureStrategy` 会抛出 `GlobalErrorCodeConstants.LOCKED` 对应的 `ServiceException`。

## 7. 依赖关系

模块自身依赖：

- `yshop-spring-boot-starter-web`（`provided`，幂等/限流需要 Web 上下文）
- `yshop-spring-boot-starter-redis`
- `com.baomidou:lock4j-redisson-spring-boot-starter`（`optional=true`）

上游使用者：

- `yshop-server` **直接依赖**本模块；
- 因此其自动配置在 server 启动时可直接生效（不依赖其它业务模块传递）。

## 8. 注意事项

1. **Lock4j 当前不可用**：依赖为 optional 且无其它模块引入，`@Lock4j` 注解在本仓库类路径中不存在，业务代码也无法使用。
2. **幂等与限流当前无业务使用**：全仓库检索不到 `@Idempotent` / `@RateLimiter` 标注（仅注解定义本身），属于已具备但未落地的基础设施。
3. 幂等 Key 只在 `setIfAbsent` 时写入，**执行成功后不会主动删除**，必须等待 `timeout` 过期。因此 `timeout` 应覆盖“可能重复提交的时间窗口”；若业务执行时间超过 `timeout`，后续请求仍可能进入。
4. 幂等 Key 格式为 `idempotent:<MD5>`；限流 Key 格式为 `rate_limiter:<MD5>`。两者都依赖 Redis 可用性。
5. `DefaultIdempotentKeyResolver` / `UserIdempotentKeyResolver` / `ClientIpRateLimiterKeyResolver` 等直接拼接 `joinPoint.getArgs()` 后做 MD5。若参数对象未实现稳定的 `toString()`，相同业务参数可能产生不同 Key。
6. 使用 `ExpressionIdempotentKeyResolver` / `ExpressionRateLimiterKeyResolver` 时，SpEL 变量名来自编译参数名。根 pom 已配置 `-parameters`，新增模块需保持该编译配置。
7. 限流基于 Redisson `RRateLimiter` 的 `RateType.OVERALL`，即**全局共享速率**；修改同一 Key 的 count/time 时，`RateLimiterRedisDAO` 会检测配置差异并调用 `setRate(...)` 重建速率。
8. `RateLimiterAspect` 在方法执行前直接抛异常（`@Before`），不会执行目标方法；`IdempotentAspect` 在异常时按 `deleteKeyWhenException` 决定是否释放 Key。
9. 幂等切面与限流切面都通过 `List<...KeyResolver>` 构建 `Class → Bean` 映射；自定义解析器必须注册为 Spring Bean，并在注解中指定其实现类。
10. 本模块的 `provided` Web 依赖意味着：只引入 protection 而不引入 Web 组件时，幂等/限流中的 `WebFrameworkUtils`、`ServletUtils` 相关解析器可能不可用（server 场景已由 web starter 满足）。
