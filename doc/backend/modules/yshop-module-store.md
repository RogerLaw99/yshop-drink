# yshop-module-store（门店模块）

> 模块路径：`yshop-drink-boot3/yshop-module-mall/yshop-module-store-api`、`yshop-drink-boot3/yshop-module-mall/yshop-module-store-biz`；文档依据本仓库源码整理。

## 1. 模块定位

门店模块是多门店/SaaS 点餐系统的门店主数据域，提供：

- 门店基础信息：名称、电话、图片、地址、经纬度、公告。
- 营业配置：营业开始/结束时间、营业状态、外卖配送距离、起送价、配送费。
- 门店与后台管理员的绑定关系（一个管理员只能绑定一个门店）。
- 门店小票打印机标识（`uniprintId`）与打印 token 缓存。
- 用户端附近门店/门店列表查询（按经纬度计算距离并排序）。
- 为商品、店铺内容、订单等模块提供门店数据与数据隔离依据。

门店 ID（`yshop_store_shop.id`）是订单、商品、分类、广告等业务的门店隔离键；管理端登录用户通过 `SecurityFrameworkUtils.getLoginUser().getShopId()` 获取所属门店。

## 2. 目录结构

```text
yshop-module-store-api/
└── src/main/java/co/yixiang/yshop/module/store/
    └── enums/
        ├── ErrorCodeConstants.java     # 门店、打印机、收支、提现等错误码
        └── WithdrawalStatusEnum.java

yshop-module-store-biz/
└── src/main/java/co/yixiang/yshop/module/store/
    ├── controller/
    │   ├── admin/storeshop/            # 管理后台门店 CRUD
    │   │   ├── StoreShopController.java
    │   │   └── vo/
    │   └── app/storeshop/              # 用户端附近门店/门店详情
    │       ├── AppStoreController.java
    │       └── vo/AppStoreShopVO.java
    ├── service/storeshop/              # StoreShopService / AppStoreShopService
    ├── dal/
    │   ├── dataobject/storeshop/       # StoreShopDO
    │   ├── mysql/storeshop/            # StoreShopMapper（含距离 SQL）
    │   └── redis/                      # 打印 token 缓存
    └── convert/storeshop/              # MapStruct 转换器
```

本模块没有 `framework/web/config`，未单独注册 Swagger 分组。

## 3. 核心功能

| 功能 | 入口类 | 说明 |
|---|---|---|
| 门店 CRUD | `StoreShopController` / `StoreShopServiceImpl` | 创建、更新、删除、详情、列表、分页、导出 |
| 管理员绑定校验 | `StoreShopServiceImpl#createShop/updateShop` | 通过 `FIND_IN_SET` 校验一个管理员只能绑定一个门店 |
| 营业状态计算 | `StoreShopServiceImpl#doShopStatus` | 根据营业起止时间与当前时间计算 `status` |
| 门店列表/分页 | `StoreShopServiceImpl` + `StoreShopMapper` | 按登录用户所属门店做数据隔离 |
| 附近门店 | `AppStoreController#getNearby` → `StoreShopMapper#getStoreList` | Haversine SQL 计算距离，返回最近一家 |
| 门店列表（用户端） | `AppStoreController#getList` | 同一 SQL，按距离升序返回全部 |
| 门店详情（用户端） | `AppStoreController#getShop` | 按 ID 查询门店 |
| 打印 token 缓存 | `PrintTokenRedisDAO` | Redis 缓存 2 小时，供门店打印机使用 |

## 4. 主要接口

> 路由前缀由框架自动添加：管理端 `/admin-api`、用户端 `/app-api`。

### 4.1 管理端 /admin-api

Controller：`StoreShopController`（`@RequestMapping("/store/shop")`）

| 方法 | 路径 | 说明 | 权限注解 |
|---|---|---|---|
| POST | `/admin-api/store/shop/create` | 创建门店 | `store:shop:create` |
| PUT | `/admin-api/store/shop/update` | 更新门店 | `store:shop:update` |
| DELETE | `/admin-api/store/shop/delete?id=` | 删除门店（逻辑删除） | `store:shop:delete` |
| GET | `/admin-api/store/shop/get?id=` | 获得门店 | `store:shop:query` |
| GET | `/admin-api/store/shop/list` | 门店列表（按登录用户门店隔离） | `store:shop:query` |
| GET | `/admin-api/store/shop/page` | 门店分页 | `store:shop:query` |
| GET | `/admin-api/store/shop/export-excel` | 导出门店 Excel | `store:shop:export` |

创建/更新请求 `StoreShopBaseVO` 字段：`name`、`mobile`、`image`、`images`（多图数组）、`address`、`addressMap`、`lng`、`lat`、`distance`（千米，0 表示不送外卖）、`minPrice`、`deliveryPrice`、`notice`、`status`、`adminId`（管理员 ID 数组）、`uniprintId`、`startTime`、`endTime`、`balance`。

分页 `StoreShopPageReqVO` 额外支持 `createTime`、`startTime`、`endTime` 区间以及 `shopId`（由服务端按登录用户覆盖）。

### 4.2 用户端 /app-api

Controller：`AppStoreController`（`@RequestMapping("/store")`）

| 方法 | 路径 | 说明 | 鉴权 |
|---|---|---|---|
| GET | `/app-api/store/nearby?lng=&lat=&kw=&shop_id=` | 返回距离最近的一家门店 | 无 |
| GET | `/app-api/store/list?lng=&lat=&kw=&shop_id=` | 返回按距离升序的门店列表 | 无 |
| GET | `/app-api/store/getShop?shop_id=` | 按 ID 获取门店 | 无 |

参数说明：

- `lng`、`lat`：用户当前经纬度（double）。
- `kw`：门店名称，SQL 中为**精确匹配**（`name = #{name}`），不是模糊搜索。
- `shop_id`：大于 0 时只查该门店，否则查全部门店。

响应 `AppStoreShopVO` 包含门店基础字段、营业时间、`status`、`isEmpty`（桌面是否空闲，当前仅在下单模块固定置 true）、`deskOrderId`，以及 SQL 计算出的距离 `dis`（单位米）。

## 5. 核心实现逻辑

### 5.1 门店创建/更新

`StoreShopServiceImpl#createShop`：

1. 遍历 `adminId`，对每个管理员执行：
   ```sql
   select count(*) from yshop_store_shop where FIND_IN_SET(#{adminId}, admin_id)
   ```
   若已存在绑定，抛「管理员 ID 已经绑定过其他门店」。
2. `StoreShopConvert` 把 VO 转为 `StoreShopDO`。
3. 调用 `doShopStatus(startTime, endTime)` 重新计算营业状态并覆盖请求中的 `status`。
4. 插入门店。

`updateShop` 逻辑相同，但管理员唯一性校验会排除当前门店 ID（`ne(id, updateId)`）。

### 5.2 营业状态计算

`doShopStatus` 只比较「时:分」，不比较日期：

1. 取当前时间、营业开始时间、营业结束时间的小时与分钟。
2. 当前小时早于开始小时 → 0（未营业）。
3. 结束小时早于当前小时 → 0。
4. 同一开始小时但当前分钟早于开始分钟 → 0。
5. 同一结束小时但当前分钟晚于结束分钟 → 0。
6. 其余 → 1（营业中）。

该状态只在创建/更新门店时计算一次，之后不会随时间自动刷新；跨天营业（如 22:00–02:00）也无法正确表达。

### 5.3 管理端数据隔离

- `getShopList()`：
  - 登录用户 `shopId == 0`（平台管理员）：`id >= 0`，返回全部门店；
  - 否则：`id = 登录用户 shopId`，只返回本门店。
- `getShopPage()` 在 `StoreShopMapper#selectPage` 中：
  - 登录用户 `shopId == 0`：把请求的 `shopId` 置空，查询全部门店；
  - 否则强制 `id = 登录用户 shopId`。
- 详情、更新、删除**没有**校验目标门店是否属于当前登录用户，存在越权操作可能（依赖菜单/前端约束）。

### 5.4 用户端附近门店

`StoreShopMapper#getStoreList` 使用 Haversine 公式按经纬度计算球面距离：

```sql
SELECT *, ROUND(6378.138 * 2 * ASIN(SQRT(
  POW(SIN((#{lat} * PI() / 180 - lat * PI() / 180) / 2), 2) +
  COS(40.0497810000 * PI() / 180) * COS(lat * PI() / 180) *
  POW(SIN((#{lon} * PI() / 180 - lng * PI() / 180) / 2), 2)
)) * 1000) AS dis
FROM yshop_store_shop
WHERE deleted = 0
  [AND name = #{name}]
  [AND id = #{shopId}]
ORDER BY dis ASC
```

- 结果中的 `dis` 单位为米，映射到 `AppStoreShopVO.dis`。
- SQL 中没有过滤 `status=1`，未营业门店也会返回。
- `name` 使用 `=` 精确匹配，不是 `LIKE`。
- 距离公式中的经度修正项固定使用 `COS(40.0497810000)`，并非按当前纬度计算，非该纬度地区会有误差。

`AppStoreController#getNearby` 取列表第一条；`#getList` 返回完整列表；`#getShop` 直接 `getById`。

### 5.5 门店打印

- `PrintTokenRedisDAO` 使用 Redis key `yshop_web_print_token_cache:`（定义在 `store/dal/redis/RedisKeyConstants`，无动态占位符），写入时过期时间 2 小时。
- `uniprintId` 保存在门店表，供小票打印机绑定使用。
- 订单模块的 `PrintMechinRedisDAO` 使用另一套 `yshop_web_print_machine_cache:{shopId}` key，两处 key 并不一致，排查打印问题时要区分。

## 6. 数据模型

### yshop_store_shop（门店）

`StoreShopDO`：

| 字段 | 含义 |
|---|---|
| `id` | 门店 ID（业务主键） |
| `name` | 门店名称 |
| `mobile` | 门店电话 |
| `image` | 门店主图 |
| `images` | 多张图片，`List<String>`，由 `StringListTypeHandler` 映射 |
| `address` | 详细地址 |
| `address_map` | 地图定位地址 |
| `lng` / `lat` | 经度 / 纬度（String） |
| `distance` | 外卖配送距离，单位千米；0 表示不送外卖 |
| `min_price` | 起送价 |
| `delivery_price` | 配送费 |
| `notice` | 公告 |
| `status` | 是否营业：0 否、1 是 |
| `admin_id` | 绑定的管理员 ID 列表，`List<String>`，`StringListTypeHandler` 映射 |
| `uniprint_id` | 打印机 ID |
| `start_time` / `end_time` | 营业开始/结束时间 |

继承 `BaseDO`：`create_time`、`update_time`、`creator`、`updater`、`deleted`。

### 门店相关的其他数据结构

- `StoreShopCreateReqVO` / `StoreShopUpdateReqVO` / `StoreShopRespVO` / `StoreShopPageReqVO` / `StoreShopExportReqVO`：位于 `controller/admin/storeshop/vo/`。
- `AppStoreShopVO`：用户端门店视图，含距离 `dis`、`isEmpty`、`deskOrderId`。
- `StoreShopExcelVO`：导出模型。

## 7. 依赖与集成

### Maven 依赖（store-biz）

- `yshop-module-store-api`
- Web、Security、MyBatis、Excel、Redis、Job、多租户、Test 等框架 starter。

### 被其他模块依赖

- **product-biz**：商品、分类保存时查询门店并回填 `shopName`。
- **shop-biz**：广告保存/更新时查询门店并回填 `shopName`。
- **order-biz**（经 product-biz 传递）：下单时读取门店配送费、门店名称；用户端订单详情返回门店信息。
- **coupon-biz**：Maven 依赖 store-biz，用于门店维度的营销数据。
- **server**：通过 `shop-biz`/`order-biz` 传递加载本模块 Controller。

### 运行期集成

- **Redis**：打印 token 缓存。
- **安全框架**：`SecurityFrameworkUtils.getLoginUser().getShopId()` 作为管理端门店数据隔离依据。
- **用户端定位**：H5/小程序传入经纬度，后端 SQL 计算距离。
- **easy-trans**：全局启用，本模块未使用 `@Trans`。

## 8. 注意事项

1. **用户端接口无鉴权**：`/app-api/store/nearby`、`/list`、`/getShop` 均没有 `@PreAuthenticated`，门店公开信息可匿名访问。
2. **`nearby` 空列表越界**：SQL 正常返回空 `List` 而不是 `null`，`AppStoreController#getNearby` 只判断 `list != null` 后直接 `list.get(0)`，无匹配门店时会抛 `IndexOutOfBoundsException`。应改为 `CollUtil.isNotEmpty(list)`。
3. **详情/更新/删除未做门店归属校验**：只有列表和分页按登录用户隔离；拥有 `store:shop:update/delete` 权限的商家可直接操作其它门店 ID。
4. **营业状态不会自动刷新**：`status` 只在创建/更新时计算一次；营业时间到达后不会自动变为营业中，需要定时任务或每次查询动态判断。
5. **跨天营业不支持**：`doShopStatus` 只比较时/分，22:00–02:00 这类配置在 23:00 会被判为未营业。
6. **距离计算误差**：Haversine SQL 的经度修正项固定为 `COS(40.0497810000)`，不是当前纬度；`lng/lat` 以 String 存储，SQL 中隐式转数值。
7. **名称搜索是精确匹配**：`kw` 参数执行 `name = #{name}`，用户输入不完整时查不到门店。
8. **未营业门店仍返回**：用户端 SQL 未过滤 `status=1`，也未按 `distance` 过滤配送范围，配送范围需要业务层另行判断。
9. **`balance` 字段不一致**：`StoreShopBaseVO` 有 `balance`，但 `StoreShopDO` 没有该字段，转换时会被丢弃；`StoreShopMapper#incMoney` 更新 `balance` 列但仓库内无调用方。若依赖门店余额，需要先补齐 DO 与业务调用。
10. **删除门店无关联校验**：删除门店不会检查是否仍有商品、分类、订单、广告引用，可能产生悬挂数据。
11. **管理员唯一绑定使用 `FIND_IN_SET`**：`admin_id` 以 JSON/逗号串存储，SQL 直接拼接管理员 ID（来自请求参数），虽然 ID 通常是数字，仍建议参数化校验。
12. **`getShop` 可能返回 null**：用户端按不存在的 `shop_id` 查询时 `success(null)`，前端需兼容。
13. **打印 token 两套 key**：本模块 `PrintTokenRedisDAO` 与订单模块 `PrintMechinRedisDAO` 使用不同 key，不要混用。
14. **路由前缀**：Controller 上的 `@RequestMapping` 不包含 `/admin-api`、`/app-api`。
