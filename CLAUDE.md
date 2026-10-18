# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 仓库概览

`yshop-drink` 是一套多门店 / SaaS 在线点餐系统（堂食扫码、外卖、自取）。同一个 Git 仓库包含三个并列工程：

| 目录 | 作用 | 技术栈 |
|---|---|---|
| `yshop-drink-boot3/` | 后端 REST API | Java 17、Spring Boot 3.2.2、MyBatis-Plus 3.5.5、Spring Security OAuth2/JWT、Redis、Maven 多模块（55 个 pom） |
| `yshop-drink-vue3/` | 商家后台管理端 | Vue 3.4、Vite 5、TypeScript、Element Plus 2.6、Pinia、UnoCSS |
| `yshop-drink-uniapp-vue3/` | 用户移动端 | UniApp（Vue 3），可构建微信小程序 / H5 |

## 环境要求

- JDK **17**，不要使用更高版本。项目中的 Lombok 1.18.30 在 JDK 24 下无法工作。
- Maven 3.8+；MySQL 8；Redis 6+（缺少 Redis 时 Redisson 会导致启动失败）；Node 22+（UniApp CLI 要求 `>=22.12.0`）以及 **pnpm 8.x**（后台管理端 lockfile 为 `lockfileVersion: '6.0'`，pnpm 11 不兼容）。
- 移动端已转为 uni-app **CLI** 工程，用 npm 运行/构建，不再依赖 HBuilderX。

## 常用命令

### 后端（`yshop-drink-boot3/`）

```bash
# 构建全部模块（跳过测试）
mvn clean install package -Dmaven.test.skip=true

# 启动——默认 profile 为 local，端口 48081
java -jar yshop-server/target/yshop-server.jar

# 运行全部测试（H2 内存库 + 内嵌 Redis，无需外部 MySQL/Redis）
mvn test

# 运行单个测试类
mvn test -pl yshop-module-member/yshop-module-member-biz -Dtest=MemberAuthServiceTest

# 在全新仓库中首次构建单个模块：加 -am 一并构建依赖模块
mvn test -pl yshop-module-member/yshop-module-member-biz -am -Dtest=MemberAuthServiceTest
```

测试使用 `unit-test` profile（`src/test/resources/application-unit-test.yaml`）、MySQL 模式的 H2 以及 16379 端口上的内嵌 Redis。依赖数据库的测试继承 `BaseDbUnitTest`，并执行 `src/test/resources/sql/create_tables.sql`。

数据库初始化：创建数据库 `yixiang-drink-open`，导入 **`yshop-drink-boot3/sql/yixiang-drink-open.sql`**。（根目录 README 中提到的 `sql/yixiang-drink.sql` 并不存在。）

### 后台管理端（`yshop-drink-vue3/`）

```bash
pnpm install          # 使用 pnpm 8.x
pnpm dev              # Vite 开发服务器，端口取 VITE_PORT（80），自动打开浏览器
pnpm build:local      # 使用 .env.local 构建
pnpm build:prod
pnpm ts:check         # vue-tsc --noEmit
pnpm lint:eslint      # eslint --fix
pnpm lint:format      # prettier
pnpm lint:style       # stylelint
```

`.env` 存放公共默认配置；`.env.local`（通过 `*.local` 被 git 忽略）用于本机覆盖。axios 的 base URL 为 `VITE_BASE_URL + VITE_API_URL`（`/admin-api` 是后端管理端前缀）。连接本机后端的配置：

```
VITE_BASE_URL='http://127.0.0.1:48081'
VITE_API_URL=/admin-api
VITE_UPLOAD_URL='http://127.0.0.1:48081/admin-api/infra/file/upload'
```

### 移动端（`yshop-drink-uniapp-vue3/`）

源码沿用 HBuilderX 布局（位于项目根目录，而非 `src/`），已配置 uni-app CLI：

```bash
npm install            # 同时执行 scripts/patch-uni-h5-vue.js 运行时补丁
npm run dev:h5         # H5 开发，http://localhost:5173/h5/
npm run build:h5
npm run dev:mp-weixin
npm run build:mp-weixin  # 产物在 dist/build/mp-weixin，用微信开发者工具导入
```

`scripts/uni.js` 负责把 `UNI_INPUT_DIR` 指向项目根目录；`scripts/patch-uni-h5-vue.js` 修复 `uni-h5-vue` + `@vue/shared@3.4.21` 的 `read only property '_'` 崩溃。后端地址在 `config/index.js` 中配置；开发环境默认是 `http://localhost:48081/app-api`。

## 后端架构

### 模块划分

```
yshop-drink-boot3/
├── yshop-dependencies/        BOM / 依赖版本管理
├── yshop-framework/           可复用 starter（自动配置）
│   ├── yshop-common                       CommonResult、PageResult、ErrorCode、工具类
│   ├── yshop-spring-boot-starter-web      API 前缀路由、全局异常处理
│   ├── yshop-spring-boot-starter-security JWT 过滤器、@PreAuthenticated、@ss
│   ├── yshop-spring-boot-starter-mybatis  BaseDO、BaseMapperX、查询包装器
│   ├── yshop-spring-boot-starter-biz-tenant  多租户（当前未启用）
│   └── ...redis、excel、job、mq、monitor、protection、websocket、test
├── yshop-server/              Spring Boot 启动入口，仅作为应用外壳
└── yshop-module-*/            业务模块
```

业务模块：`system`（用户、角色、部门、菜单、字典、OAuth2）、`infra`（文件、代码生成、定时任务、日志）、`pay`、`mp`（微信公众号）、`mall`（子模块 `order`、`product`、`shop`、`store`）、`message`（模板消息/WebSocket/Redis MQ）、`score`（积分）、`express`、`marketing`（优惠券）、`member`。

每个模块分为 `-api` 和 `-biz` 两个产物：
- `-api`：枚举、`ErrorCodeConstants`、跨模块 API 接口和 DTO（例如 `MemberUserApi`、`ProductApi`）。
- `-biz`：`controller/`、`service/`、`dal/dataobject|mysql|redis/`、`convert/`（MapStruct）、`mq/`、`api/` 实现。

`yshop-server` 有意设计为空壳：它只依赖需要在运行时加载 Controller 的 `-biz` 模块。新增模块的 REST 接口时，需要把该模块的 `-biz` 依赖加入 `yshop-server/pom.xml`（部分模块会通过传递依赖引入，例如 `order-biz` 依赖 `product-biz`）。

实际上 `-biz` 模块之间也会直接引用彼此的服务/DAL 类（例如 `order-biz` 使用 `member-biz` 的 Mapper/Service），因此 `-api` 边界并没有被严格约束。

### 请求路由

`WebProperties`（`yshop-framework/yshop-spring-boot-starter-web`）会根据 Controller 所在包自动添加前缀：

- `**.controller.admin.**` → `/admin-api`
- `**.controller.app.**` → `/app-api`

Controller 上的 `@RequestMapping` **不能**再包含该前缀（例如 `/order/store-order` 实际暴露为 `/admin-api/order/store-order`）。`/app-api` 面向小程序/H5 用户端，`/admin-api` 面向后台管理端和商家端。

### 响应与错误约定

- Controller 返回 `CommonResult<T>`；使用 `CommonResult.success(data)`。
- 分页接口使用 `PageParam` / `PageResult<T>`。
- 业务错误：在各模块的 `enums/ErrorCodeConstants.java` 中定义常量，并通过静态方法 `ServiceExceptionUtil.exception(...)` 抛出。各模块的错误码区间不能重叠，参见 `ServiceErrorCodeRange`。
- 管理端接口使用 `@PreAuthorize("@ss.hasPermission('order:store-order:create')")` 鉴权。需要登录的 App 端接口使用 `@PreAuthenticated`（未登录时抛出“未登录”，而不是“无权限”）。
- 请求/响应对象（`*ReqVO`、`*RespVO`、`*PageReqVO`）放在 `controller/.../vo/` 下。

### 持久层

- DO 继承 `BaseDO`（`createTime`、`updateTime`、`creator`、`updater`、逻辑删除 `deleted`）或 `TenantBaseDO`（额外包含 `tenantId`），并通过 `@TableName` 映射到 `yshop_*` 表。
- Mapper 继承 `BaseMapperX`，位于 `dal/mysql/<entity>/`；Service 接收 VO，并通过 MapStruct `*Convert` 类转换。
- MyBatis-Plus 已配置逻辑删除，ID 类型为 `NONE`（根据数据库类型自动适配）。
- `easy-trans` 全局启用，VO 上的 `@Trans` 字段会在响应中自动翻译。

### 安全与运行配置

- 无状态 JWT 认证（`TokenAuthenticationFilter`）；白名单 URL 配置在 `yshop.security.permit-all_urls` 以及各模块的 `AuthorizeRequestsCustomizer` 中。
- `local` profile：端口 48081，本机 MySQL `yixiang-drink-open`（root/root），Redis `127.0.0.1:6379`，关闭验证码，`yshop.security.mock-enable: true`，禁用 Quartz 自动配置。
- `dev` profile：端口 48080，使用远程 Redis。`application.yaml` 为共享配置，默认激活 `local`。
- 多租户代码存在但未启用（`yshop.tenant.enable: false`）；启用后 `yshop.tenant.ignore-tables` 中的表会被排除。
- API 文档：Knife4j/Swagger UI 位于 `/swagger-ui`，OpenAPI 位于 `/v3/api-docs`。
- 订单/消息流程使用 Redisson 延迟队列（`message` 模块），同时也支持可选的 RocketMQ/Kafka/RabbitMQ。MQ broker 虽有配置，但启动时并非必需。

## 后台管理端架构

- `src/api/<module>/` 下的 API 模块与后端 Controller 对应。Axios 实例、Token 刷新和错误处理位于 `src/config/axios/service.ts`；业务错误信息映射在 `errorCode.ts`。
- 路由是**根据后端菜单记录动态生成**的（`system_menu` 表）。`src/store/modules/permission.ts` 转换菜单数据，`src/utils/routerHelper.ts` 通过 `import.meta.glob` 将菜单中的组件路径解析到 `src/views/**/*.vue`。静态路由（登录、404、个人中心、详情页）位于 `src/router/modules/remaining.ts`。
- 因此新增后台页面通常需要同时提供：`src/views/<module>/...` 下的页面，以及数据库中组件路径和权限字符串相匹配的菜单记录。
- 按钮级权限使用 `v-hasPermi` / `v-hasRole` 指令（`src/directives/permission/`）。
- 状态管理：Pinia store 位于 `src/store/modules/`。图标使用 SVG sprite 和 UnoCSS；Element Plus 组件自动导入。

## 移动端架构

- UniApp Vue 3，入口为 `main.js` -> `App.vue`；页面注册和 tabBar 在 `pages.json` 中（主包页面以及子包 `pages-checkout`、`pages-order`、`pages-score`、`pages-user`）。
- HTTP 客户端：`api/api.js` 使用 `flyio`（通过 `#ifdef H5 / APP-PLUS / MP-WEIXIN` 按平台引入不同构建）。它会从 cookie 中取出 `accessToken` 并附加 `Authorization: Bearer <accessToken>`，同时处理 401/登录跳转。API 分组位于 `api/*.js`。
- 平台相关代码使用 UniApp 条件编译注释（`// #ifdef`、`// #endif`），在 `api/`、`utils/` 和页面中广泛使用。
- UI 组件来自 `uni_modules/uv-*`；全局样式在 `uni.scss`。

## 约定与注意事项

- **后端 Java 包根路径**为 `co.yixiang.yshop`，通过 `${yshop.info.base-package}` 配置；`@SpringBootApplication` 扫描 `${base}.server` 和 `${base}.module`。
- 新增功能时遵循现有分层：`controller/{admin|app}/<domain>/` + `vo/`、`service/<domain>/`、`dal/dataobject|mysql/<domain>/`、`convert/<domain>/`，错误码放在 `<module>-api/enums/ErrorCodeConstants`。
- 前端 TypeScript 路径别名：`@/` -> `src/`。已配置 Prettier/ESLint，提交前端改动前运行 `pnpm lint:eslint`。
- UniApp 工程使用 npm（不是 pnpm）安装依赖并构建；`postinstall` 会执行 `scripts/patch-uni-h5-vue.js`，不要删除该脚本。
- `yshop-drink-boot3/script/docker/` 下的 Docker compose 来自上游模板，其中的路径/前端名称与当前仓库不一致，仅可作为参考。
