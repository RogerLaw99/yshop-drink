# yshop-module-product（商品模块）

> 模块路径：`yshop-drink-boot3/yshop-module-mall/yshop-module-product-api`、`yshop-drink-boot3/yshop-module-mall/yshop-module-product-biz`；文档依据本仓库源码整理。

## 1. 模块定位

商品模块是多门店点餐系统的商品主数据域，负责：

- 门店商品（SPU）的创建、编辑、上下架、库存/销量维护。
- 商品分类（两级）维护与门店隔离。
- 商品规格模板（规则值）管理。
- 单规格/多规格 SKU 的组合生成、价格/库存/成本等 SKU 属性维护。
- 用户端按门店展示「分类 + 商品 + SKU 属性」菜单数据与商品详情。
- 商品评价的查询、后台回复与统计。

模块本身不维护购物车。仓库中只残留 `controller/app/cart/vo/AppStoreCartQueryVo.java`，没有购物车 Controller、DO 或表；用户端把商品/SKU/数量直接提交给订单模块下单。

## 2. 目录结构

```text
yshop-module-product-api/
└── src/main/java/co/yixiang/yshop/module/product/
    ├── api/
    │   ├── product/ProductApi.java                 # 空接口（实现与调用均已注释）
    │   └── property/ProductPropertyValueApi.java   # 声明了方法，但仓库内无实现类
    └── enums/                                      # 商品、规格、配送、评论等枚举与错误码

yshop-module-product-biz/
└── src/main/java/co/yixiang/yshop/module/product/
    ├── controller/
    │   ├── admin/
    │   │   ├── storeproduct/        # 商品管理
    │   │   ├── category/            # 分类管理
    │   │   ├── storeproductreply/   # 评价管理
    │   │   └── storeproductrule/    # 规格模板管理
    │   └── app/
    │       ├── product/             # 用户端商品列表/详情
    │       ├── category/            # 用户端分类（无有效接口）
    │       └── cart/vo/             # 仅购物车 VO 残留
    ├── service/                     # category / storeproduct / attr / attrvalue / attrresult / reply / rule
    ├── dal/
    │   ├── dataobject/              # 7 个 DO
    │   └── mysql/                   # Mapper（部分 SQL 用注解）
    ├── convert/                     # MapStruct 转换器
    └── framework/web/config/        # Swagger 分组 product
```

## 3. 核心功能

| 功能 | 入口类 | 说明 |
|---|---|---|
| 商品保存/编辑 | `StoreProductController#create` → `StoreProductServiceImpl#insertAndEditYxStoreProduct` | 保存商品主表，计算最低价、总库存，写入 SKU 与规格快照 |
| 商品分页/详情/删除 | `StoreProductController` / `StoreProductServiceImpl` | 按登录用户所属门店过滤 |
| 商品上下架 | `StoreProductController#onSale` → `StoreProductServiceImpl#onSale` | 直接更新 `is_show` |
| 规格生成 | `StoreProductController#isFormatAttr` → `getFormatAttr` | 对规格值做笛卡尔积，生成 SKU 表格数据 |
| 规格模板 | `StoreProductRuleController` / `StoreProductRuleService` | 维护可复用的规格名与规格值 JSON |
| 分类管理 | `ProductCategoryController` / `ProductCategoryServiceImpl` | 两级分类，门店隔离，存在子分类时禁止删除 |
| 商品列表（用户端） | `AppStoreProductController#goodsList` → `AppStoreProductServiceImpl#getGoodsList` | 按门店返回分类及分类下商品，并附带 SKU 属性 |
| 商品详情（用户端） | `AppStoreProductController#goodsDetail` | 返回商品主信息 + `productAttr` + `productValue` |
| 库存校验/扣减/回滚 | `AppStoreProductServiceImpl#checkProductStock/decProductStock/incProductStock` | 条件 UPDATE 扣库存，供订单模块调用 |
| 评价管理 | `StoreProductReplyController` / `StoreProductReplyServiceImpl` | 后台回复、删除、分页；连表查询用户与订单明细 |
| 评价统计 | `AppStoreProductReplyServiceImpl` | 好评率、中差评数量、商品评价列表（当前无 Controller 暴露） |

## 4. 主要接口

> 路由前缀由框架自动添加：管理端为 `/admin-api`，用户端为 `/app-api`。

### 4.1 管理端 /admin-api

#### 商品管理 — `StoreProductController`（`@RequestMapping("/product/store-product")`）

| 方法 | 路径 | 说明 | 权限注解 |
|---|---|---|---|
| POST | `/admin-api/product/store-product/create` | 创建/保存商品（请求体 `StoreProductDto`，含 `items` 与 `attrs`） | `shop:store-product:create` |
| PUT | `/admin-api/product/store-product/update` | 更新商品主表 | `shop:store-product:update` |
| DELETE | `/admin-api/product/store-product/delete?id=` | 删除商品（逻辑删除） | `shop:store-product:delete` |
| GET | `/admin-api/product/store-product/get?id=` | 获得商品 | `shop:store-product:query` |
| GET | `/admin-api/product/store-product/list?ids=` | 按 ID 集合获得商品 | `shop:store-product:query` |
| GET | `/admin-api/product/store-product/page` | 商品分页 | `shop:store-product:query` |
| GET | `/admin-api/product/store-product/export-excel` | 导出商品 Excel | `shop:store-product:export` |
| GET | `/admin-api/product/store-product/info/{id}` | 编辑页回显：规格模板 + 商品详情 + SKU | 无 |
| POST | `/admin-api/product/store-product/isFormatAttr/{id}` | 传入规格 JSON，生成 SKU 表格（`id=0` 表示新建） | 无 |
| GET | `/admin-api/product/store-product/sale?id=&type=` | 上架/下架切换 | 无 |

`StoreProductPageReqVO` 支持 `storeName`、`shopName`、`isPostage`、`isShow`、`stock`（传 0 时筛选库存为 0）、`cateId`、`catIds`。

#### 分类管理 — `ProductCategoryController`（`@RequestMapping("/product/category")`）

| 方法 | 路径 | 说明 | 权限注解 |
|---|---|---|---|
| POST | `/admin-api/product/category/create` | 创建分类 | `product:category:create` |
| PUT | `/admin-api/product/category/update` | 更新分类 | `product:category:update` |
| DELETE | `/admin-api/product/category/delete?id=` | 删除分类 | `product:category:delete` |
| GET | `/admin-api/product/category/get?id=` | 获得分类 | `product:category:query` |
| GET | `/admin-api/product/category/list?name=&shopName=&shopId=` | 分类列表（按登录用户门店过滤） | `product:category:query` |

#### 评价管理 — `StoreProductReplyController`（`@RequestMapping("/product/store-product-reply")`）

| 方法 | 路径 | 说明 | 权限注解 |
|---|---|---|---|
| PUT | `/admin-api/product/store-product-reply/update` | 更新评价/商家回复 | `product:store-product-reply:update` |
| DELETE | `/admin-api/product/store-product-reply/delete?id=` | 删除评价 | `product:store-product-reply:delete` |
| GET | `/admin-api/product/store-product-reply/get?id=` | 获得评价 | `product:store-product-reply:query` |
| GET | `/admin-api/product/store-product-reply/page` | 评价分页（连表返回昵称、头像、订单明细） | `product:store-product-reply:query` |

#### 规格模板 — `StoreProductRuleController`（`@RequestMapping("/product/store-product-rule")`）

| 方法 | 路径 | 说明 | 权限注解 |
|---|---|---|---|
| POST | `/admin-api/product/store-product-rule/save/{id}` | `id>0` 更新、否则创建 | `shop:store-product-rule:create` |
| DELETE | `/admin-api/product/store-product-rule/delete?id=` | 删除规格模板 | `shop:store-product-rule:delete` |
| GET | `/admin-api/product/store-product-rule/get?id=` | 获得规格模板 | `shop:store-product-rule:query` |
| GET | `/admin-api/product/store-product-rule/list?ids=` | 按 ID 集合获得；`ids` 为空时返回全部 | `shop:store-product-rule:query` |
| GET | `/admin-api/product/store-product-rule/page` | 规格模板分页 | `shop:store-product-rule:query` |
| GET | `/admin-api/product/store-product-rule/export-excel` | 导出 Excel | `shop:store-product-rule:export` |

### 4.2 用户端 /app-api

#### 商品 — `AppStoreProductController`（`@RequestMapping("/product")`）

| 方法 | 路径 | 说明 | 鉴权 |
|---|---|---|---|
| GET | `/app-api/product/products?type=&sid=&news=&isIntegral=&keyword=&shopId=` | 按门店返回「分类 + 商品 + SKU」菜单 | 无 |
| GET | `/app-api/product/detail/{id}` | 商品详情 | 无 |

`AppStoreProductQueryParam` 声明了 `type`、`sid`、`news`、`isIntegral`、`keyword`、`shopId`，但 `getGoodsList` 目前只使用 `shopId`，其余字段未参与查询；接口也不分页。

#### 分类 — `AppCategoryController`（`@RequestMapping("/product/category")`）

该 Controller 仅保留类的声明，唯一的 `/list` 方法已被整体注释，**当前没有可用的用户端分类接口**。

> 评价列表/详情方法存在于 `AppStoreProductReplyService`，但本仓库没有对应的 App Controller 暴露这些能力。

## 5. 核心实现逻辑

### 5.1 商品保存与 SKU 生成

`StoreProductServiceImpl#insertAndEditYxStoreProduct(StoreProductDto dto)`：

1. `computedProduct(dto.attrs)` 计算 SKU 集合的：
   - 最低售价 `minPrice`、最低原价 `minOtPrice`、最低成本 `minCost`、最低积分 `minIntegral`；
   - 总库存 `stock`（所有 SKU 求和），若 `stock <= 0` 抛 `STORE_PRODUCT_STOCK_ERROR`。
2. 校验轮播图 `sliderImage` 非空，否则抛 `STORE_PRODUCT_SLIDER_ERROR`。
3. 根据 `shopId` 查询门店，写入 `shopName`。
4. 商品主表价格取最低 SKU 价格，库存取 SKU 总库存；轮播图列表用逗号拼接。
5. `saveOrUpdate` 保存 `yshop_store_product`。
6. 规格处理：
   - `specType=0`（单规格）：把规格统一为 `规格 -> 默认`，再写入属性表。
   - `specType=1`（多规格）：使用 `dto.items`（规格项）与 `dto.attrs`（SKU 行）写入属性表。

`StoreProductAttrServiceImpl#insertYxStoreProductAttr(items, attrs, productId)`：

- 规格项写入 `yshop_store_product_attr`（`attr_name`、`attr_values` 逗号串）。
- 每个 SKU 写入 `yshop_store_product_attr_value`；`sku` 由规格值排序后逗号拼接。
- 若该 SKU 已存在则复用其 `id` 与 `unique`，否则生成 UUID。
- 先按 `product_id` 清空旧规格项与 SKU，再批量插入。
- 最后把 `{ attr: items, value: attrs }` 序列化为 JSON，写入 `yshop_store_product_attr_result`（同一商品只保留一条快照）。

### 5.2 规格笛卡尔积（isFormatAttr）

`StoreProductServiceImpl#getFormatAttr(id, jsonStr, false)`：

1. 解析请求体中的 `attrs`，每个元素包含 `value`（规格名）与 `detail`（规格值数组）。
2. `attrFormat` 逐层组合规格值，生成形如 `颜色_红-尺寸_L` 的组合键；若规格值包含 `-` 直接抛 `STORE_PRODUCT_RULE_RE`。
3. 对每个组合：
   - 规格值排序后逗号拼接为 `sku`；
   - 生成前端可用的 `value1/value2/...` 表头与数据；
   - `id>0` 时按 `productId + sku` 回填已有 SKU 的图片、价格、成本、原价、库存、条码、重量、体积、佣金、拼团/秒杀价与库存、积分。
4. 返回 `{ attr, value, header }` 给后台商品编辑页渲染 SKU 表格。

### 5.3 上下架

`StoreProductServiceImpl#onSale(id, status)` 的语义是「传入当前状态并取反」：

```java
if (ShopCommonEnum.SHOW_1.getValue().equals(status)) {
    status = ShopCommonEnum.SHOW_0.getValue();   // 当前上架 -> 下架
} else {
    status = ShopCommonEnum.SHOW_1.getValue();   // 当前下架 -> 上架
}
storeProductMapper.updateOnsale(status, id);
```

Mapper SQL：`update yshop_store_product set is_show = #{status} where id = #{id}`。

### 5.4 库存与销量

- 校验：`checkProductStock(uid, productId, cartNum, sku)` 要求商品存在且 `is_show=1`，再取 SKU 库存比较，不足时抛「商品库存不足」。
- 扣减：`decProductStock` 先执行
  `update yshop_store_product_attr_value set stock=stock-num, sales=sales+num where product_id=? and sku=? and stock>=num`，
  再执行商品级 `yshop_store_product` 的条件扣减；任一更新 0 行即抛 `PRODUCT_STOCK_LESS`。
- 回滚：`incProductStock` 对应执行 `stock=stock+num, sales=sales-num`。
- SKU 库存的 `unique` 参数实际传入的是 `sku`（规格逗号串），SQL 条件也是按 `sku` 匹配，而非 `unique` 字段。

### 5.5 用户端商品数据

`AppStoreProductServiceImpl#getGoodsList`：

1. 查询该门店启用状态的分类（`status=1`，`shop_id=shopId`），按 `sort` 升序。
2. 对每个分类查询 `is_show=1` 且 `shop_id=shopId` 的商品。
3. 对每个商品调用 `AppStoreProductAttrService#getProductAttrDetail`，把规格项转换为 `productAttr`，把 SKU 以 `sku -> StoreProductAttrValueDO` 的 Map 返回为 `productValue`。
4. 组装为 `AppCategoryRespVO.goodsList` 返回。

`getStoreProductById(id)` 返回商品主信息 + 同样的 `productAttr/productValue`。

### 5.6 分类

- 分类最多两级：`parentId=0` 为一级，其它为二级。
- 创建/更新时按 `shopId` 查询门店并回填 `shopName`。
- 删除前检查是否存在子分类，有则抛 `CATEGORY_EXISTS_CHILDREN`；删除商品校验仍为 TODO。
- 管理端列表按登录用户 `shopId` 过滤：`shopId=0` 的管理员查看全部门店，否则只看本门店。
- 父分类校验方法 `validateParentProductCategory` 已实现，但在创建/更新中调用被注释，实际不校验层级。

### 5.7 商品评价

- 后台分页通过 `StoreProductReplyMapper#allReplyList` 连表 `yshop_store_product_reply`、`yshop_user`、`yshop_store_order_cart_info`，返回昵称、头像、订单购物明细。
- 用户端服务支持：评价数量、最新一条评价、好评率、好评/中评/差评数量、按类型分页查询；评分规则为 5 分好评、2-5 分中评、小于 2 分差评。
- 评价中的 `cartInfo` 会解析为 `AppStoreCartQueryVo` 以补全 SKU 信息。

## 6. 数据模型

### yshop_store_product（商品/SPU）

`StoreProductDO` 关键字段：

| 字段 | 含义 |
|---|---|
| `id` | 商品 ID |
| `shop_id` / `shop_name` | 所属门店 |
| `image` / `slider_image` | 主图 / 轮播图（逗号串） |
| `store_name` / `store_info` / `keyword` | 名称、简介、关键字 |
| `cate_id` | 分类 ID（字符串） |
| `price` / `ot_price` / `cost` / `vip_price` | 售价、市场价、成本价、会员价 |
| `postage` / `is_postage` / `temp_id` | 邮费、是否包邮、运费模板 |
| `stock` / `sales` | 总库存 / 销量 |
| `is_show` | 上下架：0 未上架、1 上架 |
| `spec_type` | 规格类型：0 单规格、1 多规格 |
| `give_integral` | 赠送积分 |
| `is_integral` / `integral` | 是否积分兑换 / 所需积分 |
| `is_hot` / `is_benefit` / `is_best` / `is_new` / `is_good` | 首页/推荐标签 |
| `sort` / `ficti` / `browse` | 排序、虚拟销量、浏览量 |
| `description` | 商品详情 |

### yshop_store_product_category（商品分类）

`ProductCategoryDO`：`id`、`shop_id`、`shop_name`、`parent_id`、`name`、`pic_url`、`sort`、`description`、`status`。

### yshop_store_product_attr（规格项）

`StoreProductAttrDO`：`id`、`product_id`、`attr_name`、`attr_values`（逗号分隔的规格值）。

### yshop_store_product_attr_value（SKU）

`StoreProductAttrValueDO` 关键字段：`product_id`、`sku`（规格值逗号串）、`stock`、`sales`、`price`、`image`、`unique`、`cost`、`bar_code`、`ot_price`、`weight`、`volume`、`brokerage`、`brokerage_two`、`pink_price`、`pink_stock`、`seckill_price`、`seckill_stock`、`integral`。

### yshop_store_product_attr_result（规格快照）

`StoreProductAttrResultDO`：`product_id`、`result`（`{attr, value}` JSON 字符串）、`change_time`。

### yshop_store_product_rule（规格模板）

`StoreProductRuleDO`：`id`、`rule_name`、`rule_value`（`JSONArray`，由 MyBatis-Plus `FastjsonTypeHandler` 映射）。

### yshop_store_product_reply（评价）

`StoreProductReplyDO`：`uid`、`oid`、`unique`、`product_id`、`reply_type`、`product_score`、`service_score`、`comment`、`pics`、`merchant_reply_content`、`merchant_reply_time`、`is_reply`。

## 7. 依赖与集成

### Maven 依赖

- `yshop-module-product-api`
- `yshop-module-store-biz`：商品/分类保存时需要查询 `StoreShopDO` 回填门店名称。
- Web、Security、MyBatis、Excel、Test 等框架 starter。

### 代码层集成

- **订单模块**：`order-biz` 依赖 `product-biz`，调用 `AppStoreProductService` 完成库存校验、扣减、回滚、上下架商品查询，并直接读取 `StoreProductDO`、`StoreProductAttrValueDO`。
- **门店模块**：通过 `StoreShopMapper` 读取门店。
- **会员模块**：评价连表 SQL 直接引用 `yshop_user` 表；评价 VO 与订单购物明细表关联。
- **安全框架**：`SecurityFrameworkUtils.getLoginUser().getShopId()` 用于商品、分类、规格的门店数据隔离。
- **Swagger**：`ProductWebConfiguration` 注册 `product` 分组。
- **easy-trans**：全局启用，但本模块未使用 `@Trans`。

### 商品 API 的现状

- `ProductApi` 接口与 `ProductApiImpl` 的实现均被整体注释，实际为空。
- `ProductPropertyValueApi` 只有接口声明，仓库内没有实现类，也没有调用方。
- 跨模块协作因此直接依赖 `-biz` 类，而未走 `-api` 契约。

## 8. 注意事项

1. **没有购物车后端**：不存在购物车 Controller/DO/表；`AppStoreCartQueryVo` 仅用于解析评价中的订单明细。购物车由用户端本地维护，下单时提交商品数组。
2. **用户端分类接口为空**：`AppCategoryController` 的 `/list` 被整体注释，分类数据实际由 `/app-api/product/products` 一并返回。
3. **商品列表参数大多未生效**：`/app-api/product/products` 只使用 `shopId`，`type/sid/news/keyword/isIntegral` 均未参与查询，也没有分页。
4. **部分管理端接口缺少权限注解**：`info/{id}`、`isFormatAttr/{id}`、`sale` 均无 `@PreAuthorize`；商品权限前缀使用 `shop:`，与分类/评价的 `product:` 前缀不一致，菜单权限配置时需注意。
5. **上下架参数语义**：`/sale?type=` 传入的是「当前状态」，服务层取反后落库；调用方若传目标状态会得到相反结果。
6. **规格值限制**：规格值不能包含 `-`，SKU 由规格值排序后逗号拼接；单规格商品会被规范为「规格=默认」。
7. **规格快照可能为空**：`getProductInfo(id)` 直接 `storeProductAttrResult.getResult()`，若历史商品没有属性快照会空指针；`attrValues.get(0)` 也假定至少有一条 SKU。
8. **库存扣减条件更新**：SKU 与商品两级扣减都带 `stock >= num` 条件，能防止负库存；但校验与扣减发生在订单模块的两个不同锁内，不是原子操作。
9. **`sku` 与 `unique` 混用**：库存 SQL 的 `unique` 参数实际传 `sku`，按 `sku` 匹配；`StoreProductAttrValueDO.unique` 只是业务唯一值。
10. **分类层级校验被注释**：创建/更新不会校验父分类存在与最多两级；删除分类只校验子分类，未校验是否仍有商品。
11. **门店空指针风险**：创建/更新商品、分类时直接 `storeShopMapper.selectById(shopId).getName()`，`shopId` 不存在会 NPE。
12. **评价查询依赖表名**：评价连表 SQL 硬编码 `yshop_user`、`yshop_store_order_cart_info`，跨模块表耦合较深；修改表结构时需同步这些 SQL。
13. **`ProductPropertyValueApi` 无实现**：若后续引入依赖注入会启动失败，使用前需补齐实现。
14. **路由前缀**：Controller 上的 `@RequestMapping` 不包含 `/admin-api`、`/app-api`。
