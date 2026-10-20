# 扫码点餐实现说明

> 覆盖「桌台管理 → 二维码 → 多人同桌 → 共享购物车 → 合并下单/分开下单」的完整链路。
> 相关数据库脚本：`yshop-drink-boot3/sql/scan-order.sql`、`yshop-drink-boot3/sql/desk-shared-cart.sql`

## 1. 两种堂食模式

堂食点餐页顶部可切换：

| 模式 | 说明 | 购物车 | 订单 |
|---|---|---|---|
| **一起点**（默认） | 多人扫同一桌码，共同往一个购物车加菜，由一人结算 | 服务端共享购物车 | 一桌一单 |
| **分开点** | 每人各自下单、各自支付（支持 AA） | 本地购物车 | 一桌多单 |

## 2. 二维码

- 一个桌台可以配置**多个二维码**，保存在 `yshop_store_desk_qr`
- 每个二维码有独立的 `qr_token`
- **扫同一个二维码**的人 → 同一会话、共享购物车、合并一单
- **扫不同二维码**的人 → 各自独立会话、独立订单（桌号相同）
- 二维码地址格式：`https://<域名>/h5/#/pages/scan/scan?token=<qr_token>`
- 管理端「门店管理 → 桌台管理 → 二维码」可新增/删除/下载/重置每个码

## 3. 一起点（共享购物车）

### 流程

```
扫码 → 校验 token → 加入/创建桌台会话
     → 多人加购（写入服务端购物车）
     → 任意一人结算
     → 生成一笔合并订单（orderType=desk）
     → 支付 → 会话结束
```

### 会话规则

- 同一桌台同时只允许一个「进行中」会话
- 多人扫码自动加入同一会话
- 任意参与者都可以修改/删除购物车中的任意商品
- 结算成功生成订单后，会话立即关闭；再次扫码会开启新会话（用于加菜）
- 超过 4 小时未结算的会话自动失效
- 会话关闭后，未支付的订单仍可在「订单列表」继续支付

### 数据表

| 表 | 用途 |
|---|---|
| `yshop_store_desk` | 桌台（桌号、区域、座位数、启停） |
| `yshop_store_desk_qr` | 桌台二维码（一桌多码，每码独立 token） |
| `yshop_store_desk_session` | 桌台会话（按二维码隔离） |
| `yshop_store_desk_cart` | 共享购物车明细（含添加人） |

### 接口（用户端）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/app-api/store/desk/get?token=` | 根据 token 获取桌台信息（无需登录） |
| GET | `/app-api/order/desk/session?token=` | 扫码加入/创建共享会话 |
| GET | `/app-api/order/desk/session/by-id?sessionId=` | 获取会话与购物车（轮询） |
| POST | `/app-api/order/desk/cart/add` | 加购 `{sessionId, productId, sku, number}` |
| POST | `/app-api/order/desk/cart/update` | 改数量 `{id, number}`，0 表示删除 |
| DELETE | `/app-api/order/desk/cart/delete?id=` | 删除商品 |
| POST | `/app-api/order/desk/cart/clear?sessionId=` | 清空购物车 |
| POST | `/app-api/order/desk/checkout` | 结算生成一笔订单 |

### 结算参数

```json
{
  "sessionId": 1,
  "payType": "yue",
  "remark": "少辣",
  "deskPeople": 4,
  "couponId": 0,
  "mobile": "13800000000"
}
```

返回：

```json
{ "sessionId": 1, "orderId": "2112510075271643136" }
```

## 4. 分开点（各自下单）

- 仍使用客户端购物车
- 每人生成自己的订单，订单带同一个 `deskId / deskNumber`
- 再次扫码加菜只关联**本人**进行中的订单
- 相关接口：
  - `GET /app-api/order/desk-active?deskId=` 我的进行中订单
  - `GET /app-api/order/desk-summary?deskId=` 本桌订单数与合计

## 5. 桌台管理接口（管理端）

| 方法 | 路径 | 权限 |
|---|---|---|
| POST | `/admin-api/store/desk/create` | `store:desk:create` |
| PUT | `/admin-api/store/desk/update` | `store:desk:update` |
| DELETE | `/admin-api/store/desk/delete?id=` | `store:desk:delete` |
| GET | `/admin-api/store/desk/get?id=` | `store:desk:query` |
| GET | `/admin-api/store/desk/page` | `store:desk:query` |
| GET | `/admin-api/store/desk/list-by-shop?shopId=` | `store:desk:query` |
| POST | `/admin-api/store/desk/batch-create` | `store:desk:create` |
| POST | `/admin-api/store/desk/reset-token?id=` | `store:desk:update` |
| GET | `/admin-api/store/desk/qr/list?deskId=` | `store:desk:query` |
| POST | `/admin-api/store/desk/qr/create` | `store:desk:create` |
| PUT | `/admin-api/store/desk/qr/update` | `store:desk:update` |
| DELETE | `/admin-api/store/desk/qr/delete?id=` | `store:desk:delete` |
| POST | `/admin-api/store/desk/qr/reset-token?id=` | `store:desk:update` |

> 非超级管理员只能操作自己门店的桌台与二维码（按登录用户 `shopId` 隔离）。

## 6. 订单落库

`yshop_store_order` 新增：

| 字段 | 说明 |
|---|---|
| `desk_id` | 桌台编号 |
| `desk_number` | 桌号 |
| `desk_people` | 就餐人数 |
| `desk_qr_id` | 桌台二维码编号 |
| `desk_qr_name` | 桌台二维码名称（如 A01-左） |

加菜单通过 `extend_order_id` 关联原订单（分开点模式）。

## 7. 本地测试

1. 后端：`java -jar yshop-server/target/yshop-server.jar`
2. H5：`npm run dev:h5`（`0.0.0.0:5173`）
3. 手机与电脑同一 Wi-Fi，微信扫码 `doc/qr/desk-A01.png`
4. 两个微信/两个测试用户分别扫码：
   - 进入同一会话
   - 各自加购，购物车实时同步（5 秒轮询）
   - 任意一人点「去结算」→ 生成一笔合并订单
5. 后端可用 mock token 测试：`Authorization: Bearer test11`、`test12`

## 8. 已知限制

- 共享购物车的实时同步使用 5 秒轮询，不是 WebSocket 实时推送
- 结算时即关闭会话；若支付失败，订单保留为「未支付」，可从订单列表继续支付
- 不同二维码之间的订单**不合并结账**，各自独立支付
- 会话 ID 为自增数字，已登录用户理论上可构造请求修改其他桌的会话；生产环境建议增加会话参与者校验或改为随机 session key
- 小程序码生成需要微信 AppID/Secret；当前二维码为 H5 链接，小程序端已支持解析 `scene`
- 后厨打印、微信支付商户配置仍需外部资源
