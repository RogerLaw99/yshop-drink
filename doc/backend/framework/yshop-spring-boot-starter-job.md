# yshop-spring-boot-starter-job

> 所属：yshop-framework；文档依据本仓库源码整理。

## 1. 模块定位

本模块封装两类任务能力：

1. **定时任务**：基于 Quartz，统一以 `JobHandler` Bean 作为任务执行入口，通过 `SchedulerManager` 对 Quartz 的 `JobDetail/Trigger` 做增删改查、暂停、恢复与立即触发。
2. **异步任务**：通过 `@EnableAsync` 开启 Spring 异步，并自动为容器中的 `ThreadPoolTaskExecutor` 设置 `TtlRunnable` 装饰器，使 `TransmittableThreadLocal`（登录用户、租户、数据权限上下文等）能够跨线程传递。

任务日志通过 `JobLogFrameworkService` 抽象，具体持久化由业务模块（infra）实现。

### 在 yshop-server 中的依赖与启用状态

- `yshop-server/pom.xml` **没有直接声明**本模块。
- 通过 `yshop-module-infra-biz`、`yshop-module-system-biz`、`yshop-module-store-biz`、`yshop-module-coupon-biz` 传递引入。
- 两个自动配置类均通过 `AutoConfiguration.imports` 注册，启动时生效。
- **Quartz 调度器在默认 `local` 环境被关闭**：`application-local.yaml` 中

  ```yaml
  spring:
    autoconfigure:
      exclude:
        - org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration
  ```

  导致容器中没有 `Scheduler` Bean。`YshopQuartzAutoConfiguration` 使用 `Optional<Scheduler>` 容错，此时会创建 `SchedulerManager(null)` 并打印 `[定时任务 - 已禁用]`；任何调度操作都会抛出 `NOT_IMPLEMENTED`。
- `@EnableScheduling` 与 `@EnableAsync` 不受上述排除影响，仍然生效（例如 Redis MQ 的补发任务依赖 `@Scheduled`）。
- `dev` 环境未排除 `QuartzAutoConfiguration`，Quartz 按 `spring.quartz.*` 配置启用（JDBC JobStore）。
- `infra` 模块已提供 `JobLogServiceImpl`，其接口 `JobLogService` 继承 `JobLogFrameworkService`，因此任务日志能力可用。

## 2. 目录与包结构

```text
yshop-spring-boot-starter-job/
├── pom.xml
└── src/main/
    ├── java/co/yixiang/yshop/framework/quartz/
    │   ├── config/
    │   │   ├── YshopAsyncAutoConfiguration.java
    │   │   └── YshopQuartzAutoConfiguration.java
    │   └── core/
    │       ├── enums/JobDataKeyEnum.java
    │       ├── handler/
    │       │   ├── JobHandler.java
    │       │   └── JobHandlerInvoker.java
    │       ├── scheduler/SchedulerManager.java
    │       ├── service/JobLogFrameworkService.java
    │       └── util/CronUtils.java
    └── resources/META-INF/spring/
        └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

## 3. 核心类与职责

| 类/接口 | 所在包 | 职责 |
|---|---|---|
| `YshopQuartzAutoConfiguration` | `...quartz.config` | 开启 `@EnableScheduling`，创建 `SchedulerManager`；`Scheduler` 不存在时降级 |
| `YshopAsyncAutoConfiguration` | `...quartz.config` | 开启 `@EnableAsync`；为所有 `ThreadPoolTaskExecutor` 设置 `TtlRunnable::get` 任务装饰器 |
| `JobDataKeyEnum` | `...quartz.core.enums` | Quartz `JobDataMap` 的 key：任务编号、处理器名、参数、最大重试次数、重试间隔 |
| `JobHandler` | `...quartz.core.handler` | 业务任务统一接口：`String execute(String param)` |
| `JobHandlerInvoker` | `...quartz.core.handler` | `QuartzJobBean` 实现，负责取参数、调用 Handler、记录日志、失败重试 |
| `SchedulerManager` | `...quartz.core.scheduler` | 封装 Quartz Scheduler 的 add/update/delete/pause/resume/trigger |
| `JobLogFrameworkService` | `...quartz.core.service` | 任务日志创建与异步结果更新接口 |
| `CronUtils` | `...quartz.core.util` | 校验 CRON 表达式、计算接下来 n 次执行时间 |

## 4. 自动配置与生效方式

`AutoConfiguration.imports`：

```text
co.yixiang.yshop.framework.quartz.config.YshopQuartzAutoConfiguration
co.yixiang.yshop.framework.quartz.config.YshopAsyncAutoConfiguration
```

- `YshopQuartzAutoConfiguration`
  - `@AutoConfiguration` + `@EnableScheduling`；
  - `schedulerManager(Optional<Scheduler> scheduler)`：有 `Scheduler` 则包装，否则 `new SchedulerManager(null)` 并记录 `[定时任务 - 已禁用]`。
  - 没有模块自定义开关；是否可用完全取决于 Spring Boot `QuartzAutoConfiguration` 是否被排除/是否具备数据源。
- `YshopAsyncAutoConfiguration`
  - `@AutoConfiguration` + `@EnableAsync`；
  - 注册 `BeanPostProcessor`，在 `ThreadPoolTaskExecutor` 初始化前设置 `taskDecorator = TtlRunnable::get`，从而传递 TTL 上下文。
- `JobHandlerInvoker` 通过 `ApplicationContext.getBean(jobHandlerName, JobHandler.class)` 按 **Bean 名称**查找处理器；`SchedulerManager` 也以 `jobHandlerName` 作为 JobKey 与 TriggerKey。

## 5. 关键配置项

本模块**没有定义自己的 `@ConfigurationProperties`**。实际生效的是 Spring Boot Quartz 的标准配置，本仓库在 `application-local.yaml` / `application-dev.yaml` 中使用如下键：

| 配置项 | 本仓库取值 | 说明 |
|---|---|---|
| `spring.quartz.auto-startup` | `true` | 是否随应用自动启动 Scheduler |
| `spring.quartz.scheduler-name` | `schedulerName` | Scheduler 名称 |
| `spring.quartz.job-store-type` | `jdbc` | 使用 JDBC JobStore（集群共享任务） |
| `spring.quartz.wait-for-jobs-to-complete-on-shutdown` | `true` | 关闭时等待任务完成 |
| `spring.quartz.properties.org.quartz.jobStore.isClustered` | `true` | 集群模式 |
| `spring.quartz.properties.org.quartz.jobStore.class` | `LocalDataSourceJobStore` | JobStore 实现 |
| `spring.quartz.properties.org.quartz.threadPool.threadCount` | `25` | 执行线程数 |
| `spring.quartz.jdbc.initialize-schema` | `NEVER` | 不自动建表，需手工初始化 Quartz 表 |
| `spring.autoconfigure.exclude` | `local` 环境包含 `QuartzAutoConfiguration` | 排除后无 `Scheduler`，任务调度整体禁用 |

## 6. 使用方式 / 典型用法

### 6.1 编写任务处理器

```java
@Component
public class DemoJob implements JobHandler {

    @Override
    public String execute(String param) {
        // 业务逻辑
        return "执行成功";
    }
}
```

Bean 名称（默认类名首字母小写，如 `demoJob`）就是管理后台登记的 `handlerName`。

### 6.2 管理任务

`infra` 模块的 `JobServiceImpl` 注入 `SchedulerManager`，在新增/修改/删除任务时同步操作 Quartz：

```java
schedulerManager.addJob(jobId, handlerName, handlerParam, cronExpression, retryCount, retryInterval);
schedulerManager.updateJob(handlerName, handlerParam, cronExpression, retryCount, retryInterval);
schedulerManager.deleteJob(handlerName);
schedulerManager.pauseJob(handlerName);
schedulerManager.resumeJob(handlerName);
schedulerManager.triggerJob(jobId, handlerName, handlerParam); // 立即执行一次
```

### 6.3 多租户任务

```java
@TenantJob
public String execute(String param) { /* 每个租户执行一次 */ }
```

### 6.4 CRON 工具

```java
boolean ok = CronUtils.isValid("0 0/5 * * * ?");
List<LocalDateTime> next = CronUtils.getNextTimes("0 0/5 * * * ?", 5);
```

## 7. 依赖关系

模块自身依赖：

- `yshop-common`
- `spring-boot-starter-quartz`
- `jakarta.validation-api`（`JobLogFrameworkService` 参数校验注解）
- 隐式依赖：Hutool、Spring Context（由 `yshop-common` 传递）

上游使用者：

- `yshop-module-infra-biz`：任务管理、任务日志、内置清理任务；
- `yshop-module-system-biz`：`DemoJob`；
- `yshop-module-store-biz`、`yshop-module-coupon-biz` 等；
- `yshop-spring-boot-starter-biz-tenant` 依赖它实现 `TenantJobAspect`；
- `yshop-server` 通过业务模块传递引入。

## 8. 注意事项

1. **local 环境默认不执行 Quartz 调度**：`QuartzAutoConfiguration` 被排除，`SchedulerManager` 会降级为 `null`。管理后台仍可查询/编辑 `infra_job` 数据，但调度动作会抛出 `[定时任务 - 已禁用]` 的 `NOT_IMPLEMENTED` 异常。
2. `spring.quartz.jdbc.initialize-schema: NEVER` 表示不会自动创建 `QRTZ_*` 表；切换 `job-store-type=jdbc` 前必须保证数据库已初始化 Quartz 表。
3. `JobHandler` 通过 Bean 名称解析，重命名 Bean 或使用自定义 Bean 名会导致已有任务找不到处理器。
4. `JobHandlerInvoker` 标注 `@DisallowConcurrentExecution`，同一 JobDetail 不会并发执行；同时 `@PersistJobDataAfterExecution` 会持久化 JobData。
5. 失败重试逻辑：`refireCount >= retryCount` 时抛出异常终止；否则可 sleep `retryInterval` 后立即 refire。重试次数与间隔来自 JobData，管理后台可配置。
6. 任务日志为**异步更新**（`updateJobLogResultAsync`），日志更新失败只记录错误，不影响任务结果。
7. `JobLogFrameworkService` 是框架接口，业务侧必须提供实现（本仓库由 `infra` 的 `JobLogServiceImpl` 通过 `JobLogService` 间接实现）；否则 `JobHandlerInvoker` 注入失败。
8. `YshopAsyncAutoConfiguration` 会修改**容器中所有** `ThreadPoolTaskExecutor` 的 `taskDecorator`，包括业务自定义线程池；若业务已有装饰器，会被覆盖。
9. `@EnableScheduling` 由本模块开启，Redis MQ 的 `RedisPendingMessageResendJob` 等 `@Scheduled` 方法依赖它；即使 Quartz 被禁用，Spring 的 `@Scheduled` 仍会运行。
