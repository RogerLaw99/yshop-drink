# 营销模块（yshop-module-marketing，优惠券）

> 模块路径：`yshop-drink-boot3/yshop-module-marketing`；文档依据本仓库源码整理。  
> 当前营销模块下只有优惠券相关子模块：`yshop-module-coupon-api`、`yshop-module-coupon-biz`。

## 1. 模块定位

营销模块当前实现优惠券营销能力：

- 管理端维护优惠券模板：店铺、名称、面额、使用门槛、有效期、可用类型（通用/自取/外卖）、发行数量、限领数量、兑换码、所需积分、使用说明、上下架、权重；
- 用户端查看可领取优惠券、领取优惠券（按优惠券 ID 或兑换码）、查看我的优惠券与可用数量；
- 管理端查看/维护用户领取记录并导出；
- 与订单模块集成：下单时使用用户已领取的优惠券抵扣金额，取消/删除订单时按条件退回优惠券。

模块本身不负责积分扣减：优惠券模板上的 `score` 字段仅存储，领取流程未调用会员积分服务。

## 2. 目录结构

```text
yshop-module-marketing/
├── pom.xml                          # 聚合 pom（packaging=pom）
├── yshop-module-coupon-api/
│   ├── pom.xml
│   └── src/main/java/co/yixiang/yshop/module/coupon/enums/
│       ├── CouponStatusEnum.java    # 0 未使用、1 已使用、2 已失效、4 所有
│       └── ErrorCodeConstants.java  # 优惠券错误码
└── yshop-module-coupon-biz/
    ├── pom.xml
    └── src/main/java/co/yixiang/yshop/module/coupon/
        ├── controller/
        │   ├── admin/
        │   │   ├── coupon/          # CouponController + vo
        │   │   └── couponuser/      # CouponUserController + vo
        │   └── app/coupon/          # AppCouponController + vo
        ├── convert/                 # CouponConvert、CouponUserConvert
        ├── dal/
        │   ├── dataobject/
        │   │   ├── coupon/CouponDO.java
        │   │   └── couponuser/CouponUserDO.java
        │   └── mysql/
        │       ├── coupon/CouponMapper.java
        │       └── couponuser/CouponUserMapper.java
        └── service/
            ├── coupon/{CouponService,AppCouponService}(+Impl)
            └── couponuser/{CouponUserService,AppCouponUserService}(+Impl)
```

说明：

- 没有 Mapper XML，使用 MyBatis-Plus 的 `BaseMapperX` / `LambdaQueryWrapperX`。
- `coupon-api` 只有枚举和错误码，没有跨模块 API 接口；订单模块直接引用 `coupon-biz` 的 `AppCouponUserService` 与 `CouponUserDO`。
- 管理端与用户端使用不同 Service：`CouponService` / `CouponUserService` 面向后台，`AppCouponService` / `AppCouponUserService` 面向用户端。

## 3. 核心功能

| 功能 | 入口类 | 说明 |
|---|---|---|
| 优惠券模板管理 | `CouponController` + `CouponServiceImpl` | 创建、更新、删除、详情、列表、分页、Excel 导出；创建/更新时按店铺 ID 回填店铺名称 |
| 用户优惠券管理 | `CouponUserController` + `CouponUserServiceImpl` | 后台创建、更新、删除、详情、按优惠券查询领取记录、分页、导出 |
| 可领取优惠券列表 | `AppCouponController#myNotList` + `AppCouponServiceImpl#getNotList` | 查询未结束的优惠券，并标记当前用户是否已领取 |
| 领取优惠券 | `AppCouponController#receive` + `AppCouponServiceImpl#receive` | 按 ID 或兑换码领取；校验发行数量和“是否已领取” |
| 我的优惠券 | `AppCouponController#myList` + `AppCouponUserServiceImpl#getList` | 按未使用/已使用/已过期筛选 |
| 可用优惠券数量 | `AppCouponController#getCount` | 统计当前用户在某店铺、某用餐类型下未使用且在有效期内的数量 |
| 下单抵扣优惠券 | 订单模块 `AppStoreOrderServiceImpl#submit` | 按用户优惠券 ID 读取面额，校验门槛后抵扣并置为已使用 |
| 取消订单退券 | 订单模块 `AppStoreOrderServiceImpl#regressionCoupon` | 未支付且未处于特定取消状态时，把已使用优惠券恢复为未使用 |

## 4. 主要接口

> 框架根据包名自动添加前缀：`controller.admin.**` → `/admin-api`，`controller.app.**` → `/app-api`。下表路径不含前缀。  
> `CouponController` 类级路径源码为 `@RequestMapping("/coupon/")`，下列接口按 `/coupon` 展示。

### 4.1 管理端 /admin-api

#### 优惠券模板 `CouponController`（`@RequestMapping("/coupon/")`）

| 方法 | 路径 | 说明 | 权限注解 |
|---|---|---|---|
| POST | `/coupon/create` | 创建优惠券 | `@PreAuthorize("@ss.hasPermission('coupon::create')")` |
| PUT | `/coupon/update` | 更新优惠券 | `coupon::update` |
| DELETE | `/coupon/delete?id=` | 删除优惠券（逻辑删除） | `coupon::delete` |
| GET | `/coupon/get?id=` | 获得优惠券 | `coupon::query` |
| GET | `/coupon/list` | 获得通用优惠券列表（`shop_id = 0`） | `coupon::query` |
| GET | `/coupon/page` | 获得优惠券分页 | `coupon::query` |
| GET | `/coupon/export-excel` | 导出优惠券 Excel | `coupon::export` |

注意权限字符串为两个冒号（如 `coupon::create`），与常见 `模块:实体:动作` 格式不同；数据库中 `system_menu` 的权限值与此保持一致。

#### 用户领取记录 `CouponUserController`（`@RequestMapping("/coupon/user")`）

| 方法 | 路径 | 说明 | 权限注解 |
|---|---|---|---|
| POST | `/coupon/user/create` | 后台创建领取记录 | `coupon:user:create` |
| PUT | `/coupon/user/update` | 更新领取记录 | `coupon:user:update` |
| DELETE | `/coupon/user/delete?id=` | 删除领取记录 | `coupon:user:delete` |
| GET | `/coupon/user/get?id=` | 获得领取记录 | `coupon:user:query` |
| GET | `/coupon/user/list?couponId=` | 按优惠券查询领取记录 | 无（注解被注释） |
| GET | `/coupon/user/page` | 领取记录分页 | 无（注解被注释） |
| GET | `/coupon/user/export-excel` | 导出领取记录 Excel | `coupon:user:export` |

### 4.2 用户端 /app-api

#### 优惠券 `AppCouponController`（`@RequestMapping("/coupon")`）

| 方法 | 路径 | 说明 | 登录注解 |
|---|---|---|---|
| GET | `/coupon/count?shop_id=&type=` | 获取可用优惠券数量 | `@PreAuthenticated` |
| GET | `/coupon/my?shopId=&type=&page=&pagesize=` | 我的优惠券；`type` 0 未使用、1 已使用、2 已过期 | `@PreAuthenticated` |
| GET | `/coupon/not?id=&page=&pagesize=` | 获取未被领取优惠券；`id` 实际被当作 `shopId` 使用 | `@PreAuthenticated` |
| POST | `/coupon/receive` | 领取优惠券，请求体 `{id, code}` | `@PreAuthenticated` |

`/coupon/count` 的 `shop_id`、`type` 参数均未设置默认值，调用时必须传递。

## 5. 核心实现逻辑

### 5.1 后台优惠券模板管理

`CouponServiceImpl`：

- `create`：VO → `CouponDO`，按 `shopId` 查询 `StoreShopDO`，把 `shopName` 写入优惠券后插入；
- `update`：先校验优惠券存在，再按 `shopId` 重新查询店铺并写入 `shopName`，最后更新；
- `delete`：校验存在后逻辑删除；
- `getList()`：固定查询 `shop_id = 0` 的通用优惠券；
- `getPage()`：委托 `CouponMapper#selectPage`；
- `getList(exportReqVO)`：按导出条件查询。

`CouponMapper#selectPage` 带有店铺隔离逻辑：

```text
当前登录用户 shopId == 0  -> 不限制店铺，查询全部
当前登录用户 shopId != 0  -> 强制 reqVO.shopId = 当前店铺
```

导出查询 `selectList` 不包含上述店铺隔离。

### 5.2 后台领取记录管理

`CouponUserServiceImpl` 提供标准 CRUD、按优惠券 ID 查询、分页、导出；创建/更新/删除前会校验记录是否存在。领取记录主键为 `Integer`（对应 `yshop_coupon_user.id`）。

### 5.3 用户端查询

1. **可领取列表** `AppCouponServiceImpl#getNotList(uid, shopId, page, pagesize)`：
   - 条件：`end_time > now`，`shopId` 非空时等值过滤；
   - 按 `weigh DESC` 排序、分页；
   - 逐条统计 `yshop_coupon_user` 中 `(user_id, coupon_id)` 的记录数，`> 0` 则 `isReceive = 1`，否则 `0`；
   - 未过滤 `is_switch`、`start_time` 和剩余发行量。
2. **我的优惠券** `AppCouponUserServiceImpl#getList(uid, shopId, type, page, pagesize)`：
   - `type = 0` 未使用：`status = 0`、`start_time < now`、`end_time > now`；
   - `type = 1` 已使用：`status = 1`、`start_time < now`、`end_time > now`；
   - `type = 2` 已过期：`end_time < now`，不限制 `status`；
   - 其它值（包括枚举中的 `4 所有`）走 `default`，不添加状态条件；
   - 再按 `shopId`（可选）、`userId` 过滤并分页。
3. **可用数量** `AppCouponController#getCount`：
   - `user_id = 当前用户`、`shop_id = 参数`、`status = 0`；
   - `start_time < now < end_time`；
   - `type = 参数` 或 `type = 0`（通用券）。

### 5.4 领取优惠券（核心流程）

`AppCouponServiceImpl#receive(uid, id, code)`，带事务：

1. `id != null` 时按主键查询优惠券；否则按 `exchange_code` 查询；
2. 优惠券不存在 → `COUPON_NOT_EXISTS`；
3. `receive >= distribute` → `COUPON_RECEIVE_ZERO`（已领完）；
4. 查询该用户是否已领取过同一 `coupon_id`；存在任意一条记录 → `COUPON_RECEIVED`；
5. `BeanUtil.copyProperties(couponDO, CouponUserDO.class, "id")` 复制面额、门槛、有效期、类型、`score`、说明、图片等；
6. 写入 `userId`、`couponId`、`endTime`、请求中的 `exchangeCode`，保存领取记录（`status` 使用数据库默认值 0）；
7. 优惠券模板 `receive + 1` 后更新。

该流程**没有**校验：上下架状态、开始时间、结束时间、剩余数量并发、`limit` 限领数量、所需积分扣减；也**没有**加锁或原子更新 `receive`。

### 5.5 订单使用与退回优惠券

使用位置：`yshop-module-mall/yshop-module-order-biz` 的 `AppStoreOrderServiceImpl`。

**下单抵扣**（`submit`）：

1. 请求参数 `couponId` 实际是 `yshop_coupon_user.id`（用户领取记录 ID），不是 `yshop_coupon.id`；
2. `appCouponUserService.getById(param.getCouponId())` 查询领取记录；
3. 只校验 `couponUserDO.least <= 订单商品总额`，不满足抛 `COUPON_NOT_CONDITION`；
4. `couponPrice = couponUserDO.value`；
5. 立即把领取记录 `status = 1`（已使用）；
6. 订单金额 = 商品金额（外卖再加配送费）− 优惠券面额；
7. 订单保存 `coupon_id = couponUser.id`、`coupon_price = 面额`。

未校验：优惠券是否属于当前用户、是否在有效期内、店铺/可用类型是否匹配、上下架状态。

**退回优惠券**（`regressionCoupon`）：

- 条件：订单未支付（`paid != 1`）且订单状态不为 `-2`；
- 按 `yshop_coupon_user.id = store_order.coupon_id`、`status = 1`、`user_id = 订单用户` 查询；
- 查到则把 `status` 置回 0（未使用）。

已支付订单在该方法中直接返回，不退回优惠券。

### 5.6 状态与有效期

- `CouponUserDO.status`：`0` 未使用、`1` 已使用；没有单独的“已过期”状态；
- “已过期”由 `end_time < now` 动态判断；
- `CouponStatusEnum` 定义了 `2 已失效`、`4 所有`，但领取记录状态实际只使用 0/1；
- 用户端“已使用”列表仍要求 `end_time > now`，因此已使用但已过期的券只会出现在“已过期”列表中。

## 6. 数据模型

### 6.1 `yshop_coupon`（`CouponDO`，继承 `BaseDO`）

| 字段 | 含义 |
|---|---|
| `id` | 优惠券 ID |
| `shop_id` | 店铺 ID，`0` 表示通用 |
| `shop_name` | 店铺名称（逗号分隔） |
| `title` | 优惠券名称 |
| `is_switch` | 是否上架 |
| `least` | 消费多少可用 |
| `value` | 优惠券金额 |
| `start_time` / `end_time` | 有效期 |
| `weigh` | 权重，列表排序使用 |
| `type` | 可用类型：0 通用、1 自取、2 外卖 |
| `exchange_code` | 兑换码 |
| `receive` | 已领取数量 |
| `distribute` | 发行数量 |
| `score` | 所需积分（当前领取流程未使用） |
| `instructions` | 使用说明 |
| `image` | 图片 |
| `limit` | 限领数量（列名 `` `limit` ``，当前领取流程未使用） |
| `deleted` / `create_time` / `update_time` / `creator` / `updater` | 审计与逻辑删除字段 |

表没有 `tenant_id` 字段，DO 继承非租户的 `BaseDO`。

### 6.2 `yshop_coupon_user`（`CouponUserDO`，继承 `BaseDO`）

| 字段 | 含义 |
|---|---|
| `id` | 领取记录 ID（`Integer`） |
| `shop_id` / `shop_name` | 店铺信息快照 |
| `title` / `least` / `value` | 名称、门槛、面额快照 |
| `start_time` / `end_time` | 有效期快照 |
| `type` | 可用类型快照 |
| `score` | 消耗积分快照（未使用） |
| `instructions` / `image` | 使用说明、图片快照 |
| `user_id` | 会员 ID（`Integer`） |
| `status` | 0 未使用、1 已使用 |
| `coupon_id` | 优惠券模板 ID（`Integer`） |
| `exchange_code` | 兑换码 |
| `deleted` / `create_time` / `update_time` / `creator` / `updater` | 审计与逻辑删除字段 |

领取记录是“快照表”：领取时复制模板的面额、门槛、有效期等字段，之后模板修改不会影响已领取记录。

## 7. 依赖与集成

### 7.1 本模块依赖

| 依赖 | 用途 |
|---|---|
| `yshop-module-coupon-api` | 本模块枚举、错误码 |
| `yshop-module-store-biz` | `StoreShopDO`、`StoreShopMapper`，创建/更新优惠券时回填店铺名称 |
| `yshop-spring-boot-starter-web` / `security` / `mybatis` / `excel` | Web、鉴权、持久层、Excel 导出 |
| `yshop-spring-boot-starter-job` | 在 `pom.xml` 中声明，当前源码未见定时任务类 |
| `yshop-server` | 直接依赖 `yshop-module-coupon-biz`，加载本模块 Controller |

### 7.2 被其它模块调用

| 调用方 | 调用内容 |
|---|---|
| `yshop-module-mall/yshop-module-order-biz` | `AppCouponUserService`、`CouponUserDO`：下单抵扣、取消退券 |
| `yshop-module-member-biz` | `AppCouponUserService`、`CouponUserDO`：个人中心优惠券数量统计 |

注意依赖方向：`member-biz` 的 `pom.xml` 直接依赖 `coupon-biz`，而 `order-biz` 通过传递依赖获得 `coupon-biz`，并未在其 pom 中显式声明。`coupon-biz` 不依赖 `member-biz`，因此优惠券模块无法直接操作会员积分。

## 8. 注意事项

1. **领取流程缺少关键校验**：未校验 `is_switch`、`start_time`、`end_time`、店铺、可用类型、库存并发、`limit` 限领数量，也未扣减 `score` 积分。
2. **限领字段未生效**：`yshop_coupon.limit` 只存储不参与判断；实际规则是“同一用户对同一优惠券只能存在一条领取记录”，因此 `limit > 1` 也无法多次领取。
3. **并发超发风险**：`receive` 先查已领数量再 `receive + 1` 更新模板，没有行锁、乐观锁或原子 SQL，高并发下可能超过 `distribute`。
4. **通用券创建可能空指针**：`CouponServiceImpl#create/update` 直接 `storeShopMapper.selectById(shopId).getName()`；当 `shopId = 0`（通用券）或店铺不存在时查询结果为 `null`，会抛 `NullPointerException`。
5. **订单使用未校验归属与状态**：下单只校验门槛，未校验领取记录属于当前用户、未使用、未过期、店铺/类型匹配、模板上下架；`couponId` 语义是领取记录 ID，和字段名 `coupon_id` 容易混淆。
6. **后台接口权限缺失**：`/coupon/user/list`、`/coupon/user/page` 的 `@PreAuthorize` 被注释，任何已登录后台账号都可访问；虽然管理端仍需认证，但不具备按钮级权限控制。
7. **权限命名不规范**：优惠券模板使用 `coupon::create` 等双冒号格式，与其它模块 `模块:实体:动作` 不一致。
8. **导出缺少店铺隔离**：分页查询会按当前登录用户店铺过滤，但 `export-excel` 使用独立查询，不受店铺隔离限制。
9. **错误码冲突**：`COUPON_USER_NOT_EXISTS`、`COUPON_RECEIVED`、`COUPON_RECEIVE_ZERO` 都使用 `1008018001`；`COUPON_NOT_EXISTS` 使用 `1008018000`，与积分模块错误码段重叠。
10. **`/coupon/not` 参数名误导**：接口声明参数名为 `id`，控制器内部把它作为 `shopId` 使用；调用方需按 `id` 传店铺 ID。
11. **过期状态是动态判断**：没有定时任务把过期券置为失效，`status` 仍为 0；业务查询必须结合 `end_time`。
12. **类型字段语义**：`type` 在优惠券中表示“通用/自取/外卖”，在 `getCount` 中同时用于匹配 `type = 参数` 或 `type = 0`；传 0 可统计通用券。
13. **数据模型边界**：`CouponUserDO.userId`、`couponId`、`id` 均为 `Integer`，而会员 ID 为 `Long`，转换时存在 `intValue()` 截断风险（当前用户量下通常不触发）。
