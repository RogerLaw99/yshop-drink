# yshop-module-express 快递模块

> 模块路径：`yshop-drink-boot3/yshop-module-express`；文档依据本仓库源码整理。

## 1. 模块定位

`yshop-module-express` 提供两类与快递相关的能力：

1. **快递公司字典**：维护 `yshop_express` 表中的快递公司编码、名称、排序，供后台发货选择快递公司。
2. **快递鸟（KDNiao）物流查询**：保存快递鸟 `EBusinessID`、`ApiKey`、请求地址等配置到 Redis，按快递公司编码 + 快递单号实时查询物流轨迹，并把状态编码翻译为中文。

此外，源码中还实现了**快递鸟电子面单**的请求工具（`KdniaoUtil#getOrderInfo`），但目前**没有暴露任何 Controller，也没有面单数据表**，属于未接入的能力。模块不包含物流轨迹落库、订阅推送或定时跟踪任务，所有查询都是按需实时调用第三方接口。

## 2. 目录结构

```text
yshop-module-express/
├── pom.xml
├── yshop-module-express-api/
│   └── src/main/java/co/yixiang/yshop/module/express/
│       ├── enums/ErrorCodeConstants.java
│       └── kdniao/
│           ├── enums/
│           │   ├── KdniaoLogisticsCodeEnum.java    # 快递公司编码枚举
│           │   └── KdniaoLogisticsStatusEnum.java  # 物流状态枚举
│           ├── model/
│           │   ├── dto/KdniaoApiBaseDTO.java       # 快递鸟基础配置
│           │   ├── dto/KdniaoApiDTO.java           # 物流查询参数
│           │   ├── dto/KdniaoElectronicsOrderDTO.java
│           │   ├── dto/KdniaoElectronicsOrderGoodsDTO.java
│           │   ├── vo/KdniaoApiVO.java             # 物流查询响应
│           │   ├── vo/KdniaoLogisticsVO.java       # 另一套物流响应结构（当前未使用）
│           │   └── vo/KdniaoOrderVO.java           # 电子面单响应
│           └── util/KdniaoUtil.java                # HTTP 调用、签名、状态转换
└── yshop-module-express-biz/
    └── src/main/java/co/yixiang/yshop/module/express/
        ├── controller/
        │   ├── admin/express/
        │   │   ├── ExpressController.java
        │   │   └── vo/                              # Base/Create/Update/Page/Export/Resp/Excel VO
        │   └── app/express/AppExpressController.java
        ├── convert/express/ExpressConvert.java
        ├── dal/
        │   ├── dataobject/express/ExpressDO.java
        │   ├── mysql/express/ExpressMapper.java
        │   └── redis/
        │       ├── RedisKeyConstants.java           # yshop_express_cache:
        │       └── express/ExpressRedisDAO.java
        └── service/express/
            ├── ExpressService.java
            └── ExpressServiceImpl.java
```

## 3. 核心功能

| 功能 | 入口类 | 说明 |
|---|---|---|
| 快递公司 CRUD | `ExpressController`、`ExpressServiceImpl` | 对 `yshop_express` 增删改查、列表、分页、Excel 导出 |
| 快递鸟配置管理 | `ExpressController#getExpressSet/postExpressSet`、`ExpressRedisDAO` | 配置保存在 Redis，不落数据库 |
| 物流轨迹查询 | `ExpressController#getLogistic`、`AppExpressController#getLogistic`、`KdniaoUtil#getLogisticInfo` | 按快递公司编码 + 快递单号实时调用快递鸟 |
| 物流状态翻译 | `KdniaoApiVO#handleData`、`KdniaoLogisticsStatusEnum` | 填充 `statusName`、`statusExName`、每条轨迹的 `actionName` |
| 电子面单请求（未接入） | `KdniaoUtil#getOrderInfo`、`KdniaoElectronicsOrderDTO` | 已实现 RequestType 1007 的参数组装，但无 Controller/持久化 |

## 4. 主要接口

### 4.1 管理端 /admin-api

Controller 的 `@RequestMapping` 为 `/order/express`，位于 `controller.admin` 包，实际前缀为 `/admin-api`。

| 方法 | 路径 | 说明 | 权限注解 |
|---|---|---|---|
| POST | `/admin-api/order/express/create` | 创建快递公司 | `@PreAuthorize("@ss.hasPermission('order:express:create')")` |
| PUT | `/admin-api/order/express/update` | 更新快递公司 | `@PreAuthorize("@ss.hasPermission('order:express:update')")` |
| DELETE | `/admin-api/order/express/delete?id=` | 删除快递公司 | `@PreAuthorize("@ss.hasPermission('order:express:delete')")` |
| GET | `/admin-api/order/express/get?id=` | 获得快递公司 | `@PreAuthorize("@ss.hasPermission('order:express:query')")` |
| GET | `/admin-api/order/express/list` | 获得全部快递公司 | `@PreAuthorize("@ss.hasPermission('order:express:query')")` |
| GET | `/admin-api/order/express/page` | 分页查询 | `@PreAuthorize("@ss.hasPermission('order:express:query')")` |
| GET | `/admin-api/order/express/export-excel` | 导出 Excel | `@PreAuthorize("@ss.hasPermission('order:express:export')")` |
| GET | `/admin-api/order/express/set` | 获得快递鸟配置 | 无 |
| POST | `/admin-api/order/express/set` | 保存快递鸟配置 | 无 |
| GET | `/admin-api/order/express/getLogistic?shipperCode=&logisticCode=` | 查询物流轨迹 | 无 |

### 4.2 用户端 /app-api

Controller 的 `@RequestMapping` 为 `/express`，位于 `controller.app` 包，实际前缀为 `/app-api`。

| 方法 | 路径 | 说明 | 权限注解 |
|---|---|---|---|
| GET | `/app-api/express/getLogistic?shipperCode=&logisticCode=` | 查询物流轨迹 | 无（未使用 `@PreAuthenticated`） |

## 5. 核心实现逻辑

### 5.1 快递公司字典

`ExpressServiceImpl` 是标准 CRUD：

- 创建：`ExpressConvert` 转 DO 后插入，返回自增 `id`；
- 更新/删除：先 `selectById` 校验存在，不存在抛 `EXPRESS_NOT_EXISTS`；
- 分页：`code` 精确匹配、`name` 模糊匹配，按 `id` 倒序；
- 列表：`selectList()` 全量查询，供后台下拉框使用；
- 导出：按 `code`/`name` 条件查询后写 Excel。

初始化数据包含中通（ZTO）、申通（STO）、百世（HTKY）、圆通（YTO）、EMS、顺丰（SF）六家公司。

### 5.2 快递鸟配置

- `KdniaoApiBaseDTO` 字段：`eBusinessID`（用户 ID）、`apiKey`、`reqURL`、`isFree`（是否免费接口）。
- 管理端 `GET/POST /order/express/set` 直接读写 Redis：
  - Key：`yshop_express_cache:`（`RedisKeyDefine`，`STRING`，永不过期）；
  - Value：`KdniaoApiBaseDTO` 的 JSON；
  - `ExpressRedisDAO` 使用 `StringRedisTemplate`，没有租户隔离。
- `application.yaml` 中的 `yshop.trade.express.client`、`kd-niao.api-key`、`kd-niao.business-id`、`kd100.*` 在 Java 源码中没有读取点，当前**不生效**；实际配置以 Redis 中的值为准。

### 5.3 物流查询

管理端与用户端 `getLogistic` 逻辑相同：

1. 从 `ExpressRedisDAO#get()` 读取快递鸟配置；
2. 构造 `KdniaoApiDTO`：`shipperCode`、`logisticCode`、`apiKey`、`eBusinessID`、`isFree`；
3. 调用 `KdniaoUtil#getLogisticInfo`；
4. 返回 `KdniaoApiVO`。

`KdniaoUtil#getLogisticBase` 的请求细节：

- 地址：`https://api.kdniao.com/Ebusiness/EbusinessOrderHandle.aspx`；
- `RequestData`：`{"shipperCode":"...","LogisticCode":"..."}`，URL 编码；
- `RequestType`：`isFree=true` 时为 `1002`（免费），否则 `8001`（收费）；
- `DataSign`：`Base64(MD5(RequestData + ApiKey))`，再 URL 编码；
- `DataType=2`；
- 以 `application/x-www-form-urlencoded` POST 提交，使用原生 `HttpURLConnection`。

响应解析为 `KdniaoApiVO` 后调用 `handleData()`：

- `State` → 基础状态名（0 暂无轨迹、1 已揽收、2 在途、3 签收、4 问题件）；
- `StateEx` → 增值状态名（201 到达派件城市、202 派件中、211 入柜/驿站、301 正常签收等）；
- `Traces[].Action` → 每条轨迹的状态名。

### 5.4 电子面单（未接入）

`KdniaoUtil#getOrderInfo(queryDTO, goodsList)` 已实现：

- 地址：`https://api.kdniao.com/api/EOrderService`；
- `RequestType=1007`；
- `doMap` 组装发件人 `Sender`、收件人 `Receiver`、商品 `Commodity`、重量、体积、月结编号、通知揽件等字段；
- 响应解析为 `KdniaoOrderVO`（含 `PrintTemplate` 面单模板）。

但源码中没有调用该方法的管理端/用户端接口，也没有电子面单表；注释明确说明“快递鸟订单打印需要申请当地营业网点账号，目前没法测试”。

## 6. 数据模型

### 6.1 表 `yshop_express`（快递公司）

DO：`ExpressDO`，继承 `BaseDO`，主键 `id`（`Integer`，自增）。

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | mediumint unsigned | 快递公司 id |
| `code` | varchar(50) | 快递公司简称/编码，如 `ZTO`、`SF` |
| `name` | varchar(50) | 快递公司全称 |
| `sort` | int | 排序 |
| `create_time`/`update_time`/`creator`/`updater`/`deleted` | — | `BaseDO` 公共字段 |

### 6.2 Redis 配置

| Key | 类型 | 内容 | 过期 |
|---|---|---|---|
| `yshop_express_cache:` | STRING | `KdniaoApiBaseDTO` JSON | 永不过期 |

### 6.3 面单错误码

`ErrorCodeConstants` 中定义了 `ELECTRONICS_ORDER_NOT_EXISTS = 1008009000`，但当前没有电子面单 DO/Mapper，该错误码未被使用；同时该值与支付模块的 `MERCHANT_DETAILS_NOT_EXISTS` 重复。

## 7. 依赖与集成

- **内部模块**：`express-biz` 仅依赖 `express-api`；`express-api` 依赖 `yshop-common`、MyBatis starter、validation。
- **服务装配**：只有 `yshop-server` 直接依赖 `yshop-module-express-biz`，其它业务模块**没有**引用快递模块。订单模块只在 `yshop_store_order` 中保存 `delivery_name`、`delivery_id` 等字段，前端拿到快递公司编码和单号后直接调用 `/app-api/express/getLogistic`。
- **第三方服务**：快递鸟开放接口（`api.kdniao.com`），未引入官方 SDK，使用 `HttpURLConnection` 手写请求。
- **工具库**：`fastjson`（JSON 序列化）、Hutool（Base64、文件等）。
- **框架能力**：Security、MyBatis-Plus、Redis、MQ starter（未实际使用）、Excel、租户组件。
- **配置项**：`yshop.trade.express.*` 存在于 `application.yaml`，但代码未读取；有效配置为 Redis 中的 `yshop_express_cache:`。

## 8. 注意事项

1. **管理端配置与查询接口没有权限注解**：`GET/POST /admin-api/order/express/set` 与 `GET /admin-api/order/express/getLogistic` 均未使用 `@PreAuthorize`；用户端 `getLogistic` 也未使用 `@PreAuthenticated`。任何能访问服务的人都可以读取/覆盖快递鸟配置并发起查询，生产环境应补充鉴权。
2. **接口来源以 Redis 为准**：`yshop.trade.express` 下的 `kd-niao`、`kd100` 配置是死配置；`kd100` 也没有任何实现。修改快递鸟密钥应调用后台 `/order/express/set` 或直接写 Redis。
3. **免费接口限制**：源码注释说明免费 `RequestType=1002` 目前只支持圆通、申通、百世等部分快递；`KdniaoLogisticsCodeEnum` 也只列出了 STO/ZTO/YTO/YD/SF。使用顺丰、韵达等可能需要切换收费接口。
4. **成功判断使用 `==`**：`KdniaoUtil` 中 `kdniaoApiVO.getSuccess() == "false"`、`kdniaoOrderVO.getSuccess() == "false"` 是引用比较，通常不会命中；失败响应可能不会被识别为异常，而是继续执行 `handleData()`。
5. **异常被吞掉**：`sendPost` 捕获所有异常后仅 `printStackTrace()` 并返回空字符串，随后 `JSON.parseObject` 可能得到 `null` 并触发空指针；查询接口缺少健壮的错误处理。
6. **无结果缓存/无限流**：每次查询都会实时请求快递鸟，没有本地缓存、频率限制或重试策略；高频轮询物流详情会消耗接口配额。
7. **状态枚举重复值**：`NO_TRACE(0)` 与 `DEFAULT(0)` 的 `status` 都是 0，`getEnum(0)` 永远返回 `NO_TRACE`，`DEFAULT` 实际不可达（仅未知非 0 值会走到 `DEFAULT`）。
8. **电子面单未接入**：`getOrderInfo`、`KdniaoElectronicsOrderDTO`、`KdniaoOrderVO` 目前是死代码；如要启用，需要新增 Controller、面单表、网点账号配置，并处理打印模板。
9. **面单/物流响应命名**：`KdniaoApiVO`、`KdniaoOrderVO` 的字段使用大写开头（如 `State`、`LogisticCode`、`PrintTemplate`），依赖 fastjson 按字段名映射快递鸟返回 JSON，改动字段名会直接影响解析。
10. **无物流跟踪表**：模块不保存轨迹，订单详情页每次查询都依赖第三方可用性；如需“物流跟踪/订阅推送”，需要另行设计轨迹表与快递鸟回调接口。
