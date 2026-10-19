# yshop-module-system 系统模块

> 模块路径：`yshop-drink-boot3/yshop-module-system`；文档依据本仓库源码整理（`pom.xml`、`src/main/java`、`controller`、`service`、`dal/dataobject`、`application*.yaml`）。

## 1. 模块定位

`yshop-module-system` 是后端的**通用业务核心模块**，位于基础设施之上、具体交易业务之下，负责：

- 管理端用户、角色、菜单、权限、部门、岗位；
- 数据字典、地区、通知公告；
- 管理端认证（账号密码 / 短信 / 社交）与图形验证码；
- OAuth2 客户端、授权码、访问令牌、刷新令牌；
- 多租户（租户与租户套餐，当前默认关闭）；
- 短信渠道 / 模板 / 验证码 / 日志，邮件账号 / 模板 / 日志；
- 站内信模板与消息；
- 社交客户端与社交账号绑定；
- 登录日志、操作日志。

它同时通过 `-api` 向其他模块暴露跨模块能力，例如 `AdminUserApi`、`PermissionApi`、`OAuth2TokenApi`、`DeptApi`、`DictDataApi`、`SmsCodeApi`、`NotifyMessageSendApi`、`MailSendApi`。

## 2. 目录结构（api / biz 分层）

```
yshop-module-system/
├── pom.xml                          # 聚合模块（packaging=pom）
├── yshop-module-system-api/         # 对外 API、DTO、枚举、错误码
│   └── src/main/java/co/yixiang/yshop/module/system/
│       ├── api/
│       │   ├── dept/                # DeptApi、PostApi
│       │   ├── dict/                # DictDataApi
│       │   ├── logger/              # LoginLogApi、OperateLogApi
│       │   ├── mail/                # MailSendApi
│       │   ├── notify/              # NotifyMessageSendApi
│       │   ├── oauth2/              # OAuth2TokenApi
│       │   ├── permission/          # PermissionApi、RoleApi
│       │   ├── sms/                 # SmsCodeApi、SmsSendApi
│       │   ├── social/              # SocialClientApi、SocialUserApi
│       │   ├── tenant/              # TenantApi
│       │   └── user/                # AdminUserApi
│       └── enums/                   # ErrorCodeConstants、DictTypeConstants、LogRecordConstants
│                                    # SexEnum、登录/邮件/通知/OAuth2/权限/短信/社交枚举
└── yshop-module-system-biz/         # 业务实现
    └── src/main/java/co/yixiang/yshop/module/system/
        ├── api/                     # 各 *ApiImpl
        ├── controller/
        │   ├── admin/               # 管理端 Controller（映射到 /admin-api）
        │   └── app/                 # 用户端 Controller（映射到 /app-api）
        ├── convert/                 # MapStruct：AuthConvert、UserConvert、OAuth2OpenConvert 等
        ├── dal/
        │   ├── dataobject/          # DO（BaseDO 子类）
        │   ├── mysql/               # BaseMapperX 子类
        │   └── redis/               # RedisKeyConstants、OAuth2AccessTokenRedisDAO
        ├── framework/
        │   ├── captcha/             # Redis 验证码实现
        │   ├── datapermission/      # 数据权限配置
        │   ├── operatelog/          # bizlog 解析函数
        │   ├── sms/                 # 短信客户端工厂（阿里云/腾讯云/钉钉调试）
        │   └── web/                 # Swagger 分组
        ├── job/DemoJob.java         # 示例定时任务
        ├── mq/                      # 短信/邮件消息 producer、consumer、message
        └── service/                 # auth、dept、dict、logger、mail、member、notice、
                                     # notify、oauth2、permission、sms、social、tenant、user
```

依赖关系：

- `-api` 只依赖 `yshop-common` 与 `spring-boot-starter-validation`，保持轻量；
- `-biz` 依赖 `-api`、`infra-api`、`store-biz`，以及 security / mybatis / redis / job / mq / excel / tenant / data-permission / ip 等 starter。

## 3. 核心功能

| 功能 | 入口类 | 说明 |
|---|---|---|
| 管理端认证 | `AuthController`、`AdminAuthService(Impl)` | 账号密码登录、短信登录、社交登录、刷新令牌、登出、获取权限信息 |
| 图形验证码 | `CaptchaController`、`RedisCaptchaServiceImpl` | 基于 captcha-plus，Redis 存储，支持滑块拼图/文字点选 |
| 用户管理 | `UserController`、`AdminUserService(Impl)` | 增删改查、状态、重置密码、导入导出、岗位关联 |
| 个人中心 | `UserProfileController` | 查看/修改个人信息、修改密码、上传头像 |
| 角色管理 | `RoleController`、`RoleService(Impl)` | 角色 CRUD、数据范围、超级管理员识别 |
| 菜单管理 | `MenuController`、`MenuService(Impl)` | 目录/菜单/按钮树、权限标识、前端路由数据 |
| 权限分配 | `PermissionController`、`PermissionService(Impl)` | 角色-菜单、用户-角色、角色数据权限 |
| 部门 / 岗位 | `DeptController`、`PostController`、`DeptService`、`PostService` | 部门树、岗位管理、用户岗位关联 |
| 数据字典 | `DictTypeController`、`DictDataController`、`DictDataApi` | 字典类型与字典数据，管理端缓存、App 按类型查询 |
| OAuth2 授权 | `OAuth2OpenController`、`OAuth2ClientController`、`OAuth2TokenController`、`OAuth2UserController` | 客户端管理、令牌签发/校验/撤销、用户信息 scope 接口 |
| 租户 | `TenantController`、`TenantPackageController`、`TenantService` | 租户与套餐；创建租户时自动建角色和管理员 |
| 短信 | `SmsChannelController`、`SmsTemplateController`、`SmsLogController`、`SmsCallbackController` | 渠道/模板/验证码/发送日志/回执回调 |
| 邮件 | `MailAccountController`、`MailTemplateController`、`MailLogController` | 邮箱账号、模板、发送日志，异步发送 |
| 站内信 | `NotifyTemplateController`、`NotifyMessageController` | 模板、消息、已读状态、未读数 |
| 社交登录 | `SocialClientController`、`SocialUserController`、`AuthController` | 社交客户端、绑定/解绑、授权跳转与快捷登录 |
| 通知公告 | `NoticeController` | 公告 CRUD、WebSocket 推送 |
| 地区 / IP | `AreaController`、`AppAreaController` | 地区树、根据 IP 获取地区 |
| 日志 | `LoginLogController`、`OperateLogController` | 登录日志、操作日志（bizlog）分页与导出 |

## 4. 主要接口

> 下表中的路径是 Controller 上的 `@RequestMapping` 相对路径。实际访问时：
> - `controller.admin.*` 自动加前缀 `/admin-api`；
> - `controller.app.*` 自动加前缀 `/app-api`。

### 4.1 管理端 /admin-api

#### 4.1.1 认证、验证码与 OAuth2

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| POST | `/system/auth/login` | 账号密码登录 | 无需登录（`@PermitAll`） |
| POST | `/system/auth/logout` | 登出并删除令牌 | 无需登录（`@PermitAll`） |
| POST | `/system/auth/refresh-token` | 使用 refreshToken 刷新访问令牌 | 无需登录（`@PermitAll`） |
| GET | `/system/auth/get-permission-info` | 当前登录用户的用户、角色、菜单信息 | 登录即可 |
| POST | `/system/auth/sms-login` | 短信验证码登录 | 无需登录（`@PermitAll`） |
| POST | `/system/auth/send-sms-code` | 发送登录短信验证码 | 无需登录（`@PermitAll`） |
| GET | `/system/auth/social-auth-redirect` | 获取社交授权跳转地址 | 无需登录（`@PermitAll`） |
| POST | `/system/auth/social-login` | 使用 code 社交快捷登录 | 无需登录（`@PermitAll`） |
| POST | `/system/captcha/get` | 获得图形验证码 | 无需登录（`@PermitAll`） |
| POST | `/system/captcha/check` | 校验图形验证码 | 无需登录（`@PermitAll`） |
| POST | `/system/oauth2/token` | 获取访问令牌（password / authorization_code / implicit / refresh_token） | 无需登录（`@PermitAll`），需校验 client |
| DELETE | `/system/oauth2/token` | 撤销访问令牌 | 无需登录（`@PermitAll`），需校验 client |
| POST | `/system/oauth2/check-token` | 校验访问令牌 | 无需登录（`@PermitAll`） |
| GET | `/system/oauth2/authorize` | 获得授权信息 | 登录即可 |
| POST | `/system/oauth2/authorize` | 申请授权 | 登录即可 |
| POST | `/system/oauth2-client/create` | 创建 OAuth2 客户端 | `system:oauth2-client:create` |
| PUT | `/system/oauth2-client/update` | 更新 OAuth2 客户端 | `system:oauth2-client:update` |
| DELETE | `/system/oauth2-client/delete` | 删除 OAuth2 客户端 | `system:oauth2-client:delete` |
| GET | `/system/oauth2-client/get` | 获得 OAuth2 客户端 | `system:oauth2-client:query` |
| GET | `/system/oauth2-client/page` | 获得 OAuth2 客户端分页 | `system:oauth2-client:query` |
| GET | `/system/oauth2-token/page` | 获得有效访问令牌分页 | `system:oauth2-token:page` |
| DELETE | `/system/oauth2-token/delete` | 删除访问令牌 | `system:oauth2-token:delete` |
| GET | `/system/oauth2/user/get` | 获得 OAuth2 用户基本信息 | OAuth2 scope `user.read` |
| PUT | `/system/oauth2/user/update` | 更新 OAuth2 用户基本信息 | OAuth2 scope `user.write` |

#### 4.1.2 用户与个人中心

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| POST | `/system/user/create` | 新增用户 | `system:user:create` |
| PUT | `/system/user/update` | 修改用户 | `system:user:update` |
| DELETE | `/system/user/delete` | 删除用户 | `system:user:delete` |
| PUT | `/system/user/update-password` | 重置用户密码 | `system:user:update-password` |
| PUT | `/system/user/update-status` | 修改用户状态 | `system:user:update` |
| GET | `/system/user/page` | 用户分页列表 | `system:user:list` |
| GET | `/system/user/list-all-simple`、`/system/user/simple-list` | 用户精简列表 | 登录即可 |
| GET | `/system/user/get` | 用户详情 | `system:user:query` |
| GET | `/system/user/export` | 导出用户 Excel | `system:user:export` |
| GET | `/system/user/get-import-template` | 获得导入模板 | 登录即可 |
| POST | `/system/user/import` | 导入用户 | `system:user:import` |
| GET | `/system/user/profile/get` | 当前用户个人信息 | 登录即可 |
| PUT | `/system/user/profile/update` | 修改个人信息 | 登录即可 |
| PUT | `/system/user/profile/update-password` | 修改个人密码 | 登录即可 |
| POST/PUT | `/system/user/profile/update-avatar` | 上传个人头像 | 登录即可 |

#### 4.1.3 角色、菜单、权限、部门、岗位

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| POST | `/system/role/create` | 创建角色 | `system:role:create` |
| PUT | `/system/role/update` | 修改角色 | `system:role:update` |
| DELETE | `/system/role/delete` | 删除角色 | `system:role:delete` |
| GET | `/system/role/get` | 获得角色 | `system:role:query` |
| GET | `/system/role/page` | 角色分页 | `system:role:query` |
| GET | `/system/role/list-all-simple`、`/system/role/simple-list` | 角色精简列表 | 登录即可 |
| GET | `/system/role/export-excel` | 导出角色 Excel | `system:role:export` |
| POST | `/system/menu/create` | 创建菜单 | `system:menu:create` |
| PUT | `/system/menu/update` | 修改菜单 | `system:menu:update` |
| DELETE | `/system/menu/delete` | 删除菜单 | `system:menu:delete` |
| GET | `/system/menu/list` | 菜单列表（菜单管理界面） | `system:menu:query` |
| GET | `/system/menu/list-all-simple`、`/system/menu/simple-list` | 菜单精简列表（角色授权） | 登录即可 |
| GET | `/system/menu/get` | 菜单详情 | `system:menu:query` |
| GET | `/system/permission/list-role-menus` | 获得角色拥有的菜单编号 | `system:permission:assign-role-menu` |
| POST | `/system/permission/assign-role-menu` | 赋予角色菜单 | `system:permission:assign-role-menu` |
| POST | `/system/permission/assign-role-data-scope` | 赋予角色数据权限 | `system:permission:assign-role-data-scope` |
| GET | `/system/permission/list-user-roles` | 获得用户拥有的角色编号 | `system:permission:assign-user-role` |
| POST | `/system/permission/assign-user-role` | 赋予用户角色 | `system:permission:assign-user-role` |
| POST | `/system/dept/create` | 创建部门 | `system:dept:create` |
| PUT | `/system/dept/update` | 更新部门 | `system:dept:update` |
| DELETE | `/system/dept/delete` | 删除部门 | `system:dept:delete` |
| GET | `/system/dept/list` | 部门列表 | `system:dept:query` |
| GET | `/system/dept/list-all-simple`、`/system/dept/simple-list` | 部门精简树 | 登录即可 |
| GET | `/system/dept/get` | 部门详情 | `system:dept:query` |
| POST | `/system/post/create` | 创建岗位 | `system:post:create` |
| PUT | `/system/post/update` | 修改岗位 | `system:post:update` |
| DELETE | `/system/post/delete` | 删除岗位 | `system:post:delete` |
| GET | `/system/post/get` | 岗位详情 | `system:post:query` |
| GET | `/system/post/list-all-simple`、`/system/post/simple-list` | 岗位精简列表 | 登录即可 |
| GET | `/system/post/page` | 岗位分页 | `system:post:query` |
| GET | `/system/post/export` | 导出岗位 | `system:post:export` |

#### 4.1.4 字典、地区、通知公告

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| POST | `/system/dict-type/create` | 创建字典类型 | `system:dict:create` |
| PUT | `/system/dict-type/update` | 修改字典类型 | `system:dict:update` |
| DELETE | `/system/dict-type/delete` | 删除字典类型 | `system:dict:delete` |
| GET | `/system/dict-type/page` | 字典类型分页 | `system:dict:query` |
| GET | `/system/dict-type/get` | 字典类型详情 | `system:dict:query` |
| GET | `/system/dict-type/list-all-simple`、`/system/dict-type/simple-list` | 全部字典类型 | 登录即可 |
| GET | `/system/dict-type/export` | 导出字典类型 | `system:dict:query` |
| POST | `/system/dict-data/create` | 新增字典数据 | `system:dict:create` |
| PUT | `/system/dict-data/update` | 修改字典数据 | `system:dict:update` |
| DELETE | `/system/dict-data/delete` | 删除字典数据 | `system:dict:delete` |
| GET | `/system/dict-data/list-all-simple`、`/system/dict-data/simple-list` | 全部字典数据 | 登录即可 |
| GET | `/system/dict-data/page` | 字典数据分页 | `system:dict:query` |
| GET | `/system/dict-data/get` | 字典数据详情 | `system:dict:query` |
| GET | `/system/dict-data/export` | 导出字典数据 | `system:dict:export` |
| GET | `/system/area/tree` | 地区树 | 登录即可 |
| GET | `/system/area/get-by-ip` | 根据 IP 获得地区名 | 登录即可 |
| POST | `/system/notice/create` | 创建通知公告 | `system:notice:create` |
| PUT | `/system/notice/update` | 修改通知公告 | `system:notice:update` |
| DELETE | `/system/notice/delete` | 删除通知公告 | `system:notice:delete` |
| GET | `/system/notice/page` | 通知公告分页 | `system:notice:query` |
| GET | `/system/notice/get` | 通知公告详情 | `system:notice:query` |
| POST | `/system/notice/push` | 推送给 WebSocket 在线用户 | `system:notice:update` |

#### 4.1.5 租户与租户套餐

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/system/tenant/get-id-by-name` | 根据租户名获得租户编号 | 无需登录（`@PermitAll`） |
| GET | `/system/tenant/get-by-website` | 根据域名获得租户信息 | 无需登录（`@PermitAll`） |
| POST | `/system/tenant/create` | 创建租户（自动建角色、管理员） | `system:tenant:create` |
| PUT | `/system/tenant/update` | 更新租户 | `system:tenant:update` |
| DELETE | `/system/tenant/delete` | 删除租户 | `system:tenant:delete` |
| GET | `/system/tenant/get` | 获得租户 | `system:tenant:query` |
| GET | `/system/tenant/page` | 租户分页 | `system:tenant:query` |
| GET | `/system/tenant/export-excel` | 导出租户 | `system:tenant:export` |
| POST | `/system/tenant-package/create` | 创建租户套餐 | `system:tenant-package:create` |
| PUT | `/system/tenant-package/update` | 更新租户套餐 | `system:tenant-package:update` |
| DELETE | `/system/tenant-package/delete` | 删除租户套餐 | `system:tenant-package:delete` |
| GET | `/system/tenant-package/get` | 获得租户套餐 | `system:tenant-package:query` |
| GET | `/system/tenant-package/page` | 租户套餐分页 | `system:tenant-package:query` |
| GET | `/system/tenant-package/get-simple-list`、`/system/tenant-package/simple-list` | 租户套餐精简列表 | 登录即可 |

#### 4.1.6 短信、邮件、站内信、社交

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| POST | `/system/sms-channel/create` | 创建短信渠道 | `system:sms-channel:create` |
| PUT | `/system/sms-channel/update` | 更新短信渠道 | `system:sms-channel:update` |
| DELETE | `/system/sms-channel/delete` | 删除短信渠道 | `system:sms-channel:delete` |
| GET | `/system/sms-channel/get` | 获得短信渠道 | `system:sms-channel:query` |
| GET | `/system/sms-channel/page` | 短信渠道分页 | `system:sms-channel:query` |
| GET | `/system/sms-channel/list-all-simple`、`/system/sms-channel/simple-list` | 短信渠道精简列表 | 登录即可 |
| POST | `/system/sms-template/create` | 创建短信模板 | `system:sms-template:create` |
| PUT | `/system/sms-template/update` | 更新短信模板 | `system:sms-template:update` |
| DELETE | `/system/sms-template/delete` | 删除短信模板 | `system:sms-template:delete` |
| GET | `/system/sms-template/get` | 获得短信模板 | `system:sms-template:query` |
| GET | `/system/sms-template/page` | 短信模板分页 | `system:sms-template:query` |
| GET | `/system/sms-template/export-excel` | 导出短信模板 | `system:sms-template:export` |
| POST | `/system/sms-template/send-sms` | 发送短信 | `system:sms-template:send-sms` |
| GET | `/system/sms-log/page` | 短信日志分页 | `system:sms-log:query` |
| GET | `/system/sms-log/export-excel` | 导出短信日志 | `system:sms-log:export` |
| POST | `/system/sms/callback/aliyun` | 阿里云短信回执 | 无需登录（`@PermitAll`） |
| POST | `/system/sms/callback/tencent` | 腾讯云短信回执 | 无需登录（`@PermitAll`） |
| POST | `/system/mail-account/create` | 创建邮箱账号 | `system:mail-account:create` |
| PUT | `/system/mail-account/update` | 修改邮箱账号 | `system:mail-account:update` |
| DELETE | `/system/mail-account/delete` | 删除邮箱账号 | `system:mail-account:delete` |
| GET | `/system/mail-account/get` | 获得邮箱账号 | `system:mail-account:query` |
| GET | `/system/mail-account/page` | 邮箱账号分页 | `system:mail-account:query` |
| GET | `/system/mail-account/list-all-simple`、`/system/mail-account/simple-list` | 邮箱账号精简列表 | 登录即可 |
| POST | `/system/mail-template/create` | 创建邮件模板 | `system:mail-template:create` |
| PUT | `/system/mail-template/update` | 修改邮件模板 | `system:mail-template:update` |
| DELETE | `/system/mail-template/delete` | 删除邮件模板 | `system:mail-template:delete` |
| GET | `/system/mail-template/get` | 获得邮件模板 | `system:mail-template:query` |
| GET | `/system/mail-template/page` | 邮件模板分页 | `system:mail-template:query` |
| GET | `/system/mail-template/list-all-simple`、`/system/mail-template/simple-list` | 邮件模板精简列表 | 登录即可 |
| POST | `/system/mail-template/send-mail` | 发送邮件 | `system:mail-template:send-mail` |
| GET | `/system/mail-log/page` | 邮件日志分页 | `system:mail-log:query` |
| GET | `/system/mail-log/get` | 邮件日志详情 | `system:mail-log:query` |
| POST | `/system/notify-template/create` | 创建站内信模板 | `system:notify-template:create` |
| PUT | `/system/notify-template/update` | 更新站内信模板 | `system:notify-template:update` |
| DELETE | `/system/notify-template/delete` | 删除站内信模板 | `system:notify-template:delete` |
| GET | `/system/notify-template/get` | 获得站内信模板 | `system:notify-template:query` |
| GET | `/system/notify-template/page` | 站内信模板分页 | `system:notify-template:query` |
| POST | `/system/notify-template/send-notify` | 发送站内信 | `system:notify-template:send-notify` |
| GET | `/system/notify-message/get` | 获得站内信 | `system:notify-message:query` |
| GET | `/system/notify-message/page` | 站内信分页 | `system:notify-message:query` |
| GET | `/system/notify-message/my-page` | 我的站内信分页 | 登录即可 |
| PUT | `/system/notify-message/update-read` | 标记为已读 | 登录即可 |
| PUT | `/system/notify-message/update-all-read` | 全部标记为已读 | 登录即可 |
| GET | `/system/notify-message/get-unread-list` | 最新未读站内信（默认 10 条） | 登录即可 |
| GET | `/system/notify-message/get-unread-count` | 未读站内信数量 | 登录即可 |
| POST | `/system/social-client/create` | 创建社交客户端 | `system:social-client:create` |
| PUT | `/system/social-client/update` | 更新社交客户端 | `system:social-client:update` |
| DELETE | `/system/social-client/delete` | 删除社交客户端 | `system:social-client:delete` |
| GET | `/system/social-client/get` | 获得社交客户端 | `system:social-client:query` |
| GET | `/system/social-client/page` | 社交客户端分页 | `system:social-client:query` |
| POST | `/system/social-user/bind` | 绑定社交账号 | 登录即可 |
| DELETE | `/system/social-user/unbind` | 解绑社交账号 | 登录即可 |
| GET | `/system/social-user/get` | 获得社交用户 | `system:social-user:query` |
| GET | `/system/social-user/page` | 社交用户分页 | `system:social-user:query` |

#### 4.1.7 日志

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/system/login-log/page` | 登录日志分页 | `system:login-log:query` |
| GET | `/system/login-log/export` | 导出登录日志 | `system:login-log:export` |
| GET | `/system/operate-log/page` | 操作日志分页 | `system:operate-log:query` |
| GET | `/system/operate-log/export` | 导出操作日志 | `system:operate-log:export` |

### 4.2 用户端 /app-api

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/app-api/system/dict-data/type` | 按字典类型查询字典数据 | 公开（`/app-api/**` 默认放行） |
| GET | `/app-api/system/area/tree` | 获得地区树 | 公开 |
| GET | `/app-api/news/list` | 通知公告列表 | 公开 |
| GET | `/app-api/news/detail/{id}` | 通知公告详情 | 公开 |

> 说明：system 模块本身**没有会员登录接口**。用户端登录、会员资料、收货地址等位于 `yshop-module-member` 的 `controller.app.*` 下。

## 5. 核心实现逻辑

### 5.1 账号密码登录

入口：`AuthController#login` → `AdminAuthServiceImpl#login`。

1. **验证码校验**：读取 `yshop.captcha.enable`。开启时使用 `CaptchaService.verification` 校验 `captchaVerification`；失败会写一条验证码错误的登录日志。
2. **账号密码认证**：`AdminUserServiceImpl#getUserByUsername` 查询用户；`isPasswordMatch` 使用 `BCryptPasswordEncoder` 校验密码。
3. **状态校验**：用户被禁用则抛出 `AUTH_LOGIN_USER_DISABLED`。
4. **可选社交绑定**：请求携带 `socialType/socialCode/socialState` 时，调用 `SocialUserService#bindSocialUser`。
5. **创建令牌**：`createTokenAfterLoginSuccess`：
   - 写入登录日志（成功），并更新用户最后登录 IP / 时间；
   - `OAuth2TokenService#createAccessToken(userId, ADMIN, "default", null)`。
6. **令牌生成细节**（`OAuth2TokenServiceImpl`）：
   - 校验 `clientId=default` 的客户端存在且启用；
   - 生成 UUID 形式的 refreshToken 和 accessToken，写入 `system_oauth2_refresh_token`、`system_oauth2_access_token`；
   - 访问令牌有效期取客户端 `accessTokenValiditySeconds`，刷新令牌有效期取 `refreshTokenValiditySeconds`；
   - 访问令牌同时写入 Redis（key：`oauth2_access_token:{token}`，TTL 与过期时间一致）；
   - 令牌中记录 `userId`、`userType`、`clientId`、`scopes`、`shopId`、`userInfo`（昵称、部门编号）。
7. **返回**：`AuthLoginRespVO`（accessToken、refreshToken、expiresTime、userId 等）。

> 重要：这里的 accessToken 是**不透明随机字符串（UUID）**，不是 JWT。服务端通过 Redis + MySQL 校验，因此每次鉴权不是纯无状态计算，而是依赖 Redis/数据库。

### 5.2 令牌校验与登出

- **请求鉴权**：`TokenAuthenticationFilter` 从 `Authorization` 头（默认）或 `token` 参数取值；根据请求前缀推断用户类型（admin/member）；调用 `OAuth2TokenApi#checkAccessToken`。
- **令牌查询**：先查 Redis，未命中再查 MySQL；MySQL 命中且未过期时回填 Redis；过期抛 401。
- **构建上下文**：校验通过后构造 `LoginUser`（含 userId、userType、shopId、tenantId、scopes、userInfo）放入 SecurityContext。
- **Mock 登录**：当 `yshop.security.mock-enable=true` 且 token 以 `mockSecret`（默认 `test`）开头时，直接把剩余部分当作用户编号，便于本地调试；生产必须关闭。
- **登出**：`AdminAuthServiceImpl#logout` 删除访问令牌（MySQL + Redis）及对应刷新令牌，并记录登出日志。

### 5.3 刷新令牌

`OAuth2TokenServiceImpl#refreshAccessToken`：

1. 按 refreshToken 查询 `system_oauth2_refresh_token`；
2. 校验客户端编号一致；
3. 删除该 refreshToken 关联的旧 accessToken（数据库 + Redis）；
4. 若 refreshToken 已过期，删除并抛 401；
5. 基于原 refreshToken 生成新的 accessToken，返回给前端。

种子数据中 `default` 客户端的 accessToken 有效期为 18,000,000 秒（约 208 天），refreshToken 有效期为 43,200 秒（12 小时），刷新令牌反而明显短于访问令牌，实际使用时需注意这一反直觉配置。

### 5.4 权限校验（`@PreAuthorize("@ss.hasPermission(...)")`）

1. Controller 方法标注 `@PreAuthorize("@ss.hasPermission('system:user:create')")`；
2. `@ss` 对应 `SecurityFrameworkServiceImpl`，调用 `PermissionApi#hasAnyPermissions(loginUserId, permissions)`；
3. `PermissionServiceImpl#hasAnyPermissions`：
   - 无权限参数 → 直接通过；
   - 通过 `user_role_ids:{userId}` 缓存获得用户角色，过滤禁用角色；
   - 通过 `permission_menu_ids:{permission}` 缓存获得权限对应的菜单编号；
   - 通过 `menu_role_ids:{menuId}` 缓存获得拥有该菜单的角色编号；
   - 用户角色与菜单角色有交集 → 有权限；
   - 若角色包含 `super_admin`（`RoleCodeEnum.SUPER_ADMIN`）→ 直接通过。
4. **缓存失效**：角色-菜单分配、用户-角色分配、菜单增删改都会清理对应 Redis 缓存（`MENU_ROLE_ID_LIST`、`USER_ROLE_ID_LIST`、`PERMISSION_MENU_ID_LIST`）。

角色数据权限由 `yshop-spring-boot-starter-biz-data-permission` 处理，`getDeptDataPermission` 支持：全部、指定部门、本部门、本部门及子部门、仅本人五种范围。

### 5.5 登录后的权限信息与前端动态路由

`AuthController#getPermissionInfo`：

1. `userService.getUser(loginUserId)` 获得用户；
2. `permissionService.getUserRoleIdListByUserId` 获得角色编号；
3. `roleService.getRoleList` 获得启用角色；
4. `permissionService.getRoleMenuListByRoleId` 获得菜单编号（超级管理员返回全部菜单）；
5. `menuService.getMenuList(menuIds)` 获得启用菜单；
6. `AuthConvert` 组装 `AuthPermissionInfoRespVO`，供 Vue3 后台生成动态路由与按钮权限。

### 5.6 短信验证码登录

1. `POST /system/auth/send-sms-code` 校验手机号对应用户存在；
2. `SmsCodeServiceImpl#sendSmsCode`：
   - 按手机号取最近一条验证码，校验发送频率（默认 1 分钟）与当日上限（默认 10 条）；
   - 生成验证码：范围由 `yshop.sms-code.begin-code` / `end-code` 决定，local/dev 均配置为 `9999~9999`，即**固定 9999**；
   - 写入 `system_sms_code`；
   - 调用 `SmsSendService#sendSingleSms`，使用场景对应模板发送。
3. `SmsSendServiceImpl`：校验模板、渠道、手机号与模板参数；写入 `system_sms_log`；通过 `ApplicationContext.publishEvent` 发布 `SmsSendMessage`，`SmsSendConsumer` 使用 `@Async` 异步调用短信客户端（阿里云 / 腾讯云 / 钉钉调试）。
4. `POST /system/auth/sms-login`：`useSmsCode` 校验并标记已使用，查询用户后创建令牌。

短信回执由 `SmsCallbackController` 接收阿里云 / 腾讯云回调，解析后更新 `system_sms_log` 的接收状态。

### 5.7 OAuth2 开放接口

`OAuth2OpenController#postAccessToken` 支持：

| grant_type | 处理 |
|---|---|
| `password` | `OAuth2GrantServiceImpl#grantPassword` → `AdminAuthService#authenticate` → 创建令牌 |
| `authorization_code` | 消费授权码，校验 clientId、redirectUri、state 后创建令牌 |
| `refresh_token` | 调用刷新令牌逻辑 |
| `implicit` | 直接创建访问令牌 |
| `client_credentials` | 明确抛出 `UnsupportedOperationException`（当前不支持） |

客户端校验由 `OAuth2ClientServiceImpl#validOAuthClientFromCache` 完成（Redis 缓存 `oauth_client:{clientId}`），校验存在、启用、密钥、授权类型、scope、回调地址等。

### 5.8 租户创建

`TenantServiceImpl#createTenant`：

1. 校验租户名、域名唯一；
2. 校验套餐存在且启用；
3. 插入 `system_tenant`；
4. 在 `TenantUtils.execute(tenantId)` 上下文中：
   - 创建租户管理员角色（`RoleTypeEnum.TENANT`）；
   - 创建管理员用户并分配角色；
   - 回写 `contactUserId`。

当前 `yshop.tenant.enable=false`，多租户拦截器默认不生效，但相关表与代码保留，切换开关后可启用。

### 5.9 邮件与站内信

- **邮件**：`MailSendServiceImpl` 校验模板 / 账号 / 参数，写 `system_mail_log`，通过 Spring Event 发布 `MailSendMessage`，`MailSendConsumer` 异步使用 Hutool Mail 发送并回写结果。
- **站内信**：`NotifySendServiceImpl` 校验模板与参数，格式化内容后写入 `system_notify_message`；`NotifyMessageController` 提供已读、全部已读、未读列表与未读数。
- 两者都使用“模板编码 + 参数 Map”的方式，模板与参数定义分别存放在 `system_mail_template`、`system_notify_template`。

### 5.10 社交登录

1. `GET /system/auth/social-auth-redirect`：`SocialClientService#getAuthorizeUrl` 生成第三方授权地址；
2. 第三方回调后前端拿到 `code/state`；
3. `POST /system/auth/social-login`：`SocialUserService#getSocialUserByCode` 用 code 换取社交用户并找到绑定关系；
4. 未绑定则抛 `AUTH_THIRD_LOGIN_NOT_BIND`；已绑定则按 userId 创建令牌；
5. 登录后可通过 `/system/social-user/bind`、`/system/social-user/unbind` 管理绑定关系。

## 6. 数据模型

所有 DO 均继承 `BaseDO`，公共字段为：`create_time`、`update_time`、`creator`、`updater`、`deleted`（逻辑删除）。ID 策略为 MyBatis-Plus `NONE`（按数据库自动适配）。

| DO | 表名 | 说明 | 关键字段 |
|---|---|---|---|
| `AdminUserDO` | `system_users` | 管理端用户 | `id`、`username`、`password`、`nickname`、`dept_id`、`post_ids`、`mobile`、`email`、`sex`、`avatar`、`status`、`login_ip`、`login_date` |
| `DeptDO` | `system_dept` | 部门 | `id`、`name`、`parent_id`、`sort`、`leader_user_id`、`phone`、`email`、`status` |
| `PostDO` | `system_post` | 岗位 | `id`、`name`、`code`、`sort`、`status`、`remark` |
| `UserPostDO` | `system_user_post` | 用户-岗位关联 | `id`、`user_id`、`post_id` |
| `RoleDO` | `system_role` | 角色 | `id`、`name`、`code`、`sort`、`status`、`type`、`data_scope`、`data_scope_dept_ids` |
| `RoleMenuDO` | `system_role_menu` | 角色-菜单关联 | `id`、`role_id`、`menu_id` |
| `UserRoleDO` | `system_user_role` | 用户-角色关联 | `id`、`user_id`、`role_id` |
| `MenuDO` | `system_menu` | 菜单 / 目录 / 按钮 | `id`、`name`、`permission`、`type`、`sort`、`parent_id`、`path`、`icon`、`component`、`component_name`、`status`、`visible`、`keep_alive`、`always_show` |
| `DictTypeDO` | `system_dict_type` | 字典类型 | `id`、`name`、`type`、`status`、`remark`、`deleted_time` |
| `DictDataDO` | `system_dict_data` | 字典数据 | `id`、`sort`、`label`、`value`、`dict_type`、`status`、`color_type`、`css_class` |
| `OAuth2ClientDO` | `system_oauth2_client` | OAuth2 客户端 | `id`、`client_id`、`secret`、`name`、`status`、`access_token_validity_seconds`、`refresh_token_validity_seconds`、`redirect_uris`、`authorized_grant_types`、`scopes`、`auto_approve_scopes`、`authorities`、`resource_ids` |
| `OAuth2AccessTokenDO` | `system_oauth2_access_token` | 访问令牌 | `id`、`access_token`、`refresh_token`、`user_id`、`user_type`、`user_info`、`client_id`、`scopes`、`expires_time`、`shop_id` |
| `OAuth2RefreshTokenDO` | `system_oauth2_refresh_token` | 刷新令牌 | `id`、`refresh_token`、`user_id`、`user_type`、`client_id`、`scopes`、`expires_time`、`shop_id` |
| `OAuth2CodeDO` | `system_oauth2_code` | 授权码 | `id`、`code`、`user_id`、`user_type`、`client_id`、`scopes`、`redirect_uri`、`state`、`expires_time` |
| `OAuth2ApproveDO` | `system_oauth2_approve` | 授权批准记录 | `id`、`user_id`、`user_type`、`client_id`、`scope`、`approved`、`expires_time` |
| `TenantDO` | `system_tenant` | 租户 | `id`、`name`、`contact_user_id`、`contact_name`、`contact_mobile`、`status`、`domain`（字段 website）、`package_id`、`expire_time`、`account_count` |
| `TenantPackageDO` | `system_tenant_package` | 租户套餐 | `id`、`name`、`status`、`remark`、`menu_ids` |
| `SmsChannelDO` | `system_sms_channel` | 短信渠道 | `id`、`signature`、`code`、`status`、`api_key`、`api_secret`、`callback_url` |
| `SmsTemplateDO` | `system_sms_template` | 短信模板 | `id`、`type`、`status`、`code`、`name`、`content`、`params`、`api_template_id`、`channel_id`、`channel_code` |
| `SmsCodeDO` | `system_sms_code` | 短信验证码 | `id`、`mobile`、`code`、`scene`、`create_ip`、`today_index`、`used`、`used_time`、`used_ip` |
| `SmsLogDO` | `system_sms_log` | 短信日志 | `id`、`channel_id`、`channel_code`、`template_id`、`template_code`、`mobile`、`user_id`、`user_type`、`send_status`、`send_time`、`receive_status`、`api_*` |
| `MailAccountDO` | `system_mail_account` | 邮箱账号 | `id`、`mail`、`username`、`password`、`host`、`port`、`ssl_enable`、`starttls_enable` |
| `MailTemplateDO` | `system_mail_template` | 邮件模板 | `id`、`name`、`code`、`account_id`、`nickname`、`title`、`content`、`params`、`status` |
| `MailLogDO` | `system_mail_log` | 邮件日志 | `id`、`user_id`、`user_type`、`to_mail`、`account_id`、`from_mail`、`template_id`、`template_code`、`template_params`、`send_status`、`send_time`、`send_message_id`、`send_exception` |
| `NotifyTemplateDO` | `system_notify_template` | 站内信模板 | `id`、`name`、`code`、`type`、`nickname`、`content`、`params`、`status` |
| `NotifyMessageDO` | `system_notify_message` | 站内信消息 | `id`、`user_id`、`user_type`、`template_id`、`template_code`、`template_type`、`template_content`、`template_params`、`read_status`、`read_time` |
| `SocialClientDO` | `system_social_client` | 社交客户端 | `id`、`name`、`social_type`、`user_type`、`status`、`client_id`、`client_secret`、`agent_id` |
| `SocialUserDO` | `system_social_user` | 社交用户 | `id`、`type`、`openid`、`token`、`raw_token_info`、`nickname`、`avatar`、`raw_user_info`、`code`、`state` |
| `SocialUserBindDO` | `system_social_user_bind` | 社交账号绑定 | `id`、`user_id`、`user_type`、`social_user_id`、`social_type` |
| `LoginLogDO` | `system_login_log` | 登录日志 | `id`、`log_type`、`trace_id`、`user_id`、`user_type`、`username`、`result`、`user_ip`、`user_agent` |
| `OperateLogDO` | `system_operate_log` | 操作日志 | `id`、`trace_id`、`user_id`、`user_type`、`type`、`sub_type`、`biz_id`、`action`、`extra`、`request_method`、`request_url`、`user_ip`、`user_agent` |
| `NoticeDO` | `system_notice` | 通知公告 | `id`、`title`、`type`、`content`、`status` |

关系要点：

- 用户 ↔ 角色：`system_user_role`；
- 角色 ↔ 菜单：`system_role_menu`；
- 用户 ↔ 岗位：`system_user_post`；
- 用户通过 `dept_id` 关联部门；
- 菜单的 `permission` 字段是 `@ss.hasPermission` 的权限标识来源；
- 登录令牌表与 Redis 共同承担会话存储。

## 7. 依赖与集成

### 7.1 Maven 依赖

`yshop-module-system-api`：

- `yshop-common`
- `spring-boot-starter-validation`（optional）

`yshop-module-system-biz`：

- `yshop-module-system-api`
- `yshop-module-infra-api`（头像上传使用 `FileApi`）
- `yshop-module-store-biz`（令牌中计算 `shopId`）
- `yshop-spring-boot-starter-biz-data-permission`
- `yshop-spring-boot-starter-biz-tenant`
- `yshop-spring-boot-starter-biz-ip`
- `yshop-spring-boot-starter-security`
- `yshop-spring-boot-starter-mybatis`
- `yshop-spring-boot-starter-redis`
- `yshop-spring-boot-starter-job`
- `yshop-spring-boot-starter-mq`
- `yshop-spring-boot-starter-excel`
- `spring-boot-starter-mail`
- `spring-boot-starter-justauth`
- `wx-java-mp-spring-boot-starter`、`wx-java-miniapp-spring-boot-starter`
- `aliyun-java-sdk-core`、`aliyun-java-sdk-dysmsapi`、`tencentcloud-sdk-java-sms`
- `spring-boot-starter-captcha-plus`
- `hutool-extra`

### 7.2 对外暴露的跨模块 API

| API | 使用方示例 |
|---|---|
| `AdminUserApi` | infra 代码生成器获取作者昵称；其他模块查询用户 |
| `PermissionApi` / `RoleApi` | `@ss` 权限判断、角色查询 |
| `OAuth2TokenApi` | Security 的 `TokenAuthenticationFilter` |
| `DeptApi` / `PostApi` | 其他模块获取部门/岗位 |
| `DictDataApi` | 其他模块按字典类型取数据 |
| `SmsCodeApi` / `SmsSendApi` | 会员登录验证码、业务通知短信 |
| `MailSendApi` | 业务邮件 |
| `NotifyMessageSendApi` | 业务站内信 |
| `SocialUserApi` / `SocialClientApi` | 会员社交登录 |
| `TenantApi` | 租户校验 |

### 7.3 与 infra 的双向协作

- `system-biz` → `infra-api`：个人头像上传调用 `FileApi`；
- `infra-biz` → `system-api`：代码生成器通过 `AdminUserApi` 获取当前用户昵称作为作者；
- Maven 层不存在循环：`system-biz` 依赖 `infra-api`，`infra-biz` 依赖 `system-api`。

### 7.4 与 member 的解耦

`MemberServiceImpl` 不直接依赖 `member-api`，而是根据 `yshop.info.base-package` 反射加载 `co.yixiang.yshop.module.member.api.user.MemberUserApi`，用于在短信 / 邮件 / 登出日志中获取会员手机号、邮箱。这样 system 模块无需编译期依赖 member 模块。

## 8. 注意事项

1. **accessToken 不是 JWT**：实现为 UUID 随机串，校验依赖 Redis + `system_oauth2_access_token` 表；清理 Redis 或数据库会导致已登录用户掉线。
2. **默认 OAuth2 客户端凭据是公开的种子数据**：`client_id=default`、`client_secret=admin123`，上线前必须修改。
3. **默认客户端的 refreshToken 有效期短于 accessToken**（12h vs 约 208 天），刷新逻辑因此更依赖 accessToken 本身，配置时需评估。
4. **`/app-api/**` 在 Spring Security 层整体放行**，system 的 App 字典、地区、公告接口均为公开接口；新增 App 接口需要自行做登录校验。
5. **本地固定短信验证码**：`application.yaml` 中 `begin-code=9999`、`end-code=9999`，local/dev 下短信验证码实际固定为 `9999`，仅适合测试。
6. **图形验证码本地关闭**：`application-local.yaml` 中 `yshop.captcha.enable=false`，dev 默认开启。
7. **Mock 登录**：local 下 `yshop.security.mock-enable=true`，任何以 `test` 开头的 token 都会被当作 `test` 后数字对应的用户；生产必须为 `false`。
8. **`system_users` 表名是复数**，与其他 `system_*` 表命名不一致，修改表名时需同步 `AdminUserDO`。
9. **多租户默认关闭**：`yshop.tenant.enable=false`；租户表、套餐表、租户创建流程仍然存在，切换开关前需检查 `ignore-tables` 与菜单过滤逻辑。
10. **超级管理员绕过权限**：角色编码 `super_admin` 会跳过菜单权限判断；不要给普通角色分配该编码。
11. **权限缓存**：权限判断依赖 `user_role_ids`、`permission_menu_ids`、`menu_role_ids` 三类 Redis 缓存；直接改数据库授权关系而不清理缓存会短时间不生效。
12. **短信/邮件并非真正的外部 MQ**：`mq` 包中的 Producer 使用 `ApplicationContext.publishEvent` + `@Async` 消费，进程重启会丢失在途消息；跨节点场景需替换为 `yshop-spring-boot-starter-mq` 支持的实现。
13. **`OAuth2TokenServiceImpl#getShopId`** 使用 `FIND_IN_SET(admin_id)` 并拼接 userId；当前 userId 为 `Long`，不存在注入风险，但属于数据库方言绑定（MySQL），迁移数据库时需改写。
14. **`MemberServiceImpl` 使用反射**：若未引入 member 模块，运行时会因找不到 `MemberUserApi` 而异常；`yshop-server` 通过 `message-biz` / `score-biz` 间接引入了 member 模块。
15. **用户导入默认密码**：`sys.user.init-password` 默认为 `yshopyuanma`，导入用户后应立即要求修改。
16. **操作日志**：使用 bizlog `@LogRecord` 注解记录，与 infra 的 API 访问日志是两套体系。
