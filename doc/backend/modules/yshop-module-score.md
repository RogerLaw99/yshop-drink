# 积分模块（yshop-module-score）

> 模块路径：`yshop-drink-boot3/yshop-module-score`；文档依据本仓库源码整理。

## 1. 模块定位

积分模块实现“积分商城”能力，不负责积分的产生与账户余额存储：

- 管理端维护积分商品（标题、主图、组图、详情、所需积分、库存、销量、权重、上下架）；
- 用户端浏览积分商品、提交兑换订单；
- 兑换时校验商品、收货地址、库存和用户积分，扣减会员积分、扣减商品库存并增加销量；
- 提供用户端积分订单列表、详情、确认收货；
- 提供管理端积分订单查询、修改（发货）、删除、导出、确认收货。

会员积分余额与积分流水属于 `yshop-module-member`：余额字段为 `yshop_user.integral`，流水表为 `yshop_user_bill`。本模块通过直接依赖 `member-biz` 调用其 Service 完成积分扣减与流水写入。

## 2. 目录结构

```text
yshop-module-score/
├── pom.xml                          # 聚合 pom
├── yshop-module-score-api/
│   ├── pom.xml
│   └── src/main/java/co/yixiang/yshop/module/score/enums/
│       ├── ErrorCodeConstants.java  # 积分商城错误码
│       └── OrderStatusEnum.java     # -1 全部、0 待发货、1 待收货、2 已完成、6 已删除
└── yshop-module-score-biz/
    ├── pom.xml
    └── src/main/java/co/yixiang/yshop/module/score/
        ├── controller/
        │   ├── admin/
        │   │   ├── scoreorder/      # ScoreOrderController + vo
        │   │   └── scoreproduct/    # ScoreProductController + vo
        │   └── app/
        │       ├── order/           # AppScoreOrderController + param/vo
        │       └── product/         # AppScoreProductController + param/vo
        ├── convert/                 # ScoreOrderConvert、ScoreProductConvert
        ├── dal/
        │   ├── dataobject/
        │   │   ├── scoreorder/ScoreOrderDO.java
        │   │   └── scoreproduct/ScoreProductDO.java
        │   └── mysql/               # ScoreOrderMapper、ScoreProductMapper
        └── service/
            ├── scoreorder/{ScoreOrderService,AppScoreOrderService}(+Impl)
            └── scoreproduct/{ScoreProductService,AppScoreProductService}(+Impl)
```

说明：

- 没有 Mapper XML，全部使用 MyBatis-Plus `BaseMapperX` / `LambdaQueryWrapperX`。
- `score-api` 当前只有枚举和错误码，没有跨模块 API 接口/DTO；其它模块也没有调用积分商城。
- 管理端和用户端使用两套 Service：`ScoreOrderService` / `ScoreProductService` 面向后台，`AppScoreOrderService` / `AppScoreProductService` 面向用户端。

## 3. 核心功能

| 功能 | 入口类 | 说明 |
|---|---|---|
| 积分商品管理 | `ScoreProductController` + `ScoreProductServiceImpl` | 商品 CRUD、分页、列表、Excel 导出 |
| 积分商品展示 | `AppScoreProductController` + `AppScoreProductServiceImpl` | 用户端商品列表（按权重倒序）、商品详情 |
| 积分兑换下单 | `AppScoreOrderController#submit` + `AppScoreOrderServiceImpl#submit` | 校验商品、地址、库存、积分；生成订单；扣积分、扣库存、加销量、写流水 |
| 我的积分订单 | `AppScoreOrderController#orderList` | 按 `type`（-1 全部、0 待发货、1 待收货、2 已完成）分页 |
| 订单详情 | `AppScoreOrderController#orderList(id)` | 校验订单属于当前用户，补充商品信息与状态文案 |
| 用户确认收货 | `AppScoreOrderController#take` | 将 `have_received` 置 1 |
| 后台订单管理 | `ScoreOrderController` + `ScoreOrderServiceImpl` | 分页、详情、更新（发货）、删除、导出、确认收货 |

## 4. 主要接口

> 框架根据包名自动添加前缀：`controller.admin.**` → `/admin-api`，`controller.app.**` → `/app-api`。下表路径不含前缀。

### 4.1 管理端 /admin-api

#### 积分商城订单 `ScoreOrderController`（`@RequestMapping("/score/order")`）

| 方法 | 路径 | 说明 | 权限注解 |
|---|---|---|---|
| PUT | `/score/order/update` | 更新订单（源码中强制 `haveDelivered = 1`，用于发货） | `score:order:update` |
| DELETE | `/score/order/delete?id=` | 删除订单 | `score:order:delete` |
| GET | `/score/order/get?id=` | 订单详情（含商品、用户信息） | `score:order:query` |
| GET | `/score/order/list?ids=` | 批量获得订单 | `score:order:query` |
| GET | `/score/order/page` | 订单分页 | `score:order:query` |
| GET | `/score/order/export-excel` | 导出订单 Excel | `score:order:export` |
| GET | `/score/order/take?id=` | 后台确认收货 | `score:order:update` |

权限注解均为 `@PreAuthorize("@ss.hasPermission('...')")` 形式。

#### 积分商品 `ScoreProductController`（`@RequestMapping("/score/product")`）

| 方法 | 路径 | 说明 | 权限注解 |
|---|---|---|---|
| POST | `/score/product/create` | 创建积分商品 | `score:product:create` |
| PUT | `/score/product/update` | 更新积分商品 | `score:product:update` |
| DELETE | `/score/product/delete?id=` | 删除积分商品 | `score:product:delete` |
| GET | `/score/product/get?id=` | 商品详情 | `score:product:query` |
| GET | `/score/product/list?ids=` | 批量获得商品 | `score:product:query` |
| GET | `/score/product/page` | 商品分页 | `score:product:query` |
| GET | `/score/product/export-excel` | 导出商品 Excel | `score:product:export` |

### 4.2 用户端 /app-api

#### 积分订单 `AppScoreOrderController`（`@RequestMapping("/score-order")`）

| 方法 | 路径 | 说明 | 登录注解 |
|---|---|---|---|
| POST | `/score-order/submit` | 提交积分兑换订单，参数：`productId`、`addressId`、`num` | `@PreAuthenticated` |
| GET | `/score-order/list?type=&page=&limit=` | 我的积分订单列表 | `@PreAuthenticated` |
| GET | `/score-order/detail?id=` | 订单详情 | `@PreAuthenticated` |
| GET | `/score-order/take?id=` | 确认收货 | `@PreAuthenticated` |

`type` 取值：`-1` 全部、`0` 待发货、`1` 待收货、`2` 已完成。

#### 积分商品 `AppScoreProductController`（`@RequestMapping("/score-product")`）

| 方法 | 路径 | 说明 | 登录注解 |
|---|---|---|---|
| GET | `/score-product/list?page=&limit=` | 积分商品列表，按 `weigh` 倒序 | 无 |
| GET | `/score-product/detail?id=` | 积分商品详情 | 无 |

## 5. 核心实现逻辑

### 5.1 积分商品查询

- `AppScoreProductServiceImpl#getList(page, limit)`：`LambdaQueryWrapper` 按 `weigh DESC` 排序后分页，转换为 `AppScoreProductVO`。
- `AppScoreProductServiceImpl#getDetail(id)`：按主键查询后转换；不存在时返回 `null`。
- 列表与详情均未校验 `is_switch`（上下架）和库存，下架商品仍可被查询到。

### 5.2 积分兑换下单（核心流程）

入口：`AppScoreOrderServiceImpl#submit(uid, AppScoreOrderParam)`，方法带 `@Transactional(propagation = REQUIRED, rollbackFor = Exception.class)`。

1. 按 `productId` 查询 `ScoreProductDO`，不存在抛 `PRODUCT_NOT_EXISTS`；
2. 按 `addressId` 查询 `UserAddressDO`，不存在抛 `USER_ADDRESS_NOT_EXISTS`；
3. 查询当前会员 `MemberUserDO`；
4. 校验库存：`stock <= 0` 抛 `PRODUCT_NOT_STOCK`（注意：只判断是否为 0，不判断是否小于购买数量）；
5. 校验积分：`memberUserDO.integral.intValue() < product.score` 抛 `SCORE_NOT`；
6. 使用 `IdUtil.getSnowflake(0, 0).nextIdStr()` 生成订单号；
7. 构造 `ScoreOrderDO`：
   - `havePaid = 1`，表示积分订单无需在线支付；
   - 收货信息来自所选地址（`address + detail`、`realName`、`phone`）；
   - `score = product.score`，`totalScore = product.score`；
   - `number = param.num`；
8. 保存订单；
9. 扣减会员积分：`userService.decScore(uid, scoreOrderDO.getScore())`；
10. 扣库存、加销量：`deStockIncSale(productId, num)`；
11. 写积分支出流水：`billService.expend(uid, "积分兑换", "integral", "pay_product", score, 当前积分, "...")`。

### 5.3 库存扣减与并发控制

`deStockIncSale` 使用 Redisson 分布式锁，锁 key 固定为 `score:order:stock:lock`：

```java
RLock lock = redissonClient.getLock("score:order:stock:lock");
if (lock.tryLock()) {
    try {
        scoreProductMapper.decStockIncSales(number, productId);
    } finally {
        lock.unlock();
    }
}
```

对应 SQL：

```sql
update yshop_score_product
   set stock = stock - #{num}, sales = sales + #{num}
 where id = #{productId} and stock >= #{num}
```

说明：锁为全局锁而非按商品加锁；`tryLock()` 未设置等待时间，获取失败时静默跳过扣库存。

### 5.4 用户订单列表与详情

- `orderList(uid, type, page, limit)`：
  - 固定 `uid`，按 id 倒序；
  - `type` 经 `OrderStatusEnum.toType()` 转换：
    - `-1` 全部，不加状态条件；
    - `0` 待发货：`have_paid = 1`、`have_delivered = 0`、`have_received = 0`；
    - `1` 待收货：`have_paid = 1`、`have_delivered = 1`、`have_received = 0`；
    - `2` 已完成：`have_paid = 1`、`have_received = 1`（不限制是否发货）；
  - 分页后为每条订单补充 `AppScoreProductVO` 和 `statusText`。
- `orderDetail(uid, id)`：按 `uid + id` 查询，转换为 VO 后补充商品与状态文案；查不到时不做空值保护。
- `take(uid, id)`：按 `uid + id` 查订单，不存在抛 `ORDER_NOT_EXISTS`，否则把 `have_received` 置 1。

状态文案：待发货 / 待收货 / 已完成。

### 5.5 管理端订单处理

- `getOrderPage`：使用 `ScoreOrderMapper#selectPage`，实际只使用 `orderId`、`customerName`、`customerPhone` 和 `type` 过滤；其它 VO 字段（uid、productId、时间等）未参与查询。
- `getOrder(id)`：补充商品信息和下单用户信息（`UserRespVO`）。
- `updateOrder`：校验存在后按 VO 更新，并把 `haveDelivered` 强制置 1。
- `takeOrder`：校验存在后把 `haveReceived` 置 1。
- `deleteOrder`：逻辑删除（`deleted` 字段）。
- `exportOrderExcel`：按导出条件查询后写 Excel。

### 5.6 管理端商品管理

标准 CRUD：`createProduct` / `updateProduct` / `deleteProduct` / `getProduct` / `getProductList` / `getProductPage` / `getProductList(export)`；更新、删除前校验商品存在。删除为逻辑删除。

## 6. 数据模型

### 6.1 `yshop_score_product`（`ScoreProductDO`，继承 `BaseDO`）

| 字段 | 含义 |
|---|---|
| `id` | 商品 ID |
| `title` | 商品标题 |
| `image` | 主图 |
| `images` | 组图，`StringListTypeHandler` 负责 JSON/字符串转换 |
| `desc` | 详情（列名使用反引号 `` `desc` ``） |
| `score` | 兑换所需积分 |
| `weigh` | 权重，列表按其倒序 |
| `stock` | 库存 |
| `sales` | 销量 |
| `is_switch` | 是否上架：0 否、1 是 |
| `deleted` / `create_time` / `update_time` / `creator` / `updater` / `tenant_id` | 审计与逻辑删除字段 |

### 6.2 `yshop_score_order`（`ScoreOrderDO`，继承 `BaseDO`）

| 字段 | 含义 |
|---|---|
| `id` | 订单主键 |
| `uid` | 会员 ID |
| `order_id` | 业务订单号（雪花 ID 字符串） |
| `product_id` | 积分商品 ID |
| `number` | 购买数量 |
| `score` | 单个商品所需积分 |
| `total_score` | 总消耗积分（源码实际写入单个商品积分） |
| `ip` | 下单 IP |
| `express_sn` / `express_number` / `express_company` | 快递公司编码、快递单号、快递公司 |
| `customer_name` / `customer_phone` / `customer_address` | 收货人、电话、地址 |
| `status` | 订单状态：0 取消、1 正常（布尔字段） |
| `have_paid` | 是否已支付：0 否、1 是 |
| `have_delivered` | 是否已发货：0 否、1 是 |
| `have_received` | 是否已收货：0 否、1 是 |
| `deleted` / `create_time` / `update_time` / `creator` / `updater` / `tenant_id` | 审计与逻辑删除字段 |

## 7. 依赖与集成

| 依赖 | 用途 |
|---|---|
| `yshop-module-score-api` | 本模块枚举、错误码 |
| `yshop-module-member-biz` | `MemberUserService`（用户查询、积分扣减）、`AppUserAddressService`（收货地址）、`UserBillService`（积分流水）、`BillDetailEnum`、`MemberUserDO`/`UserAddressDO` |
| `yshop-spring-boot-starter-redis` | Redisson 分布式锁 |
| `yshop-spring-boot-starter-mybatis` | MyBatis-Plus 持久层 |
| `yshop-spring-boot-starter-security` | `@PreAuthenticated` 用户端登录校验 |
| `yshop-spring-boot-starter-excel` | 订单/商品 Excel 导出 |
| `yshop-server` | 直接依赖 `yshop-module-score-biz`，加载本模块 Controller |

调用关系：

- 用户端 `/app-api/score-order/**`、`/app-api/score-product/**`；
- 后台 `/admin-api/score/order/**`、`/admin-api/score/product/**`；
- 积分账户与流水完全落在 member 模块，本模块不维护积分账户表。

## 8. 注意事项

1. **数量未参与积分计算**：`submit` 中 `totalScore` 和 `decScore` 都只使用单个商品积分 `product.score`，未乘以 `num`；而库存扣减使用 `num`。购买多件时积分少扣，属于明显的业务/金额风险。
2. **流水余额记录不准确**：写积分流水时传入的是扣减前查出的 `memberUserDO.getIntegral()`，不是扣减后的余额。
3. **库存校验不完整**：只判断 `stock <= 0`，未判断 `stock >= num`；最终 SQL 带 `stock >= num` 条件，但 Mapper 返回值未被检查，扣减失败不会回滚订单。
4. **分布式锁使用不规范**：使用全局固定锁 key，`tryLock()` 无超时；获取失败时直接跳过库存扣减且不报错；`unlock()` 前未判断当前线程是否持有锁。
5. **错误码冲突**：`SCORE_NOT` 与 `PRODUCT_NOT_STOCK` 都是 `1008018002`，无法区分“积分不足”和“库存不足”；同时与优惠券模块的 `1008018000`～`1008018002` 段重叠。
6. **状态流转不严格**：用户 `take` 不要求已发货；后台 `update` 无论传入什么都会把 `haveDelivered` 置 1；`status` 布尔字段未参与业务判断。
7. **商品上下架未过滤**：用户端列表和详情未过滤 `is_switch`，下架商品仍可查看；下单也未校验上下架状态。
8. **管理端缺少创建订单接口**：存在 `ScoreOrderCreateReqVO` 和菜单权限 `score:order:create`，但 `ScoreOrderController` 没有 `create` 接口。
9. **管理端分页过滤字段有限**：`ScoreOrderPageReqVO` 中的多数字段未在 Mapper 中使用。
10. **事务边界**：下单方法有事务，但库存扣减使用独立 Redisson 锁且异常处理有限；积分扣减使用 SQL 直接更新，建议关注并发与回滚一致性。
