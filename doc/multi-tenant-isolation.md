# 多商户数据隔离方案（县城 SaaS）

> 目标：一套系统服务县城内多个商家，商家之间数据互不可见。
> 现有工程已内置多租户框架（`yshop-spring-boot-starter-biz-tenant`）。
>
> **当前状态：第一阶段已完成并验证**（共享库 + tenant_id，管理端与用户端租户识别已打通）。

## 0. 已实施内容

| 项目 | 状态 |
|---|---|
| 数据库迁移 | `sql/multi-tenant.sql`，60 张表具备 `tenant_id`，历史数据归入租户 1 |
| 多租户开关 | `yshop.tenant.enable: true` |
| 忽略表审计 | 子表已加 `tenant_id` 并移出忽略列表；仅保留全局字典/系统表 |
| 管理端 | 登录时按租户名获取租户并携带 `tenant-id`，数据自动隔离 |
| 用户端 | 扫码解析二维码 → 返回 `tenantId` → 后续请求携带 `tenant-id` |
| 商户选择 | 新增 `/app-api/tenant/list` 与 `pages/tenant/tenant` 页面 |
| 默认套餐 | 套餐 111 已更新为全部业务菜单，排除平台“租户管理”菜单 |
| 隔离验证 | 租户 1 / 租户 150 的门店、桌台、会话、订单互不可见 |

### 测试账号

| 租户 | 租户名 | 账号 | 密码 |
|---|---|---|---|
| 1 | yshop | admin | admin123 |
| 150 | testmerchant | merchantb | merchant123 |

### 本地测试结论

- 租户 1 只能看到自己的 2 个门店、桌台、订单
- 租户 150 只能看到自己创建的 `tenant2shop`、桌台 B01
- 用租户 1 访问租户 150 的桌台 ID → 返回“桌台不存在”
- App 扫码租户 150 的二维码 → 返回 `tenantId=150`
- App 携带不同 `tenant-id` 查询门店 → 只返回对应商户数据

## 1. 当前现状

| 项目 | 状态 |
|---|---|
| 多租户框架 | 代码完整（TenantContext、DB 拦截器、Redis、MQ、Job 等） |
| 租户管理 | `system_tenant`、`system_tenant_package`、TenantController 已存在 |
| 开关 | `yshop.tenant.enable: false`（未启用） |
| 已有 tenant_id 的表 | 41 张（订单、会员、商品、系统用户等） |
| 缺少 tenant_id 的核心表 | `yshop_store_shop`、`yshop_store_desk`、`yshop_store_desk_qr`、`yshop_store_desk_session`、`yshop_store_desk_cart`、`yshop_coupon*`、`yshop_order_number` 等 |
| 忽略租户过滤的表 | `yshop_store_product_attr*`、`yshop_store_order_cart_info`、`yshop_store_order_status` 等 |

> 直接打开 `tenant.enable=true` 会有数据串户风险，必须先补齐。

## 2. 隔离方案对比

| 方案 | 隔离强度 | 成本 | 运维 | 适用 |
|---|---|---|---|---|
| A. 共享库 + `tenant_id`（推荐） | 中 | 低 | 一套系统、统一升级 | 10–100 家中小商户 |
| B. 每商户独立数据库/实例 | 高 | 高 | N 套部署、N 次升级/备份 | ≤5 家、高要求商户 |
| C. 仅用 `shop_id` 隔离 | 低 | 低 | 简单 | 单商户多门店，不适合多商户 |

**建议：县城场景采用方案 A。** 若个别大商户要求独立，采用混合模式：共享库为主，独立库为辅。

## 3. 推荐数据模型

```
租户（merchant / tenant）
  └── 门店（shop）           yshop_store_shop.tenant_id
        ├── 桌台（desk）      yshop_store_desk.tenant_id
        │     └── 二维码      yshop_store_desk_qr.tenant_id
        ├── 商品              yshop_store_product.tenant_id
        ├── 订单              yshop_store_order.tenant_id
        └── 会员              yshop_user.tenant_id（可选：会员是否跨商户）
```

- **租户 = 商户**：一个商家一个 `tenant_id`
- **门店 = shop**：同一商户可有多家店
- **数据权限**：租户隔离（tenant）+ 门店隔离（shop）两层同时保留

## 4. 租户识别方式

### 管理端
- 登录用户 `system_users.tenant_id` 写入 Token
- `TenantContextWebFilter` 自动设置租户上下文
- 商户管理员只能看到本租户数据
- 平台运营方通过 `@TenantIgnore` 做跨租户查询（需单独角色/权限）

### 用户端（关键）
三种可选方式：

1. **每商户独立小程序/域名（最简单、隔离最好）**
   - 域名或 AppID 映射到 `tenant_id`
   - 扫码/访问时由域名解析出租户
   - 缺点：每个商户都要注册小程序、备案域名

2. **共用一个 H5/小程序（成本低）**
   - 二维码中携带门店/桌台信息
   - 后端由 `QR token → desk → shop → tenant` 解析出租户
   - 请求头带 `tenant-id`，由前端在扫码后设置
   - 需要商户列表/切换逻辑（如果顾客要浏览多家店）

3. **子域名模式**
   - `merchantA.example.com`、`merchantB.example.com`
   - 泛域名解析 + 备案，前端按子域名确定租户

> 不能直接信任客户端传入的 `tenantId`；必须由域名、登录态或二维码反查得出。

## 5. 数据层改造清单

- [ ] 给所有业务表补 `tenant_id`
- [ ] 唯一索引包含 `tenant_id`（店铺名、桌号、二维码名称、优惠券名等）
- [ ] 现有数据按商户回填 `tenant_id`
- [ ] 保留 `shop_id`，形成 tenant → shop 两级隔离
- [ ] 审计 `ignore-tables`，确认哪些表可以不过滤
- [ ] 子表（商品规格、订单明细、订单状态）确保通过父表访问且不越权

## 6. 其他隔离点

| 资源 | 处理 |
|---|---|
| Redis | 使用 `TenantRedisCacheManager`，自定义 key 必须带 tenantId |
| 文件/OSS | 路径加 `tenant/{tenantId}/` 前缀 |
| MQ | 使用框架的租户透传（Redis/Kafka/RabbitMQ/RocketMQ） |
| 定时任务 | 使用 `@TenantJob` 按租户执行 |
| 日志 | 日志中输出 tenantId，便于排障 |
| 导出/报表 | 查询自动带租户过滤 |
| 支付回调 | 回调 URL 携带 tenantId 或通过商户配置反查 |

## 7. 商户开通流程

1. 平台运营方创建租户（名称、联系人、到期时间、套餐）
2. 初始化租户数据：
   - 管理员账号
   - 角色与菜单
   - 默认门店
   - 支付/公众号/小程序配置
   - 字典与基础数据
3. 商户登录后台，创建门店/桌台/二维码/商品
4. 顾客扫码下单
5. 租户到期/停用：禁止登录与下单

## 8. 安全要求

- 所有租户查询必须经过框架拦截器，禁止手写漏掉 `tenant_id` 的 SQL
- 平台级跨租户接口必须显式 `@TenantIgnore` 并做权限校验
- 增加跨租户越权测试（用 A 商户 token 访问 B 商户 ID）
- 二维码/桌台/会话/订单的查询必须同时校验 tenant + shop
- 共享购物车 sessionId 建议改为随机 key，避免猜测

## 9. 实施阶段

### 第一阶段：租户底座
1. 补齐缺失表的 `tenant_id`
2. 启用 `yshop.tenant.enable`
3. 租户初始化流程
4. 管理端登录与数据隔离验证

### 第二阶段：用户端租户识别
1. 确定独立小程序/共用小程序的方案
2. 二维码 → 门店 → 租户解析
3. 请求头/域名租户上下文
4. 跨租户越权测试

### 第三阶段：运营能力
1. 平台运营后台（租户列表、到期、套餐）
2. 商户开通/停用
3. 数据统计与对账

## 10. 需要确认的业务问题

1. 预计接入多少家商户？
2. 每个商户是否需要独立小程序/独立域名？
3. 顾客账号是否需要在不同商户之间通用？
4. 是否有平台运营方统一管理所有商户？
5. 是否有商户要求独立数据库/私有部署？
