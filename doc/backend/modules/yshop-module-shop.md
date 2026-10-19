# yshop-module-shop（店铺内容模块）

> 模块路径：`yshop-drink-boot3/yshop-module-mall/yshop-module-shop-api`、`yshop-drink-boot3/yshop-module-mall/yshop-module-shop-biz`；文档依据本仓库源码整理。

## 1. 模块定位

店铺内容模块承担点餐小程序/H5 的「店铺装修与内容配置」职责，属于轻量级 CRUD + 用户端只读查询模块，主要包含四类数据：

- **广告图（Shop Ads）**：首页轮播/广告位，可按门店配置（`shop_id=0` 表示全部门店）。
- **服务菜单（Service）**：用户端「我的服务」入口，支持页面、小程序、菜单、内容、电话等类型。
- **素材库（Material）**：图片/视频素材的元数据管理（只存 URL，不负责上传）。
- **素材分组（Material Group）**：素材库的分类维度。

该模块不涉及商品、订单、库存，主要服务于前端展示与运营配置。门店维度的数据来自 `store` 模块。

> 注意：本模块名为 `shop`，但**不是店铺/门店主数据模块**；门店主数据在 `yshop-module-store`。本模块的 `shopId` 是关联门店的过滤条件。

## 2. 目录结构

```text
yshop-module-shop-api/
└── src/main/java/co/yixiang/yshop/module/shop/
    └── enums/ErrorCodeConstants.java     # 素材、分组、广告、服务等错误码

yshop-module-shop-biz/
└── src/main/java/co/yixiang/yshop/module/shop/
    ├── controller/
    │   ├── admin/
    │   │   ├── material/          # 素材库管理
    │   │   ├── materialgroup/     # 素材分组管理
    │   │   ├── service/           # 服务菜单管理
    │   │   └── shopads/           # 广告图管理
    │   └── app/
    │       ├── ad/                # 用户端广告列表
    │       └── service/           # 用户端服务菜单
    ├── service/
    │   ├── material/              # MaterialService / Impl
    │   ├── materialgroup/         # MaterialGroupService / Impl
    │   ├── service/               # ServiceService、AppServiceService
    │   └── shopads/               # ShopAdsService、AppShopAdsService
    ├── dal/
    │   ├── dataobject/            # material / materialgroup / service / shopads
    │   └── mysql/                 # 对应 Mapper
    ├── convert/                   # MapStruct 转换器
    └── framework/web/config/      # Swagger 分组 shop
```

## 3. 核心功能

| 功能 | 入口类 | 说明 |
|---|---|---|
| 广告图 CRUD | `ShopAdsController` / `ShopAdsServiceImpl` | 管理广告图、显隐、权重、支持门店；保存时回填门店名 |
| 用户端广告列表 | `AppAdController#getList` → `AppShopAdsService#list` | 返回全局广告 + 当前门店广告，附 `isActive` 配置 |
| 服务菜单 CRUD | `ServiceController` / `ServiceServiceImpl` | 管理服务入口的类型、图标、页面路径、电话、权重、上下架 |
| 用户端服务菜单 | `AppServiceController#getList` | 仅返回 `status=1`，按 `weigh` 倒序 |
| 服务内容详情 | `AppServiceController#getContent` | 按 ID 返回单条服务内容 |
| 素材库 CRUD | `MaterialController` / `MaterialServiceImpl` | 图片/视频素材元数据；分页按类型、分组、名称、URL 过滤 |
| 素材分组 CRUD | `MaterialGroupController` / `MaterialGroupServiceImpl` | 素材分组；列表接口返回全部，不分页 |

## 4. 主要接口

> 路由前缀由框架自动添加：管理端 `/admin-api`、用户端 `/app-api`。

### 4.1 管理端 /admin-api

#### 广告图 — `ShopAdsController`（`@RequestMapping("/shop/ads")`）

| 方法 | 路径 | 说明 | 权限注解 |
|---|---|---|---|
| POST | `/admin-api/shop/ads/create` | 创建广告图 | `shop:ads:create` |
| PUT | `/admin-api/shop/ads/update` | 更新广告图 | `shop:ads:update` |
| DELETE | `/admin-api/shop/ads/delete?id=` | 删除广告图 | `shop:ads:delete` |
| GET | `/admin-api/shop/ads/get?id=` | 获得广告图 | `shop:ads:query` |
| GET | `/admin-api/shop/ads/list?ids=` | 按 ID 集合获得 | `shop:ads:query` |
| GET | `/admin-api/shop/ads/page` | 广告图分页 | `shop:ads:query` |
| GET | `/admin-api/shop/ads/export-excel` | 导出 Excel | `shop:ads:export` |

广告图字段：`image`、`isSwitch`（是否显示）、`weigh`（权重）、`shopId`（VO 说明为「所支持的店铺 id，用 `,` 区分，0 代表全部」）、`shopName`。

#### 服务菜单 — `ServiceController`（`@RequestMapping("/shop/service")`）

| 方法 | 路径 | 说明 | 权限注解 |
|---|---|---|---|
| POST | `/admin-api/shop/service/create` | 创建服务菜单 | `shop:service:create` |
| PUT | `/admin-api/shop/service/update` | 更新服务菜单 | `shop:service:update` |
| DELETE | `/admin-api/shop/service/delete?id=` | 删除服务菜单 | `shop:service:delete` |
| GET | `/admin-api/shop/service/get?id=` | 获得服务菜单 | `shop:service:query` |
| GET | `/admin-api/shop/service/list?ids=` | 按 ID 集合获得 | `shop:service:query` |
| GET | `/admin-api/shop/service/page` | 服务菜单分页 | `shop:service:query` |
| GET | `/admin-api/shop/service/export-excel` | 导出 Excel | `shop:service:export` |

服务菜单字段：`name`、`image`、`type`、`content`、`pid`、`appId`、`pages`、`phone`、`weigh`、`status`。`type` 取值语义：`pages` 页面、`miniprogram` 跳转小程序、`menu` 菜单、`content` 内容、`call` 电话。

#### 素材库 — `MaterialController`（`@RequestMapping("/shop/material")`）

| 方法 | 路径 | 说明 | 权限注解 |
|---|---|---|---|
| POST | `/admin-api/shop/material/create` | 创建素材 | 无（`@PreAuthorize` 被注释） |
| PUT | `/admin-api/shop/material/update` | 更新素材 | 无（被注释） |
| DELETE | `/admin-api/shop/material/delete?id=` | 删除素材 | 无（被注释） |
| GET | `/admin-api/shop/material/get?id=` | 获得素材 | 无（被注释） |
| GET | `/admin-api/shop/material/list?ids=` | 按 ID 集合获得 | 无（被注释） |
| GET | `/admin-api/shop/material/page` | 素材分页 | 无（被注释） |

素材字段：`type`（1 图片、2 视频，源码为 String）、`groupId`、`name`、`url`。

#### 素材分组 — `MaterialGroupController`（`@RequestMapping("/shop/material-group")`）

| 方法 | 路径 | 说明 | 权限注解 |
|---|---|---|---|
| POST | `/admin-api/shop/material-group/create` | 创建分组 | 无（被注释） |
| PUT | `/admin-api/shop/material-group/update` | 更新分组 | 无（被注释） |
| DELETE | `/admin-api/shop/material-group/delete?id=` | 删除分组 | 无（被注释） |
| GET | `/admin-api/shop/material-group/get?id=` | 获得分组 | 无（被注释） |
| GET | `/admin-api/shop/material-group/list` | 分组列表（全部） | 无（被注释） |
| GET | `/admin-api/shop/material-group/page` | 分组分页 | 无（被注释） |

分组字段：`name`。

### 4.2 用户端 /app-api

#### 广告 — `AppAdController`（`@RequestMapping("/ad")`）

| 方法 | 路径 | 说明 | 鉴权 |
|---|---|---|---|
| GET | `/app-api/ad/list?shop_id=` | 查询广告列表；返回 `{ list, isActive }` | 无 |

查询条件：`shop_id = 0 OR shop_id = 当前门店 ID`，即「全局广告 + 当前门店广告」。

#### 服务菜单 — `AppServiceController`（`@RequestMapping("/service")`）

| 方法 | 路径 | 说明 | 鉴权 |
|---|---|---|---|
| GET | `/app-api/service/list` | 服务菜单列表，仅 `status=1`，按 `weigh` 倒序 | 无 |
| GET | `/app-api/service/content?id=` | 服务菜单内容详情 | 无 |

## 5. 核心实现逻辑

### 5.1 广告图

- 后台创建/更新时，把 `ShopAdsBaseVO.shopId`（String）直接传给 `storeShopMapper.selectById(...)`：
  - 查不到门店：`shopName = "全部"`；
  - 查到门店：`shopName = 门店名称`。
- 用户端 `AppAdController#getList` 使用 MyBatis-Plus 条件：
  ```java
  .eq(ShopAdsDO::getShopId, 0).or().eq(ShopAdsDO::getShopId, shopId)
  ```
  即只支持「全局」或「单个门店 ID 精确相等」。
- 源码中原本使用 `FIND_IN_SET` 支持逗号分隔多门店的写法已被注释，因此当前**不支持一个广告绑定多个门店**。
- 返回体中的 `isActive` 来自配置项 `yshop.info.isActive`，用于前端判断某些活动/功能是否启用。
- 广告列表没有按 `isSwitch` 过滤，显隐字段由前端自行处理或后台控制。

### 5.2 服务菜单

- 后台为纯 CRUD，没有层级树组装；`pid` 仅作为字段保存。
- 用户端列表条件：`status = ShopCommonEnum.IS_STATUS_1 (1)`，`order by weigh desc`。
- 用户端内容接口直接 `getById(id)`，**不校验 status**，已下架服务仍可通过 ID 获取内容。
- `ServiceDO.id` 是 `Long`，而 `AppServiceController#getContent` 使用 `Integer id` 接收，依赖 Spring 类型转换。

### 5.3 素材库与分组

- 素材只保存 `type/groupId/name/url` 元数据，不处理文件上传、存储、删除物理文件；上传能力在 `infra` 文件模块。
- `groupId` 为 String，未做外键约束，也不校验分组是否存在。
- 素材分页/导出条件：创建时间区间、类型、分组 ID、名称模糊、URL 精确。
- 素材分组列表接口没有查询参数，使用空 `MaterialGroupExportReqVO` 查出全部数据。

### 5.4 数据隔离

- 本模块的管理端接口**没有使用登录用户的 `shopId` 做数据过滤**；广告的门店范围完全由请求参数 `shopId` 决定。
- 门店数据校验/回填依赖 `store` 模块的 `StoreShopMapper`。

## 6. 数据模型

### yshop_shop_ads（广告图）

`ShopAdsDO`：

| 字段 | 含义 |
|---|---|
| `id` | 主键 |
| `image` | 图片地址 |
| `is_switch` | 是否显示 |
| `weigh` | 权重 |
| `shop_id` | 支持门店 ID，`0` 表示全部；VO 约定可逗号分隔 |
| `shop_name` | 门店名称（冗余） |

### yshop_service（服务菜单）

`ServiceDO`：`id`、`name`、`image`、`type`、`content`、`pid`、`app_id`、`pages`、`phone`、`weigh`、`status`。

### yshop_material（素材库）

`MaterialDO`：`id`、`type`（1 图片/2 视频）、`group_id`、`name`、`url`。

### yshop_material_group（素材分组）

`MaterialGroupDO`：`id`、`name`。

以上 DO 均继承 `BaseDO`（`create_time`、`update_time`、`creator`、`updater`、`deleted`）。

## 7. 依赖与集成

### Maven 依赖

- `yshop-module-shop-api`
- `yshop-module-store-biz`：广告保存/更新时查询门店并回填 `shopName`。
- Web、Security、MyBatis、Excel、Test 等框架 starter。

### 运行期集成

- **store 模块**：`StoreShopMapper.selectById`。
- **配置中心/配置文件**：`yshop.info.isActive`。
- **用户端**：H5/小程序通过 `/app-api/ad/list` 与 `/app-api/service/*` 渲染首页广告和服务菜单。
- **文件模块**：素材 URL 的上传/存储由 `infra` 文件模块负责，本模块只保存链接。
- **Swagger**：`ShopWebConfiguration` 注册 `shop` 分组。
- **easy-trans**：全局启用，本模块未使用 `@Trans`。
- **`-api` 产物**：只有 `ErrorCodeConstants`，没有跨模块 API 接口。

## 8. 注意事项

1. **素材/分组接口没有权限校验**：`MaterialController`、`MaterialGroupController` 上的 `@PreAuthorize` 全部被注释，任何能访问管理端路由的请求都可增删改查素材。
2. **用户端接口全部公开**：`/app-api/ad/list`、`/app-api/service/*` 没有 `@PreAuthenticated`，符合首页公开数据的定位，但不应返回敏感内容。
3. **广告多门店配置未实现**：VO 与注释约定 `shopId` 是逗号分隔的门店集合，但保存时把整串当单个 ID 查询门店，用户端又用精确相等匹配；多门店广告实际不可用，且 `shopName` 可能被写成「全部」。
4. **广告显隐未过滤**：用户端查询没有加 `isSwitch=1` 条件，是否显示取决于前端。
5. **服务内容不校验状态**：`/service/content?id=` 可能返回已下架服务。
6. **素材分组无关联校验**：删除分组不会处理组内素材，`MaterialDO.groupId` 也没有外键。
7. **错误码分布**：`MATERIAL_NOT_EXISTS=1009001000`、`MATERIAL_GROUP_NOT_EXISTS=1009001001`；`ADS_NOT_EXISTS=1008017000`、`SERVICE_NOT_EXISTS=1008017001`、`RECHARGE_NOT_EXISTS=1008017002`。与 order（`1008007xxx`）、store（`1008016xxx`）不重叠，但广告/服务错误码落在 `1008017xxx` 段，扩展时需避免占用。
8. **类型不一致**：`MaterialDO.type` 为 String，但注释语义为数字 1/2；`ServiceDO.id` 为 Long，App 接口用 Integer 接收；调用方需按实际序列化结果处理。
9. **门店名称冗余**：`shop_name` 在保存时回填，门店改名后历史广告不会自动更新。
10. **模块边界**：shop 模块只做展示配置，门店营业状态、配送费等交易参数应读取 store 模块，不应在本模块重复维护。
11. **路由前缀**：Controller 上的 `@RequestMapping` 不包含 `/admin-api`、`/app-api`。
