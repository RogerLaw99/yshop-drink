# yshop-dependencies 依赖版本管理（BOM）

> 模块路径：`yshop-drink-boot3/yshop-dependencies`；文档依据 `yshop-dependencies/pom.xml`、根 `pom.xml` 及各子模块 POM 整理。

## 1. 模块定位

`yshop-dependencies` 是整个后端工程的 **BOM（Bill of Materials，物料清单）模块**：

- `packaging` 为 `pom`，没有 `src` 目录，不产出可执行代码；
- 只通过 `<dependencyManagement>` 统一声明依赖版本，不真正引入依赖；
- 业务模块（`yshop-module-*`、`yshop-framework/*`、`yshop-server`）通过根 `yshop` 的 BOM 导入获得版本约束，从而在声明依赖时可以省略 `<version>`；
- 同时负责声明工程内自研 starter 的坐标与版本，避免各模块重复写版本号。

Maven 坐标：

```xml
<groupId>co.yixiang.boot</groupId>
<artifactId>yshop-dependencies</artifactId>
<version>${revision}</version>
<packaging>pom</packaging>
```

其中 `revision` 默认为 `3.0.0`。

## 2. 模块结构

BOM 本身只有一个 `pom.xml`。它与根工程、子模块的关系如下：

```
yshop（根工程，packaging=pom，revision=3.0.0）
├── dependencyManagement
│   └── import co.yixiang.boot:yshop-dependencies:${revision}
├── build
│   └── flatten-maven-plugin（统一 ${revision}）
└── modules
    ├── yshop-dependencies        ← 本模块（BOM）
    ├── yshop-framework           ← 技术组件（14 个 starter）
    ├── yshop-server              ← 可执行启动模块
    └── yshop-module-*            ← 业务模块（多数再拆为 -api / -biz）
```

`yshop-dependencies` 不继承根 `yshop`，而是独立声明自己的 `revision` 属性；其余模块都继承根 `yshop`，因此能通过根工程导入 BOM。

## 3. 核心内容

### 3.1 Spring Boot 基础 BOM

BOM 首先导入 Spring Boot 官方依赖清单：

| 依赖 | 版本 | 作用 |
|---|---|---|
| `org.springframework.boot:spring-boot-dependencies` | `3.2.2` | 作为整个依赖体系的基础版本来源 |

Spring Boot BOM 负责 Spring Framework、Jackson、Tomcat、日志、测试等大量传递依赖的版本；本 BOM 在其之上再做显式覆盖或补充。

### 3.2 工程内自研组件

这些坐标的版本统一使用 `${revision}`：

| 依赖 | 说明 |
|---|---|
| `co.yixiang.boot:yshop-common` | 通用工具、`CommonResult`、`PageResult`、异常体系 |
| `yshop-spring-boot-starter-web` | API 前缀路由、全局异常、Swagger、XSS、访问日志 |
| `yshop-spring-boot-starter-security` | Spring Security、Token 过滤器、`@PreAuthenticated`、`@ss` |
| `yshop-spring-boot-starter-websocket` | WebSocket 支持 |
| `yshop-spring-boot-starter-mybatis` | `BaseDO`、`BaseMapperX`、查询包装器 |
| `yshop-spring-boot-starter-redis` | Redis / Redisson / Spring Cache 封装 |
| `yshop-spring-boot-starter-job` | Quartz 定时任务封装 |
| `yshop-spring-boot-starter-mq` | 消息队列抽象 |
| `yshop-spring-boot-starter-protection` | 分布式锁、幂等、限流等服务保障 |
| `yshop-spring-boot-starter-monitor` | Actuator / 监控 |
| `yshop-spring-boot-starter-excel` | EasyExcel 导入导出 |
| `yshop-spring-boot-starter-test` | 单元测试基座（scope=test） |
| `yshop-spring-boot-starter-biz-tenant` | 多租户 |
| `yshop-spring-boot-starter-biz-data-permission` | 数据权限 |
| `yshop-spring-boot-starter-biz-ip` | IP / 地区能力 |

### 3.3 第三方依赖版本清单

#### Web / 接口文档

| 依赖 | 版本 | 用途 |
|---|---|---|
| `org.springdoc:springdoc-openapi-starter-webmvc-api` | `2.2.0` | OpenAPI 文档 |
| `org.springdoc:springdoc-openapi-ui` | `2.2.0` | Swagger UI |
| `com.github.xiaoymin:knife4j-openapi3-jakarta-spring-boot-starter` | `4.3.0` | Knife4j 中文接口文档 |

#### 数据库 / ORM / 缓存

| 依赖 | 版本 | 用途 |
|---|---|---|
| `com.alibaba:druid-spring-boot-3-starter` | `1.2.21` | Druid 连接池与监控 |
| `com.baomidou:mybatis-plus-spring-boot3-starter` | `3.5.5` | MyBatis-Plus |
| `com.baomidou:mybatis-plus-generator` | `3.5.5` | 解析数据库表结构（代码生成器） |
| `com.baomidou:dynamic-datasource-spring-boot3-starter` | `4.3.0` | 多数据源 |
| `com.github.yulichang:mybatis-plus-join-boot-starter` | `1.4.10` | 联表查询 |
| `com.fhs-opensource:easy-trans-*` | `2.2.11` | VO 字段自动翻译 |
| `org.redisson:redisson-spring-boot-starter` | `3.26.0` | Redis 客户端、分布式能力 |
| `com.dameng:DmJdbcDriver18` | `8.1.3.62` | 达梦数据库驱动 |

#### 消息队列 / 定时任务 / 服务保障 / 监控

| 依赖 | 版本 | 用途 |
|---|---|---|
| `org.apache.rocketmq:rocketmq-spring-boot-starter` | `2.3.0` | RocketMQ |
| `com.baomidou:lock4j-redisson-spring-boot-starter` | `2.2.7` | 分布式锁 |
| `org.apache.skywalking:apm-toolkit-*` | `9.0.0` | 链路追踪 |
| `de.codecentric:spring-boot-admin-starter-server/client` | `3.2.1` | Spring Boot Admin 监控 |
| `io.opentracing:opentracing-*` | `0.33.0` | OpenTracing API |

#### 测试

| 依赖 | 版本 | 用途 |
|---|---|---|
| `org.springframework.boot:spring-boot-starter-test` | `3.2.2` | Spring Boot 测试 |
| `org.mockito:mockito-inline` | `5.2.0` | mock final 类 / 静态方法 |
| `com.github.fppt:jedis-mock` | `1.0.13` | 内嵌 Redis |
| `uk.co.jemos.podam:podam` | `8.0.1.RELEASE` | 随机生成 POJO |

#### 工作流

| 依赖 | 版本 | 用途 |
|---|---|---|
| `org.flowable:flowable-spring-boot-starter-process` | `7.0.1` | Flowable 流程引擎 |
| `org.flowable:flowable-spring-boot-starter-actuator` | `7.0.1` | Flowable 监控 |

#### 工具 / 文件 / 办公

| 依赖 | 版本 | 用途 |
|---|---|---|
| `org.projectlombok:lombok` | `1.18.30` | 简化 POJO |
| `org.mapstruct:mapstruct` / `mapstruct-processor` / `mapstruct-jdk8` | `1.5.5.Final` | VO / DO 转换 |
| `cn.hutool:hutool-all` | `5.8.25` | Hutool 5 工具集 |
| `org.dromara.hutool:hutool-extra` | `6.0.0-M10` | Hutool 6 扩展（邮件等） |
| `com.alibaba:easyexcel` | `3.3.3` | Excel 导入导出 |
| `commons-io:commons-io` | `2.15.1` | IO 工具 |
| `org.apache.tika:tika-core` | `2.9.1` | 文件类型识别 |
| `org.apache.velocity:velocity-engine-core` | `2.3` | 代码生成模板引擎 |
| `com.alibaba:fastjson` | `1.2.83` | JSON（历史组件） |
| `com.google.guava:guava` | `33.0.0-jre` | 本地缓存、集合工具 |
| `com.google.inject:guice` | `5.1.0` | 依赖注入 |
| `com.alibaba:transmittable-thread-local` | `2.14.5` | 线程上下文传递 |
| `commons-net:commons-net` | `3.10.0` | FTP |
| `com.jcraft:jsch` | `0.1.55` | SFTP |
| `com.xingyuv:spring-boot-starter-captcha-plus` | `2.0.3` | 图形验证码 |
| `org.lionsoul:ip2region` | `2.7.0` | IP 归属地 |
| `org.jsoup:jsoup` | `1.17.2` | XSS 清洗 / HTML 解析 |
| `io.github.mouzt:bizlog-sdk` | `3.0.6` | 操作日志 |

#### 三方云服务 / 社交 / 支付

| 依赖 | 版本 | 用途 |
|---|---|---|
| `com.squareup.okio:okio` | `3.5.0` | OkHttp 依赖 |
| `com.squareup.okhttp3:okhttp` | `4.11.0` | HTTP 客户端 |
| `io.minio:minio` | `8.5.7` | S3 / MinIO 对象存储 |
| `com.aliyun:aliyun-java-sdk-core` | `4.6.4` | 阿里云 SDK |
| `com.aliyun:aliyun-java-sdk-dysmsapi` | `2.2.1` | 阿里云短信 |
| `com.tencentcloudapi:tencentcloud-sdk-java-sms` | `3.1.880` | 腾讯云短信 |
| `com.xingyuv:spring-boot-starter-justauth` | `2.0.5` | 社交登录 |
| `com.github.binarywang:weixin-java-pay` | `4.6.0` | 微信支付 |
| `com.github.binarywang:wx-java-mp-spring-boot-starter` | `4.6.0` | 微信公众号 |
| `com.github.binarywang:wx-java-miniapp-spring-boot-starter` | `4.6.0` | 微信小程序 |
| `org.jeecgframework.jimureport:jimureport-spring-boot3-starter` | `1.6.6-beta2` | 积木报表 |
| `xerces:xercesImpl` | `2.12.2` | XML 解析 |
| `com.egzosn:pay-spring-boot-starter` | `1.0.5` | 支付抽象 |
| `com.egzosn:pay-java-ali` / `pay-java-wx` / `pay-java-web-support` | `2.14.9` | 支付宝 / 微信支付实现 |
| `com.google.zxing:core` | `3.3.3` | 二维码 |

### 3.4 版本属性一览

BOM 内集中定义了以下属性（节选）：

```
revision=3.0.0
spring.boot.version=3.2.2
springdoc.version=2.2.0          knife4j.version=4.3.0
druid.version=1.2.21             mybatis-plus.version=3.5.5
dynamic-datasource.version=4.3.0 mybatis-plus-join.version=1.4.10
easy-trans.version=2.2.11        redisson.version=3.26.0
rocketmq-spring.version=2.3.0    lock4j.version=2.2.7
skywalking.version=9.0.0         spring-boot-admin.version=3.2.1
flowable.version=7.0.1           captcha-plus.version=2.0.3
lombok.version=1.18.30           mapstruct.version=1.5.5.Final
hutool-5.version=5.8.25          hutool-6.version=6.0.0-M10
easyexcel.verion=3.3.3           velocity.version=2.3
fastjson.version=1.2.83          guava.version=33.0.0-jre
minio.version=8.5.7              weixin-java.version=4.6.0
jimureport.version=1.6.6-beta2   pay.version=2.14.9
zxing.version=3.3.3
```

## 4. 版本策略

### 4.1 CI-friendly 版本号与 flatten

- 根工程与 BOM 都使用 Maven 的 CI-friendly 占位符 `${revision}`，默认值 `3.0.0`；
- 根 `pom.xml` 配置 `flatten-maven-plugin`（`flattenMode=resolveCiFriendliesOnly`、`updatePomFile=true`），在 `process-resources` 阶段生成 `.flattened-pom.xml`，在 `clean` 阶段清理；
- 安装 / 发布时使用展开后的 POM，避免下游拿到无法解析的 `${revision}`；
- 仓库中可见多个 `.flattened-pom.xml` 文件。

### 4.2 BOM 导入链

```
子模块依赖版本
   ↑
根 yshop 的 dependencyManagement
   ↑ import
yshop-dependencies 的 dependencyManagement
   ↑ import
spring-boot-dependencies
```

子模块继承根 `yshop`，因此：

- 由 BOM 管理的第三方依赖，子模块 **不写版本**；
- 工程内自研模块仍常写 `<version>${revision}</version>`（虽然 BOM 也管理了它们）；
- Spring Boot 官方依赖由导入的 BOM 决定。

### 4.3 编译插件策略

根 `pom.xml` 的 `maven-compiler-plugin` 统一配置了注解处理器路径：

- `spring-boot-configuration-processor`
- `lombok`
- `mapstruct-processor`

并开启 `-parameters`，以解决 Spring Boot 3.2 的参数名发现（Parameter Name Discovery）问题。

### 4.4 仓库与镜像

根 POM 配置了华为云与阿里云 Maven 镜像：

```xml
<repository><id>huaweicloud</id><url>https://mirrors.huaweicloud.com/repository/maven/</url></repository>
<repository><id>aliyunmaven</id><url>https://maven.aliyun.com/repository/public</url></repository>
```

它们只影响下载源，不改变版本解析规则。

## 5. 如何被引用

### 5.1 根工程导入 BOM

`yshop-drink-boot3/pom.xml`：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>co.yixiang.boot</groupId>
            <artifactId>yshop-dependencies</artifactId>
            <version>${revision}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 5.2 子模块引用依赖

例如 `yshop-module-infra/yshop-module-infra-biz/pom.xml`：

```xml
<dependency>
    <groupId>co.yixiang.boot</groupId>
    <artifactId>yshop-spring-boot-starter-mybatis</artifactId>
</dependency>
<dependency>
    <groupId>org.apache.velocity</groupId>
    <artifactId>velocity-engine-core</artifactId>
</dependency>
```

以上均未写版本，版本来自 BOM。

引用工程内业务模块时通常显式写 `${revision}`：

```xml
<dependency>
    <groupId>co.yixiang.boot</groupId>
    <artifactId>yshop-module-system-api</artifactId>
    <version>${revision}</version>
</dependency>
```

`yshop-server/pom.xml` 直接引入需要对外暴露 REST API 的 `-biz` 模块，同样使用 `${revision}`。

## 6. 构建与发布

```bash
# 在 yshop-drink-boot3 下
mvn clean install -Dmaven.test.skip=true
```

- 先构建根与 BOM，再构建 framework 与各 module；
- `flatten` 会为每个模块生成 `.flattened-pom.xml`；
- 若只构建单个模块，首次需加 `-am` 一并构建依赖模块；
- 发布到私服时应发布根 POM、BOM 以及所有被依赖的 module 制品。

## 7. 注意事项

1. **版本属性重复定义**：根 `pom.xml` 与 BOM 都定义了 `spring.boot.version`、`lombok.version`、`mapstruct.version` 等。编译插件读取根属性，实际依赖版本读取 BOM 属性；两者若不一致会产生“编译用 A、运行时用 B”的隐性风险，升级时需同步修改。
2. **BOM 中声明但本仓库未使用的依赖**：如 Flowable、积木报表、`pay-java-*`、`weixin-java-pay`、`screw.version`（仅属性、无依赖声明）等，多为上游脚手架保留，会增大依赖管理面。
3. **Hutool 5 与 Hutool 6 并存**：`cn.hutool:hutool-all:5.8.25` 与 `org.dromara.hutool:hutool-extra:6.0.0-M10` 同时存在，包名不同，使用时需注意导入来源。
4. **Fastjson 1.2.83 属历史版本**，存在已知安全风险；新代码应优先使用 Jackson / Hutool JSON。
5. **BOM 未显式管理 MySQL 驱动**：MySQL Connector/J 的版本由 Spring Boot BOM 决定。
6. **`yshop-spring-boot-starter-test` 在 BOM 中声明了 `scope=test`**，子模块引用时无需再写 scope，但该 scope 会随 dependencyManagement 生效。
7. **没有 Maven Enforcer**：工程要求 JDK 17，但构建阶段未强制校验 JDK 版本；在 JDK 21/24 下使用 Lombok 1.18.30 可能出现兼容问题。
8. **`revision` 必须可解析**：独立使用 BOM 时它自带 `revision`；其他模块依赖父工程继承。单独拷贝某个子模块 POM 构建时，需保证父工程 / BOM 可被解析。
