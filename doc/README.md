# yshop-drink 项目文档

> 本文档集基于仓库源码静态分析整理，覆盖后端全部 Maven 模块、两个前端工程的功能与实现逻辑。
> 生成日期：2026-10-19

## 1. 项目概览

`yshop-drink` 是一套多门店 / SaaS 在线点餐系统（堂食扫码、外卖、自取），由三个并列工程组成：

| 工程 | 作用 | 技术栈 | 文档 |
|---|---|---|---|
| `yshop-drink-boot3/` | 后端 REST API | Java 17、Spring Boot 3.2.2、MyBatis-Plus、Spring Security | [后端文档](#2-后端模块) |
| `yshop-drink-vue3/` | 商家后台管理端 | Vue 3.4、Vite 5、TypeScript、Element Plus、Pinia | [yshop-drink-vue3.md](frontend/yshop-drink-vue3.md) |
| `yshop-drink-uniapp-vue3/` | 用户移动端 | UniApp（Vue 3）、flyio、Pinia | [yshop-drink-uniapp-vue3.md](frontend/yshop-drink-uniapp-vue3.md) |

默认本地运行环境：

| 服务 | 地址/端口 |
|---|---|
| 后端 API | http://localhost:48081 |
| 商家后台 | http://localhost:80 |
| 用户端 H5 | http://localhost:5173/h5/ |
| MySQL | 127.0.0.1:3306（库 `yixiang-drink-open`） |
| Redis | 127.0.0.1:6379 |

## 2. 后端模块

### 2.1 工程级

| 文档 | 说明 |
|---|---|
| [yshop-dependencies.md](backend/yshop-dependencies.md) | BOM 依赖版本管理、`${revision}` 版本策略、第三方依赖分组 |
| [yshop-server.md](backend/yshop-server.md) | 启动入口、模块装配、三套 profile 配置、启动/打包流程 |

### 2.2 framework（可复用 Starter，15 篇）

| 文档 | 说明 |
|---|---|
| [yshop-common.md](backend/framework/yshop-common.md) | 基础 POJO、枚举、异常、工具类（非 Starter） |
| [yshop-spring-boot-starter-web.md](backend/framework/yshop-spring-boot-starter-web.md) | API 前缀路由、全局异常、XSS、访问日志 |
| [yshop-spring-boot-starter-security.md](backend/framework/yshop-spring-boot-starter-security.md) | Token 过滤、`@PreAuthenticated`、`@ss` 权限、白名单 |
| [yshop-spring-boot-starter-mybatis.md](backend/framework/yshop-spring-boot-starter-mybatis.md) | `BaseDO`、`BaseMapperX`、多数据源、逻辑删除 |
| [yshop-spring-boot-starter-redis.md](backend/framework/yshop-spring-boot-starter-redis.md) | RedisTemplate、Redisson、缓存封装 |
| [yshop-spring-boot-starter-excel.md](backend/framework/yshop-spring-boot-starter-excel.md) | Excel 导入导出、字典转换 |
| [yshop-spring-boot-starter-test.md](backend/framework/yshop-spring-boot-starter-test.md) | H2/内嵌 Redis 测试基类 |
| [yshop-spring-boot-starter-job.md](backend/framework/yshop-spring-boot-starter-job.md) | Quartz 定时任务封装 |
| [yshop-spring-boot-starter-monitor.md](backend/framework/yshop-spring-boot-starter-monitor.md) | 链路 Trace、健康检查 |
| [yshop-spring-boot-starter-mq.md](backend/framework/yshop-spring-boot-starter-mq.md) | Redis Pub/Sub / Stream、MQ 抽象 |
| [yshop-spring-boot-starter-protection.md](backend/framework/yshop-spring-boot-starter-protection.md) | 幂等、限流、分布式锁（当前多数未启用） |
| [yshop-spring-boot-starter-websocket.md](backend/framework/yshop-spring-boot-starter-websocket.md) | WebSocket 服务端与发送器 |
| [yshop-spring-boot-starter-biz-tenant.md](backend/framework/yshop-spring-boot-starter-biz-tenant.md) | 多租户（代码存在，运行时关闭） |
| [yshop-spring-boot-starter-biz-data-permission.md](backend/framework/yshop-spring-boot-starter-biz-data-permission.md) | 数据权限注解与拦截 |
| [yshop-spring-boot-starter-biz-ip.md](backend/framework/yshop-spring-boot-starter-biz-ip.md) | IP 归属地工具库 |

### 2.3 业务模块

| 文档 | 说明 |
|---|---|
| [yshop-module-system.md](backend/modules/yshop-module-system.md) | 用户/角色/部门/菜单/字典/OAuth2/租户/站内信 |
| [yshop-module-infra.md](backend/modules/yshop-module-infra.md) | 文件、代码生成、定时任务、日志、配置、监控 |
| [yshop-module-order.md](backend/modules/yshop-module-order.md) | 订单下单、支付、退款、状态流转、购物明细 |
| [yshop-module-product.md](backend/modules/yshop-module-product.md) | 商品、分类、SKU、属性、积分商品 |
| [yshop-module-shop.md](backend/modules/yshop-module-shop.md) | 店铺、广告、素材、服务内容 |
| [yshop-module-store.md](backend/modules/yshop-module-store.md) | 门店、营业状态、附近门店、配送范围 |
| [yshop-module-member.md](backend/modules/yshop-module-member.md) | 会员、地址、余额、积分账户、社交登录 |
| [yshop-module-score.md](backend/modules/yshop-module-score.md) | 积分商品、积分兑换订单 |
| [yshop-module-marketing.md](backend/modules/yshop-module-marketing.md) | 优惠券模板、领取、使用 |
| [yshop-module-pay.md](backend/modules/yshop-module-pay.md) | 支付服务商配置、回调处理、支付通知 |
| [yshop-module-mp.md](backend/modules/yshop-module-mp.md) | 微信公众号/小程序账号、消息、菜单、素材、粉丝 |
| [yshop-module-message.md](backend/modules/yshop-module-message.md) | 模板消息/订阅消息、Redis Stream、延迟队列 |
| [yshop-module-express.md](backend/modules/yshop-module-express.md) | 快递公司字典、快递鸟物流查询 |

## 3. 前端

| 文档 | 说明 |
|---|---|
| [yshop-drink-vue3.md](frontend/yshop-drink-vue3.md) | 商家后台：技术栈、目录、路由体系、API 层、Pinia、页面清单、权限、核心流程 |
| [yshop-drink-uniapp-vue3.md](frontend/yshop-drink-uniapp-vue3.md) | 用户移动端：多端构建、页面与分包、flyio 请求层、登录、扫码点餐、下单支付、订单、积分 |

### 3.1 专项文档

| 文档 | 说明 |
|---|---|
| [scan-order.md](scan-order.md) | 扫码点餐：桌台管理、二维码令牌、共享购物车、一起点/分开点、合并下单 |

## 4. 后端模块关系速览

```
yshop-server（空壳启动入口）
├── system      用户/权限/菜单/OAuth2/租户
├── infra       文件/代码生成/定时任务/日志/WebSocket
├── member      会员/地址/余额/积分账户
├── score       积分商品与兑换
├── marketing   优惠券
├── mall
│   ├── order     订单（依赖 product/member/store/message/pay）
│   ├── product   商品与 SKU
│   ├── shop      店铺与素材
│   └── store     门店
├── pay         支付配置与回调
├── mp          公众号/小程序
├── message     消息与延迟队列
└── express     快递查询
```

请求路由由 Controller 包名自动加前缀：

- `**.controller.admin.**` → `/admin-api`
- `**.controller.app.**` → `/app-api`

## 5. 跨模块共性问题（阅读源码时需注意）

以下为文档生成过程中确认的实际行为，不是设计意图：

1. `/app-api/**` 在 Spring Security 层整体放行；需要登录的 App 接口必须显式加 `@PreAuthenticated`，否则匿名可用。
2. accessToken 实际是 UUID 随机串（存 Redis + MySQL），不是 JWT。
3. 多租户、Lock4j 幂等/限流、`@BizTrace` 等能力存在但未启用或未装配。
4. 错误码区间存在多处重叠（member / score / coupon），排障时不能仅凭 code 判断模块。
5. 部分管理端接口缺少 `@PreAuthorize`，部分权限字符串格式不统一（如 `coupon::create`）。
6. 订单/积分/优惠券在并发场景下存在超卖、少扣、超发风险，详见各模块“注意事项”小节。
7. `local` profile 关闭验证码并开启 Mock 登录（token 以 `test` 开头），仅供本地开发。
8. 生产配置存在敏感信息明文与地址残留（`.env.prod`、YAML 中的第三方密钥等）。

## 6. 文档维护

- 文档按“一个 Maven 模块一篇、一个前端工程一篇”组织。
- 新增/删除模块时同步更新本索引。
- 修改核心流程或接口后，请同步更新对应文档的“主要接口”“核心实现逻辑”“注意事项”章节。
- 目录结构：

```
doc/
├── README.md
├── backend/
│   ├── yshop-dependencies.md
│   ├── yshop-server.md
│   ├── framework/        # 15 篇
│   └── modules/          # 13 篇
└── frontend/
    ├── yshop-drink-vue3.md
    └── yshop-drink-uniapp-vue3.md
```
