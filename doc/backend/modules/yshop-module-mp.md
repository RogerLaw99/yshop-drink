# yshop-module-mp 微信公众号模块

> 模块路径：`yshop-drink-boot3/yshop-module-mp`；文档依据本仓库源码整理。

## 1. 模块定位

`yshop-module-mp` 提供**多公众号账号**的运营管理能力，基于 WxJava（`wx-java-mp-spring-boot-starter`）实现：

- 公众号账号配置与本地缓存；
- 微信服务器接入校验（签名校验、明文/AES 消息解析）；
- 消息接收与保存、客服消息发送；
- 自定义菜单、自动回复、素材、粉丝、标签、草稿、发布、数据统计；
- 一个形式上的“小程序账号”管理入口（复用 `mp_account` 表，能力有限）。

模块只有 `admin` Controller，没有 `app` Controller；微信服务器回调接口虽然位于 `controller.admin.open` 包下，但被安全白名单放行。业务消息通知（模板消息/订阅消息）不在本模块，而在 `yshop-module-message`。

## 2. 目录结构

```text
yshop-module-mp/
├── pom.xml
├── yshop-module-mp-api/
│   └── src/main/java/co/yixiang/yshop/module/mp/
│       ├── enums/ErrorCodeConstants.java
│       ├── enums/MpAccountEnum.java
│       └── enums/message/
│           ├── MpAutoReplyMatchEnum.java
│           ├── MpAutoReplyTypeEnum.java
│           └── MpMessageSendFromEnum.java
└── yshop-module-mp-biz/
    └── src/main/java/co/yixiang/yshop/module/mp/
        ├── controller/admin/
        │   ├── account/          # MpAccountController、MaAccountController
        │   ├── material/         # MpMaterialController
        │   ├── menu/             # MpMenuController
        │   ├── message/          # MpMessageController、MpAutoReplyController
        │   ├── news/             # MpDraftController、MpFreePublishController
        │   ├── open/             # MpOpenController（微信回调）
        │   ├── statistics/       # MpStatisticsController
        │   ├── tag/              # MpTagController
        │   └── user/             # MpUserController
        ├── convert/              # MapStruct 转换器
        ├── dal/
        │   ├── dataobject/       # account/material/menu/message/tag/user DO
        │   └── mysql/            # 各 Mapper
        ├── framework/
        │   ├── mp/config/MpConfiguration.java
        │   ├── mp/core/          # MpServiceFactory、DefaultMpServiceFactory、MpContextHolder、MpUtils
        │   └── web/config/MpWebConfiguration.java
        └── service/
            ├── account/ material/ menu/ message/ statistics/ tag/ user/
            └── handler/          # 微信事件处理器（menu/message/other/user）
```

## 3. 核心功能

| 功能 | 入口类 | 说明 |
|---|---|---|
| 公众号账号管理 | `MpAccountController`、`MpAccountServiceImpl` | 增删改查、分页、精简列表、生成二维码、清空 API 配额 |
| 小程序账号管理 | `MaAccountController` | 复用公众号账号创建/更新/删除/查询接口，权限前缀为 `ma:account:*` |
| 多账号服务工厂 | `DefaultMpServiceFactory` | 按 appId/账号 id 构建并缓存 `WxMpService`、`WxMpMessageRouter` |
| 微信接入与消息路由 | `MpOpenController` | 校验签名、解析明文/AES 消息、交给路由器处理并返回 XML |
| 消息接收与客服消息 | `MessageReceiveHandler`、`MpMessageServiceImpl` | 保存粉丝上行消息、下载媒体文件、主动发送客服消息 |
| 自动回复 | `MpAutoReplyServiceImpl`、`MessageAutoReplyHandler` | 关注回复、按消息类型回复、关键词完全/半匹配回复 |
| 自定义菜单 | `MpMenuServiceImpl`、`MenuHandler` | 同步菜单到公众号、落库、点击菜单时回复配置消息 |
| 粉丝管理 | `MpUserServiceImpl`、`SubscribeHandler`、`UnsubscribeHandler` | 关注时拉取并保存粉丝、取消关注更新状态、全量同步、打标签 |
| 标签管理 | `MpTagServiceImpl` | 创建/更新/删除公众号标签、从公众号同步标签及粉丝数 |
| 素材管理 | `MpMaterialServiceImpl` | 上传临时/永久素材、上传图文图片、下载媒体到文件服务 |
| 草稿与发布 | `MpDraftController`、`MpFreePublishController` | 直接调用 WxJava 草稿箱/发布能力 |
| 数据统计 | `MpStatisticsServiceImpl` | 粉丝增减、累计粉丝、消息发送、接口分析 |
| 公众号回调上下文 | `MpContextHolder` | 用 TransmittableThreadLocal 保存当前 appId，支持多公众号 |

## 4. 主要接口

### 4.1 管理端 /admin-api

所有 Controller 位于 `controller.admin` 包，框架自动添加 `/admin-api` 前缀。

#### 公众号账号 `/mp/account`

| 方法 | 路径 | 说明 | 权限注解 |
|---|---|---|---|
| POST | `/admin-api/mp/account/create` | 创建公众号账号 | `mp:account:create` |
| PUT | `/admin-api/mp/account/update` | 更新公众号账号 | `mp:account:update` |
| DELETE | `/admin-api/mp/account/delete?id=` | 删除公众号账号 | `mp:account:delete` |
| GET | `/admin-api/mp/account/get?id=` | 获得公众号账号 | `mp:account:query` |
| GET | `/admin-api/mp/account/page` | 分页查询公众号账号 | `mp:account:query` |
| GET | `/admin-api/mp/account/list-all-simple` | 公众号精简列表 | `mp:account:query` |
| PUT | `/admin-api/mp/account/generate-qr-code?id=` | 生成公众号二维码 | `mp:account:qr-code` |
| PUT | `/admin-api/mp/account/clear-quota?id=` | 清空公众号 API 配额 | `mp:account:clear-quota` |

#### 小程序账号 `/ma/account`

| 方法 | 路径 | 说明 | 权限注解 |
|---|---|---|---|
| POST | `/admin-api/ma/account/create` | 创建小程序账号 | `ma:account:create` |
| PUT | `/admin-api/ma/account/update` | 更新小程序账号 | `ma:account:update` |
| DELETE | `/admin-api/ma/account/delete?id=` | 删除小程序账号 | `ma:account:delete` |
| GET | `/admin-api/ma/account/get?id=` | 获得小程序账号 | `ma:account:query` |
| GET | `/admin-api/ma/account/page` | 分页查询小程序账号 | `ma:account:query` |

#### 微信回调 `/mp/open`

| 方法 | 路径 | 说明 | 权限注解 |
|---|---|---|---|
| GET | `/admin-api/mp/open/{appId}` | 微信服务器校验签名，返回 `echostr` | 无；`/admin-api/mp/open/**` 在 `yshop.security.permit-all_urls` 白名单 |
| POST | `/admin-api/mp/open/{appId}` | 接收微信消息/事件推送，返回 XML | 无；同上 |

#### 素材 `/mp/material`

| 方法 | 路径 | 说明 | 权限注解 |
|---|---|---|---|
| POST | `/admin-api/mp/material/upload-temporary` | 上传临时素材（multipart） | `mp:material:upload-temporary` |
| POST | `/admin-api/mp/material/upload-permanent` | 上传永久素材（multipart） | `mp:material:upload-permanent` |
| POST | `/admin-api/mp/material/upload-news-image` | 上传图文内容中的图片 | `mp:material:upload-news-image` |
| DELETE | `/admin-api/mp/material/delete-permanent?id=` | 删除素材 | `mp:material:delete` |
| GET | `/admin-api/mp/material/page` | 素材分页 | `mp:material:query` |

#### 菜单 `/mp/menu`

| 方法 | 路径 | 说明 | 权限注解 |
|---|---|---|---|
| POST | `/admin-api/mp/menu/save` | 保存菜单并同步到公众号 | `mp:menu:save` |
| DELETE | `/admin-api/mp/menu/delete?accountId=` | 删除公众号菜单 | `mp:menu:delete` |
| GET | `/admin-api/mp/menu/list?accountId=` | 获得菜单列表 | `mp:menu:query` |

#### 自动回复 `/mp/auto-reply`

| 方法 | 路径 | 说明 | 权限注解 |
|---|---|---|---|
| GET | `/admin-api/mp/auto-reply/page` | 自动回复分页 | `mp:auto-reply:query` |
| GET | `/admin-api/mp/auto-reply/get?id=` | 获得自动回复 | `mp:auto-reply:query` |
| POST | `/admin-api/mp/auto-reply/create` | 创建自动回复 | `mp:auto-reply:create` |
| PUT | `/admin-api/mp/auto-reply/update` | 更新自动回复 | `mp:auto-reply:update` |
| DELETE | `/admin-api/mp/auto-reply/delete?id=` | 删除自动回复 | `mp:auto-reply:delete` |

#### 消息 `/mp/message`

| 方法 | 路径 | 说明 | 权限注解 |
|---|---|---|---|
| GET | `/admin-api/mp/message/page` | 消息分页 | `mp:message:query` |
| POST | `/admin-api/mp/message/send` | 给粉丝发送客服消息 | `mp:message:send` |

#### 草稿 `/mp/draft`

| 方法 | 路径 | 说明 | 权限注解 |
|---|---|---|---|
| GET | `/admin-api/mp/draft/page` | 从公众号查询草稿分页 | `mp:draft:query` |
| POST | `/admin-api/mp/draft/create?accountId=` | 创建草稿，请求体 `WxMpAddDraft` | `mp:draft:create` |
| PUT | `/admin-api/mp/draft/update?accountId=&mediaId=` | 更新草稿 | `mp:draft:update` |
| DELETE | `/admin-api/mp/draft/delete?accountId=&mediaId=` | 删除草稿 | `mp:draft:delete` |

#### 发布 `/mp/free-publish`

| 方法 | 路径 | 说明 | 权限注解 |
|---|---|---|---|
| GET | `/admin-api/mp/free-publish/page` | 已发布图文分页 | `mp:free-publish:query` |
| POST | `/admin-api/mp/free-publish/submit?accountId=&mediaId=` | 发布草稿 | `mp:free-publish:submit` |
| DELETE | `/admin-api/mp/free-publish/delete?accountId=&articleId=` | 删除发布记录 | `mp:free-publish:delete` |

#### 标签 `/mp/tag`

| 方法 | 路径 | 说明 | 权限注解 |
|---|---|---|---|
| POST | `/admin-api/mp/tag/create` | 创建标签 | `mp:tag:create` |
| PUT | `/admin-api/mp/tag/update` | 更新标签 | `mp:tag:update` |
| DELETE | `/admin-api/mp/tag/delete?id=` | 删除标签 | `mp:tag:delete` |
| GET | `/admin-api/mp/tag/get?id=` | 标签详情 | `mp:tag:query` |
| GET | `/admin-api/mp/tag/page` | 标签分页 | `mp:tag:query` |
| GET | `/admin-api/mp/tag/list-all-simple` | 标签精简列表 | `mp:account:query`（注意：不是 `mp:tag:*`） |
| POST | `/admin-api/mp/tag/sync?accountId=` | 从公众号同步标签 | `mp:tag:sync` |

#### 粉丝 `/mp/user`

| 方法 | 路径 | 说明 | 权限注解 |
|---|---|---|---|
| GET | `/admin-api/mp/user/page` | 粉丝分页 | `mp:user:query` |
| GET | `/admin-api/mp/user/get?id=` | 粉丝详情 | `mp:user:query` |
| PUT | `/admin-api/mp/user/update` | 更新昵称、备注、标签 | `mp:user:update` |
| POST | `/admin-api/mp/user/sync?accountId=` | 从公众号全量同步粉丝（异步） | `mp:user:sync` |

#### 统计 `/mp/statistics`

| 方法 | 路径 | 说明 | 权限注解 |
|---|---|---|---|
| GET | `/admin-api/mp/statistics/user-summary` | 粉丝增减数据 | `mp:statistics:query` |
| GET | `/admin-api/mp/statistics/user-cumulate` | 粉丝累计数据 | `mp:statistics:query` |
| GET | `/admin-api/mp/statistics/upstream-message` | 消息发送概况 | `mp:statistics:query` |
| GET | `/admin-api/mp/statistics/interface-summary` | 接口分析数据 | `mp:statistics:query` |

### 4.2 用户端 /app-api

本模块**没有 `controller.app` 包，不提供 `/app-api` 接口**。微信服务器的回调走 `/admin-api/mp/open/{appId}`，但它是给微信平台调用的，不是用户端接口。用户端公众号登录、绑定等能力位于 `member` / `system` 模块。

## 5. 核心实现逻辑

### 5.1 多公众号服务初始化与缓存

`MpAccountServiceImpl`：

- `@PostConstruct initLocalCache()`：忽略多租户，查询全部 `mp_account`，调用 `MpServiceFactory#init(accounts)` 构建每个账号的 `WxMpService` 和 `WxMpMessageRouter`，同时以 `appId -> MpAccountDO` 建立本地缓存。
- `@Scheduled(initialDelay=60, fixedRate=60)`：每 60 秒检查 `mp_account.update_time` 最大值，有变更则重建缓存，用于多节点部署时各节点自行刷新。
- 创建/更新/删除账号后立即调用 `initLocalCache()`。
- 表结构不存在时捕获异常并提示“表结构未导入”，避免影响应用启动。

`DefaultMpServiceFactory#buildMpService` 使用 `WxMpRedisConfigImpl`，把 access token 等存放在 Redis，key 前缀取 `wx.mp.config-storage.key-prefix`。

### 5.2 微信回调：签名校验与消息路由

`MpOpenController`：

1. **GET `/mp/open/{appId}`**：通过 `MpServiceFactory` 找到 `WxMpService`，执行 `checkSignature(timestamp, nonce, signature)`，成功返回 `echostr`，失败返回“非法请求”。
2. **POST `/mp/open/{appId}`**：
   - 从缓存按 appId 查账号，设置 `MpContextHolder` 的 appId，并在该账号的 `tenantId` 下执行；
   - 再次校验签名；
   - `encrypt_type` 为空按明文 `WxMpXmlMessage.fromXml` 解析，为 `aes` 时按 AES 加密消息解析；
   - 交给 `WxMpMessageRouter#route`，有回复则按明文/加密方式返回 XML。

`DefaultMpServiceFactory#buildMpMessageRouter` 的规则顺序：

1. `MessageReceiveHandler`：记录所有事件日志/保存消息（`next()` 继续后续规则）。
2. 客服会话事件 → `KfSessionHandler`。
3. 门店审核事件 → `StoreCheckNotifyHandler`。
4. 菜单 `CLICK` → `MenuHandler`；`VIEW` → `NullHandler`。
5. 关注 → `SubscribeHandler`；取消关注 → `UnsubscribeHandler`。
6. 上报地理位置事件与地理位置消息 → `LocationHandler`。
7. 扫码事件 → `ScanHandler`。
8. 默认规则 → `MessageAutoReplyHandler`。

其中 `KfSessionHandler`、`NullHandler`、`ScanHandler`、`StoreCheckNotifyHandler` 当前直接抛出 `UnsupportedOperationException`，属于预留扩展点。

### 5.3 关注 / 取消关注

- **关注**：`SubscribeHandler` 调用公众号 `userInfo` 获取粉丝信息，`MpUserService#saveUser` 按 `appId + openid` 新增或更新 `mp_user`，然后调用 `MpAutoReplyService#replyForSubscribe` 返回欢迎语；若未配置关注回复，则默认回复文本“感谢关注”。
- **取消关注**：`UnsubscribeHandler` 调用 `MpUserService#updateUserUnsubscribe`，把 `subscribe_status` 置为禁用并记录 `unsubscribe_time`。

### 5.4 消息接收与自动回复

- `MessageReceiveHandler` → `MpMessageServiceImpl#receiveMessage`：
  - 关注事件不记录（此时粉丝表可能还没有数据）；
  - 按 `appId + openid` 找到粉丝，转换并保存 `mp_message`；
  - 有 `mediaId`/`thumbMediaId` 时通过 `MpMaterialService#downloadMaterialUrl` 下载到文件服务。
- `MessageAutoReplyHandler` → `MpAutoReplyServiceImpl#replyForMessage`：
  1. 文本消息先按关键词“完全匹配”查询；
  2. 未命中再按“半匹配”（`LIKE`）查询；
  3. 仍未命中则按消息类型（`request_message_type`）查询；
  4. 取第一条，转换为 `MpMessageSendOutReqBO`，由 `MpMessageService#sendOutMessage` 记录消息并生成被动回复 XML。
- 自动回复冲突校验：
  - `SUBSCRIBE`：同一账号只允许一条关注回复；
  - `MESSAGE`：同一账号同一请求消息类型只允许一条；
  - `KEYWORD`：同一账号同一关键词只允许一条。

### 5.5 菜单

`MpMenuServiceImpl#saveMenu`：

1. 校验账号与菜单参数（`MpUtils#validateButton` 按按钮类型选择校验分组）；
2. 通过 `WxMpService#getMenuService().menuCreate` 同步到公众号；
3. 删除该账号旧菜单记录，按“顶级菜单 + 子菜单”两级写入 `mp_menu`；
4. 粉丝点击 `CLICK` 菜单时，`MenuHandler` 根据 `appId + menuKey` 查菜单，再按 `replyMessageType` 返回配置的文本/图片/语音/视频/图文/音乐。

`deleteMenuByAccountId` 会先调用公众号 `menuDelete`，再删除本地记录。

### 5.6 素材

`MpMaterialServiceImpl`：

- 临时素材：先上传到公众号得到 `mediaId`，再上传到本项目文件服务并写 `mp_material`（`permanent=false`）。
- 永久素材：调用 `materialFileUpload`，保存 `mpUrl`、标题、描述等（`permanent=true`）。
- 删除素材：仅永久素材会调用公众号 `materialDelete`，然后删除本地记录。
- `downloadMaterialUrl`：先查库，未命中则从公众号下载并上传文件服务；媒体文件在微信侧只保留 3 天，因此需要转存。
- 文件服务通过 `infra` 模块的 `FileApi#createFile` 完成。

### 5.7 粉丝、标签与统计

- `MpUserServiceImpl#syncUser`：`@Async` 异步执行，调用公众号 `userList` 分页拉取 openid，再每 100 个一批调用 `userInfoList`，批量新增/更新 `mp_user`。
- `MpUserServiceImpl#updateUser`：先通过公众号接口取消旧标签、设置新标签，再更新本地昵称/备注/标签。
- `MpTagServiceImpl`：创建/更新/删除均先同步公众号再落库；`syncTag` 拉取公众号标签列表，与本地按 `tagId` 合并（新增、更新、删除），并更新粉丝数 `count`。
- `MpStatisticsServiceImpl`：直接调用公众号 `getDataCubeService()` 的粉丝、消息、接口统计接口。

### 5.8 草稿与发布

`MpDraftController`、`MpFreePublishController` 不做本地持久化，直接通过 `MpServiceFactory` 获取 `WxMpService` 后调用 WxJava 的草稿箱/发布服务；发布列表中的缩略图会用 `mp_material` 中的 `mediaId -> url` 映射替换为可访问地址。

## 6. 数据模型

所有表均带 `creator`、`create_time`、`updater`、`update_time`、`deleted`、`tenant_id` 公共字段（`mp_account` 使用 `TenantBaseDO`，其余使用 `BaseDO`，但表结构都含 `tenant_id`）。

### 6.1 `mp_account` 公众号账号

DO：`MpAccountDO`（`TenantBaseDO`）

| 字段 | 说明 |
|---|---|
| `id` | 主键 |
| `name` | 公众号名称 |
| `account` | 公众号微信号 |
| `app_id` | 公众号 AppID（全局唯一校验） |
| `app_secret` | 公众号密钥 |
| `url` | 公众号 URL（表中有，DO 未映射） |
| `token` | 服务器配置 Token |
| `aes_key` | 消息加解密密钥 |
| `qr_code_url` | 二维码图片 URL |
| `remark` | 备注 |
| `tenant_id` | 租户编号 |

### 6.2 `mp_message` 公众号消息

DO：`MpMessageDO`（`autoResultMap=true`）

关键字段：`id`、`msg_id`、`account_id`、`app_id`、`user_id`、`openid`、`type`、`send_from`、`content`、`media_id`/`media_url`、`recognition`、`format`、`title`、`description`、`thumb_media_id`/`thumb_media_url`、`url`、`location_x`/`location_y`/`scale`/`label`、`articles`（JSON，`ArticleTypeHandler`）、`music_url`/`hq_music_url`、`event`、`event_key`。

`Article` 内部类字段：`title`、`description`、`picUrl`、`url`。

### 6.3 `mp_auto_reply` 自动回复

DO：`MpAutoReplyDO`（`autoResultMap=true`）

关键字段：`id`、`account_id`、`app_id`、`type`（1 关注/2 消息/3 关键词）、`request_keyword`、`request_match`（1 完全/2 半匹配）、`request_message_type`、`response_message_type`、`response_content`、`response_media_id`/`response_media_url`、`response_title`、`response_description`、`response_thumb_media_id`/`response_thumb_media_url`、`response_articles`（JSON）、`response_music_url`、`response_hq_music_url`。

### 6.4 `mp_menu` 菜单

DO：`MpMenuDO`（`autoResultMap=true`）

关键字段：`id`、`account_id`、`app_id`、`name`、`menu_key`、`parent_id`（顶级为 0）、`type`、`url`、`mini_program_app_id`、`mini_program_page_path`、`article_id`、`reply_message_type`、`reply_content`、`reply_media_id`/`reply_media_url`、`reply_title`、`reply_description`、`reply_thumb_media_id`/`reply_thumb_media_url`、`reply_articles`（JSON）、`reply_music_url`、`reply_hq_music_url`。

### 6.5 `mp_material` 素材

DO：`MpMaterialDO`

关键字段：`id`、`account_id`、`app_id`、`media_id`、`type`、`permanent`、`url`（文件服务地址）、`name`、`mp_url`（公众号地址，永久素材）、`title`、`introduction`。

### 6.6 `mp_user` 粉丝

DO：`MpUserDO`（`autoResultMap=true`）

关键字段：`id`、`openid`、`union_id`、`subscribe_status`、`subscribe_time`、`unsubscribe_time`、`nickname`、`head_image_url`、`language`、`country`、`province`、`city`、`remark`、`tag_ids`（JSON，`LongListTypeHandler`）、`account_id`、`app_id`。

### 6.7 `mp_tag` 标签

DO：`MpTagDO`

关键字段：`id`、`tag_id`（公众号标签 ID）、`name`、`count`（粉丝数，需同步更新）、`account_id`、`app_id`。

## 7. 依赖与集成

- **内部模块**：`system-api`（错误码/用户相关）、`infra-api`（`FileApi` 文件存储）；`mp-biz` 被 `yshop-server` 直接依赖。
- **第三方 SDK**：`com.github.binarywang:wx-java-mp-spring-boot-starter`、`wx-java-miniapp-spring-boot-starter`。
- **框架能力**：Security（`@PreAuthorize`、白名单）、MyBatis-Plus、Redis、MQ starter、Excel、租户组件、Swagger 分组。
- **配置项**：
  - `wx.mp.app-id`、`wx.mp.secret`、`wx.mp.config-storage.*`（供 WxJava starter / `WxMpProperties` 使用）；
  - `wx.miniapp.*`（小程序配置；本模块的 `MaAccountController` 实际未使用该配置）；
  - `yshop.security.permit-all_urls` 中的 `/admin-api/mp/open/**`；
  - `yshop.tenant.ignore-urls` 中的 `/admin-api/mp/open/**`。
- **数据库**：`mp_account`、`mp_message`、`mp_auto_reply`、`mp_menu`、`mp_material`、`mp_user`、`mp_tag`。

## 8. 注意事项

1. **小程序账号能力名不副实**：`MpAccountBaseVO` 有 `isMain`、`isMiapp` 字段，但 `MpAccountDO` 和 `mp_account` 表都没有对应列，MapStruct 转换会忽略；`MaAccountController` 设置 `isMini`/`isMiapp` 后，`MpAccountMapper#selectPage` 也未使用该条件。结果是公众号和小程序账号共用同一张表、同一套查询，“小程序账号”并没有真正的数据隔离。
2. **分页筛选未生效**：`MpAccountPageReqVO.isMini`、`MpUserPageReqVO.unionId` 均未在对应 Mapper 中使用；`MpAccountDO` 也未映射表中的 `url` 字段。
3. **`mp:tag:list-all-simple` 权限串为 `mp:account:query`**：与其他标签接口不一致，配置菜单权限时需注意。
4. **自动回复分页复用了消息 VO**：`MpAutoReplyController#getAutoReplyPage` 接收的是 `MpMessagePageReqVO`，而不是专门的自动回复分页 VO，字段语义存在混用。
5. **部分事件处理器未实现**：客服会话、菜单 VIEW、扫码、门店审核处理器会抛出 `UnsupportedOperationException`；若微信推送这些事件且被路由到对应 Handler，会导致回调处理异常，需要按业务补全。
6. **关注消息不落库**：`receiveMessage` 对 `subscribe` 事件直接返回，关注欢迎语由自动回复单独处理。
7. **多租户与多账号**：回调处理通过 `TenantUtils.execute(account.getTenantId(), ...)` 切换租户；`appId` 唯一性校验忽略租户，因为微信回调只能靠 appId 识别账号。`MpContextHolder` 使用 `TransmittableThreadLocal`，处理完必须 `clear()`。
8. **素材时效**：微信临时素材 media_id 仅保留 3 天，代码会把媒体下载并转存到本项目文件服务；永久素材删除会同步删除公众号侧素材。
9. **昵称/头像限制**：源码注释说明 2021-12-27 后公众号接口不再返回头像和昵称，`mp_user` 中这两个字段可能为空，需要网页授权等方式补充。
10. **回调路径在 admin 包下**：`/admin-api/mp/open/**` 虽带 admin 前缀，但通过安全白名单对微信服务器开放；不要误加登录校验。
