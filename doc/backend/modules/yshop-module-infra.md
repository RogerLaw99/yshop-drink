# yshop-module-infra 基础设施模块

> 模块路径：`yshop-drink-boot3/yshop-module-infra`；文档依据本仓库源码整理（`pom.xml`、`src/main/java`、`controller`、`service`、`dal/dataobject`、`application*.yaml`）。

## 1. 模块定位

`yshop-module-infra` 是后端的**基础设施与研发效率模块**，为上层业务提供两类能力：

1. **基础设施运维**
   - 文件存储（本地磁盘、数据库、FTP、SFTP、S3/MinIO 等）；
   - 文件配置与主配置管理；
   - 参数配置（`infra_config`）；
   - 动态数据源配置；
   - Quartz 定时任务与任务日志；
   - API 访问日志、API 错误日志；
   - Redis 监控信息；
   - Actuator / Spring Boot Admin 健康检查与监控。
2. **研发工具**
   - 数据库表导入、代码生成、预览与下载；
   - Demo01 / Demo02 / Demo03 示例工程（单表、树表、主子表），同时作为代码生成模板的验证样例。

对外通过 `-api` 暴露 `FileApi`、`ApiAccessLogApi`、`ApiErrorLogApi`、`WebSocketSenderApi`。

## 2. 目录结构（api / biz 分层）

```
yshop-module-infra/
├── pom.xml                          # 聚合模块（packaging=pom）
├── yshop-module-infra-api/          # 对外 API、DTO、枚举
│   └── src/main/java/co/yixiang/yshop/module/infra/
│       ├── api/
│       │   ├── file/FileApi.java
│       │   ├── logger/ApiAccessLogApi.java、ApiErrorLogApi.java
│       │   └── websocket/WebSocketSenderApi.java
│       ├── enums/DictTypeConstants.java、ErrorCodeConstants.java
└── yshop-module-infra-biz/          # 业务实现
    └── src/main/java/co/yixiang/yshop/module/infra/
        ├── api/                     # FileApiImpl、日志 API 实现、WebSocket API 实现
        ├── controller/
        │   ├── admin/               # codegen、config、db、demo、file、job、logger、redis
        │   └── app/file/            # App 文件上传
        ├── convert/                 # MapStruct 转换器
        ├── dal/
        │   ├── dataobject/          # DO
        │   └── mysql/               # Mapper
        ├── enums/                   # codegen、config、job、logger 枚举
        ├── framework/
        │   ├── codegen/             # CodegenProperties、CodegenConfiguration
        │   ├── file/                # 文件客户端体系（core/client）
        │   ├── monitor/             # Spring Boot Admin Server
        │   ├── security/            # 模块级安全配置
        │   └── web/                 # Swagger 分组
        ├── job/                     # 内置清理 Job
        ├── service/                 # codegen、config、db、demo、file、job、logger
        └── websocket/               # Demo WebSocket 消息
```

`-api` 仅依赖 `yshop-common` 与参数校验；`-biz` 依赖 `system-api`、`infra-api` 及 mybatis / redis / job / mq / excel / websocket / monitor 等 starter。

## 3. 核心功能

| 功能 | 入口类 | 说明 |
|---|---|---|
| 文件上传 / 下载 | `FileController`、`AppFileController`、`FileService(Impl)` | 后端上传、预签名直传、下载、删除、分页 |
| 文件配置 | `FileConfigController`、`FileConfigService(Impl)` | 多存储配置、主配置切换、连接测试、客户端缓存 |
| 文件客户端 | `FileClientFactory(Impl)`、`DB/Local/FTP/SFTP/S3 FileClient` | 统一上传/下载/删除接口，按 `storage` 反射实例化 |
| 代码生成 | `CodegenController`、`CodegenService(Impl)`、`CodegenBuilder`、`CodegenEngine` | 表结构导入、字段配置、同步、预览、下载 |
| 参数配置 | `ConfigController`、`ConfigService(Impl)` | 键值配置、可见性、内置配置保护 |
| 数据源配置 | `DataSourceConfigController`、`DataSourceConfigService(Impl)` | 动态数据源管理，连接可用性校验 |
| 定时任务 | `JobController`、`JobService(Impl)`、`SchedulerManager` | Quartz 任务的创建、暂停/恢复、触发、同步 |
| 任务日志 | `JobLogController`、`JobLogService(Impl)` | 执行日志记录、分页、导出、清理 |
| API 访问日志 | `ApiAccessLogController`、`ApiAccessLogFilter` | 记录请求参数、耗时、结果、操作模块 |
| API 错误日志 | `ApiErrorLogController`、`GlobalExceptionHandler` | 记录异常堆栈、处理状态 |
| Redis 监控 | `RedisController` | `INFO`、`DBSIZE`、命令统计 |
| 健康检查 / 监控 | Actuator、`AdminServerConfiguration` | `/actuator/*`、Spring Boot Admin `/admin` |
| 示例工程 | `Demo01ContactController`、`Demo02CategoryController`、`Demo03StudentController` | 单表、树表、主子表代码生成样例 |

## 4. 主要接口

> 实际路径 = `/admin-api` + `@RequestMapping` 相对路径；App 接口前缀为 `/app-api`。

### 4.1 管理端 /admin-api

#### 4.1.1 文件与文件配置

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| POST | `/infra/file/upload` | 上传文件（后端中转） | 登录即可 |
| GET | `/infra/file/presigned-url` | 获得前端直传预签名地址 | 登录即可 |
| POST | `/infra/file/create` | 前端直传后登记文件记录 | 登录即可 |
| DELETE | `/infra/file/delete` | 删除文件（存储 + 记录） | `infra:file:delete` |
| GET | `/infra/file/{configId}/get/**` | 下载文件 | 无需登录（`@PermitAll`） |
| GET | `/infra/file/page` | 文件分页 | `infra:file:query` |
| POST | `/infra/file-config/create` | 创建文件配置 | `infra:file-config:create` |
| PUT | `/infra/file-config/update` | 更新文件配置 | `infra:file-config:update` |
| PUT | `/infra/file-config/update-master` | 设为 Master 配置 | `infra:file-config:update` |
| DELETE | `/infra/file-config/delete` | 删除文件配置（Master 不可删） | `infra:file-config:delete` |
| GET | `/infra/file-config/get` | 获得文件配置 | `infra:file-config:query` |
| GET | `/infra/file-config/page` | 文件配置分页 | `infra:file-config:query` |
| GET | `/infra/file-config/test` | 测试文件配置（上传二维码） | `infra:file-config:query` |

#### 4.1.2 代码生成

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/infra/codegen/db/table/list` | 数据库表列表（过滤已导入表） | `infra:codegen:query` |
| GET | `/infra/codegen/table/list` | 已导入表定义列表 | `infra:codegen:query` |
| GET | `/infra/codegen/table/page` | 已导入表定义分页 | `infra:codegen:query` |
| GET | `/infra/codegen/detail` | 表与字段明细 | `infra:codegen:query` |
| POST | `/infra/codegen/create-list` | 从数据库表创建生成器定义 | `infra:codegen:create` |
| PUT | `/infra/codegen/update` | 更新表与字段定义 | `infra:codegen:update` |
| PUT | `/infra/codegen/sync-from-db` | 从数据库同步表结构 | `infra:codegen:update` |
| DELETE | `/infra/codegen/delete` | 删除生成器定义 | `infra:codegen:delete` |
| GET | `/infra/codegen/preview` | 预览生成代码 | `infra:codegen:preview` |
| GET | `/infra/codegen/download` | 下载生成代码（zip） | `infra:codegen:download` |

#### 4.1.3 参数配置与数据源

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| POST | `/infra/config/create` | 创建参数配置 | `infra:config:create` |
| PUT | `/infra/config/update` | 修改参数配置 | `infra:config:update` |
| DELETE | `/infra/config/delete` | 删除参数配置（系统内置不可删） | `infra:config:delete` |
| GET | `/infra/config/get` | 获得参数配置 | `infra:config:query` |
| GET | `/infra/config/get-value-by-key` | 按键名查询值（不可见配置拒绝返回） | 登录即可 |
| GET | `/infra/config/page` | 参数配置分页 | `infra:config:query` |
| GET | `/infra/config/export` | 导出参数配置 | `infra:config:export` |
| POST | `/infra/data-source-config/create` | 创建数据源配置 | `infra:data-source-config:create` |
| PUT | `/infra/data-source-config/update` | 更新数据源配置 | `infra:data-source-config:update` |
| DELETE | `/infra/data-source-config/delete` | 删除数据源配置 | `infra:data-source-config:delete` |
| GET | `/infra/data-source-config/get` | 获得数据源配置（id=0 返回 master） | `infra:data-source-config:query` |
| GET | `/infra/data-source-config/list` | 数据源列表（含 master） | `infra:data-source-config:query` |

#### 4.1.4 定时任务与任务日志

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| POST | `/infra/job/create` | 创建定时任务 | `infra:job:create` |
| PUT | `/infra/job/update` | 更新定时任务（仅 NORMAL 状态） | `infra:job:update` |
| PUT | `/infra/job/update-status` | 开启 / 暂停任务 | `infra:job:update` |
| DELETE | `/infra/job/delete` | 删除定时任务 | `infra:job:delete` |
| PUT | `/infra/job/trigger` | 立即触发一次 | `infra:job:trigger` |
| POST | `/infra/job/sync` | 同步数据库任务到 Quartz | `infra:job:create` |
| GET | `/infra/job/get` | 获得定时任务 | `infra:job:query` |
| GET | `/infra/job/page` | 定时任务分页 | `infra:job:query` |
| GET | `/infra/job/export-excel` | 导出定时任务 | `infra:job:export` |
| GET | `/infra/job/get_next_times` | 获得下 N 次执行时间 | `infra:job:query` |
| GET | `/infra/job-log/get` | 获得任务日志 | `infra:job:query` |
| GET | `/infra/job-log/page` | 任务日志分页 | `infra:job:query` |
| GET | `/infra/job-log/export-excel` | 导出任务日志 | `infra:job:export` |

#### 4.1.5 日志、Redis 与示例

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/infra/api-access-log/page` | API 访问日志分页 | `infra:api-access-log:query` |
| GET | `/infra/api-access-log/export-excel` | 导出访问日志 | `infra:api-access-log:export` |
| PUT | `/infra/api-error-log/update-status` | 更新错误日志处理状态 | `infra:api-error-log:update-status` |
| GET | `/infra/api-error-log/page` | API 错误日志分页 | `infra:api-error-log:query` |
| GET | `/infra/api-error-log/export-excel` | 导出错误日志 | `infra:api-error-log:export` |
| GET | `/infra/redis/get-monitor-info` | Redis 监控信息 | `infra:redis:get-monitor-info` |
| POST | `/infra/demo01-contact/create` | 创建示例联系人 | `infra:demo01-contact:create` |
| PUT | `/infra/demo01-contact/update` | 更新示例联系人 | `infra:demo01-contact:update` |
| DELETE | `/infra/demo01-contact/delete` | 删除示例联系人 | `infra:demo01-contact:delete` |
| GET | `/infra/demo01-contact/get` | 获得示例联系人 | `infra:demo01-contact:query` |
| GET | `/infra/demo01-contact/page` | 示例联系人分页 | `infra:demo01-contact:query` |
| GET | `/infra/demo01-contact/export-excel` | 导出示例联系人 | `infra:demo01-contact:export` |
| POST | `/infra/demo02-category/create` | 创建示例分类 | `infra:demo02-category:create` |
| PUT | `/infra/demo02-category/update` | 更新示例分类 | `infra:demo02-category:update` |
| DELETE | `/infra/demo02-category/delete` | 删除示例分类 | `infra:demo02-category:delete` |
| GET | `/infra/demo02-category/get` | 获得示例分类 | `infra:demo02-category:query` |
| GET | `/infra/demo02-category/list` | 示例分类列表 | `infra:demo02-category:query` |
| GET | `/infra/demo02-category/export-excel` | 导出示例分类 | `infra:demo02-category:export` |
| POST | `/infra/demo03-student/create` | 创建示例学生 | `infra:demo03-student:create` |
| PUT | `/infra/demo03-student/update` | 更新示例学生 | `infra:demo03-student:update` |
| DELETE | `/infra/demo03-student/delete` | 删除示例学生 | `infra:demo03-student:delete` |
| GET | `/infra/demo03-student/get` | 获得示例学生 | `infra:demo03-student:query` |
| GET | `/infra/demo03-student/page` | 示例学生分页 | `infra:demo03-student:query` |
| GET | `/infra/demo03-student/export-excel` | 导出示例学生 | `infra:demo03-student:export` |
| GET/POST/PUT/DELETE | `/infra/demo03-student/demo03-course/*` | 学生课程子表 CRUD / 分页 / 列表 | `infra:demo03-student:*` |
| GET/POST/PUT/DELETE | `/infra/demo03-student/demo03-grade/*` | 学生班级子表 CRUD / 分页 | `infra:demo03-student:*` |

### 4.2 用户端 /app-api

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| POST | `/app-api/infra/file/upload` | App 上传文件 | 公开（`/app-api/**` 默认放行） |

> App 文件上传没有 `@PreAuthenticated`，也没有 `@PreAuthorize`，任何能访问后端的客户端都可以调用；如需限制，应在网关或业务层补充校验。

## 5. 核心实现逻辑

### 5.1 文件上传（后端中转）

入口：`FileController#uploadFile` / `AppFileController#uploadFile` → `FileServiceImpl#createFile(name, path, content)`。

1. `FileTypeUtils.getMineType(content, name)` 使用 Apache Tika 识别 MIME 类型；
2. 若未指定 `path`，`FileUtils.generatePath` 按日期 / 哈希生成存储路径；
3. `FileConfigService#getMasterFileClient()` 获取主配置对应的 `FileClient`；
4. `FileClientFactoryImpl`：
   - 以 `configId` 为 key 缓存客户端；
   - 首次根据 `FileStorageEnum` 反射实例化客户端并 `init()`；
   - 配置更新时 `refresh(config)`；
5. `client.upload(content, path, type)` 返回可访问 URL；
6. 写入 `infra_file`：`config_id`、`name`、`path`、`url`、`type`、`size`；
7. 返回 URL。

支持的存储器（`FileStorageEnum`）：

| storage | 实现 | 配置类 | 说明 |
|---|---|---|---|
| 1 | `DBFileClient` | `DBFileClientConfig` | 内容存入 `infra_file_content` |
| 10 | `LocalFileClient` | `LocalFileClientConfig` | 本地磁盘 |
| 11 | `FtpFileClient` | `FtpFileClientConfig` | FTP |
| 12 | `SftpFileClient` | `SftpFileClientConfig` | SFTP |
| 20 | `S3FileClient` | `S3FileClientConfig` | S3 / MinIO / 阿里云 OSS / 腾讯云 COS 等 |

### 5.2 文件配置与客户端缓存

`FileConfigServiceImpl`：

- `infra_file_config.config` 以 JSON 保存具体存储参数，解析时按 `storage` 转为对应 `FileClientConfig` 并做 Bean Validation；
- 通过 Guava `LoadingCache` 缓存 `FileClient`，刷新周期 10 秒；
- 特殊 key `0` 表示“主配置”（`master=true`），上传默认使用主配置；
- `update-master` 会先把所有配置置为非 master，再设置目标配置；
- Master 配置不允许删除；
- `test` 接口读取 classpath 中的二维码图片并上传，用于验证配置连通性。

### 5.3 文件下载、删除与前端直传

- **下载**：`GET /infra/file/{configId}/get/**`，从 URI 中截取 `/get/` 之后的路径，URL 解码后按 `configId` 取客户端内容，以附件形式写回响应；文件不存在返回 404。
- **删除**：`FileService#deleteFile` 先按 id 查记录，再调用对应客户端删除存储对象，最后删除数据库记录。
- **前端直传**：
  1. `GET /presigned-url` 获取对象存储预签名地址；
  2. 前端直接上传到对象存储；
  3. `POST /create` 登记文件元数据。

### 5.4 代码生成

入口：`CodegenController` → `CodegenServiceImpl`。

#### 5.4.1 表结构读取

`DatabaseTableServiceImpl`：

- 数据源可以是 `id=0` 的 master（来自 `DynamicDataSourceProperties`），也可以是 `infra_data_source_config` 中的自定义数据源；
- 使用 MyBatis-Plus Generator 的 `ConfigBuilder` 解析 `TableInfo`；
- 默认排除视图、`ACT_*`、`QRTZ_*`、`FLW_*`、Oracle 系统表、名称含 `$` 的表；
- 日期类型统一使用 `LocalDateTime`。

#### 5.4.2 导入与编辑

1. `create-list`：逐表解析，校验表注释、字段注释非空；已导入的表拒绝重复导入；
2. `CodegenBuilder` 构建 `CodegenTableDO` 与 `CodegenColumnDO`：
   - 默认场景 `ADMIN`；
   - 前端类型取 `yshop.codegen.front-type`（当前配置为 `10`，即 Vue2）；
   - 作者取当前登录用户昵称（`AdminUserApi`）；
   - 无主键时把第一个字段设为主键；
3. `update`：保存表配置与字段配置（是否新增/修改/列表/查询、HTML 类型、字典类型等）；
4. `sync-from-db`：对比数据库字段，新增缺失字段、删除已消失字段、重建发生变化的字段。

#### 5.4.3 生成与下载

`CodegenEngine` 基于 Velocity 模板：

- **后端模板**：Controller、PageReqVO / ListReqVO / RespVO / SaveReqVO、DO、Mapper、Mapper XML、Service、ServiceImpl、单元测试、ErrorCode 常量、SQL / H2 SQL；
- **前端模板**：支持 Vue2、Vue3、Vue3 Schema、Vue3 VBen 四套（`CodegenFrontTypeEnum`：10/20/21/30）；
- 主子表场景会额外加载子表定义与字段，生成子表 DO / Mapper / Service / 前端表单与列表；
- 生成结果为 `Map<文件路径, 文件内容>`，`preview` 直接返回，`download` 打包为 zip。

模板资源位于：

```
src/main/resources/codegen/
├── java/       # controller、dal、enums、service、test
├── sql/        # sql.vm、h2.vm
├── vue/        # Vue2
├── vue3/       # Vue3 Element Plus
├── vue3_schema/
└── vue3_vben/
```

### 5.5 定时任务

入口：`JobController` / `JobService`。

1. **创建**：
   - 校验 cron 表达式（`CronUtils`）；
   - `handlerName` 唯一；
   - 通过 `SpringUtil.getBean(handlerName)` 校验对应 Bean 存在且实现 `JobHandler`；
   - 插入 `infra_job`（初始状态 `INIT`）；
   - 调用 `SchedulerManager#addJob` 注册到 Quartz，再把状态改为 `NORMAL`。
2. **更新**：仅 `NORMAL` 状态允许修改，避免暂停任务被意外恢复；更新数据库后同步 Quartz。
3. **启停**：`NORMAL` → `resumeJob`，`STOP` → `pauseJob`。
4. **触发**：`triggerJob` 立即执行一次。
5. **同步**：`syncJob` 遍历全部任务，先删后建，最后按状态恢复暂停。
6. **日志**：框架的 `JobHandlerInvoker` 在任务前后调用 `JobLogService`：
   - 开始时写 `infra_job_log`（RUNNING）；
   - 结束后异步更新结束时间、耗时、成功/失败、执行结果。

内置清理任务（均为 `JobHandler` 实现，需在后台创建/同步对应 Job 后才会被调度）：

| 类 | 作用 | 保留天数 | 单批条数 |
|---|---|---|---|
| `JobLogCleanJob` | 清理任务日志 | 14 天 | 100 |
| `AccessLogCleanJob` | 清理 API 访问日志 | 14 天 | 100 |
| `ErrorLogCleanJob` | 清理 API 错误日志 | 14 天 | 100 |

### 5.6 API 访问日志

由 `yshop-spring-boot-starter-web` 中的 `ApiAccessLogFilter` 记录：

1. 请求进入时记录开始时间、query 参数、JSON body；
2. 执行过滤链，正常或异常都会生成日志；
3. 用户信息取自 `WebFrameworkUtils`；
4. 结果码 / 消息取自 `CommonResult` 或异常根因；
5. 记录 traceId、applicationName、method、URL、UA、IP；
6. 自动脱敏 `password`、`token`、`accessToken`、`refreshToken`；
7. 操作模块与名称优先取 `@ApiAccessLog`，否则取 `@Tag` / `@Operation`；
8. 默认记录请求参数、**不记录响应体**，需要响应体时在 `@ApiAccessLog` 上开启；
9. 可通过 `@ApiAccessLog(enable=false)` 关闭单个接口；
10. 写入 `infra_api_access_log`。

`application-local.yaml` 中 `yshop.access-log.enable=false`，本地默认不记录。

### 5.7 API 错误日志

`GlobalExceptionHandler#defaultExceptionHandler` 捕获未预期异常后：

1. 记录 error 日志；
2. 构建 `ApiErrorLogCreateReqDTO`：异常类名、消息、根因、完整堆栈、首个堆栈元素（文件/方法/行号）、请求信息、用户信息；
3. 调用 `ApiErrorLogApi#createApiErrorLog` 写入 `infra_api_error_log`；
4. 返回统一的 `CommonResult` 500。

后台可更新处理状态、分页查询和导出。

### 5.8 参数配置

`ConfigServiceImpl`：

- `infra_config` 保存 `config_key` / `value` / `type` / `visible`；
- `type=SYSTEM` 的内置配置不允许删除；
- `get-value-by-key` 对 `visible=false` 的配置抛错，避免敏感值泄漏给前端；
- 创建时校验 key 唯一。

### 5.9 Redis 监控

`RedisController#getRedisMonitorInfo` 通过 `StringRedisTemplate` 执行 `INFO`、`DBSIZE`、`INFO commandstats`，由 `RedisConvert` 组装为监控响应。

### 5.10 健康检查与监控

- Actuator 基础路径 `/actuator`，`management.endpoints.web.exposure.include=*`；
- 提供 `health`、`info`、`metrics` 等标准端点；
- `AdminServerConfiguration` 标注 `@EnableAdminServer`，提供 Spring Boot Admin 服务端（上下文 `/admin`）；
- `application-local.yaml` 明确排除了 Spring Boot Admin 的 Server / UI / Client 自动配置，本地不启用；
- dev 环境启用 Admin Client 并注册到本机 `/admin`。

### 5.11 示例工程

- `demo01`：单表联系人（`yshop_demo01_contact`）；
- `demo02`：树表分类（`yshop_demo02_category`，`parent_id`）；
- `demo03`：主子表学生 / 课程 / 班级（`yshop_demo03_student`、`yshop_demo03_course`、`yshop_demo03_grade`）。

它们既是可运行的 CRUD 示例，也是代码生成器“单表 / 树表 / 主子表”模板的对照实现。

## 6. 数据模型

所有 DO 均继承 `BaseDO`，公共字段：`create_time`、`update_time`、`creator`、`updater`、`deleted`（逻辑删除）。

| DO | 表名 | 说明 | 关键字段 |
|---|---|---|---|
| `CodegenTableDO` | `infra_codegen_table` | 代码生成表定义 | `id`、`data_source_config_id`、`scene`、`table_name`、`table_comment`、`module_name`、`business_name`、`class_name`、`class_comment`、`author`、`template_type`、`front_type`、`parent_menu_id`、`master_table_id`、`sub_join_column_id`、`sub_join_many`、`tree_parent_column_id`、`tree_name_column_id` |
| `CodegenColumnDO` | `infra_codegen_column` | 代码生成字段定义 | `id`、`table_id`、`column_name`、`data_type`、`column_comment`、`nullable`、`primary_key`、`ordinal_position`、`java_type`、`java_field`、`dict_type`、`example`、`create_operation`、`update_operation`、`list_operation`、`list_operation_condition`、`list_operation_result`、`html_type` |
| `ConfigDO` | `infra_config` | 参数配置 | `id`、`category`、`name`、`config_key`、`value`、`type`、`visible`、`remark` |
| `DataSourceConfigDO` | `infra_data_source_config` | 动态数据源配置 | `id`、`name`、`url`、`username`、`password` |
| `FileDO` | `infra_file` | 文件元数据 | `id`、`config_id`、`name`、`path`、`url`、`type`、`size` |
| `FileConfigDO` | `infra_file_config` | 文件存储配置 | `id`、`name`、`storage`、`remark`、`master`、`config`（JSON） |
| `FileContentDO` | `infra_file_content` | 数据库文件内容 | `id`（String）、`config_id`、`path`、`content`（BLOB） |
| `JobDO` | `infra_job` | 定时任务 | `id`、`name`、`status`、`handler_name`、`handler_param`、`cron_expression`、`retry_count`、`retry_interval`、`monitor_timeout` |
| `JobLogDO` | `infra_job_log` | 任务执行日志 | `id`、`job_id`、`handler_name`、`handler_param`、`execute_index`、`begin_time`、`end_time`、`duration`、`status`、`result` |
| `ApiAccessLogDO` | `infra_api_access_log` | API 访问日志 | `id`、`trace_id`、`user_id`、`user_type`、`application_name`、`request_method`、`request_url`、`request_params`、`response_body`、`user_ip`、`user_agent`、`operate_module`、`operate_name`、`operate_type`、`begin_time`、`end_time`、`duration`、`result_code`、`result_msg` |
| `ApiErrorLogDO` | `infra_api_error_log` | API 错误日志 | `id`、`user_id`、`trace_id`、`user_type`、`application_name`、`request_method`、`request_url`、`request_params`、`user_ip`、`user_agent`、`exception_time`、`exception_name`、`exception_message`、`exception_root_cause_message`、`exception_stack_trace`、`exception_class_name`、`exception_file_name`、`exception_method_name`、`exception_line_number`、`process_status`、`process_time`、`process_user_id` |
| `Demo01ContactDO` | `yshop_demo01_contact` | 示例：单表联系人 | `id`、`name`、`sex`、`birthday`、`description`、`avatar` |
| `Demo02CategoryDO` | `yshop_demo02_category` | 示例：树表分类 | `id`、`name`、`parent_id` |
| `Demo03StudentDO` | `yshop_demo03_student` | 示例：学生（主表） | `id`、`name`、`sex`、`birthday`、`description` |
| `Demo03CourseDO` | `yshop_demo03_course` | 示例：学生课程（子表） | `id`、`student_id`、`name`、`score` |
| `Demo03GradeDO` | `yshop_demo03_grade` | 示例：学生班级（子表） | `id`、`student_id`、`name`、`teacher` |

## 7. 依赖与集成

### 7.1 Maven 依赖

`yshop-module-infra-api`：

- `yshop-common`
- `spring-boot-starter-validation`（optional）

`yshop-module-infra-biz`：

- `yshop-module-system-api`（代码生成器获取当前用户昵称）
- `yshop-module-infra-api`
- `yshop-spring-boot-starter-biz-tenant`
- `yshop-spring-boot-starter-security`
- `yshop-spring-boot-starter-websocket`
- `yshop-spring-boot-starter-mybatis`
- `mybatis-plus-generator`（解析表结构）
- `yshop-spring-boot-starter-redis`
- `yshop-spring-boot-starter-job`
- `yshop-spring-boot-starter-mq`
- `yshop-spring-boot-starter-excel`
- `velocity-engine-core`（代码生成模板）
- `yshop-spring-boot-starter-monitor`
- `spring-boot-admin-starter-server`
- `commons-net`（FTP）
- `jsch`（SFTP）
- `minio`（S3 兼容存储）
- `tika-core`（文件类型识别）

### 7.2 对外暴露的 API

| API | 作用 | 典型使用方 |
|---|---|---|
| `FileApi` | 上传文件、按路径读文件 | system 头像上传、业务图片 |
| `ApiAccessLogApi` | 创建访问日志 | Web 框架的访问日志过滤器 |
| `ApiErrorLogApi` | 创建错误日志 | `GlobalExceptionHandler` |
| `WebSocketSenderApi` | 发送 WebSocket 消息 | 通知公告、业务推送 |

### 7.3 与 system 的协作

- `infra-biz` → `system-api`：代码生成时通过 `AdminUserApi#getUser` 获取作者昵称；
- `system-biz` → `infra-api`：个人头像上传使用 `FileApi`；
- 两者通过 `-api` 解耦，Maven 层不存在循环依赖。

### 7.4 与框架组件

- 文件上传依赖 `yshop-spring-boot-starter-web` 的全局异常与响应封装；
- 代码生成依赖 `yshop-spring-boot-starter-mybatis` 的 `BaseDO`、`BaseMapperX`、`LambdaQueryWrapperX` 作为模板变量；
- 定时任务依赖 `yshop-spring-boot-starter-job` 的 `JobHandler` 与 `SchedulerManager`；
- 监控依赖 `yshop-spring-boot-starter-monitor` 与 Spring Boot Admin。

## 8. 注意事项

1. **App 文件上传完全公开**：`/app-api/infra/file/upload` 既无 `@PreAuthenticated` 也无 `@PreAuthorize`，且 `/app-api/**` 在安全配置中整体放行；公网部署时应通过网关、限流或业务校验限制滥用。
2. **文件大小限制**：由 `spring.servlet.multipart` 控制，默认单文件 16MB、单请求 32MB；大文件需改用预签名直传。
3. **主文件配置必须存在**：上传默认使用 `master=true` 的配置，若 `infra_file_config` 中没有主配置，`getMasterFileClient` 将拿不到客户端并报错。
4. **`FileConfigDO.config` 是 JSON**：不同 `storage` 对应不同配置类；手工改库时必须保证 JSON 结构与目标配置类一致，否则反序列化 / 校验失败。
5. **`infra_file_content.id` 是 String**：与大多数自增 Long 主键不同，使用数据库存储文件内容时需注意。
6. **删除文件不可恢复**：`deleteFile` 同时删除存储对象与数据库记录；对象存储删除失败会中断流程。
7. **代码生成默认前端类型是 Vue2**：`yshop.codegen.front-type=10`，而本仓库管理端使用 Vue3。生成前端代码前应改为 `20`（Vue3 标准）、`21`（Vue3 Schema）或 `30`（VBen），否则产物与现有前端不匹配。
8. **代码生成要求表和字段必须有注释**：`validateTableInfo` 会在表注释、字段注释为空时直接失败；建表时应写 `COMMENT`。
9. **代码生成只生成代码，不自动建菜单**：生成结果中的 SQL 脚本需要人工执行，菜单与权限仍需在系统中配置。
10. **`yshop.codegen.db-schemas` 依赖 master 数据源的 `name` 属性**，而 YAML 中未显式配置 `spring.datasource.dynamic.datasource.master.name`；调整数据源结构时需验证代码生成器能否正确取到库名。
11. **local 环境定时任务不可用**：`application-local.yaml` 排除了 `QuartzAutoConfiguration`，`SchedulerManager` 收到空的 `Scheduler`；要测试 Job 请使用 `dev`。
12. **内置清理 Job 不会自动注册**：`JobLogCleanJob`、`AccessLogCleanJob`、`ErrorLogCleanJob` 只是 Spring Bean，必须在 `infra_job` 中创建记录并同步到 Quartz 后才会执行。
13. **Job 的 `handler_name` 必须是 Spring Bean 名称**：`JobServiceImpl` 通过 `SpringUtil.getBean(handlerName)` 校验，名称写错会在创建时报 `JOB_HANDLER_BEAN_NOT_EXISTS`。
14. **访问日志本地默认关闭**：`yshop.access-log.enable=false`；开启后记录请求参数但不记录响应体，且会自动脱敏密码与 token 类字段。
15. **错误日志可能重复**：`GlobalExceptionHandler` 与部分 Mapper 的日志级别已做调整，若把 `ApiErrorLogMapper` 调成 DEBUG 可能出现重复堆栈，`application-local.yaml` 中已设为 INFO。
16. **Actuator 暴露全部端点**：`include=*` 且未单独配置安全白名单；生产环境应限制端点、增加认证或仅内网暴露。
17. **Spring Boot Admin 本地不可用**：`local` profile 排除了相关自动配置；dev 下可通过 `/admin` 查看。
18. **Demo 表使用 `yshop_demo*` 前缀**：演示数据与业务数据混在同一库中，生产部署前应清理或排除。
19. **`infra_data_source_config` 中的密码是明文**：数据源配置直接存库，需配合数据库访问控制与脱敏。
20. **文件下载接口为 `@PermitAll`**：`/admin-api/infra/file/{configId}/get/**` 不需要登录，只要知道 `configId` 和路径即可读取；敏感文件不应使用该通道。
