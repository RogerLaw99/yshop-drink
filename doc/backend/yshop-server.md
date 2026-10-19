# yshop-server 启动入口

> 模块路径：`yshop-drink-boot3/yshop-server`；文档依据 `yshop-server/pom.xml`、`YshopServerApplication.java`、`application.yaml`、`application-local.yaml`、`application-dev.yaml`、`Dockerfile` 整理。

## 1. 模块定位

`yshop-server` 是整个后端的**可执行启动模块**，本身几乎不包含业务代码：

- `packaging=jar`，通过 `spring-boot-maven-plugin` 打成可执行 Fat Jar；
- 只包含启动类 `YshopServerApplication`、一个空的 `DefaultController`、配置文件与日志配置；
- 通过依赖各业务模块的 `-biz` 制品，把 Controller / Service / Mapper 装入同一个 Spring 容器；
- 依赖 `yshop-spring-boot-starter-protection` 提供服务保障能力。

Maven 坐标：

```xml
<groupId>co.yixiang.boot</groupId>
<artifactId>yshop-server</artifactId>
<version>${revision}</version>
<packaging>jar</packaging>
```

## 2. 目录结构

```
yshop-server/
├── pom.xml
├── Dockerfile
└── src/main/
    ├── java/co/yixiang/yshop/server/
    │   ├── YshopServerApplication.java      # 启动类
    │   └── controller/DefaultController.java # 空 Controller，占位/404 提示
    └── resources/
        ├── application.yaml                 # 公共配置 + 默认激活 local
        ├── application-local.yaml           # 本机开发配置
        ├── application-dev.yaml             # 开发/测试环境配置
        ├── logback-spring.xml               # 日志配置
        └── META-INF/spring/
            └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

`AutoConfiguration.imports` 中仅有一行：

```text
com.egzosn.pay.spring.boot.autoconfigue.PayAutoConfiguration
```

用于显式导入支付 starter 的自动配置。

## 3. 启动入口

`co.yixiang.yshop.server.YshopServerApplication`：

```java
@SpringBootApplication(scanBasePackages = {
        "${yshop.info.base-package}.server",
        "${yshop.info.base-package}.module"
})
public class YshopServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(YshopServerApplication.class, args);
    }
}
```

关键点：

| 项 | 值 | 说明 |
|---|---|---|
| 基础包 | `co.yixiang.yshop` | 由 `yshop.info.base-package` 配置 |
| 扫描包 | `co.yixiang.yshop.server`、`co.yixiang.yshop.module` | 扫描启动类与所有业务模块 |
| Web 端口 | `local` 48081 / `dev` 48080 | 见配置文件 |
| 默认 Profile | `local` | `application.yaml` 中 `spring.profiles.active` |
| API 前缀 | `/admin-api`、`/app-api` | 由 `WebProperties` 按 Controller 包名自动添加 |

`DefaultController` 是一个空的 `@RestController`，源码注释说明其目的是“解决部分 module 未开启时的 404 提示”，当前没有任何请求映射。

## 4. 依赖的 -biz 模块

### 4.1 直接依赖

`yshop-server/pom.xml` 直接声明以下业务模块：

| 依赖 | 所属工程 | 作用 |
|---|---|---|
| `yshop-module-system-biz` | system | 用户、角色、菜单、字典、OAuth2、租户、短信、邮件等 |
| `yshop-module-infra-biz` | infra | 文件、代码生成、定时任务、日志、配置、Redis 监控 |
| `yshop-module-pay-biz` | pay | 支付 |
| `yshop-module-express-biz` | express | 快递查询 |
| `yshop-module-message-biz` | message | 模板消息 / WebSocket / 延迟消息 |
| `yshop-module-mp-biz` | mp | 微信公众号 |
| `yshop-module-shop-biz` | mall | 店铺 |
| `yshop-module-order-biz` | mall | 订单 |
| `yshop-module-coupon-biz` | marketing | 优惠券 |
| `yshop-module-score-biz` | score | 积分 |
| `yshop-spring-boot-starter-protection` | framework | 分布式锁、限流、幂等 |

此外还引入 `spring-boot-configuration-processor`（optional），用于生成配置元数据。

### 4.2 传递依赖

部分模块没有在 `yshop-server` 中直接声明，而是通过其他 `-biz` 传递进入运行时：

| 传递模块 | 传递路径 | 说明 |
|---|---|---|
| `yshop-module-member-biz` | `message-biz` / `score-biz` → `member-biz` | 用户端会员登录、地址、个人中心接口依赖它加载 |
| `yshop-module-store-biz` | `system-biz`、`shop-biz`、`product-biz`、`coupon-biz` → `store-biz` | 门店能力 |
| `yshop-module-product-biz` | `order-biz` → `product-biz` | 商品能力 |

这意味着：如果移除 `message-biz` 或 `score-biz`，`member-biz` 的 Controller 可能一起从应用中消失。新增模块的 REST 接口时，仍应按项目约定在 `yshop-server/pom.xml` 中显式加入对应 `-biz`。

### 4.3 根工程模块全景

根 `pom.xml` 聚合：

```
yshop-dependencies
yshop-framework
yshop-server
yshop-module-system
yshop-module-infra
yshop-module-pay
yshop-module-mp
yshop-module-mall          （product / order / shop / store）
yshop-module-message
yshop-module-score
yshop-module-express
yshop-module-marketing     （coupon）
yshop-module-member
```

每个业务模块通常拆为 `-api`（接口 / DTO / 枚举 / 错误码）与 `-biz`（Controller / Service / DAL / 转换器）两个 jar。

## 5. 配置文件

### 5.1 application.yaml（公共配置）

| 配置项 | 值 | 说明 |
|---|---|---|
| `spring.application.name` | `yshop-server` | 应用名，用于日志、MQ、Spring Boot Admin |
| `spring.profiles.active` | `local` | 默认激活本机配置 |
| `spring.main.allow-circular-references` | `true` | 允许三层架构下的循环依赖 |
| `spring.servlet.multipart.max-file-size` | `16MB` | 单文件上传上限 |
| `spring.servlet.multipart.max-request-size` | `32MB` | 单次请求上传上限 |
| `spring.mvc.pathmatch.matching-strategy` | `ANT_PATH_MATCHER` | 兼容旧路径匹配 |
| `spring.jackson.serialization.write-dates-as-timestamps` | `true` | 日期序列化为时间戳 |
| `spring.cache.type` | `REDIS` | 使用 Redis 作为缓存 |
| `spring.cache.redis.time-to-live` | `1h` | 缓存默认 TTL |
| `springdoc.api-docs.path` | `/v3/api-docs` | OpenAPI 地址 |
| `springdoc.swagger-ui.path` | `/swagger-ui` | Swagger UI 地址 |
| `knife4j.enable` | `true` | 开启 Knife4j，中文界面 |
| `mybatis-plus.global-config.db-config.id-type` | `NONE` | 由数据源类型自动适配主键策略 |
| `mybatis-plus.global-config.db-config.logic-delete-value` | `1` | 逻辑删除值 |
| `mybatis-plus.global-config.db-config.logic-not-delete-value` | `0` | 未删除值 |
| `mybatis-plus.type-aliases-package` | `${yshop.info.base-package}.module.*.dal.dataobject` | DO 别名扫描 |
| `easy-trans.is-enable-global` | `true` | 全局启用 VO 翻译 |
| `yshop.info.version` | `3.0.0` | 版本号 |
| `yshop.info.base-package` | `co.yixiang.yshop` | Java 基础包 |
| `yshop.web.admin-ui.url` | `http://dashboard.yshop.yixiang.co` | 管理后台地址 |
| `yshop.security.permit-all_urls` | `/admin-api/mp/open/**` | 免登录 URL（微信公众号回调） |
| `yshop.websocket.enable` | `true` | 开启 WebSocket |
| `yshop.websocket.path` | `/infra/ws` | WebSocket 路径 |
| `yshop.websocket.sender-type` | `local` | 单机消息发送 |
| `yshop.captcha.enable` | `true` | 图形验证码开关 |
| `yshop.codegen.base-package` | `${yshop.info.base-package}` | 代码生成包名 |
| `yshop.codegen.db-schemas` | `${spring.datasource.dynamic.datasource.master.name}` | 代码生成目标库 |
| `yshop.codegen.front-type` | `10` | 前端模板类型：Vue2 标准模板（见 `CodegenFrontTypeEnum`） |
| `yshop.tenant.enable` | `false` | 当前未启用多租户 |
| `yshop.sms-code.*` | 10 分钟过期、1 分钟频率、每天 10 条、验证码范围 9999~9999 | 短信验证码策略 |
| `yshop.trade.order.*` | 支付 2h、收货 14d、评论 7d | 交易时效 |
| `jeecg.jmreport.saas-mode` | `tenant` | 积木报表 SaaS 模式 |

`application.yaml` 还包含 RocketMQ Producer、Kafka Producer/Consumer 的基础配置，以及验证码底图、字体、水印等参数。

### 5.2 application-local.yaml（本机开发）

| 配置项 | 值 | 说明 |
|---|---|---|
| `server.port` | `48081` | 本机端口 |
| 排除自动配置 | `QuartzAutoConfiguration`、Spring Boot Admin Server/UI/Client | 本机不启用 Quartz 与 Admin |
| 数据源 | `jdbc:mysql://127.0.0.1:3306/yixiang-drink-open`，root/root | 主库与懒加载从库 |
| Druid | 初始 1、最小 1、最大 20、慢 SQL 100ms | 连接池配置 |
| Redis | `127.0.0.1:6379`，db 0，无密码 | 本地 Redis |
| Quartz | `auto-startup=true`、JDBC JobStore、集群、25 线程、`initialize-schema=NEVER` | 但自动配置已被 `exclude`，实际不会创建 Scheduler |
| Lock4j | 获取超时 3000ms、锁超时 30000ms | 分布式锁 |
| Actuator | `/actuator`，暴露 `*` | 健康检查 / 监控 |
| Spring Boot Admin | 客户端地址 `http://127.0.0.1:${server.port}/admin` | 因排除自动配置，本地不生效 |
| 日志文件 | `${user.home}/logs/yshop-server.log` | 同时把各 module mapper 调到 debug |
| 微信 | 公众号 / 小程序测试 AppID、Secret | 明文测试凭据 |
| 业务开关 | `yshop.captcha.enable=false`、`yshop.security.mock-enable=true`、`yshop.xss.enable=false` | 便于本机登录联调 |
| 其他 | `yshop.access-log.enable=false`、`yshop.demo=false`、`yshop.file-path=/Users/hupeng/drink/file/`、`yshop.h5=http://localhost:80` | `file-path` 为 macOS 路径 |
| JustAuth | 钉钉、企业微信、微信小程序、微信公众号 | 社交登录 |

### 5.3 application-dev.yaml（开发/测试环境）

| 配置项 | 值 | 说明 |
|---|---|---|
| `server.port` | `48080` | 开发环境端口 |
| 数据源 | 本机 MySQL `yixiang-drink-open`，root/root | 与 local 相同 |
| Redis | `400-infra.server.yixiang.co:6379`，db 1 | 远程 Redis |
| Quartz | `auto-startup=true`、JDBC、集群 | 未排除自动配置，定时任务可用 |
| Spring Boot Admin | 客户端地址指向本机 `/admin` | 服务端上下文路径 `/admin` |
| 微信 | 公众号 / 小程序测试凭据 | 明文 |
| 业务开关 | `yshop.xss.enable=false`、`yshop.demo=true` | 演示模式开启 |
| 文件 / H5 | `yshop.file-path=/www/wwwroot/drink/file/`、`yshop.h5=https://www.yixiang.co/h5` | Linux 路径 |
| JustAuth | 与 local 基本一致 | 社交登录 |

### 5.4 Profile 差异速查

| 项 | local | dev |
|---|---|---|
| 端口 | 48081 | 48080 |
| Redis | 本机 6379 / db0 | 远程 / db1 |
| Quartz | 自动配置被排除（不启用） | 启用 |
| Spring Boot Admin | 自动配置被排除 | 启用 |
| 图形验证码 | 关闭 | 默认开启 |
| Mock 登录 | 开启（token=`test`+userId） | 关闭 |
| 演示模式 | 关闭 | 开启 |
| 文件路径 | macOS 路径 | Linux 路径 |

## 6. 启动流程

1. **加载配置**：`SpringApplication.run` 读取 `application.yaml`，默认激活 `local`，再叠加 `application-local.yaml`。
2. **组件扫描**：扫描 `co.yixiang.yshop.server` 与 `co.yixiang.yshop.module`，加载所有 `-biz` 中的 `@RestController`、`@Service`、`@Configuration`、`@Component`。
3. **技术组件自动配置**：`yshop-framework` 各 starter 通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册（Web、Security、MyBatis、Redis、Job、MQ、Monitor、Protection、Tenant、DataPermission、IP 等）。
4. **Web 路由前缀**：`YshopWebAutoConfiguration` 通过 `WebProperties` 为 `**.controller.admin.**` 包下的 Controller 加 `/admin-api`，为 `**.controller.app.**` 加 `/app-api`。
5. **安全链初始化**：
   - 构建无状态（STATELESS）过滤链，关闭 CSRF；
   - 扫描所有 `@PermitAll` 方法并放行；
   - 放行 `yshop.security.permit-all_urls` 与整个 `/app-api/**`；
   - 在 `UsernamePasswordAuthenticationFilter` 之前加入 `TokenAuthenticationFilter`；
   - 其余请求必须认证。
6. **Token 过滤器**：从 `Authorization`（或参数 `token`）取出访问令牌，调用 `OAuth2TokenApi.checkAccessToken`；令牌先查 Redis、再查 MySQL，校验有效期后构建 `LoginUser` 放入 SecurityContext。
7. **数据层初始化**：动态数据源（master/slave）+ Druid 连接池；MyBatis-Plus 设置逻辑删除、主键策略、`BaseDO` 自动填充；Mapper 由 `@MapperScan` 扫描。
8. **缓存 / Redis**：Spring Cache 使用 Redis，默认 TTL 1 小时；Redisson 用于分布式锁、延迟队列等。
9. **定时任务**：`yshop-spring-boot-starter-job` 的 `SchedulerManager` 依赖可选的 `Scheduler`；local 排除了 `QuartzAutoConfiguration`，因此 `SchedulerManager` 收到空对象，任务被禁用；dev 下使用 JDBC JobStore 集群模式。
10. **接口文档与 WebSocket**：SpringDoc / Knife4j 暴露 `/v3/api-docs`、`/swagger-ui`；WebSocket 注册在 `/infra/ws`。
11. **监控**：Actuator 暴露全部端点；dev 下启用 Spring Boot Admin Server（`/admin`）与 Client。
12. **启动完成**：内嵌 Tomcat 监听配置端口（local 48081 / dev 48080），日志输出 `Started YshopServerApplication`。

## 7. 打包与运行

### 7.1 Maven 构建

```bash
cd yshop-drink-boot3
mvn clean install package -Dmaven.test.skip=true
```

- `yshop-server` 的 `<finalName>` 为 `yshop-server`；
- `spring-boot-maven-plugin` 执行 `repackage`，生成：
  - `yshop-server/target/yshop-server.jar`：可执行 Fat Jar；
  - `yshop-server/target/yshop-server.jar.original`：原始普通 Jar；
- 首次只构建 server 时需加 `-am`，确保依赖模块已安装。

### 7.2 运行

```bash
# 默认 local，端口 48081
java -jar yshop-server/target/yshop-server.jar

# 指定 dev
java -jar yshop-server/target/yshop-server.jar --spring.profiles.active=dev
```

### 7.3 Docker

`yshop-server/Dockerfile`：

```dockerfile
FROM eclipse-temurin:21-jre
WORKDIR /yshop-server
COPY ./target/yshop-server.jar app.jar
ENV TZ=Asia/Shanghai
ENV JAVA_OPTS="-Xms512m -Xmx512m -Djava.security.egd=file:/dev/./urandom"
ENV ARGS=""
EXPOSE 48080
CMD java ${JAVA_OPTS} -jar app.jar $ARGS
```

### 7.4 数据库初始化

- 数据库名：`yixiang-drink-open`；
- 初始化脚本：`yshop-drink-boot3/sql/yixiang-drink-open.sql`（约 16 MB）；
- 脚本包含建表、字典、菜单、租户、OAuth2 默认客户端等种子数据；
- 默认 OAuth2 客户端：`client_id=default`、`client_secret=admin123`、授权类型 `password/authorization_code/implicit/refresh_token`。

## 8. 注意事项

1. **默认启动的是 `local`**，端口 48081；`dev` 才是 48080。Dockerfile 中 `EXPOSE 48080` 与默认 profile 不一致，容器运行时需显式指定 `--spring.profiles.active=dev` 或映射正确端口。
2. **JDK 版本不一致**：POM 指定 Java 17，`Dockerfile` 使用 `eclipse-temurin:21-jre`。项目说明明确要求 JDK 17（Lombok 1.18.30 在更高版本可能不兼容），生产镜像建议改为 17。
3. **`/app-api/**` 在 Spring Security 层整体放行**，用户端接口的登录校验依靠业务代码中的 `@PreAuthenticated` 或 Service 内判断；新增 App 接口时不能默认认为“已登录”。
4. **`local` profile 排除了 Quartz 自动配置**，虽然 `spring.quartz.auto-startup=true`，定时任务实际不会运行；需要测试 Job 时请使用 `dev` 或移除 `exclude`。
5. **`local` profile 排除了 Spring Boot Admin 自动配置**，`/admin` 监控台在本地不可用。
6. **配置文件中存在明文敏感信息**：微信 AppSecret、支付/快递密钥、JustAuth Secret、腾讯地图 Key、数据库密码等均直接提交在 YAML 中，生产环境应改为环境变量或配置中心。
7. **`yshop.file-path` 当前未被 Java 代码引用**（全局搜索仅出现在两个 YAML），且 local 使用 macOS 路径 `/Users/hupeng/drink/file/`，不要误以为它是本机文件存储目录；真正的文件存储配置在 `infra_file_config` 表中。
8. **`yshop.codegen.db-schemas` 引用 `spring.datasource.dynamic.datasource.master.name`**，但 YAML 的 master 数据源未显式配置 `name`；修改数据源配置时需留意代码生成器的库名解析。
9. **循环依赖被全局允许**（`allow-circular-references=true`），便于三层架构注入，但也会掩盖设计问题。
10. **`yshop-server` 未直接依赖 `member-biz`**，会员端接口通过 `message-biz` / `score-biz` 传递引入；清理依赖时需用 `mvn dependency:tree` 确认，避免接口凭空消失。
11. **新增模块接口**：除在根 POM 注册模块外，还要把对应 `-biz` 加入 `yshop-server/pom.xml`，否则 Controller 不会被扫描。
