# yshop-module-message 消息模块

> 模块路径：`yshop-drink-boot3/yshop-module-message`；文档依据本仓库源码整理。

## 1. 模块定位

`yshop-module-message` 主要承担两类职责：

1. **微信消息通知**：维护公众号模板消息/小程序订阅消息的模板配置（`yshop_wechat_template`），并根据订单事件发送支付成功、发货成功、退款成功、充值成功通知。
2. **Redis 延迟队列基础设施**：提供 `DelayedQueueListener` 接口、线程池配置器和 Redisson 延迟队列轮询任务，供订单模块实现“30 分钟未支付自动取消”“60 分钟自动确认收货”等延迟业务。

需要说明的是：**本模块源码中没有 WebSocket 实现**。POM 描述中的 “websocket 推送消息” 并未体现在 `yshop-module-message` 的 Java 类中；WebSocket 能力由 `yshop-framework/yshop-spring-boot-starter-websocket` 提供，并在 `infra` 模块中有示例监听器，配置项为 `yshop.websocket.*`。本模块实际使用的是 **Redis Stream**（微信通知）和 **Redisson 延迟队列**（订单延时任务）。

## 2. 目录结构

```text
yshop-module-message/
├── pom.xml
├── yshop-module-message-api/
│   └── src/main/java/co/yixiang/yshop/module/message/
│       ├── enums/ErrorCodeConstants.java
│       └── enums/WechatTempateEnum.java          # 注意类名拼写为 Tempate
└── yshop-module-message-biz/
    └── src/main/java/co/yixiang/yshop/module/message/
        ├── controller/admin/wechattemplate/
        │   ├── WechatTemplateController.java
        │   └── vo/                               # Base/Create/Update/Page/Export/Resp/Excel VO
        ├── convert/wechattemplate/WechatTemplateConvert.java
        ├── dal/
        │   ├── dataobject/wechattemplate/WechatTemplateDO.java
        │   └── mysql/wechattemplate/WechatTemplateMapper.java
        ├── mq/
        │   ├── consumer/WeixinNoticeConsumer.java  # Redis Stream 消费者
        │   ├── message/WeixinNoticeMessage.java    # Stream Key: weixin.msg.notice
        │   └── producer/WeixinNoticeProducer.java
        ├── redismq/
        │   ├── DelayedQueueListener.java           # 延迟队列监听接口
        │   ├── DelayedQueueListenerConfigurer.java # 线程池与监听器启动
        │   ├── DelayedQueuePollTask.java           # Redisson 队列轮询
        │   └── msg/OrderMsg.java                   # 订单延迟消息体
        ├── service/wechattemplate/
        │   ├── WechatTemplateService.java
        │   └── WechatTemplateServiceImpl.java
        └── supply/
            ├── WeixinTemplateService.java          # 公众号模板消息发送
            └── WeiXinSubscribeService.java         # 小程序订阅消息发送
```

## 3. 核心功能

| 功能 | 入口类 | 说明 |
|---|---|---|
| 微信模板配置 CRUD | `WechatTemplateController`、`WechatTemplateServiceImpl` | 维护 `yshop_wechat_template`，支持分页、列表、Excel 导出 |
| 微信通知 MQ 投递 | `WeixinNoticeProducer`、`WeixinNoticeMessage` | 写入 Redis Stream `weixin.msg.notice` |
| 微信通知 MQ 消费 | `WeixinNoticeConsumer` | 按消息类型、模板 key 分发到公众号或小程序发送服务 |
| 公众号模板消息 | `WeixinTemplateService` | 通过 `WxMpService` 发送模板消息 |
| 小程序订阅消息 | `WeiXinSubscribeService` | 通过 `WxMaService` 发送一次性订阅消息 |
| 延迟队列基础能力 | `DelayedQueueListener`、`DelayedQueueListenerConfigurer`、`DelayedQueuePollTask` | 基于 Redisson `RBlockingDeque` + `RDelayedQueue`，每个监听器一个消费线程 |
| 订单延迟消息体 | `OrderMsg` | 仅包含 `orderId` |

`WechatTempateEnum` 定义的模板编号：

| 枚举 | value | 含义 |
|---|---|---|
| `PAY_SUCCESS` | `pay_success` | 支付成功通知 |
| `DELIVERY_SUCCESS` | `delivery_success` | 发货成功通知 |
| `REFUND_SUCCESS` | `refund_success` | 退款成功通知 |
| `RECHARGE_SUCCESS` | `recharge_success` | 充值成功通知 |
| `TEMPLATES` | `template` | 公众号模板消息类型 |
| `SUBSCRIBE` | `subscribe` | 小程序订阅消息类型 |

## 4. 主要接口

### 4.1 管理端 /admin-api

Controller 的 `@RequestMapping` 为 `/message/wechat-template`，位于 `controller.admin` 包。

| 方法 | 路径 | 说明 | 权限注解 |
|---|---|---|---|
| POST | `/admin-api/message/wechat-template/create` | 创建微信模板 | `@PreAuthorize("@ss.hasPermission('message:wechat-template:create')")` |
| PUT | `/admin-api/message/wechat-template/update` | 更新微信模板 | `@PreAuthorize("@ss.hasPermission('message:wechat-template:update')")` |
| DELETE | `/admin-api/message/wechat-template/delete?id=` | 删除微信模板 | `@PreAuthorize("@ss.hasPermission('message:wechat-template:delete')")` |
| GET | `/admin-api/message/wechat-template/get?id=` | 获得微信模板 | `@PreAuthorize("@ss.hasPermission('message:wechat-template:query')")` |
| GET | `/admin-api/message/wechat-template/list?ids=` | 按编号集合获得列表 | `@PreAuthorize("@ss.hasPermission('message:wechat-template:query')")` |
| GET | `/admin-api/message/wechat-template/page` | 分页查询 | `@PreAuthorize("@ss.hasPermission('message:wechat-template:query')")` |
| GET | `/admin-api/message/wechat-template/export-excel` | 导出 Excel | `@PreAuthorize("@ss.hasPermission('message:wechat-template:export')")` |

模板创建/更新请求的主要字段：`tempkey`（模板编号）、`name`、`content`、`tempid`（微信侧模板 ID）、`status`、`type`（`template` / `subscribe`）。

### 4.2 用户端 /app-api

本模块**没有 `controller.app` 包，不提供 `/app-api` 接口**。消息发送由订单等模块在服务端内部调用 `WeixinNoticeProducer` 触发。

## 5. 核心实现逻辑

### 5.1 模板配置读取

发送前通过 `WechatTemplateService#lambdaQuery()` 按 `type + tempkey` 查询唯一模板：

- `type = "template"`：公众号模板消息；
- `type = "subscribe"`：小程序订阅消息；
- 未配置模板时抛出错误码 `9999999`，提示“请后台配置 key:xxx 订阅消息 id”；
- `status = 0` 时返回空字符串，调用方不再发送；
- 否则返回 `tempid` 作为微信模板 ID。

初始化 SQL 中目前只有 `pay_success` 的两条配置（一条 `subscribe`、一条 `template`），发货/退款/充值模板需要自行在后台维护。

### 5.2 微信通知 Redis Stream

**生产端**：`WeixinNoticeProducer#sendNoticeMessage(...)` 组装 `WeixinNoticeMessage`，通过 `RedisMQTemplate#send` 写入 Stream。消息包含：

`tempkey`、`type`、`orderId`、`price`、`uid`、`time`、`deliveryName`、`deliveryId`、`id`（订单主键）、`numberId`（取餐号）、`productName`、`shopName`。

`getStreamKey()` 返回 `weixin.msg.notice`。

**消费端**：`WeixinNoticeConsumer` 继承 `AbstractRedisStreamMessageListener<WeixinNoticeMessage>`，在 `onMessage` 中分发：

- `type = template`（公众号）：
  - `tempkey = pay_success` → `weixinTemplateService.paySuccessNotice(orderId, price, uid)`
  - `tempkey = delivery_success` → `weixinTemplateService.deliverySuccessNotice(orderId, deliveryName, deliveryId, uid)`
  - `tempkey = refund_success` → `weixinTemplateService.refundSuccessNotice("您的订单退款申请被通过，钱款将很快还至您的支付账户。", orderId, deliveryName, uid, time)`
- `type = subscribe`（小程序）：
  - `tempkey = pay_success` → `weiXinSubscribeService.paySuccessNotice(numberId, productName, shopName, uid, id, orderId)`
  - `tempkey = delivery_success` → `weiXinSubscribeService.deliverySuccessNotice(orderId, deliveryName, deliveryId, uid)`
  - `tempkey = refund_success` → `weiXinSubscribeService.refundSuccessNotice(orderId, deliveryName, uid, time)`

注意：退款分支把 `deliveryName` 当作金额参数传入，属于字段复用；发送内容中的“金额”可能并不是真实退款金额。

### 5.3 公众号模板消息

`WeixinTemplateService`：

- 通过注入的 `WxMpService`（由 `wx-java-mp-spring-boot-starter` 按 `wx.mp.*` 配置创建的单账号 Bean）发送。
- `getUserOpenid(uid)` 从 `MemberUserDO#getWxProfile()` 中取公众号 `openid`；为空则跳过。
- 消息数据使用 `first`、`keyword1`、`keyword2`、`keyword3`、`remark` 等键，颜色统一 `#000000`。
- 跳转 URL 固定为 `https://www.yixiang.co`。
- 发送异常仅 `printStackTrace()`，不会向上抛出。

### 5.4 小程序订阅消息

`WeiXinSubscribeService`：

- 通过注入的 `WxMaService` 发送一次性订阅消息。
- `getUserOpenid(uid)` 读取 `MemberUserDO#getRoutineOpenid()`（小程序 openid）。
- 支付成功模板字段：`character_string1`（取餐号）、`thing2`（商品名）、`time3`（当前时间）、`thing4`（门店名）、`thing5`（固定文案“意向点餐系统为您服务！”）。
- 跳转页面：支付成功 `/pages/components/pages/orders/detail?id={orderId}`，退款 `/order/detail/{orderId}`，发货 `/order/detail/{orderId}`。
- 发送异常同样只 `printStackTrace()`。

### 5.5 实际调用点

当前源码中**只有支付成功通知被实际触发**：订单模块 `AppStoreOrderServiceImpl#paySuccess` 在“非堂食且会员登录类型为小程序”时调用：

```java
weixinNoticeProducer.sendNoticeMessage(uid, WechatTempateEnum.PAY_SUCCESS.getValue(),
        WechatTempateEnum.SUBSCRIBE.getValue(), orderId, "", "", "", "",
        orderId主键, numberId, productName, shopName);
```

发货通知与退款通知的 `sendNoticeMessage` 调用在 `StoreOrderServiceImpl` 中被注释，`rechargeSuccessNotice` 在两个发送服务中虽已实现，但未检索到调用方。

### 5.6 Redisson 延迟队列

`DelayedQueueListenerConfigurer` 在 `afterPropertiesSet()` 时：

1. 断言 `DelayedQueueListener` 列表非空；
2. 创建线程数 = 监听器数量的 `ThreadPoolExecutor`；
3. 为每个监听器提交一个 `DelayedQueuePollTask`。

`DelayedQueuePollTask`：

1. `redissonClient.getBlockingDeque(delayedQueueKey())`；
2. `redissonClient.getDelayedQueue(blockingDeque)` 触发订阅，避免消息丢失；
3. 循环 `blockingDeque.take()`，调用监听器 `consume(message)`；
4. 中断时退出循环；异常只记录日志；
5. `finally` 中调用 `whenExceptionFinally()`（注意：`finally` 每轮都会执行，并不只在异常时执行）。

订单模块的两个实现：

| 实现类 | 队列 Key | 消费动作 |
|---|---|---|
| `OrderUnPayListener` | `order-unpay-cancel-queue` | 调用 `cancelOrder` 自动取消未支付订单（默认 30 分钟） |
| `OrderAutoConfirmListener` | `order:unconfirm:` | 调用 `takeOrder` 自动确认收货（默认 60 分钟） |

生产端在订单模块中直接使用 `RDelayedQueue#offer(message, delay, TimeUnit.MINUTES)` 投递 `OrderMsg`。

## 6. 数据模型

### 6.1 表 `yshop_wechat_template`（微信模板）

DO：`WechatTemplateDO`，继承 `BaseDO`，主键 `id`（`Integer`）。

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | int unsigned | 模板 id |
| `tempkey` | char(50) | 模板编号，如 `pay_success`、`delivery_success` |
| `name` | char(100) | 模板名 |
| `content` | varchar(1000) | 回复内容/模板内容说明 |
| `tempid` | char(100) | 微信侧模板 ID |
| `status` | tinyint | 状态，0 为停用（发送时返回空 ID） |
| `type` | varchar(30) | `template` 公众号模板消息 / `subscribe` 小程序订阅消息 |
| `creator`/`updater`/`create_time`/`update_time`/`deleted` | — | `BaseDO` 公共字段 |

表上另有 `tempkey` 普通索引，但**没有建立 `(type, tempkey)` 唯一约束**，代码按唯一结果查询；若存在重复配置，MyBatis-Plus `one()` 可能抛错。

### 6.2 MQ 消息

`WeixinNoticeMessage` 继承 `AbstractRedisStreamMessage`，Stream Key 为 `weixin.msg.notice`，字段见 5.2。

`OrderMsg` 为普通 `Serializable` 类，仅含 `orderId`，用于 Redisson 延迟队列。

## 7. 依赖与集成

- **内部模块**：`member-biz`（`MemberUserService`、`MemberUserDO`，用于取 openid/小程序 openid）；`yshop-server` 直接依赖本模块。
- **订单模块**：`yshop-module-order-biz` 依赖 `yshop-module-message-biz`，使用 `WeixinNoticeProducer`、`WeixinNoticeMessage`、`WechatTempateEnum`、`DelayedQueueListener`、`OrderMsg`。
- **框架能力**：`yshop-spring-boot-starter-mq`（Redis Stream）、`yshop-spring-boot-starter-redis`（Redisson/StringRedisTemplate）、Security、Excel、租户组件。
- **第三方 SDK**：源码使用 `WxMpService`、`WxMaService`。本模块 POM 未直接声明 WxJava 依赖，而是通过 `yshop-module-member-biz` 传递引入 `wx-java-mp-spring-boot-starter`、`wx-java-miniapp-spring-boot-starter`。
- **配置项**：
  - `wx.mp.*`：公众号 app-id/secret、Redis 存储、HTTP 客户端；
  - `wx.miniapp.*`：小程序 appid/secret；
  - `spring.data.redis.*` / Redisson 配置；
  - 通用 MQ 配置（RocketMQ/Kafka 等仅为框架可选项，本模块使用的是 Redis Stream 与 Redisson）。
- **数据库**：仅 `yshop_wechat_template`。

## 8. 注意事项

1. **没有 WebSocket 代码**：本模块名下的 “websocket” 仅出现在 POM 描述中；实际 WebSocket 端点在 `infra`（`yshop.websocket.path=/infra/ws`），消息发送器由 `yshop-spring-boot-starter-websocket` 提供。
2. **仅支付成功通知生效**：发货、退款通知的生产调用被注释，充值通知无调用方；若需要这些通知，必须恢复/新增 `WeixinNoticeProducer` 调用。
3. **公众号与小程序使用的是 starter 单账号 Bean**：`WeixinTemplateService` 注入全局 `WxMpService`，`WeiXinSubscribeService` 注入全局 `WxMaService`，与 `yshop-module-mp` 的多公众号 `MpServiceFactory` 不是同一套体系；配置只来自 `wx.mp` / `wx.miniapp`。
4. **模板缺失直接抛异常**：`getTempId` 找不到配置会抛 `9999999`，且该异常发生在消息消费线程中，会被 Redisson/Stream 消费框架捕获；需要提前在后台维护模板。
5. **发送失败被吞掉**：两个发送服务的 `WxErrorException` 都只 `printStackTrace()`，不会重试、不会记录业务失败状态，排查线上问题需依赖日志。
6. **退款消息字段复用**：消费者把 `deliveryName` 传入退款金额参数，模板渲染出的金额可能不正确。
7. **`@TenantIgnore` 标在 private 方法上**：`WeiXinSubscribeService#getTempId`、`getUserOpenid` 为 private，Spring AOP 代理无法拦截 private 方法，该注解实际不会生效；代码在调用处额外使用了 `TenantUtils.executeIgnore`。
8. **延迟队列线程模型**：每个监听器一个永久阻塞线程；`DelayedQueueListenerConfigurer` 断言监听器列表非空，若部署中移除订单模块且没有其他监听器，应用启动会失败；`whenExceptionFinally()` 在每轮 `finally` 都会执行，与“异常时最终处理”的注释语义不一致。
9. **状态语义**：模板 `status=0` 时发送服务返回空模板 ID 并跳过发送，不是抛异常；模板 `status` 使用 `Byte` 类型，前端传值需注意。
10. **类名拼写**：枚举类名为 `WechatTempateEnum`（少了一个 `l`），检索代码时不要按 `WechatTemplateEnum` 查找。
