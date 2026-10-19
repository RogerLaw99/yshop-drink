# yshop-spring-boot-starter-redis

> 所属：yshop-framework；文档依据本仓库源码整理。

## 1. 模块定位

`yshop-spring-boot-starter-redis` 是 Redis 封装 Starter，Maven 坐标 `co.yixiang.boot:yshop-spring-boot-starter-redis`。`pom.xml` 描述为“Redis 封装拓展”。

模块基于 Redisson Spring Boot Starter 与 Spring Cache，提供：

- 自定义 `RedisTemplate`（String key、JSON value）。
- Spring Cache 的 Redis 实现配置（统一 key 前缀、JSON 序列化、TTL）。
- 支持 `cacheName#ttl` 语法的自定义过期时间。
- Redis Key 定义与静态注册表。
- 一个静态 `RedisUtil` 便捷工具类。

模块没有自定义 Redisson 客户端配置；`RedissonConfig.java` 整个文件被注释掉，Redisson 客户端由 `redisson-spring-boot-starter` 的自动配置提供。

## 2. 目录与包结构

根包：`co.yixiang.yshop.framework.redis`

```text
co.yixiang.yshop.framework.redis
├── config
│   ├── RedissonConfig            （整个文件被注释，未生效）
│   ├── YshopCacheAutoConfiguration
│   ├── YshopCacheProperties
│   └── YshopRedisAutoConfiguration
├── core
│   ├── RedisKeyDefine
│   ├── RedisKeyRegistry
│   └── TimeoutRedisCacheManager
└── util/redis/RedisUtil
```

自动配置注册文件：`src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`。

## 3. 核心类与职责

| 类/接口 | 所在包 | 职责 |
|---|---|---|
| `YshopRedisAutoConfiguration` | `framework.redis.config` | `@AutoConfiguration(before = RedissonAutoConfigurationV2.class)`；注册 `RedisTemplate<String, Object>`：key/hashKey 使用 String 序列化，value/hashValue 使用 JSON 序列化，并为 JSON `ObjectMapper` 注册 `JavaTimeModule`。 |
| `YshopCacheAutoConfiguration` | `framework.redis.config` | `@EnableCaching`；启用 `CacheProperties`、`YshopCacheProperties`；注册 `RedisCacheConfiguration`（单冒号前缀、JSON value 序列化、TTL/空值/前缀策略）和 `RedisCacheManager`（使用 `BatchStrategies.scan` + `TimeoutRedisCacheManager`）。 |
| `YshopCacheProperties` | `framework.redis.config` | 配置前缀 `yshop.cache`；目前只有 `redisScanBatchSize`，默认 30。 |
| `TimeoutRedisCacheManager` | `framework.redis.core` | 继承 `RedisCacheManager`；当 cache name 形如 `key#10m`、`key#1h`、`key#1d` 时，把 `#` 后的时间解析为 TTL，并从缓存名中移除。支持 `d/h/m/s`，纯数字默认按秒。 |
| `RedisKeyDefine` | `framework.redis.core` | Redis Key 定义对象：`keyTemplate`、`keyType`、`valueType`、`timeoutType`、`timeout`、`memo`；构造时自动注册到 `RedisKeyRegistry`；提供 `formatKey(Object...)`。 |
| `RedisKeyRegistry` | `framework.redis.core` | 静态 `List<RedisKeyDefine>` 注册表，提供 `add`、`list`、`size`。 |
| `RedisUtil` | `framework.redis.util.redis` | 静态工具类，封装 `expire`、`getExpire`、`hasKey`、`del`、`get`、`set`、`set(key,value,time)`、`incr`、`decr`；通过 `SpringUtil.getBean("redisTemplate", RedisTemplate.class)` 获取模板。 |
| `RedissonConfig` | `framework.redis.config` | **整个文件被注释**，不参与编译与运行；原先意图是手工创建单机 `RedissonClient`。 |

## 4. 自动配置与生效方式

`AutoConfiguration.imports` 注册：

```text
co.yixiang.yshop.framework.redis.config.YshopRedisAutoConfiguration
co.yixiang.yshop.framework.redis.config.YshopCacheAutoConfiguration
```

关键点：

- `YshopRedisAutoConfiguration` 在 `RedissonAutoConfigurationV2` 之前生效，确保自定义 `RedisTemplate` 优先。
- `YshopCacheAutoConfiguration` 使用 `@EnableCaching` 开启注解缓存，并注册 `@Primary` 的 `RedisCacheConfiguration`。
- 缓存 key 前缀使用单冒号拼接：`${spring.cache.redis.key-prefix}:cacheName:`；若未配置 `key-prefix`，则为 `cacheName:`。
- `RedisCacheManager` 的批量策略由 `yshop.cache.redis-scan-batch-size` 控制（默认 30）。
- Redisson 客户端的连接配置来自 `spring.data.redis.*`，由 `redisson-spring-boot-starter` 读取；本模块未覆盖。
- `spring.factories` 不存在于本模块。

## 5. 关键配置项

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `yshop.cache.redis-scan-batch-size` | `30` | Redis SCAN 每次返回数量 |
| `spring.cache.type` | Spring Boot 默认值 | 本仓库配置为 `REDIS` |
| `spring.cache.redis.key-prefix` | 无 | 缓存 key 前缀；为空时使用 `cacheName:` |
| `spring.cache.redis.time-to-live` | 无 | 默认缓存 TTL；本仓库配置为 `1h` |
| `spring.cache.redis.cache-null-values` | Spring Boot 默认值 | 为 `false` 时禁用缓存 null 值 |
| `spring.cache.redis.use-key-prefix` | Spring Boot 默认值 | 为 `false` 时禁用 key 前缀 |
| `spring.data.redis.host` | `localhost`（Spring Boot/Redisson 默认） | Redis 地址；本仓库 local 为 `127.0.0.1` |
| `spring.data.redis.port` | `6379` | Redis 端口；本仓库 local 为 `6379` |
| `spring.data.redis.database` | `0` | Redis 数据库索引 |
| `spring.data.redis.password` | 无 | Redis 密码 |
| `spring.data.redis.repositories.enabled` | `true` | 本仓库 `application.yaml` 显式设为 `false`，禁用 Spring Data Redis Repository 扫描 |

## 6. 使用方式 / 典型用法

直接使用 `RedisTemplate`（JSON value）：

```java
@Resource
private RedisTemplate<String, Object> redisTemplate;

redisTemplate.opsForValue().set("yshop:demo:key", data, Duration.ofMinutes(10));
Object value = redisTemplate.opsForValue().get("yshop:demo:key");
```

使用静态工具类：

```java
RedisUtil.set("yshop:demo:key", data, 600);
Object value = RedisUtil.get("yshop:demo:key");
RedisUtil.del("yshop:demo:key");
```

自定义缓存过期时间：

```java
@Cacheable(cacheNames = "system_user#10m", key = "#id")
public UserDO getUser(Long id) {
    return userMapper.selectById(id);
}
```

定义 Redis Key：

```java
public interface RedisKeyConstants {
    RedisKeyDefine YSHOP_ORDER_CACHE_KEY =
            new RedisKeyDefine("确认订单数据缓存", "yshop:order:%s",
                    RedisKeyDefine.KeyTypeEnum.STRING, OrderDO.class, Duration.ofHours(1));
}
```

之后可通过 `RedisKeyRegistry.list()` 获取所有已注册的 Key 定义。

## 7. 依赖关系

直接依赖：

- `co.yixiang.boot:yshop-common`。
- `org.redisson:redisson-spring-boot-starter`（BOM 版本 3.26.0）。
- `org.springframework.boot:spring-boot-starter-cache`。
- `com.fasterxml.jackson.datatype:jackson-datatype-jsr310`。

Redisson Starter 同时引入 Redis 客户端与 Spring Data Redis 所需的连接工厂；本模块在此之上覆盖 `RedisTemplate` 与 Cache 配置。

## 8. 注意事项

1. `RedissonConfig.java` 整个文件被注释，属于无效代码；不要以为该模块会手工创建 `RedissonClient`。
2. `RedisUtil` 在类初始化时通过 `SpringUtil.getBean("redisTemplate")` 获取 Bean，因此必须在 Spring 容器初始化完成后使用；在静态初始化阶段或非 Spring 环境调用可能失败。
3. `RedisUtil` 内部使用 `e.printStackTrace()` 吞掉异常并返回 `false`，生产代码建议优先使用 `RedisTemplate` 或自行处理异常。
4. `TimeoutRedisCacheManager` 的 TTL 语法依赖 `#`；如果 cache name 本身包含 `#` 且分割后不是两段，则不会应用自定义 TTL。
5. 本仓库未使用 Spring Data Redis 的 Repository，已通过 `spring.data.redis.repositories.enabled=false` 关闭扫描。
6. 缓存 value 使用 JSON 序列化，并注册了 `JavaTimeModule`，因此 `LocalDateTime` 可以正常读写。
7. 修改 `spring.cache.redis.key-prefix` 会影响已有缓存 key 的命中，发布时需评估缓存失效影响。
