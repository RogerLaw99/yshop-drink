# yshop-module-order（订单模块）

> 模块路径：`yshop-drink-boot3/yshop-module-mall/yshop-module-order-api`、`yshop-drink-boot3/yshop-module-mall/yshop-module-order-biz`；文档依据本仓库源码整理。

## 1. 模块定位

订单模块是点餐/SaaS 系统的交易核心，负责把「门店 + 商品 SKU + 用户购物意图」落成订单，并贯通支付、履约、退款、统计全过程。具体职责包括：

- 用户端下单：堂食扫码（`desk`）、到店自取（`takein`）、外卖（`takeout`）三种订单类型。
- 价格计算：商品 SKU 单价 × 数量、门店配送费、优惠券抵扣、积分赠送。
- 库存扣减与回滚：下单扣 SKU/商品库存并加销量，取消/退款时回滚。
- 支付编排：微信 H5/公众号/小程序、支付宝 H5、余额支付；对接 `pay` 模块的 `PayServiceManager`。
- 订单状态与操作流水：支付、发货、收货、退款、删除等状态变更均写入订单操作记录。
- 超时任务：未支付 30 分钟自动取消、发货后 60 分钟自动确认收货（Redisson 延迟队列 + `message` 模块监听器）。
- 后台订单管理与经营统计：分页、详情、发货、线下支付、退款、导出、今日/昨日/本月成交统计。

该模块不维护服务端购物车表。用户端把商品 ID、规格、数量数组直接提交给下单接口，订单创建成功后再异步保存订单购物明细。

## 2. 目录结构

```text
yshop-module-order-api/
└── src/main/java/co/yixiang/yshop/module/order/
    └── enums/                      # 订单状态、支付方式、操作类型、错误码等
        ├── AdminOrderStatusEnum.java
        ├── OrderStatusEnum.java
        ├── OrderLogEnum.java
        ├── PayTypeEnum.java
        ├── UpdateOrderEnum.java
        └── ErrorCodeConstants.java

yshop-module-order-biz/
└── src/main/java/co/yixiang/yshop/module/order/
    ├── controller/
    │   ├── admin/storeorder/       # 管理后台订单接口
    │   │   ├── StoreOrderController.java
    │   │   └── vo/                 # 创建/更新/分页/导出/退款等 VO
    │   └── app/order/              # 用户端订单接口
    │       ├── AppOrderController.java
    │       ├── param/              # 下单、支付、退款、收货等参数
    │       └── vo/                 # AppStoreOrderQueryVo 等
    ├── service/
    │   ├── storeorder/             # 订单主服务 + 异步统计服务
    │   ├── storeordercartinfo/     # 订单购物明细
    │   └── storeorderstatus/       # 订单操作流水
    ├── dal/
    │   ├── dataobject/             # storeorder / cartinfo / status / ordernumber
    │   ├── mysql/                  # MyBatis-Plus Mapper
    │   └── redis/                  # 订单缓存、统计缓存、打印 token
    ├── convert/storeorder/         # MapStruct 转换器
    ├── handle/                     # 延迟队列监听器（未支付取消、自动收货）
    └── mq/consumer/                # 支付成功消息消费者
```

`-api` 产物只有枚举与错误码，没有对外 RPC 接口；订单能力目前通过 REST Controller 暴露。

## 3. 核心功能

| 功能 | 入口类 | 说明 |
|---|---|---|
| 用户下单 | `AppOrderController#create` → `AppStoreOrderServiceImpl#createOrder` | 校验桌号/地址，计算金额，生成订单号，扣库存，写订单明细 |
| 用户支付 | `AppOrderController#pay` → `AppStoreOrderServiceImpl#pay` | 按支付方式组装微信/支付宝支付参数，余额支付直接扣款 |
| 支付成功处理 | `AppStoreOrderServiceImpl#paySuccess` | 更新支付状态、流水、购买次数，发送微信订阅消息 |
| 支付回调 | `AppOrderController#payBack` | 委托 `PayServiceManager.payBack` 处理第三方异步通知 |
| 支付消息消费 | `PayNoticeConsumer#onMessage` | 消费 `PayNoticeMessage`，调用 `paySuccess` |
| 订单列表/详情 | `AppOrderController#orderList/detail` | 按状态筛选，组装购物明细、门店、排队号、状态文案 |
| 确认收货 | `AppOrderController#take` → `takeOrder` | 已完成订单发放赠送积分 |
| 申请退款 | `AppOrderController#refund` → `orderApplyRefund` | 仅记录退款申请，后台审核 |
| 取消订单 | `AppOrderController#cancel` → `cancelOrder` | 仅未支付订单，回滚库存/优惠券并删除订单 |
| 订单删除 | `AppOrderController#del` → `removeOrder` | 仅已完成订单可删除 |
| 个人中心统计 | `AppOrderController#user_count` | 从 Redis 读取异步计算的订单数量/金额 |
| 后台订单管理 | `StoreOrderController` | 增删改查、分页、发货、线下支付、收货、退款、导出 |
| 经营数据统计 | `AsyncStoreOrderService#getOrderTimeData` | 异步汇总今日/昨日/本月/上周成交额与订单数 |
| 超时取消 | `OrderUnPayListener` | 监听延迟队列，30 分钟后取消未支付订单 |
| 自动收货 | `OrderAutoConfirmListener` | 监听延迟队列，60 分钟后自动确认收货 |

## 4. 主要接口

> 路由前缀由框架按包名自动添加：`controller/admin/**` → `/admin-api`，`controller/app/**` → `/app-api`。下表路径已包含实际前缀。

### 4.1 管理端 /admin-api

Controller：`StoreOrderController`（`@RequestMapping("/order/store-order")`）

| 方法 | 路径 | 说明 | 权限注解 |
|---|---|---|---|
| POST | `/admin-api/order/store-order/create` | 创建订单（后台直接插入） | `order:store-order:create` |
| PUT | `/admin-api/order/store-order/update` | 更新订单；`updateType=orderSend` 时执行发货逻辑 | `order:store-order:update` |
| DELETE | `/admin-api/order/store-order/delete` | 逻辑删除订单（`isSystemDel=1`） | `order:store-order:delete` |
| GET | `/admin-api/order/store-order/pay` | 订单线下支付（现金） | `order:store-order:delete`（源码如此） |
| GET | `/admin-api/order/store-order/take` | 后台确认收货 | `order:store-order:delete`（源码如此） |
| GET | `/admin-api/order/store-order/get?id=` | 订单详情（含用户、购物明细、状态文案） | `order:store-order:query` |
| GET | `/admin-api/order/store-order/list?ids=` | 按 ID 集合获得订单 | `order:store-order:query` |
| GET | `/admin-api/order/store-order/page` | 订单分页 | `order:store-order:query` |
| GET | `/admin-api/order/store-order/record-list?id=` | 订单操作记录列表 | `order:store-order:query` |
| GET | `/admin-api/order/store-order/export-excel` | 导出订单 Excel | `order:store-order:export` |
| GET | `/admin-api/order/store-order/count` | 订单统计（异步刷新后读 Redis） | 无 |
| POST | `/admin-api/order/store-order/refund` | 订单退款（`id`、`payPrice`） | `order:store-order:update` |
| GET | `/admin-api/order/store-order/notice` | 当前门店最近 30 秒已支付订单数 | 无 |

分页请求 `StoreOrderPageReqVO` 支持：`orderId`、`numberId`、`shopId`、`realName`、`userPhone`、`orderType`、`createTime` 区间、`orderStatus`（0 未支付/1 未发货/2 待收货/3 待评价/4 交易完成/5 已退款/6 已删除）、`payStatus`（实际按 `pay_type` 过滤）、`type=BUSINESS`（工作台视图）。

### 4.2 用户端 /app-api

Controller：`AppOrderController`（`@RequestMapping("/order")`）

| 方法 | 路径 | 说明 | 鉴权 |
|---|---|---|---|
| POST | `/app-api/order/create` | 创建订单 | `@PreAuthenticated` |
| POST | `/app-api/order/pay` | 发起支付，返回支付参数 | `@PreAuthenticated` |
| ANY | `/app-api/order/notify/payBack{detailsId}.json` | 第三方支付异步回调 | 无 |
| GET | `/app-api/order/list?type=&page=&limit=` | 订单列表；`type`：-1 全部、0 未支付、1 待发货、2 待收货、3 待评价（源码未启用）、4 已完成、-3 退款 | `@PreAuthenticated` |
| GET | `/app-api/order/detail/{key}` | 订单详情，`key` 可为订单号/唯一值/扩展订单号 | `@PreAuthenticated` |
| POST | `/app-api/order/take` | 确认收货，参数 `uni` | `@PreAuthenticated` |
| POST | `/app-api/order/refund` | 申请退款 | 无（但代码调用 `getLoginUserId()`） |
| POST | `/app-api/order/del` | 删除已完成订单 | `@PreAuthenticated` |
| POST | `/app-api/order/cancel` | 取消未支付订单，参数 `id` | `@PreAuthenticated` |
| POST | `/app-api/order/user_count` | 个人中心订单统计 | `@PreAuthenticated` |
| GET | `/app-api/order/getShop?shopId=` | 获取店铺信息，固定返回 `isEmpty=true` | `@PreAuthenticated` |

下单参数 `AppOrderParam` 关键字段：

- `shopId`：门店 ID。
- `orderType`：`takein` 自取、`takeout` 外卖、`desk` 扫码点餐。
- `productId[]`、`spec[]`、`number[]`：商品 ID、SKU 规格串、数量，三者按下标一一对应。
- `addressId`：外卖必填。
- `couponId`：优惠券用户记录 ID。
- `gettime`：取餐时间（分钟偏移）。
- `deskNumber`、`deskId`、`deskPeople`：堂食信息。
- `remark`、`payType`、`orderId`：备注、支付方式、堂食复用的订单号。

## 5. 核心实现逻辑

### 5.1 下单流程

`AppStoreOrderServiceImpl#createOrder(Long uid, AppOrderParam param)`：

1. 若 `orderType=desk` 且 `deskNumber` 为空，抛出 `STORE_ORDER_DESK_NOT`。
2. 按 `param.shopId` 查询 `StoreShopDO`，取配送费 `deliveryPrice` 作为邮费。
3. 获取 Redisson 锁 `cart:check:stock:lock`，逐个商品调用 `AppStoreProductService#checkProductStock` 校验上下架与 SKU 库存；同时按 SKU（`spec` 中的 `|` 替换为 `,`）查询 `StoreProductAttrValueDO.price` 累加商品金额。
4. 若使用优惠券：读取 `CouponUserDO`，校验门槛 `least <= sumPrice`，累加 `couponPrice`，并把优惠券状态置为已使用（`status=1`）。
5. 计算应付金额：
   - 外卖：`sumPrice + deliveryPrice - couponPrice - deductionPrice`
   - 自取/堂食：`sumPrice - couponPrice - deductionPrice`
   - `deductionPrice` 当前恒为 0。
6. 计算赠送积分：累加每个商品的 `giveIntegral`（按商品个数，不乘购买数量）。
7. 生成订单：
   - 雪花 ID 作为 `orderId`（字符串订单号）。
   - 插入 `yshop_order_number`，把返回主键作为取餐号 `numberId`。
   - `getTime = now + gettime 分钟`，写入 `shopId`、`shopName`、`uid`、`orderType`。
   - 外卖：要求 `addressId` 非空，读取 `UserAddressDO` 写入收货人、电话、地址。
   - 金额、优惠、积分、备注等写入 `yshop_store_order`，`paid=0`、`status=0`。
8. `deStockIncSale`：获取锁 `cart:do:stock:lock`，对每个 SKU 执行条件更新 `stock = stock - num, sales = sales + num WHERE stock >= num`，同时更新商品总库存/销量。
9. 异步 `StoreOrderCartInfoServiceImpl#saveCartInfo` 写入 `yshop_store_order_cart_info`（商品名、图片、规格、单价、数量）。
10. 写入订单操作记录（`OrderLogEnum.CREATE_ORDER`）。
11. 非堂食订单投递 Redisson 延迟队列 `order-unpay-cancel-queue`，延迟 `ORDER_OUTTIME_UNPAY=30` 分钟。
12. 返回 `{ orderId }`。

### 5.2 支付流程

`AppStoreOrderServiceImpl#pay`：

1. 用 `uni` + `uid` 查询订单；同时兼容余额充值流水 `UserBillDO`。
2. 订单已支付或流水已支付时抛 `ORDER_PAY_FINISH`。
3. 按 `PayTypeEnum` 分支：
   - `weixin` + `from=H5`：`MWEB` 下单，返回 `mweb_url` 并拼接同步跳转地址。
   - `weixin` + `from=WECHAT`：公众号 JSAPI，使用 `MemberUserDO.openid`。
   - `weixin` 其他：小程序 JSAPI，使用 `routineOpenid`。
   - `yue`：校验余额，`decPrice` 扣减后直接 `paySuccess`。
   - `alipay`：`WAP` 支付宝 H5 下单。
4. 第三方支付成功后，由支付回调或 `PayNoticeConsumer` 调用 `paySuccess(orderId, payType)`。

`paySuccess` 主要动作：

- 若命中充值流水，则完成流水并把充值金额加到用户余额。
- 更新订单 `paid=1`、`payTime`、`payType`。
- 增加用户购买次数、写消费流水、写订单操作记录。
- 小程序用户且非堂食订单，通过 `WeixinNoticeProducer` 发送支付成功订阅消息。
- 使用 `@TenantIgnore` 忽略租户上下文（当前多租户未启用）。

### 5.3 订单状态流转

订单状态由三个维度组合：`paid`（是否支付）、`status`（履约阶段）、`refundStatus`（退款状态），另加 `isSystemDel`（系统删除）。

| paid | status | refundStatus | isSystemDel | 用户端展示 | 后台状态 |
|---|---|---|---|---|---|
| 0 | 0 | 0 | 0 | 未支付 | 未支付 |
| 1 | 0 | 0 | 0 | 制作中/待发货 | 未发货 |
| 1 | 1 | 0 | 0 | 待取餐/配送中 | 待收货 |
| 1 | 2 | 0 | 0 | 已取餐/已收货 | 待评价 |
| 1 | 3 | 0 | 0 | 交易完成 | 交易完成 |
| 1 | * | 1 | 0 | 申请退款中 | 退款中 |
| 1 | * | 2 | 0 | 已退款 | 已退款 |
| * | * | * | 1 | — | 已删除 |

状态常量：

- `paid`：`OrderInfoEnum.PAY_STATUS_0/1`。
- `status`：`0` 待发货/制作中、`1` 待收货/配送中、`2` 已收货/已取餐、`3` 已完成。
- `refundStatus`：`0` 正常、`1` 退款中、`2` 已退款。
- 订单类型：`OrderLogEnum.ORDER_TAKE_DESK/TAKE_OUT/TAKE_IN`。

后台发货 `StoreOrderServiceImpl#updateStoreOrder`：

- `updateType=orderSend` 且 `orderType=takein`：直接调用 `takeStoreOrder`（自取即完成）。
- `updateType=orderSend` 且 `orderType=takeout`：`status=1`（配送中），并投递 60 分钟自动收货延迟队列。
- `orderType=desk`：仅记录发货操作（堂食为商业版功能，源码保留 TODO）。

### 5.4 取消、超时与自动收货

- 用户取消：仅 `paid=0` 可取消；回滚库存、回滚优惠券状态，然后物理删除订单。
- 未支付超时：延迟队列消息由 `OrderUnPayListener` 消费，调用 `cancelOrder(orderId, null)`；若订单不存在或已支付则不处理。
- 自动收货：`OrderAutoConfirmListener` 消费 `order:unconfirm:` 队列，调用 `takeOrder(orderId, null)`。
- 两个监听器均实现 `message` 模块的 `DelayedQueueListener<OrderMsg>`，并通过 `TenantUtils.executeIgnore` 忽略租户。
- `handle/RedisDelayHandle` 是旧的 `@PostConstruct` 线程轮询实现，已标 `@Deprecated` 且 `@Component` 被注释，不参与运行。

### 5.5 退款

- 用户申请：`orderApplyRefund` 只把 `refundStatus` 置 1，保存原因、说明、图片、申请时间，并写操作记录。
- 后台确认：`StoreOrderServiceImpl#orderRefund(id, price, type, salesId)`：
  - 校验订单、用户、已退款状态、退款金额不超过 `payPrice`。
  - 余额支付：`MemberUserService#incMoney` 退回余额。
  - 微信支付：调用 `PayServiceManager#refund`（小程序商户配置 `PayIdEnum.WX_MINIAPP`），先做退款查询防止重复退款；`yshop.demo=true` 时禁止微信退款。
  - 支付宝：源码明确抛出「支付宝暂时不支持退款」。
  - 更新 `refundStatus=2`、`refundPrice`，写退款流水、操作记录，并回滚库存。
- 退款回滚库存使用订单购物明细 `spec`（`|` 转 `,`）恢复 SKU 库存。

### 5.6 统计

- `StoreOrderStatusServiceImpl#create` 每次写操作记录后异步调用 `AsyncStoreOrderService#orderData(uid)`，计算用户订单数、支付总额、未支付/未发货/待收货/待评价/已完成数量，写入 Redis `yshop_order_count_cache:{uid}`。
- `getOrderTimeData` 计算今日、昨日、本月、上周成交额与订单数，并叠加用户总数、订单总数、总交易额、商品数，写入 `yshop_admin_order_count_cache:`。
- 统计类使用 `@Async` 注解，`count` 接口触发异步任务后立即读取 Redis，存在读到旧值的可能。

## 6. 数据模型

所有表均为 `yshop_` 前缀；继承 `BaseDO` 的实体额外包含 `create_time`、`update_time`、`creator`、`updater`、`deleted`。

### yshop_store_order（订单主表）

`StoreOrderDO` 关键字段：

| 字段 | 含义 |
|---|---|
| `id` | 主键 |
| `order_id` | 业务订单号（雪花 ID） |
| `extend_order_id` | 扩展订单号 |
| `uid` | 会员用户 ID |
| `real_name` / `user_phone` / `user_address` | 收货信息（外卖） |
| `cart_id` | 购物车 ID（当前下单写入空串） |
| `total_num` | 商品总件数 |
| `total_price` / `pay_price` | 商品总额 / 实付金额 |
| `total_postage` / `pay_postage` / `freight_price` | 邮费相关 |
| `coupon_id` / `coupon_price` | 优惠券及抵扣金额 |
| `deduction_price` | 抵扣金额（当前恒 0） |
| `paid` / `pay_time` / `pay_type` | 支付状态、时间、方式 |
| `order_type` | `takein/takeout/desk` |
| `status` | 履约状态 |
| `refund_status` / `refund_price` / `refund_reason_*` | 退款状态与原因 |
| `gain_integral` / `use_integral` / `pay_integral` / `back_integral` | 积分相关 |
| `shop_id` / `shop_name` | 门店 |
| `number_id` | 取餐号（`yshop_order_number.id`） |
| `get_time` | 预计取餐时间 |
| `is_system_del` | 后台逻辑删除标记 |
| `unique` | 唯一值，用于前端确认页/查询 |
| `out_trade_no` | 退款单号 |
| `delivery_*` | 配送/快递信息 |

### yshop_store_order_cart_info（订单购物明细）

`StoreOrderCartInfoDO`：`oid`（订单 ID）、`order_id`、`cart_id`、`product_id`、`cart_info`、`unique`、`is_after_sales`、`title`、`image`、`number`、`spec`、`price`。

### yshop_store_order_status（订单操作记录）

`StoreOrderStatusDO`：`oid`、`change_type`（对应 `OrderLogEnum`）、`change_message`、`change_time`。

### yshop_order_number（取餐号）

`OrderNumberDO`：`id`、`order_id`。下单时插入一条记录，主键即取餐号。

### 关键枚举

- `OrderStatusEnum`：用户端列表筛选类型。
- `AdminOrderStatusEnum`：后台订单状态筛选类型。
- `OrderLogEnum`：订单操作类型（创建、支付、发货、收货、退款、删除等）。
- `PayTypeEnum`：`cash/alipay/weixin/yue/integral`。
- `UpdateOrderEnum`：`updateOrder/orderSend/remark/sendInfo`。
- `ErrorCodeConstants`：订单错误码段 `1008007xxx`。

## 7. 依赖与集成

### Maven 直接依赖（order-biz）

- `yshop-module-order-api`
- `yshop-module-pay-biz`：`PayServiceManager`、支付消息 `PayNoticeMessage`。
- `yshop-module-message-biz`：微信订阅消息、Redis 延迟队列抽象。
- `yshop-module-product-biz`：商品、SKU、库存服务。

### 代码层直接引用（通过传递依赖）

- `member`：`MemberUserService`、`UserAddressService`、`UserBillService`、`AppUserOrderCountVo`（经 `message-biz → member-biz` 传递）。
- `coupon`：`AppCouponUserService`、`CouponUserDO`（经 `member-biz → coupon-biz` 传递）。
- `store`：`AppStoreShopService`、`StoreShopDO`、`StoreShopConvert`（经 `product-biz → store-biz` 传递）。
- `pay`：`MerchantDetailsService`、`MerchantDetailsDO`。
- `framework.common`：`OrderInfoEnum`、`ShopCommonEnum`、`ShopConstants`。

### 基础设施集成

- **Redisson 延迟队列**：`RBlockingDeque` + `RDelayedQueue`；队列 key 定义在 `ShopConstants`。
- **Redis Stream MQ**：`PayNoticeConsumer` 消费支付通知。
- **Redis 缓存**：用户订单统计、后台统计、确认订单缓存、打印机 token。
- **微信订阅消息**：`WeixinNoticeProducer`。
- **第三方支付**：`com.egzosn.pay.spring.boot.core.PayServiceManager`。

## 8. 注意事项

1. **权限注解存在误用**：`/pay`、`/take` 使用 `order:store-order:delete`；`/count`、`/notice` 没有 `@PreAuthorize`。其中 `/notice` 内部调用 `SecurityFrameworkUtils.getLoginUser().getShopId()`，未登录时可能空指针。
2. **App 退款接口缺少 `@PreAuthenticated`**：`/app-api/order/refund` 没有登录注解，却调用 `getLoginUserId()`，与其它 App 接口不一致。
3. **库存并发**：下单校验与扣减使用两个不同锁，且 `tryLock()` 不带等待时间和租约时间；获取锁失败时不会报错，而是直接跳过校验/扣减，存在超卖或库存未扣风险。建议改为带等待时间并检查返回值。
4. **异步写购物明细**：`saveCartInfo` 标注 `@Async`，下单事务提交前就可能执行查询；且订单创建后立即查询详情，明细可能尚未写入。
5. **取餐号主键映射**：`OrderNumberDO.id` 没有 `@TableId`，但代码依赖插入后的主键回填 `numberId`；在 MyBatis-Plus 默认策略下主键可能无法自动回填，需要结合实际库验证。
6. **优惠券校验**：仅按 `couponId` 查询并校验门槛，未校验该券是否属于当前用户、是否在有效期内；使用时直接置为已使用。
7. **赠送积分**：按商品条目累加 `giveIntegral`，未乘以购买数量。
8. **堂食功能**：`desk` 类型订单的路由、桌台、复用在源码中均为 TODO，仅校验桌号非空；桌面点餐属商业版功能。
9. **支付宝退款**：源码明确不支持；微信退款需要证书，`yshop.demo=true` 时直接拒绝。
10. **后台统计竞态**：`/count` 先触发 `@Async` 刷新再立即读 Redis，可能返回旧数据。
11. **逻辑删除**：后台 `delete` 不物理删除，而是 `is_system_del=1`；用户端 `cancel` 则是物理删除订单，且不回写操作记录（删除前已回滚库存/优惠券）。
12. **easy-trans**：框架全局启用，但 mall 模块的 VO/DO 未使用 `@Trans`，跨模块数据由代码手工拼装。
13. **路由前缀**：Controller 上的 `@RequestMapping` 不包含 `/admin-api`、`/app-api`，由 `WebProperties` 按包名自动添加。
