# yshop-module-pay 支付模块

> 模块路径：`yshop-drink-boot3/yshop-module-pay`；文档依据本仓库源码整理。

## 1. 模块定位

`yshop-module-pay` 是支付基础能力模块，职责集中在两件事：

1. **支付服务商配置管理**：维护支付宝、微信支付的商户号、AppID、私钥/证书、回调地址、签名方式等配置，数据落库到 `merchant_details` 表。
2. **支付回调适配**：基于 `egzosn/pay-java-spring-boot-starter` 注册支付宝、微信支付回调处理器，把第三方支付成功通知转换成内部 Redis Stream 消息。

需要特别说明：**本模块本身没有下单、退款 Controller**。用户端下单支付、支付回调入口、退款审批均位于 `yshop-module-mall/yshop-module-order-biz`，它们通过 `PayServiceManager` 与本模块维护的 `merchant_details` 配置协作。本模块是“配置 + 回调适配 + MQ 通知”的支付支撑层。

## 2. 目录结构

```text
yshop-module-pay/
├── pom.xml
├── yshop-module-pay-api/
│   ├── pom.xml
│   └── src/main/java/co/yixiang/yshop/module/pay/
│       ├── config/
│       │   ├── MerchantPayServiceConfigurer.java   # 支付服务配置：JDBC 商户配置 + 回调 Handler
│       │   ├── PayAutoConfiguration.java           # 空配置类（注解已注释）
│       │   ├── handlers/
│       │   │   ├── AliPayMessageHandler.java       # 支付宝回调处理
│       │   │   └── WxPayMessageHandler.java        # 微信支付回调处理
│       │   └── interceptor/
│       │       ├── AliPayMessageInterceptor.java   # 未启用
│       │       └── WxPayMessageInterceptor.java    # 未启用
│       ├── enums/ErrorCodeConstants.java
│       └── mq/
│           ├── message/PayNoticeMessage.java       # Redis Stream 消息体
│           └── producer/PayNoticeProducer.java     # 发送 order.pay.notice
└── yshop-module-pay-biz/
    ├── pom.xml
    └── src/main/java/co/yixiang/yshop/module/pay/
        ├── controller/admin/merchantdetails/
        │   ├── MerchantDetailsController.java
        │   └── vo/                                 # Base/Create/Update/Page/Export/Resp/Excel VO
        ├── convert/merchantdetails/MerchantDetailsConvert.java
        ├── dal/
        │   ├── dataobject/merchantdetails/MerchantDetailsDO.java
        │   └── mysql/merchantdetails/MerchantDetailsMapper.java
        ├── http/HttpRequestNoticeNewParams.java    # 包装 HttpServletRequest 给 pay-java
        └── service/merchantdetails/
            ├── MerchantDetailsService.java
            └── MerchantDetailsServiceImpl.java
```

`-api` 子模块除枚举/MQ 外，还包含回调 Handler 与支付服务配置类，这是与常见 “api 只放接口/DTO” 分层不同的地方；`yshop-server` 的组件扫描范围为 `co.yixiang.yshop.module`，因此这些类会被直接扫描注册。

## 3. 核心功能

| 功能 | 入口类 | 说明 |
|---|---|---|
| 支付服务商配置 CRUD | `MerchantDetailsController`、`MerchantDetailsServiceImpl` | 对 `merchant_details` 表增删改查、分页、Excel 导出 |
| 支付 SDK 商户配置装配 | `MerchantPayServiceConfigurer` | 使用 `JdbcTemplate` 从 `merchant_details` 读取商户配置，供 `PayServiceManager` 使用 |
| 支付宝回调处理 | `AliPayMessageHandler` | 交易状态为 `TRADE_SUCCESS`/`TRADE_FINISHED` 时发送支付成功 MQ |
| 微信支付回调处理 | `WxPayMessageHandler` | `result_code=SUCCESS` 时发送支付成功 MQ |
| 支付成功消息投递 | `PayNoticeProducer`、`PayNoticeMessage` | 写入 Redis Stream `order.pay.notice` |
| 第三方通知请求适配 | `HttpRequestNoticeNewParams` | 把 `HttpServletRequest` 适配为 pay-java 的 `NoticeRequest` |

## 4. 主要接口

### 4.1 管理端 /admin-api

Controller 的 `@RequestMapping` 为 `/pay/merchant-details`，位于 `controller.admin` 包，框架自动添加 `/admin-api` 前缀。

| 方法 | 路径 | 说明 | 权限注解 |
|---|---|---|---|
| POST | `/admin-api/pay/merchant-details/create` | 创建支付服务商配置 | `@PreAuthorize("@ss.hasPermission('pay:merchant-details:create')")` |
| PUT | `/admin-api/pay/merchant-details/update` | 更新支付服务商配置 | `@PreAuthorize("@ss.hasPermission('pay:merchant-details:update')")` |
| DELETE | `/admin-api/pay/merchant-details/delete?id=` | 删除支付服务商配置 | `@PreAuthorize("@ss.hasPermission('pay:merchant-details:delete')")` |
| GET | `/admin-api/pay/merchant-details/get?id=` | 获得单个配置 | `@PreAuthorize("@ss.hasPermission('pay:merchant-details:query')")` |
| GET | `/admin-api/pay/merchant-details/list?ids=` | 按编号集合获得配置列表 | `@PreAuthorize("@ss.hasPermission('pay:merchant-details:query')")` |
| GET | `/admin-api/pay/merchant-details/page` | 分页查询 | `@PreAuthorize("@ss.hasPermission('pay:merchant-details:query')")` |
| GET | `/admin-api/pay/merchant-details/export-excel` | 导出 Excel | `@PreAuthorize("@ss.hasPermission('pay:merchant-details:export')")` |

### 4.2 用户端 /app-api

本模块没有 `controller.app` 包，**不提供 `/app-api` 接口**。实际用户端支付相关接口在订单模块（仅作调用关系说明）：

| 方法 | 路径 | 说明 | 所在模块/入口 |
|---|---|---|---|
| POST | `/app-api/order/pay` | 订单支付下单，返回微信/支付宝支付参数 | `order`，`AppOrderController#pay`（`@PreAuthenticated`） |
| ANY | `/app-api/order/notify/payBack{detailsId}.json` | 第三方支付异步回调，如 `payBackwx_miniapp.json` | `order`，`AppOrderController#payBack`（免登录） |
| POST | `/app-api/order/refund` | 用户提交退款申请 | `order`，`AppOrderController#refundVerify` |
| POST | `/admin-api/order/store-order/refund` | 管理端审批退款 | `order`，`StoreOrderController#refund` |

## 5. 核心实现逻辑

### 5.1 支付服务配置装配

`MerchantPayServiceConfigurer` 实现 pay-java starter 的 `PayServiceConfigurer`：

- `configure(MerchantDetailsServiceConfigurer merchants)`：使用 `merchants.jdbc().cache(false).template(jdbcTemplate)`，即商户配置直接从 `merchant_details` 表读取，不开启 SDK 本地缓存。
- `configure(PayMessageConfigurer configurer)`：向支付宝平台（`AliPaymentPlatform`）注册 `AliPayMessageHandler`，向微信平台（`WxPaymentPlatform`）注册 `WxPayMessageHandler`。两个拦截器类虽存在，但 `@Component` 被注释，且 `addInterceptor` 调用也被注释，当前未启用。

`yshop-server/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 引入了 `com.egzosn.pay.spring.boot.autoconfigue.PayAutoConfiguration`（注意源码包名中为 `autoconfigue`），由 SDK 自动装配 `PayServiceManager`。

### 5.2 支付下单（订单模块调用，支付模块提供配置）

`AppStoreOrderServiceImpl#pay(uid, param)` 的流程：

1. 通过 `uni` 查询订单（也兼容充值流水 `UserBillDO`），校验订单不存在/已支付。
2. 按 `PayTypeEnum` 分支：
   - **微信 H5**：`detailsId = PayIdEnum.WX_WECHAT`（`wx_wechat`），交易类型 `MWEB`，取 `merchant_details.return_url` 拼接到 `mweb_url` 的 `redirect_url`。
   - **微信公众号**：`detailsId = PayIdEnum.WX_WECHAT`，交易类型 `JSAPI`，设置会员 `openid`。
   - **微信小程序**：`detailsId = PayIdEnum.WX_MINIAPP`（`wx_miniapp`），交易类型 `JSAPI`，设置 `routineOpenid`。
   - **支付宝 H5**：`detailsId = PayIdEnum.ALI_H5`（`ali_h5`），交易类型 `WAP`，调用 `manager.toPay(payOrder)`。
   - **余额**：直接调用 `yuePay`，扣减会员余额后执行 `paySuccess`。
3. 通过 `PayServiceManager#getOrderInfo` / `toPay` 生成第三方支付参数。

`detailsId` 并非数字，而是 `PayIdEnum` 中的字符串键（`wx_h5`、`wx_miniapp`、`wx_wechat`、`ali_h5` 等），与 `merchant_details.details_id` 主键一一对应。

### 5.3 支付回调

1. 第三方请求 `/app-api/order/notify/payBack{detailsId}.json`。
2. `AppOrderController#payBack` 调用 `PayServiceManager#payBack(detailsId, new HttpRequestNoticeNewParams(request))`，SDK 根据 `detailsId` 找到商户配置并完成验签。
3. 验签通过后进入对应 Handler：
   - `AliPayMessageHandler`：`trade_status` 为 `TRADE_SUCCESS` 或 `TRADE_FINISHED` 时，取 `out_trade_no`，调用 `PayNoticeProducer#sendPayNoticeMessage(orderId, "alipay")`。
   - `WxPayMessageHandler`：`result_code` 为 `SUCCESS` 时，取 `out_trade_no`，调用 `PayNoticeProducer#sendPayNoticeMessage(orderId, "weixin")`。
4. `PayNoticeProducer` 将 `PayNoticeMessage` 写入 Redis Stream，Stream Key 为 `order.pay.notice`。
5. 订单模块的 `PayNoticeConsumer`（`AbstractRedisStreamMessageListener<PayNoticeMessage>`）消费后调用 `AppStoreOrderService#paySuccess(orderId, payType)`：
   - 若命中充值流水，则更新流水状态并给会员加余额；
   - 否则更新 `yshop_store_order` 的 `paid=1`、`pay_type`、`pay_time`；
   - 增加用户购买次数、写入订单状态记录和用户账单；
   - 非堂食且用户来源为小程序时，通过 `WeixinNoticeProducer` 发送支付成功订阅消息。

### 5.4 退款

退款逻辑不在本模块，位于 `StoreOrderServiceImpl#orderRefund`：

- **余额支付**：直接 `incMoney` 退回会员余额。
- **微信支付**：可先 `manager.refundQuery(PayIdEnum.WX_MINIAPP, ...)` 查询；再 `manager.refund(PayIdEnum.WX_MINIAPP, refundOrder)` 发起退款，成功后把本次退款单号写入订单 `out_trade_no`。
- **支付宝**：当前代码直接抛出“支付宝暂时不支持退款”。
- 退款后写入用户账单、订单状态并回滚库存。

本模块没有退款回调 Handler，当前退款为同步调用并依赖返回结果处理。

## 6. 数据模型

### 6.1 表 `merchant_details`（支付服务商配置）

DO：`MerchantDetailsDO`，继承 `BaseDO`，主键 `detailsId`（`@TableId(type = IdType.INPUT)`，字符串）。

| 字段 | 类型 | 说明 |
|---|---|---|
| `details_id` | char(32) | 主键/配置编号，业务中使用 `wx_miniapp`、`wx_wechat`、`ali_h5` 等 |
| `pay_type` | varchar(60) | 支付渠道，如 `aliPay`、`wxPay` |
| `appid` | varchar(32) | 应用 AppID |
| `mch_id` | varchar(32) | 商户号/合作伙伴 ID |
| `cert_store_type` | varchar(16) | 证书存储类型：`PATH`/`STR`/`INPUT_STREAM`/`CLASS_PATH`/`URL` |
| `key_private` | mediumtext | 私钥或私钥证书 |
| `key_public` | mediumtext | 公钥或公钥证书 |
| `key_cert` | varchar(20480) | 附加证书（如 SSL 证书） |
| `key_cert_pwd` | varchar(32) | 证书密码 |
| `notify_url` | varchar(256) | 异步回调地址 |
| `return_url` | varchar(256) | 同步跳转地址 |
| `sign_type` | varchar(16) | 签名方式，如 `MD5`、`RSA2` |
| `seller` | varchar(32) | 收款账号（支付宝场景） |
| `sub_app_id` / `sub_mch_id` | varchar(32) | 子 AppID / 子商户号 |
| `input_charset` | varchar(16) | 编码，默认 `utf-8` |
| `is_test` | tinyint(1) | 是否测试环境 |
| `creator`/`updater`/`create_time`/`update_time`/`deleted` | — | `BaseDO` 公共字段 |

### 6.2 MQ 消息

`PayNoticeMessage` 继承 `AbstractRedisStreamMessage`：

- `orderId`：订单编号（必填）；
- `payType`：`weixin` / `alipay`；
- `streamKey`：`order.pay.notice`。

## 7. 依赖与集成

- **订单模块**：`yshop-module-order-biz` 依赖 `yshop-module-pay-biz`，使用 `MerchantDetailsService` 读取配置、`PayServiceManager` 下单/退款、`HttpRequestNoticeNewParams` 处理回调；并消费 `PayNoticeMessage`。
- **第三方 SDK**：`com.egzosn:pay-java-wx`、`pay-java-ali`、`pay-spring-boot-starter`、`pay-java-web-support`。
- **框架能力**：MyBatis-Plus、Redis、`yshop-spring-boot-starter-mq`（Redis Stream）、Security、Excel、租户组件。
- **数据库**：`merchant_details` 表；`yshop-server` 的 `application.yaml` 已把 `/admin-api/pay/notify/**` 加入租户忽略 URL，但当前代码未使用该回调路径。
- **配置项**：`application-local.yaml` / `application-dev.yaml` 中的 `yshop.pay.order-notify-url`、`yshop.pay.refund-notify-url` 在当前源码中没有对应的 `@ConfigurationProperties` 或读取点，属于历史遗留配置；实际回调地址以 `merchant_details.notify_url` 为准。
- **错误码**：`MERCHANT_DETAILS_NOT_EXISTS = 1008009000`，与快递模块的 `ELECTRONICS_ORDER_NOT_EXISTS` 使用相同错误码，存在跨模块重复。

## 8. 注意事项

1. **模块边界**：不要在本模块寻找下单/退款接口；真实入口在 `order` 模块，本模块只维护支付配置和回调 Handler。
2. **回调路径不是 `/admin-api/pay/notify/**`**：实际为 `/app-api/order/notify/payBack{detailsId}.json`，且在租户忽略 URL 中配置的是 `/app-api/order/notify/*`。修改支付回调地址时必须同步更新 `merchant_details.notify_url`。
3. **详情 ID 与枚举强绑定**：`PayIdEnum` 的值必须与 `merchant_details.details_id` 完全一致；初始化数据中已存在 `ali_app`、`ali_h5`、`wx_h5`、`wx_miniapp`、`wx_wechat`。
4. **回调幂等性**：`paySuccess` 会重复更新订单状态、累计购买次数和写账单；代码未显式判断订单是否已处理，生产环境需要关注第三方重复通知或 MQ 重投带来的幂等问题。
5. **微信退款固定使用 `wx_miniapp` 配置**：`StoreOrderServiceImpl` 退款时使用 `PayIdEnum.WX_MINIAPP`，若实际交易来自公众号或 H5，仍会走小程序商户配置，需要确认商户配置是否通用。
6. **未启用的拦截器**：`AliPayMessageInterceptor`、`WxPayMessageInterceptor` 当前未注册；`WxPayMessageInterceptor#intercept` 返回 `false`，即使启用也会中断微信回调处理。
7. **安全**：`merchant_details` 包含私钥、证书、证书密码等敏感信息，管理端接口虽使用 `@PreAuthorize` 保护，但列表/详情/导出接口会原样返回这些字段，建议按环境做脱敏或加密存储。
8. **日志配置残留**：`application-local.yaml` 配置了 `co.yixiang.yshop.module.pay.dal.mysql.notify.PayNotifyTaskMapper` 的日志级别，但本仓库不存在该类/表，属于从上游 pay 模块裁剪后的残留。
