# 会员模块（yshop-module-member）

> 模块路径：`yshop-drink-boot3/yshop-module-member`；文档依据本仓库源码整理。

## 1. 模块定位

会员模块是 C 端用户账户中心，负责：

- 会员注册与多端登录：手机密码、手机验证码、微信小程序、微信公众号授权；
- 访问令牌的创建、刷新、登出（令牌本身由 system 模块的 OAuth2 服务签发）；
- 会员资料维护：昵称、头像、生日、性别、手机号；
- 收货地址维护（用户端 + 管理端）；
- 会员余额、积分字段的维护与账单流水记录（`yshop_user.now_money`、`yshop_user.integral`、`yshop_user_bill`）；
- 向其它模块暴露会员查询 API（`MemberUserApi`）。

需要区分两个概念：

- **会员积分余额**：存放在 `yshop_user.integral`，由本模块的 `MemberUserService` / `UserBillService` 维护；
- **积分商城**：由 `yshop-module-score` 实现，本模块只提供积分扣减能力。

## 2. 目录结构

```text
yshop-module-member/
├── pom.xml                              # 聚合 pom
├── yshop-module-member-api/
│   ├── pom.xml
│   └── src/main/java/co/yixiang/yshop/module/member/
│       ├── api/user/
│       │   ├── MemberUserApi.java       # 跨模块会员查询接口
│       │   └── dto/{MemberUserRespDTO,WechatUserDto}.java
│       └── enums/
│           ├── BillDetailEnum.java      # 账单种类/类型枚举
│           ├── BillEnum.java            # 收支方向/账单状态
│           ├── ErrorCodeConstants.java  # 错误码
│           └── LoginTypeEnum.java       # 登录来源
└── yshop-module-member-biz/
    ├── pom.xml
    └── src/main/java/co/yixiang/yshop/module/member/
        ├── api/user/MemberUserApiImpl.java
        ├── controller/
        │   ├── admin/
        │   │   ├── user/                # MemberUserController + vo
        │   │   ├── useraddress/         # UserAddressController + vo
        │   │   └── userbill/            # UserBillController + vo
        │   └── app/
        │       ├── auth/                # AppAuthController + vo
        │       ├── user/                # AppUserController + vo
        │       ├── address/             # AppUserAddressController + param/vo
        │       ├── social/              # AppSocialUserController + vo
        │       └── weixin/              # AppWxMpController
        ├── convert/                     # MapStruct：auth/social/user/useraddress/userbill
        ├── dal/
        │   ├── dataobject/
        │   │   ├── user/MemberUserDO.java
        │   │   ├── useraddress/UserAddressDO.java
        │   │   └── userbill/UserBillDO.java
        │   ├── mysql/                   # BaseMapperX 子接口
        │   └── redis/
        │       ├── RedisKeyConstants.java
        │       └── order/MiniRedisDAO.java   # 小程序 session_key 缓存
        ├── framework/web/config/MemberWebConfiguration.java
        └── service/
            ├── auth/MemberAuthService(+Impl)
            ├── user/{MemberUserService,UserService}(+Impl)
            ├── useraddress/{AppUserAddressService,UserAddressService}(+Impl)
            └── userbill/UserBillService(+Impl)
```

说明：

- 本模块没有 Mapper XML，持久层全部使用 MyBatis-Plus 的 `BaseMapperX` / `LambdaQueryWrapperX`。
- `MemberWebConfiguration` 只注册 Swagger 分组，不包含安全白名单配置。
- 模块测试位于 `yshop-module-member-biz/src/test/java/.../MemberAuthServiceTest.java`。

## 3. 核心功能

| 功能 | 入口类 | 说明 |
|---|---|---|
| 手机密码登录 | `AppAuthController#login` / `MemberAuthServiceImpl#login` | 校验手机号、BCrypt 密码、用户状态；可选绑定社交账号；签发 OAuth2 令牌并写登录日志 |
| 短信验证码登录 | `AppAuthController#smsLogin` | 校验短信验证码；按 `openid`/`routine_openid` 查用户；不存在则自动注册，再签发令牌 |
| 微信小程序登录 | `AppAuthController#weixinMiniAppLogin`、`#weixinMiniAppLogin2`、`#weixinMiniAppLogin3` | 分别对应手机号 code 登录、`wx.login` code 换 openid、encryptedData 解密手机号登录 |
| 微信公众号登录 | `AppAuthController#wechatAuth` | 使用网页授权 code 获取 openid，存在则登录，不存在则自动注册 |
| 令牌刷新/登出/改密 | `AppAuthController` | 刷新 refreshToken、删除 accessToken、修改密码 |
| 会员管理 | `MemberUserController` + `UserServiceImpl` | 后台创建、更新、删除、查询、分页；调整余额与积分 |
| 个人中心 | `AppUserController` + `MemberUserService` | 获取资料、修改昵称/生日/性别/头像、修改手机号、查看账单 |
| 收货地址 | `AppUserAddressController`、`UserAddressController` | 用户端增改、默认地址、删除、分页；管理端 CRUD |
| 余额/积分流水 | `UserBillService` | 写入收入/支出流水；用户端按余额/积分和类型查询 |
| 社交账号绑定 | `AppSocialUserController` | 调用 system 模块 `SocialUserApi` 绑定/解绑 |
| 微信 JS-SDK 签名 | `AppWxMpController` | 基于 `WxMpService` 生成公众号 JSAPI 签名 |
| 跨模块会员 API | `MemberUserApi` / `MemberUserApiImpl` | 按 id、ids、昵称、手机号查询会员 DTO |

## 4. 主要接口

> 框架根据 Controller 所在包自动添加前缀：`controller.admin.**` → `/admin-api`，`controller.app.**` → `/app-api`。下表路径不含该前缀。  
> `/app-api/**` 在安全过滤链中整体 `permitAll`，用户端登录校验主要依赖 `@PreAuthenticated`；未标注的 App 接口实际可匿名访问。

### 4.1 管理端 /admin-api

#### 会员用户 `MemberUserController`（`@RequestMapping("/member/user")`）

| 方法 | 路径 | 说明 | 权限注解 |
|---|---|---|---|
| POST | `/member/user/create` | 创建用户 | `@PreAuthorize("@ss.hasPermission('member:user:create')")` |
| PUT | `/member/user/update` | 更新用户 | `member:user:update` |
| PUT | `/member/user/updateMony` | 调整用户余额与积分 | `member:user:update` |
| DELETE | `/member/user/delete?id=` | 删除用户 | `member:user:delete` |
| GET | `/member/user/get?id=` | 获得用户 | `member:user:query` |
| GET | `/member/user/list?ids=` | 获得用户列表 | `member:user:query` |
| GET | `/member/user/page` | 获得用户分页 | `member:user:query` |

#### 用户地址 `UserAddressController`（`@RequestMapping("/member/user-address")`）

| 方法 | 路径 | 说明 | 权限注解 |
|---|---|---|---|
| POST | `/member/user-address/create` | 创建用户地址 | `member:user-address:create` |
| PUT | `/member/user-address/update` | 更新用户地址 | `member:user-address:update` |
| DELETE | `/member/user-address/delete?id=` | 删除用户地址 | `member:user-address:delete` |
| GET | `/member/user-address/get?id=` | 获得用户地址 | `member:user-address:query` |
| GET | `/member/user-address/list?ids=` | 获得地址列表 | `member:user-address:query` |
| GET | `/member/user-address/page` | 获得地址分页 | `member:user-address:query` |

#### 用户账单 `UserBillController`（`@RequestMapping("/member/user-bill")`）

| 方法 | 路径 | 说明 | 权限注解 |
|---|---|---|---|
| GET | `/member/user-bill/page` | 获得用户账单分页 | `member:user-bill:query` |

### 4.2 用户端 /app-api

#### 认证 `AppAuthController`（`@RequestMapping("/member/auth")`）

| 方法 | 路径 | 说明 | 登录注解 |
|---|---|---|---|
| POST | `/member/auth/login` | 手机 + 密码登录，可携带 social 参数绑定社交账号 | 无 |
| POST | `/member/auth/logout` | 登出，删除访问令牌 | `@PermitAll` |
| POST | `/member/auth/refresh-token?refreshToken=` | 刷新令牌 | 无 |
| POST | `/member/auth/sms-login` | 手机 + 验证码登录 | 无 |
| POST | `/member/auth/send-sms-code` | 发送手机验证码（场景 `MEMBER_LOGIN`） | 无 |
| POST | `/member/auth/update-password` | 修改密码 | `@PreAuthenticated` |
| POST | `/member/auth/weixin-mini-app-login` | 微信小程序手机号 code + 登录 code 一键登录 | 无 |
| POST | `/member/auth/auth-session` | 小程序 `wx.login` code 登录（返回 openId） | 无 |
| POST | `/member/auth/auth-miniapp-login` | 小程序 encryptedData + iv + openid 解密手机号登录 | 无 |
| GET | `/member/auth/auth-wechat-login?code=` | 微信公众号网页授权登录 | 无 |

源码中已注释、当前不生效的接口：`/reset-password`、`/social-auth-redirect`、`/social-login`。

#### 个人中心 `AppUserController`（`@RequestMapping("/member/user")`）

| 方法 | 路径 | 说明 | 登录注解 |
|---|---|---|---|
| POST | `/member/user/update-nickname` | 修改昵称、生日、性别、头像、手机号 | `@PreAuthenticated` |
| POST | `/member/user/update-avatar` | 上传并修改头像（`avatarFile`） | `@PreAuthenticated` |
| GET | `/member/user/get` | 获得基本信息 | `@PreAuthenticated` |
| POST | `/member/user/update-mobile` | 双短信验证码修改手机号 | `@PreAuthenticated` |
| GET | `/member/user/get-info` | 获得个人中心聚合信息 | `@PreAuthenticated` |
| GET | `/member/user/getBill` | 分页获取余额/积分账单（`cate`、`type`、`page`、`pagesize`） | `@PreAuthenticated` |

`getBill` 参数含义：`cate` 0=余额、1=积分；`type` 0=全部、1=消费、2=充值、3=退款。

#### 收货地址 `AppUserAddressController`（`@RequestMapping("/address")`）

| 方法 | 路径 | 说明 | 登录注解 |
|---|---|---|---|
| GET | `/address/city_list` | 中国行政区树（省级） | 无 |
| POST | `/address/addAndEdit` | 新增或修改地址 | `@PreAuthenticated` |
| POST | `/address/default/set/{id}` | 设置默认地址 | `@PreAuthenticated` |
| POST | `/address/del/{id}` | 删除地址 | `@PreAuthenticated` |
| GET | `/address/list` | 地址分页列表 | `@PreAuthenticated` |
| POST | `/address/getDistanceFromLocation` | 计算两点经纬度距离 | 无 |

#### 社交账号 `AppSocialUserController`（`@RequestMapping("/system/social-user")`）

| 方法 | 路径 | 说明 | 登录注解 |
|---|---|---|---|
| POST | `/system/social-user/bind` | 绑定社交账号 | 无（内部使用 `getLoginUserId()`，未登录时为 null） |
| DELETE | `/system/social-user/unbind` | 解绑社交账号 | 无（同上） |

#### 微信 `AppWxMpController`（`@RequestMapping("/member/wx-mp")`）

| 方法 | 路径 | 说明 | 登录注解 |
|---|---|---|---|
| GET | `/member/wx-mp/create-jsapi-signature?url=` | 创建公众号 JS-SDK 签名 | 无 |

## 5. 核心实现逻辑

### 5.1 登录与令牌签发

1. **手机密码登录**：`MemberAuthServiceImpl#login`
   - `login0()` 按手机号查询 `MemberUserDO`；
   - 使用 `PasswordEncoder.matches()` 校验 BCrypt 密码；
   - 校验 `status == CommonStatusEnum.ENABLE`；
   - 失败分别写入登录日志（`LoginResultEnum.BAD_CREDENTIALS` / `USER_DISABLED`）；
   - 若请求带 `socialType`，调用 `SocialUserApi.bindSocialUser()` 完成绑定；
   - `createTokenAfterLoginSuccess()`：写登录日志 → 调用 `OAuth2TokenApi.createAccessToken()` → 返回 `AppAuthLoginRespVO`。
2. **短信登录**：先 `SmsCodeApi.useSmsCode()` 校验验证码；若传了 `openid`，按 `from` 判断使用 `routine_openid` 还是 `openid` 查找用户；用户不存在则 `createUserIfAbsent()` 注册并回填 openid；已存在但手机号为空则补写手机号；最后签发令牌并补充 `userInfo`。
3. **小程序登录的三种入口**
   - `weixinMiniAppLogin`：`wx.getPhoneNumber` 的 `phoneCode` 换取手机号 → 按手机号创建/获取用户 → 绑定 `WECHAT_MINI_APP` 社交账号 → 签发令牌；
   - `weixinMiniAppLogin2`：`wx.login` 的 code 调 `getSessionInfo()`，把 `sessionKey` 以 openid 为 key 写入 Redis（`MiniRedisDAO`），按 `routine_openid` 查用户；查到则签发令牌，查不到也返回 openId；
   - `weixinMiniAppLogin3`：用 openid 从 Redis 取 sessionKey，解密 `encryptedData` 得到手机号；按手机号查/建用户并回填 `routine_openid`，再签发令牌。
4. **公众号登录**：`mpService.getOAuth2Service().getAccessToken(code)` 获取 openid，按 `openid` 查用户；不存在则创建并写入昵称、头像、username。
5. **统一令牌**：所有登录最终调用 `OAuth2TokenApi.createAccessToken()`，`clientId` 为默认客户端，`userType` 为 `UserTypeEnum.MEMBER`。登出调用 `removeAccessToken()`，刷新调用 `refreshAccessToken()`。

### 5.2 会员注册

`MemberUserServiceImpl#createUserIfAbsent(mobile, registerIp, from)`：

- 手机号非空且已存在：直接返回；
- 否则创建用户：随机 UUID 作为初始密码并 BCrypt 加密，`nickname = username = mobile`，`status = ENABLE`，`loginType = from`；
- 手机号为空时（公众号首次授权等）不做去重，直接新建用户。

### 5.3 余额与积分变动

本模块统一通过 `MemberUserService` 更新 `yshop_user`，通过 `UserBillService` 写 `yshop_user_bill`：

- **后台调整**：`UserServiceImpl#updateMony`
  - `ptype == 1`（`ShopCommonEnum.ADD_1`）增加余额，否则减少余额；
  - `itype == 1` 增加积分，否则减少积分；
  - 减少后小于 0 时强制置 0；
  - 同步写入收入/支出流水（余额种类 `now_money`，积分种类 `integral`）；
  - 最后更新用户余额和积分。
- **订单赠送积分**：`order` 模块在订单完成后调用 `MemberUserService` 增加积分并写 `TYPE_9`（奖励）流水；取消/退款时回退积分并写 `TYPE_8`（减去）流水。
- **积分兑换扣减**：`score` 模块调用 `MemberUserService#decScore()`（SQL 直接 `integral = integral - #{score}`）并写积分支出流水。
- **余额增减**：`incMoney`、`decPrice` 通过 Mapper 的 `@Update` 原子增减；订单支付/退款会调用。

### 5.4 账单查询

`UserBillServiceImpl#getBillList(uid, cate, type, page, limit)`：

1. `cate == 1` → `category = integral`，否则 `category = now_money`；
2. 固定过滤 `uid`、`status = 1`（有效），按 id 倒序；
3. `type` 映射：1 → `pay_product`（消费）、2 → `recharge`（充值）、3 → `pay_product_refund`（退款），其它为全部；
4. 使用 MyBatis-Plus `Page` 分页后转换为 `AppUserBillVO`。

管理端 `UserBillController#page` 使用 `UserBillPageReqVO`，可按 uid、linkId、收支方向、标题、种类、类型、时间、状态等条件分页。

### 5.5 收货地址

`AppUserAddressServiceImpl`：

- `addAndEdit`：若提交 `isDefault = 1`，先把该用户所有地址置为非默认；再按 `id` 是否为空决定新增或更新；
- `setDefault`：先把该用户全部地址置为非默认，再把目标地址置为默认；
- `getList`：按 `uid` 分页；
- 删除直接使用 MyBatis-Plus `removeById(id)`。

### 5.6 个人资料

- `updateUserNickname`：更新昵称、生日、性别、头像、手机号；
- `updateUserAvatar`：通过 `FileApi.createFile()` 上传文件，返回访问路径并写回用户；
- `updateUserMobile`：分别校验旧手机验证码和新手机验证码（场景 `MEMBER_UPDATE_MOBILE`），再更新手机号；
- `getAppUser`：返回个人中心聚合信息，并统计优惠券数量、累计消费金额。

## 6. 数据模型

### 6.1 `yshop_user`（`MemberUserDO`，继承 `TenantBaseDO`）

| 字段 | 含义 |
|---|---|
| `id` | 用户 ID |
| `username` / `password` | 账号、BCrypt 密码 |
| `nickname` / `avatar` | 昵称、头像 |
| `mobile` | 手机号 |
| `status` | 状态，`1` 正常、`0` 禁用 |
| `now_money` | 账户余额 |
| `integral` | 剩余积分 |
| `brokerage_price` | 佣金金额 |
| `login_type` | 登录来源（h5/wechat/routine 等） |
| `openid` | 公众号 openid |
| `routine_openid` | 小程序 openid |
| `register_ip` / `login_ip` / `login_date` | 注册、最后登录信息 |
| `gender` / `birthday` | 性别、生日 |
| `pay_count` / `spread_uid` / `spread_count` | 购买次数、推广关系 |
| `tenant_id` / `deleted` / `creator` / `updater` / `create_time` / `update_time` | 多租户与审计字段 |

`wx_profile` 为微信用户 JSON，使用 `FastjsonTypeHandler` 存储，源码已标注 `@Deprecated`。

### 6.2 `yshop_user_address`（`UserAddressDO`，继承 `BaseDO`）

关键字段：`id`、`uid`、`real_name`、`phone`、`province`、`city`、`city_id`、`district`、`detail`、`address`、`post_code`、`longitude`、`latitude`、`is_default`。

### 6.3 `yshop_user_bill`（`UserBillDO`，继承 `BaseDO`）

| 字段 | 含义 |
|---|---|
| `id` | 流水 ID |
| `uid` | 用户 ID |
| `link_id` | 关联业务 ID |
| `pm` | `0` 支出、`1` 获得（`BillEnum`） |
| `title` | 账单标题 |
| `category` | 明细种类：`now_money` 金额、`integral` 积分（`BillDetailEnum.CATEGORY_*`） |
| `type` | 明细类型：`recharge`、`brokerage`、`pay_product`、`extract`、`pay_product_refund`、`system_add`、`system_sub`、`deduction`、`gain`、`sign` |
| `number` / `balance` | 本次变动值、变动后余额 |
| `mark` | 备注 |
| `status` | `0` 待确定、`1` 有效、`-1` 无效 |
| `extend_field` | 扩展字段 |

## 7. 依赖与集成

### 7.1 本模块依赖

| 依赖 | 用途 |
|---|---|
| `yshop-module-system-api` | `OAuth2TokenApi`（令牌）、`LoginLogApi`（登录日志）、`SmsCodeApi`（短信验证码）、`SocialUserApi`（社交绑定） |
| `yshop-module-infra-api` | `FileApi`（头像上传） |
| `yshop-module-coupon-biz` | `AppCouponUserService`、`CouponUserDO`，用于个人中心优惠券统计（源码为 biz 直接依赖） |
| `yshop-module-shop-biz` | 在 `pom.xml` 中声明，当前源码未见直接引用 |
| `wx-java-mp-spring-boot-starter` | 公众号 `WxMpService` |
| `wx-java-miniapp-spring-boot-starter` | 小程序 `WxMaService` |
| `yshop-spring-boot-starter-redis` | `StringRedisTemplate` 缓存小程序 session_key |
| `yshop-spring-boot-starter-mybatis` / `security` / `web` / `excel` | 持久层、鉴权、Web、导出 |

### 7.2 被其它模块调用

| 调用方 | 调用内容 |
|---|---|
| `yshop-module-score-biz` | 直接依赖 `member-biz`：`MemberUserService`、`AppUserAddressService`、`UserBillService`、`BillDetailEnum` |
| `yshop-module-mall/yshop-module-order-biz` | 通过传递依赖使用 `MemberUserService`、`UserBillService`、会员 DO/地址 DO/账单枚举 |
| `yshop-module-message-biz` | `MemberUserService`、`MemberUserMapper`、`MemberUserDO`、`WechatUserDto` |
| `yshop-server` | 通过 `score-biz`、`order-biz` 等传递加载本模块 Controller，未直接声明 `member-biz` |

`MemberUserApi` 是名义上的跨模块 API（id/ids/昵称/手机号查询、`saveWechatMember`），但实际多个业务模块直接引用了 `member-biz` 的 Service 和 DO。

## 8. 注意事项

1. **错误码重复**：`USER_ADDRESS_PARAM_NOT_EXISTS` 与 `USER_BILL_NOT_EXISTS` 都使用 `1004004001`；`MINI_AUTH_LOGIN_BAD` 与 `MINI_AUTH_LOGIN_BAD2` 都使用 `1004004002`，排障时不能仅凭错误码区分。
2. **`updateMony` 同时修改余额和积分**：即使只调整其中一项，另一项也会走“增加/减少”分支；`ptype`、`itype` 为 `null` 时按“减少”处理，且不会因缺少参数报错。方法未加事务，余额、积分、流水可能不一致。
3. **积分扣减非原子校验**：`decScore` 直接执行 `integral - score`，未加 `integral >= score` 条件；并发或绕过前置校验时可能出现负积分。
4. **地址越权风险**：`/address/del/{id}`、`/address/default/set/{id}` 只按地址 ID 操作，未校验地址是否属于当前用户；`addAndEdit` 更新时也未校验归属。
5. **社交绑定接口未加 `@PreAuthenticated`**：`/system/social-user/bind`、`/unbind` 内部取 `getLoginUserId()`，未登录时得到 `null` 仍会调用下游绑定逻辑。
6. **App 接口默认匿名**：安全过滤链对 `/app-api/**` 整体放行，是否必须登录完全取决于 `@PreAuthenticated`，新增接口时需显式确认。
7. **管理端用户分页参数**：`UserPageReqVO` 同时存在 `mobile` 和 `phone`，Mapper 实际用 `reqVO.getPhone()` 匹配 `mobile` 字段，传 `mobile` 不生效。
8. **小程序 session_key 缓存无 TTL**：`MiniRedisDAO.set()` 未设置过期时间，openid 维度缓存会长期存在。
9. **微信 JSON 字段已废弃**：`MemberUserDO.wxProfile` 标注 `@Deprecated`，但仍参与 ORM 映射。
10. **多租户未启用**：`MemberUserDO` 继承 `TenantBaseDO`，当前 `yshop.tenant.enable: false`；`yshop_user` 表含 `tenant_id`。
