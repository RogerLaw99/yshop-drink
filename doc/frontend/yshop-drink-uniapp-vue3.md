# yshop-drink-uniapp-vue3 用户移动端

> 本文档基于 `yshop-drink-uniapp-vue3/` 目录下的真实源码做静态分析，重点覆盖入口、页面、API、状态管理、下单支付与登录授权流程。未运行构建，页面行为以源码实现为准；文中行号对应本次分析时的文件内容。

## 1. 项目定位

`yshop-drink-uniapp-vue3` 是 yshop-drink 多门店点餐系统的**用户移动端**，承担顾客侧的门店选择、点餐、购物车、下单支付、订单查询、优惠券、积分商城、地址与个人资料等功能。商家管理端是独立的 `yshop-drink-vue3` 工程，后端为 `yshop-drink-boot3`。

| 项目 | 值 |
|---|---|
| 包名 | `yshop-miniapp`（`package.json`） |
| 版本 | `1.2.0` |
| UniApp 应用标识 | `__UNI__ADC0FB0`（`manifest.json`） |
| 主要构建目标 | H5、微信小程序（`package.json` 仅提供这两类脚本） |
| 入口 | `main.js` → `App.vue` |
| 页面注册 | `pages.json`：4 个主包 tab 页 + 4 个子包，共 25 个页面 |
| 源码风格 | HBuilderX 布局，源码位于项目根目录，没有 `src/`；由 CLI 脚本重定向输入目录后构建 |

已实现的核心能力：门店定位与切换、自取/外卖点餐、规格选择与购物车、优惠券、下单与多种支付方式、订单列表/详情/收货/退款、积分商品兑换与积分订单、手机号验证码登录、微信小程序静默登录、H5 微信 OAuth、个人中心与地址管理。

**未实现或仅有后端能力、前端未接入的功能：**

| 功能 | 实际情况 |
|---|---|
| 扫码点餐/堂食桌号 | 后端订单参数支持 `deskId`、`deskNumber`、`deskPeople`，前端只提供“自取/外卖”，没有扫码入口或桌号参数 |
| H5 微信 JSAPI 支付 | `pay.vue` 中 `W-JSAPI` 分支为空，`getWechatConfig` 虽被导入但未调用 |
| APP 支付 | `APP` 分支为空；`package.json` 也没有 app-plus 平台依赖和构建脚本 |
| 余额充值 | `api/user.js` 有 `balanceGetMoneyList`、`balanceRecharge`，但页面未调用，后端 app 控制器也没有对应接口 |
| 独立购物车页 | `pages-checkout/cart/cart` 已注册，但源码内没有跳转到该页的入口；点餐页使用内嵌购物车弹层 |
| 国际化/分享 | 依赖声明了 `vue-i18n`，但源码未使用；未发现 `onShareAppMessage`/`onShareTimeline` |
| 退出登录入口 | `pages-user/login/logout` 已注册，静态页面代码中没有直接跳转；可能由后端“我的服务”菜单数据驱动进入 |

## 2. 技术栈

| 技术 | 版本 | 用途 |
|---|---|---|
| Vue | `^3.4.21` | 组合式 API、`<script setup>` |
| uni-app | `3.0.0-5020620260917001` | 多端框架，Vue 3 + Vite 编译器 |
| Vite | `5.2.8` | 构建工具 |
| Pinia | `^2.1.6` | 全局状态管理 |
| flyio | `^0.6.14` | HTTP 请求库，按平台引入不同构建 |
| weixin-js-sdk | `^1.6.3` | H5 微信 JS-SDK（当前仅声明，支付流程未真正使用） |
| sass / less | `^1.77.8` / `^4.2.0` | 样式预处理 |
| uv-ui（`uni_modules/uv-*`） | 1.x | 页面主要 UI 组件 |
| uni-ui（`uni-card`、`uni-icons`） | 1.3.x | 店铺列表卡片等 |
| vue-i18n | `^9.1.9` | 已声明但未使用 |
| rollup | `4.14.3` | 固定版本，供 Vite 使用 |

Node 版本要求见仓库根 `CLAUDE.md`：UniApp CLI 需要 Node `>=22.12.0`；移动端使用 npm（不是 pnpm）。

## 3. 目录结构

```text
yshop-drink-uniapp-vue3/
├─ api/                 # flyio 实例与按业务域拆分的接口
├─ components/          # 本地公共组件：layout/blank/list-cell/modal
├─ config/              # 后端地址、上传地址、公众号 AppID、订单状态常量
├─ hooks/               # usePage/useGlobalProperties（当前页面未使用）
├─ pages/               # 主包：index/menu/order/mine 四个 tab 页
├─ pages-checkout/      # 子包：pay/remark/packages/shop/cart
├─ pages-order/         # 子包：orders/detail/refund
├─ pages-score/         # 子包：积分商品与积分订单
├─ pages-user/          # 子包：登录、地址、优惠券、资料、账单
├─ scripts/             # uni.js（CLI 入口重定向）、patch-uni-h5-vue.js
├─ static/              # 图片、iconfont、全局样式 style/app.scss
├─ store/               # Pinia store.js
├─ uni_modules/         # uv-ui、uni-card、uni-icons、uni-scss
├─ utils/               # cookie/router/util/logger/querystring/index
├─ App.vue              # 应用生命周期、H5 微信 OAuth、小程序静默登录
├─ main.js              # createSSRApp、Pinia、全局工具插件
├─ pages.json           # 页面、分包、tabBar、globalStyle
├─ manifest.json        # 各端应用配置
├─ uni.scss             # 全局 SCSS 变量
├─ vite.config.js       # uni() 插件
├─ vue.config.js        # 仅保留注释，无有效配置
├─ index.html           # H5 模板
├─ jsconfig.json        # @/* 路径别名
└─ uni.promisify.adaptor.js  # uni API Promise 化返回约定
```

说明：

- 没有 `src/` 目录，所有源码在根目录；`scripts/uni.js` 通过 `UNI_INPUT_DIR` 让官方 CLI 从根目录读取。
- 路径别名 `@/*` 映射到项目根目录（`jsconfig.json`），例如 `@/api/order`、`@/utils/util`。
- `dist/` 下当前存在 `dist/build/mp-weixin` 构建产物；`.gitignore` 忽略 `/dist`。

## 4. 启动、构建与多端支持

### 4.1 npm 命令

| 命令 | 实际动作 | 输出/访问 |
|---|---|---|
| `npm install` | 安装依赖，随后自动执行 `postinstall` | 调用 `scripts/patch-uni-h5-vue.js` |
| `npm run dev:h5` | `node scripts/uni.js` | H5 开发服务器，默认 `http://localhost:5173/h5/`；产物在内存中，磁盘目录为 `dist/dev/h5` |
| `npm run build:h5` | `node scripts/uni.js build` | `dist/build/h5`，部署时注意 H5 基础路径为 `/h5/` |
| `npm run dev:mp-weixin` | `node scripts/uni.js -p mp-weixin` | `dist/dev/mp-weixin`，用微信开发者工具导入 |
| `npm run build:mp-weixin` | `node scripts/uni.js build -p mp-weixin` | `dist/build/mp-weixin`，用微信开发者工具导入 |
| `npm test` | `echo "Error: no test specified" && exit 1` | 项目没有自动化测试 |

### 4.2 scripts 的作用

| 文件 | 作用 |
|---|---|
| `scripts/uni.js` | 将 `process.env.UNI_INPUT_DIR` 设为项目根目录（`path.resolve(__dirname, '..')`），再 `require('@dcloudio/vite-plugin-uni/bin/uni.js')`，从而兼容 HBuilderX 风格的无 `src/` 工程；参数原样透传给官方 CLI。 |
| `scripts/patch-uni-h5-vue.js` | 修复 `@dcloudio/uni-h5-vue` 与 `@vue/shared@3.4.21+` 的运行时崩溃：把 `def(children, "_", type)` 改为 `def(children, "_", type, true)`，使只读属性 `_` 可写，避免 `Cannot assign to read only property '_'`。脚本会依次处理 `vue.runtime.esm.js` 和 `vue.runtime.cjs.js`，文件不存在或不含目标字符串时跳过。 |

`vite.config.js` 仅注册 `@dcloudio/vite-plugin-uni`；`vue.config.js` 全部内容被注释，没有实际代理配置。

### 4.3 多端支持现状

| 端 | 状态 | 说明 |
|---|---|---|
| H5 | 支持 | `manifest.json` 的 `h5.router.base` 为 `/h5/`；H5 专属代码包括微信 OAuth、微信内 401 特殊跳转、支付宝表单支付、微信 MWEB 跳转 |
| 微信小程序 | 支持 | `manifest.json` 中 `mp-weixin.appid = wx001e2dc50bf532df`，声明定位权限与 `getLocation`/`chooseLocation` 私有信息 |
| APP-PLUS | 代码有分支，工程未配套 | `api.js`、`App.vue`、`utils` 中存在 `#ifdef APP-PLUS`，但 `package.json` 没有 app-plus 平台包和脚本，APP 支付分支为空 |
| 其他小程序（支付宝/百度/头条等） | Manifest 有占位 | 没有对应 npm 脚本，API 层也没有平台专用构建 |

## 5. 页面与分包结构

### 5.1 主包与 tabBar

主包 4 个页面同时是 tabBar 页面，`globalStyle.navigationStyle` 为 `custom`，因此各页面自行使用 `uv-navbar` 绘制导航栏。

| 页面路径 | 标题 | tabBar | 职责 |
|---|---|---|---|
| `pages/index/index` | 首页 | 首页 | 广告轮播、自取/外卖入口、卡券与积分商城入口 |
| `pages/menu/menu` | 点餐 | 点餐 | 定位/选店、分类商品、规格弹窗、购物车、去结算 |
| `pages/order/order` | 订单 | 订单 | 订单列表、状态筛选、确认收餐、跳转详情 |
| `pages/mine/mine` | 我的 | 我的 | 用户信息、优惠券/积分/余额/历史消费、服务菜单 |

tabBar 配置：

| 项 | 值 |
|---|---|
| 默认颜色 | `#919293` |
| 选中颜色 | `#09b4f1` |
| 背景色 | `#fff` |
| 图标目录 | `static/images/tabBar/` |

### 5.2 子包页面

#### `pages-checkout`（结算相关，5 页）

| 路径 | 标题 | 职责 |
|---|---|---|
| `pay/pay` | 支付 | 确认订单、自取/外卖切换、地址、取餐/送达时间、优惠券、备注、支付方式、创建订单并发起支付 |
| `remark/remark` | 备注 | 50 字限制、快捷备注、回填到支付页 |
| `packages/index` | 可用优惠券 | 结算时选择/取消优惠券，校验门槛金额 |
| `shop/shop` | 店铺 | 搜索门店、导航/致电、选择门店并回到点餐页 |
| `cart/cart` | 购物车 | 独立购物车页（已注册但无入口），增减/清空/去结算 |

#### `pages-order`（订单相关，3 页）

| 路径 | 标题 | 职责 |
|---|---|---|
| `orders/orders` | 历史订单 | 与主包订单页近乎重复的订单列表 |
| `orders/detail` | 订单详情 | 订单进度、商品、金额、门店信息、收货/退款入口 |
| `orders/refund` | 退款 | 整单退款申请，原因固定“协商一致” |

#### `pages-user`（用户相关，8 页）

| 路径 | 标题 | 职责 |
|---|---|---|
| `login/login` | 登录 | 手机号+验证码登录、微信小程序手机号快捷登录、协议勾选 |
| `login/logout` | 退出登录 | 清除本地用户信息与 token，回到登录页 |
| `address/address` | 我的地址 | 地址列表、滑动删除、选择地址（菜单/支付/积分场景） |
| `address/add` | 添加/编辑地址 | 收货人、电话、地图选点、详细地址、默认地址 |
| `coupons/coupons` | 我的卡券 | 我的优惠券/未领优惠券、兑换码、领取 |
| `mine/userinfo` | 用户信息 | 头像、昵称、性别、生日、加入时间、保存 |
| `mine/content` | 内容 | 富文本展示后端服务内容（用户协议/隐私政策等） |
| `balance/bill` | 账单 | 余额/积分账单，按类型分页加载 |

#### `pages-score`（积分商城，5 页）

| 路径 | 标题 | 职责 |
|---|---|---|
| `scoreproduct/list` | 积分商城 | 双列瀑布流积分商品 |
| `scoreproduct/detail` | 产品详情 | 轮播图、积分/销量/库存、富文本详情、立即兑换 |
| `scoreproduct/confirm` | 确认 | 选择地址、确认兑换 |
| `scoreproduct/order` | 订单列表 | 积分订单状态筛选、确认收货 |
| `scoreproduct/orderDetail` | 订单详情 | 积分订单信息、物流公司/单号、物流轨迹、确认收货 |

### 5.3 注册与可达性说明

- `pages-user/mine/service` 被 `pages/mine/mine.vue:176` 引用，但 `pages.json` 没有注册，文件也不存在。后端服务菜单若返回 `type: 'menu'`，该跳转会失败。
- `pages-checkout/cart/cart` 已注册但源码中没有 `navigateTo` 指向它。
- `pages-user/login/logout` 仅页面自身可达性依赖外部入口；`store.logout()` 也没有被页面调用。
- 主包 `pages/order/order` 和子包 `pages-order/orders/orders` 功能高度重复，后者不是主流程入口。

## 6. API 层与请求封装

### 6.1 flyio 实例与平台条件编译

`api/api.js` 是唯一的 HTTP 基础封装：

| 平台 | 引入的 flyio 构建 |
|---|---|
| H5 | `flyio/dist/npm/fly` |
| APP-PLUS | `flyio/dist/npm/wx` |
| MP-WEIXIN | `flyio/dist/npm/wx` |

`baseURL` 来自 `config/index.js` 的 `VUE_APP_API_URL`，即 `http://localhost:48081/app-api`（开发）或 `https://apidc.yixiang.co/app-api`（生产）。

### 6.2 请求处理流程

1. 业务 API 调用 `api.get/post/put/patch/delete/head`。
2. `baseRequest` 从 `cookie.get('accessToken')` 读取 token（实际是 `uni.getStorageSync`）。
3. 默认 `login: true`；只有 `login !== false` 且 token 存在时才附加 `Authorization: Bearer <token>`，避免空 Bearer。
4. `post/put/patch` 使用 `data`，`get/delete/head` 使用 `params`，最终统一交给 `fly.request(url, params || data, option)`。
5. 响应拦截器原样返回 response；错误拦截器处理 HTTP 401。
6. 成功回调中先取 `res.data`（后端 `CommonResult`）。
7. H5 专属：业务码 `1004004002` 且当前在微信内时，读取 `cookie.get('index_url')` 并 `location.href` 跳回；这是微信 OAuth/会话失效的兜底。
8. `res.status !== 200` → reject `{ msg: '请求失败', res, data }`。
9. 业务 `data.code == 401` → `uni.hideLoading()`、`handleLoginFailure()`（`uni.redirectTo('/pages-user/login/login')`）、toast 并 reject。
10. 业务 `data.code != 0` → toast `data.msg` 并 reject。
11. `data.code == 0` → resolve `data.data`，因此页面拿到的直接是业务数据，不是 `CommonResult`。

错误处理汇总：

| 场景 | 行为 |
|---|---|
| HTTP 401 | `handleLoginFailure()`，reject `{ msg: '未登录', toLogin: true }` |
| 业务码 401 | 关闭 loading、跳登录页、toast 后端 msg |
| 业务码 5109 | toast 后端 msg，然后按普通错误 reject |
| 业务码 1004004002（仅 H5） | 微信内跳回 `index_url` |
| 其他非 0 业务码 | toast 后端 msg 并 reject |

### 6.3 token 与本地存储

| 键 | 写入位置 | 读取位置 | 说明 |
|---|---|---|---|
| `accessToken` | `store.SET_TOKEN`、登录页、`cookie.set` | `api.js`、`store.isLogin/init`、App.vue | Bearer token |
| `userinfo` | `SET_MEMBER`、登录页 | `store.init` | 用户对象 |
| `cart` | 点餐页/购物车页 `uni.setStorageSync` | 点餐页、支付页、购物车页 | 购物车数组 |
| `paytype` | 支付页 | 支付页 | 上次支付方式 |
| `score_product` | 积分详情页 `cookie.set` | 积分确认页 | 待兑换商品 |
| `shopAd`、`isActive` | 首页 | 点餐页 | 店铺广告图/活动标记 |
| `wx_auth`、`redirect`、`index_url` | H5 OAuth | H5 OAuth/API 跳转 | 微信授权状态与回跳地址 |

### 6.4 业务域 API 清单

#### `api/auth.js`（登录授权，均 `login: false`）

| 方法 | 请求 | 地址 | 说明 |
|---|---|---|---|
| `userLogin` | POST | `/member/auth/sms-login` | 手机号 + 验证码登录 |
| `smsSend` | POST | `/member/auth/send-sms-code` | 发送短信验证码 |
| `userLoginForWechatMini` | POST | `/member/auth/auth-miniapp-login` | 微信小程序手机号快捷登录 |
| `userAuthSession` | POST | `/member/auth/auth-session` | 小程序 code 换 openId/会话 |
| `wechatAuth` | GET | `/member/auth/auth-wechat-login` | H5 微信 OAuth code 登录 |

#### `api/goods.js`

| 方法 | 请求 | 地址 | 说明 |
|---|---|---|---|
| `shopNearby` | GET | `/store/nearby` | 获取最近门店（lat/lng/shop_id/kw） |
| `menuGoods` | GET | `/product/products` | 门店商品分类与商品列表（shopId） |

#### `api/market.js`

| 方法 | 请求 | 地址 | 说明 |
|---|---|---|---|
| `shopGetList` | GET | `/store/list` | 门店列表/搜索 |
| `menuAds` | GET | `/ad/list` | 首页广告 |

#### `api/order.js`

| 方法 | 请求 | 地址 | 说明 |
|---|---|---|---|
| `orderTakeFoods` | GET | `/order/list` | 与 `orderGetOrders` 重复，页面未使用 |
| `orderSubmit` | POST | `/order/create` | 创建订单 |
| `orderGetOrders` | GET | `/order/list` | 订单分页列表（page/limit/type） |
| `orderDetail` | GET | `/order/detail/{id}` | 订单详情，参数可为 uni 或订单号 |
| `orderReceive` | POST | `/order/take` | 确认收餐（`{ uni }`） |
| `orderRefund` | POST | `/order/refund` | 整单退款申请 |
| `payUnify` | POST | `/order/pay` | 统一支付（uni/from/paytype） |
| `getWechatConfig` | GET | `/member/wx-mp/create-jsapi-signature` | 微信 JS-SDK 签名；已导入但未使用 |

#### `api/address.js`

| 方法 | 请求 | 地址 | 说明 |
|---|---|---|---|
| `addressDelete` | POST | `/address/del/{id}` | 删除地址 |
| `shopGetDistanceFromLocation` | POST | `/address/getDistanceFromLocation` | 计算地址到门店距离 |
| `getAddressAddAndEdit` | POST | `/address/addAndEdit` | 新增/编辑地址 |
| `addressAll` | GET | `/address/list` | 地址列表 |

#### `api/coupon.js`

| 方法 | 请求 | 地址 | 说明 |
|---|---|---|---|
| `couponReceive` | POST | `/coupon/receive` | 领取优惠券或兑换码兑换 |
| `couponMine` | GET | `/coupon/my` | 我的优惠券 |
| `couponIndexApi` | GET | `/coupon/not` | 未领取优惠券 |
| `couponCount` | GET | `/coupon/count` | 可用优惠券数量 |

#### `api/user.js`

| 方法 | 请求 | 地址 | 说明 |
|---|---|---|---|
| `userGetUserInfo` | GET | `/member/user/get-info` | 当前用户信息 |
| `mineService` | GET | `/service/list` | 个人中心服务菜单 |
| `mineServiceContent` | GET | `/service/content` | 服务内容富文本 |
| `userEdit` | POST | `/member/user/update-nickname` | 修改昵称/性别/生日/头像等 |
| `balanceGetBillList` | GET | `/member/user/getBill` | 余额/积分账单 |
| `balanceGetMoneyList` | GET | `/recharge/getMoneyList` | 充值面额列表；页面未调用 |
| `balanceRecharge` | POST | `/member/user/recharge` | 余额充值；页面未调用 |

#### `api/score.js`

| 方法 | 请求 | 地址 | 说明 |
|---|---|---|---|
| `scoreShopDetail` | GET | `/score-product/detail` | 积分商品详情 |
| `scoreShopIndex` | GET | `/score-product/list` | 积分商品列表 |
| `scoreShopExchange` | POST | `/score-order/submit` | 提交积分兑换订单 |
| `scoreShopOrder` | GET | `/score-order/list` | 积分订单列表 |
| `scoreShopOrderDetail` | GET | `/score-order/detail` | 积分订单详情 |
| `scoreShopReceive` | GET | `/score-order/take` | 积分订单确认收货 |
| `getLogistic` | GET | `/express/getLogistic` | 物流轨迹 |

### 6.5 接口层注意事项

- `/order/list` 被两个函数重复封装；页面统一使用 `orderGetOrders`，`orderTakeFoods` 未使用。
- `/recharge/getMoneyList` 与 `/member/user/recharge` 在当前后端 `controller/app` 下没有对应的 `@RequestMapping` 实现（仅存在 `AppUserRechargeVO`），这两个前端方法也没有调用点。
- `getWechatConfig` 在 `pay.vue` 中导入但未调用，且在 API 层直接读取 `location.href`；若在非 H5 端调用会报错。
- H5 支付 API 的 `from` 取值为 `h5` 或 `wechat`；小程序为 `routine`。

## 7. 状态管理

`main.js` 使用 `createPinia()` 注册 Pinia。全项目只有一个 store：`store/store.js` 中的 `useMainStore`（id 为 `main`）。

### 7.1 state

| 字段 | 默认值 | 用途 |
|---|---|---|
| `store` | `{}` | 当前门店对象 |
| `cart` | `[]` | 购物车（页面实际还使用了 `storage.cart`） |
| `orderType` | `'takein'` | `takein`=自取，`takeout`=外卖 |
| `address` | `{}` | 当前收货地址 |
| `addresses` | `{}` | 地址列表缓存 |
| `member` | `{}` | 用户信息 |
| `openid` | `''` | 微信 openId |
| `token` | `''` | accessToken |
| `lang` | `'zh-cn'` | 上传接口语言头 |
| `cookieKey` | `'YSESSID=yshop-e4dk4o2utr3c0n95tp42p745ai'` | 硬编码值，未被使用 |
| `location` | `{}` | 当前经纬度 |
| `mycoupon` | `{}` | 结算时选中的优惠券 |

### 7.2 getters / actions

| 名称 | 类型 | 行为 |
|---|---|---|
| `isLogin` | getter | `!!(state.token || cookie.get('accessToken'))` |
| `SET_ORDER_TYPE` | action | 设置自取/外卖 |
| `SET_MEMBER` | action | 设置用户并写入 `userinfo` 存储 |
| `SET_ADDRESS` / `SET_ADDRESSES` | action | 设置当前地址/地址列表 |
| `SET_STORE` | action | 设置当前门店 |
| `SET_CART` / `REMOVE_CART` | action | 设置/清空购物车 |
| `SET_LOCATION` | action | 设置定位 |
| `SET_OPENID` / `SET_TOKEN` | action | 设置 openId/token；token 同时写入存储 |
| `SET_COUPON` / `DEL_COUPON` | action | 设置/清除结算优惠券 |
| `setSelectAddress(id)` | action | 从 `addresses` 中按 id 选地址 |
| `init()` | action | 冷启动从存储恢复 `accessToken`、`userinfo` |
| `logout()` | action | 清空用户/购物车/token，移除存储并 `navigateTo` 登录页 |
| `setCookie(provider)` | action | 该 action 写法有误：使用了不存在的 `state.cookie`，且把 provider 写入 `cookieKey` 存储；当前没有调用点 |

`utils/index.js` 以插件形式注入全局属性：`$yrouter`、`$cookie`、`$toAuth`、`$onClickLeft`、`$platform`。`$onClickLeft` 统一调用 `router.back()`；无历史页面时 `router.back()` 会 `switchTab` 到首页。

## 8. 核心业务流程

### 8.1 定位、门店选择与门店信息

**点餐页初始化（`pages/menu/menu.vue`）**

1. `onLoad` 调用 `init()` 并 `refreshCart()`。
2. `init()` 检查 `location` 是否已有经纬度：
   - 没有则 `uni.getLocation({ type: 'wgs84' })`。
   - 成功 → `getShopList({ latitude, longitude })`。
   - 失败 → toast“获取位置失败，请检查是否开启相关权限”，并回退到北京坐标 `39.919990, 116.456270`。
   - 注意：失败回调和之后的兜底判断都可能触发 `getShopList`，存在重复请求风险。
3. `getShopList` 保存 `location`，调用 `shopNearby({ lat, lng, shop_id, kw: '' })`。
4. 根据 `shop.status` 生成公告：未营业时显示“店铺营业时间为 xx:xx - xx:xx，不在营业时间内无法下单”。
5. `SET_STORE(shop)`，再调用 `menuGoods({ shopId })` 获取分类商品，结束后关闭 loading。
6. 门店有 `notice` 时页面顶部显示 `uv-notice-bar`。

**门店列表页（`pages-checkout/shop/shop.vue`）**

1. `onLoad` 调用 `getShop()`，请求 `/store/list`，参数 `lat/lng/kw/shop_id: 0`。
2. 列表展示距离、配送距离/配送费、地址、营业时间，并提供“导航”“致电”。
3. 选择门店：`SET_STORE(shop)` → `uni.$emit('refreshMenu')` → `switchTab('/pages/menu/menu')`。
4. 点餐页监听 `refreshMenu` 并重新 `init()`；`onUnload` 时 `uni.$off`，避免重复监听。

**地址选择（`pages-user/address/address.vue`）**

1. `onLoad` 读取 `is_choose` 和 `scene`（`menu`/`pay`/`scoreShop`）。
2. 非选择模式只展示和编辑，不触发选择逻辑。
3. `menu`/`pay` 场景调用 `/address/getDistanceFromLocation`，比较返回距离与门店 `distance`：
   - 超过门店配送范围 → toast“不在配送范围！”。
   - 否则 `SET_ADDRESS`、`SET_ORDER_TYPE('takeout')`，并把实际距离写入 `store.far`、`store.far_text`，`SET_LOCATION` 为地址经纬度。
4. `menu` 场景 `switchTab` 回点餐页；`pay` 场景 `navigateBack`；`scoreShop` 场景只设置地址并返回。

### 8.2 点餐与购物车

1. 点餐页左侧为分类，右侧为商品；`calcSize()` 用 `uni.createSelectorQuery()` 计算每个分类的 `top/bottom`，用于滚动联动。
2. 点击分类 → 设置 `currentCateId` 并滚动到对应位置；商品列表滚动用 `throttle(..., 100)` 计算当前分类。
3. 点击商品/“选规格” → 打开详情弹窗，默认选中每个规格的第一项。
4. 规格选择：
   - `newValue` 保存当前规格值数组，`valueStr = newValue.join(',')`。
   - 在 `good.productValue[valueStr]` 查找 SKU；未命中时把规格值排序后再查（兼容顺序不同）。
   - 更新 `price`、`stock`、`image`、`valueStr`。
5. 加入购物车：
   - 以 `id + valueStr` 匹配已有条目，存在则累加数量，否则 push `{ id, cate_id, name, price, number, image, valueStr }`。
   - 每次变更都写回 `uni.setStorageSync('cart', ...)`。
6. 购物车栏显示总数量、总价；外卖时校验起送价，未达起送价按钮显示“差 X 元起送”。
7. 购物车弹层支持增减、清空；`refreshCart()` 会用最新商品列表过滤掉已下架商品。
8. 去结算前校验：
   - 未登录 → 跳登录页。
   - `store.status == 0` → 不在营业时间。
   - 外卖且 `store.distance <= 0` → 本店不支持外卖。
   - 外卖且 `store.distance < store.far` → 地址不在配送范围。
9. 校验通过后保存 `cart` 并 `navigateTo('/pages-checkout/pay/pay')`。

**独立购物车页 `pages-checkout/cart/cart.vue`** 读取同一份 `storage.cart`，提供数量增减、清空和去结算；它复用与点餐页相同的登录/营业时间/配送范围校验，但源码内没有进入该页的入口。

### 8.3 下单与支付（`pages-checkout/pay/pay.vue`）

**进入页面**

1. `onLoad` 读取 `storage.cart`，并接收 `remark` 查询参数。
2. `onShow`：
   - 同步 `main.mycoupon`。
   - 默认送达时间设为当前时间 + 1 小时。
   - 根据 `orderType` 初始化“自取/外卖”开关。
   - 请求 `/coupon/count` 获取可用券数量（自取 type=1，外卖 type=2）。
   - 从 `storage.paytype` 恢复支付方式，默认 `weixin`。

**页面计算**

| 计算项 | 规则 |
|---|---|
| `total` | 购物车各项 `number * price` 之和 |
| `amount` | `total` + 外卖配送费（`store.distance > 0` 时加 `store.deliveryPrice`）- 优惠券面额，最小为 0 |
| 优惠券过滤 | `packages/index` 中用 `coupon.least` 与订单金额比较 |

**可编辑项**

| 项 | 行为 |
|---|---|
| 自取/外卖开关 | 调用 `SET_ORDER_TYPE`；若已选券与新订单类型不匹配则清空券并重新请求可用券数量 |
| 收货地址 | 外卖时跳 `/pages-user/address/address?is_choose=true&scene=pay` |
| 门店 | 跳店铺列表页 |
| 取餐时间 | 自取提供“立即用餐 / 10~50 分钟后”选择器 |
| 预计送达时间 | 外卖时间选择器，要求至少 1 小时后 |
| 优惠券 | 跳 `/pages-checkout/packages/index` |
| 备注 | 跳 remark 页，最多 50 字 |
| 支付方式 | 余额、微信；H5 额外提供支付宝 |

**提交订单**

1. 外卖必须选择地址；校验 `store.min_price` 起送价。
2. 组装创建订单参数：

| 字段 | 值 |
|---|---|
| `orderType` | `takein` / `takeout` |
| `addressId` | 外卖为地址 id，自取为 0 |
| `shopId` | 当前门店 id |
| `mobile` | `member.mobile` |
| `gettime` | 自选取餐时间偏移（0/10/.../50），外卖仍传该字段 |
| `payType` | `yue` / `weixin` / `alipay` |
| `remark` | 备注 |
| `productId[]` | 购物车商品 id 数组 |
| `spec[]` | 每个商品的 `valueStr`，逗号替换为 `|` |
| `number[]` | 每个商品数量 |
| `couponId` | 优惠券 id，没有则为 0 |

3. `POST /order/create`，成功返回含 `orderId` 的对象。
4. 清除店铺中的已选优惠券。
5. 金额为 0 时自动走余额支付；否则按 `payType` 分流。
6. 支付完成后删除 `storage.cart` 并 `switchTab` 到订单 tab。

**支付分支**

| payType / trade_type | 端 | 实现 |
|---|---|---|
| `yue` | 全端 | `POST /order/pay { uni, from, paytype: 'yue' }`；成功后扣减 `member.money`、清空购物车、跳订单页 |
| `weixin`，`MWEB` | H5（微信外） | `location.href = data.data` 跳转微信 H5 支付 |
| `weixin`，`JSAPI` | 微信小程序 | `uni.requestPayment`，使用 `timeStamp`、`nonceStr`、`package`、`paySign`，`signType: 'MD5'` |
| `weixin`，`W-JSAPI` | H5（微信内） | 分支为空，未实现公众号 JSAPI 支付 |
| `weixin`，`APP` | APP | 分支为空 |
| `alipay` | H5 | 微信内直接提示用普通浏览器；否则取返回的 HTML 表单插入页面并 `document.forms[0].submit()` |

**注意**：余额支付成功后执行的是 `member.value.money -= amount`，而界面展示和门店信息使用的是 `member.nowMoney`，字段不一致；H5 微信内支付和 APP 支付未完成，实际可用性取决于运行端。

### 8.4 订单列表、详情、收货与退款

**订单列表（主包 `pages/order/order.vue` 与子包 `pages-order/orders/orders.vue`）**

1. `onLoad` 检查登录，未登录跳登录页。
2. 状态 tab：全部 `-1`、待支付 `0`、进行中 `1`、已完成 `4`、退款单 `-3`。
3. `GET /order/list`，参数 `page`、`limit=10`、`type`。
4. 列表项展示门店名、取餐号、订单编号、状态标题、商品明细、下单时间、实付金额。
5. “确认收到餐”显示条件：`paid > 0 && status < 2 && refundStatus == 0`，调用 `POST /order/take { uni: orderId }`。
6. 下拉刷新和触底加载都调用 `getOrders(false)`；只有切换 tab 时传 `true` 重置页码。

**订单详情（`pages-order/orders/detail.vue`）**

1. `GET /order/detail/{id}`。
2. 进度图标按 `paid`、`status`、`orderType` 点亮：已下单 → 制作中 → 配送中（仅外卖）→ 已送达/请取餐。
3. 展示商品、支付方式、订单金额、配送费、优惠金额、实付金额、下单时间、门店、订单号、就餐方式、取餐时间、制作完成时间、备注。
4. 当 `paid > 0 && refundStatus == 0` 时显示“确认收到餐”和“申请退款”。

**退款（`pages-order/orders/refund.vue`）**

1. 从详情页带入 `orderId`、`payPrice`、`totalPrice`。
2. 展示订单金额、退款金额、退款类型“整个订单”、原因“协商一致”。
3. 提交 `POST /order/refund { uni, text: '协商一致', refundReasonWapExplain }`，成功 toast“提交成功”。

### 8.5 优惠券

**我的卡券页（`pages-user/coupons/coupons.vue`）**

1. 两个 tab：我的优惠券（`/coupon/my`）、未领优惠券（`/coupon/not`）。
2. 顶部兑换码：`POST /coupon/receive { code }`，成功后刷新两个列表。
3. “立即领取”：`POST /coupon/receive { id }`。
4. “立即使用”：只执行 `switchTab` 到点餐页，不自动选券。
5. 优惠券详情弹窗展示有效期、领取时间、门槛/面额、适用范围（通用/自取/外卖）、适用店铺。

**结算选券（`pages-checkout/packages/index.vue`）**

1. 接收 `amount`、`coupon_id`、`shop_id`、`type`。
2. 请求 `/coupon/my { shop_id, type, page: 1, pagesize: 10000 }`。
3. 选择时校验 `least <= amount`，通过后写入 `main.mycoupon` 并 `navigateBack`。
4. 取消选择会清空 `coupon_id`，并尝试清理上一页的 `coupon`。

### 8.6 积分商城

1. **列表**：`/score-product/list`，`uv-waterfall` 双列展示，`onShow` 重新加载，触底追加。
2. **详情**：`/score-product/detail { id }`，展示轮播图、积分、销量、库存、富文本详情；“立即兑换”把商品写入 `storage.score_product` 后进入确认页。
3. **确认页**：读取 `storage.score_product`；要求已登录且已选择地址；提交 `/score-order/submit { productId, addressId, num: 1 }`，成功后跳积分订单列表。
4. **积分订单**：`/score-order/list { page, pagesize, type }`，tab 为全部 `-1`、待发货 `0`、待收货 `1`、已完成 `2`；确认收货调用 `GET /score-order/take { id }`。
5. **积分订单详情**：`/score-order/detail { id }`，展示商品、消耗积分、状态、时间、订单号、收货地址、快递公司/单号；物流按钮调用 `/express/getLogistic { shipperCode: expressSn, logisticCode: expressNumber }` 并渲染物流轨迹。

### 8.7 登录与授权

**H5 微信 OAuth（`App.vue`）**

1. `onShow` 中仅 H5 且 `isWeixin()` 为真时执行 `oAuth()`。
2. 若已有 `wx_auth`、`accessToken` 且 `isLogin`，直接结束。
3. 从 URL 解析 `code`；没有 code 时拼装微信授权地址：
   - `appid = APP_ID`（`wxdbdbc123c8c30b45`）
   - `redirect_uri = location.origin + '/h5/#/pages/index/index'`
   - `scope = snsapi_userinfo`
   - 同时把当前页写入 `cookie.redirect`，把首页写入 `cookie.index_url`。
4. 回调拿到 code 后调用 `GET /member/auth/auth-wechat-login { code }`，写入 `wx_auth`、openId、用户信息、accessToken。

**微信小程序静默登录**

1. `App.vue` 的 `onLaunch` 先 `main.init()` 恢复本地会话；未登录时执行 `wechatMiniLogin()`。
2. `onShow` 同样在未登录时执行。
3. `uni.login({ provider: 'weixin' })` 取 code，调用 `POST /member/auth/auth-session { code }`。
4. 保存 `openId`；若响应同时包含 `userInfo` 和 `accessToken`，写入用户和 token。
5. 登录页 `onShow` 在没有 openid 时也会做一次静默登录。

**手机号验证码登录（`pages-user/login/login.vue`）**

1. 输入手机号，前端用 `uv-ui-tools` 的 `mobile` 校验。
2. `POST /member/auth/send-sms-code { mobile, scene: 1 }`，`uv-code` 启动 60 秒倒计时。
3. 必须勾选用户协议/隐私政策。
4. `POST /member/auth/sms-login { mobile, code, from, openid }`：
   - `from`：小程序 `routine`，H5 `h5`，H5 微信内 `wechat`。
5. 成功后写入 `userinfo`、`accessToken` 存储，更新 store，2 秒后 `navigateBack`。

**微信小程序手机号快捷登录**

1. 按钮 `open-type="getPhoneNumber"`。
2. 校验协议后调用 `POST /member/auth/auth-miniapp-login { encryptedData, iv, openid }`。
3. 成功后写入用户与 token，2 秒后返回上一页。

**退出登录**

`pages-user/login/logout.vue` 删除 `userinfo`、`accessToken`，清空 store 用户和 token，toast 后 `redirectTo` 登录页。

### 8.8 个人中心、资料、地址与账单

**个人中心（`pages/mine/mine.vue`）**

- `onLoad` 获取服务菜单 `/service/list`；`onShow` 在已登录时获取 `/member/user/get-info` 并刷新 store。
- 顶部展示头像、昵称、优惠券数、积分、余额、历史消费。
- 服务菜单按后端 `type` 分流：
  - `contact`：仅展示，没有点击处理。
  - `call`：`uni.makePhoneCall(phone)`。
  - `pages`：登录后 `navigateTo(pages)`。
  - `miniprogram`：`navigateToMiniProgram({ appId })`。
  - `menu`：跳 `pages-user/mine/service`（该页面不存在，见第 11 节）。
  - `content`：跳 `pages-user/mine/content`。

**用户资料（`pages-user/mine/userinfo.vue`）**

- 编辑头像、昵称、性别、生日；手机号和加入时间只读。
- 头像通过 `uni.uploadFile` 上传到 `VUE_APP_UPLOAD_URL`，再调用 `/member/user/update-nickname` 保存。
- 上传请求头使用 `member.value.accessToken`，但 store 的用户对象通常不含该字段，实际可能发送 `Bearer undefined`（见第 11 节）。

**地址管理**

- 列表 `/address/list`，支持滑动删除 `/address/del/{id}`。
- 新增/编辑 `/address/addAndEdit`；地址通过 `uni.chooseLocation` 选点，保存经纬度。
- 支持默认地址开关。

**账单（`pages-user/balance/bill.vue`）**

- 参数 `cate`：`0` 显示“全部/消费/充值/退款”四个 tab；`1` 显示“全部/消费”两个 tab。
- 请求 `/member/user/getBill { cate, type, page, pagesize }`。
- 使用 `swiper` + `scroll-view` 实现多 tab 分页加载，触底请求下一页。

## 9. 配置与环境

### 9.1 `config/index.js`

| 配置 | 值 | 说明 |
|---|---|---|
| `DEV_API_URL` | `http://localhost:48081/app-api` | `NODE_ENV === 'development'` 时使用 |
| `PROD_API_URL` | `https://apidc.yixiang.co/app-api` | 生产地址，硬编码 |
| `VUE_APP_API_URL` | 按环境二选一 | 外层有 `#ifdef APP-PLUS || H5 || MP-WEIXIN` |
| `VUE_APP_RESOURCES_URL` | `https://h5.yixiang.co/static` | 已声明，源码未使用 |
| `VUE_APP_UPLOAD_URL` | `VUE_APP_API_URL + '/infra/file/upload'` | 头像上传 |
| `APP_ID` | `wxdbdbc123c8c30b45` | H5 公众号 OAuth 使用 |
| `orderStatus` | 0 未支付、1 待发货、2 待收货、3 待评价、4 已完成、5 退款中、6 已退款、7 退款 | 导出但页面未使用 |
| `orderReStatus` | 0~6 的订单状态文案 | 导出但页面未使用 |

切换后端：直接修改 `config/index.js` 的两个常量；项目没有 `.env` 文件，也没有读取 `VITE_*`/`UNI_*` 环境变量。

### 9.2 条件编译

| 宏 | 使用位置 | 作用 |
|---|---|---|
| `#ifdef H5` | `App.vue`、`api/api.js`、`pay.vue`、`utils/util.js`、各页面样式 | 微信 OAuth、401 特殊跳转、支付宝支付、页面高度适配 |
| `#ifdef MP-WEIXIN` | `App.vue`、`api/api.js`、`login.vue`、积分页导航栏 | 小程序静默登录、微信支付、手机号快捷登录 |
| `#ifdef APP-PLUS` | `api/api.js`、`utils/index.js` | 使用 wx 版 flyio、设置 `$platform = 'app'` |
| `#ifndef H5` | `menu.vue`、`bill.vue` | 非 H5 使用不同的内容高度/页面背景 |
| `#ifndef MP` | `index.vue` | 非小程序增加顶部占位 |

### 9.3 `manifest.json` 关键配置

| 项 | 值 |
|---|---|
| `name` | `yshop-miniapp` |
| `appid` | `__UNI__ADC0FB0` |
| `versionName` / `versionCode` | `1.0.0` / `1` |
| `transformPx` | `false` |
| `vueVersion` | `"3"` |
| `uniStatistics.enable` | `false` |
| `h5.router.base` | `/h5/` |
| H5 腾讯地图 key | `OGABZ-Y5OCF-5UWJ5-N7DHH-VFIG7-DHFEB` |
| `mp-weixin.appid` | `wx001e2dc50bf532df` |
| `mp-weixin.setting.urlCheck` | `false` |
| 小程序定位权限 | `scope.userLocation`，描述“定位最近的门店” |
| 小程序私有信息 | `getLocation`、`chooseLocation` |
| APP 微信 OAuth/支付 appid | `wx7c84ede33062d1e4`（`app-plus.distribute.sdkConfigs`） |
| APP 模块 | `Payment`、`OAuth` |

### 9.4 环境相关风险

- 生产 API 地址写死在源码中，没有多环境配置文件。
- H5 微信 OAuth 的 `APP_ID` 与小程序 `appid` 不同，分别用于公众号网页授权和小程序登录，不能混用。
- 前端门店字段使用 `min_price`、`far`，而当前后端 `AppStoreShopVO` 定义的是 `minPrice`、`distance`，且未发现全局蛇形命名配置；`far` 由地址页在选择地址后动态写入。联调时需重点核对，否则起送价/配送范围判断可能失效。

## 10. 组件与工具

### 10.1 本地组件

| 组件 | 路径 | 作用 |
|---|---|---|
| `layout` | `components/layout/layout.vue` | 仅包裹 slot，`.layout { min-height: 100% }` |
| `blank` | `components/blank/blank.vue` | 按 `size`（px）生成垂直占位 |
| `list-cell` | `components/list-cell/list-cell.vue` | 通用列表行，支持箭头、点击态、分割线、内边距、自定义背景/字号/颜色 |
| `modal` | `components/modal/modal.vue` | 居中弹窗，支持自定义内容、标题、按钮组、遮罩关闭、缩放/淡入动画 |

这些组件位于 `components/组件名/组件名.vue`，依赖 uni-app 默认 easycom 自动注册；项目未显式配置 easycom 规则。

### 10.2 uni_modules 组件实际引用

| 组件 | 引用次数（约） | 主要用途 |
|---|---|---|
| `uv-navbar` | 25 | 所有页面自定义导航栏 |
| `uv-toast` | 14 | 轻提示 |
| `uv-button` | 9 | 操作按钮 |
| `uv-empty` | 9 | 空状态 |
| `uv-tabs` | 5 | 订单/账单/优惠券 tab |
| `uv-list` / `uv-list-item` | 各 5 | 退款页信息行 |
| `uv-cell` / `uv-cell-group` | 3 / 1 | 个人中心服务菜单 |
| `uv-search` | 2 | 店铺搜索、兑换码 |
| `uv-avatar` | 1 | 用户头像 |
| `uv-switch` | 1 | 自取/外卖切换 |
| `uv-swiper` | 1 | 首页广告轮播 |
| `uv-swipe-action(-item)` | 各 1 | 地址左滑删除 |
| `uv-sticky` | 1 | 首页吸顶导航 |
| `uv-steps` / `uv-steps-item` | 各 1 | 积分订单物流轨迹 |
| `uv-loading-icon` | 1 | 点餐页加载 |
| `uv-notice-bar` | 1 | 门店公告 |
| `uv-load-more` | 1 | 积分商品加载状态 |
| `uv-popup` | 1 | 点餐页购物车弹层 |
| `uv-waterfall` | 1 | 积分商品双列瀑布流 |
| `uv-code` | 1 | 验证码倒计时 |
| `uv-form` / `uv-form-item` | 各 1 | 积分确认页表单容器 |
| `uv-input` | 1 | 退款留言 |
| `uni-card` | 1 | 店铺列表卡片 |
| `uni-icons` | 间接 | uni-card/uni-scss 依赖 |

`uni_modules` 中还包含未被业务页面直接引用的 `uv-action-sheet`、`uv-badge`、`uv-gap`、`uv-image`、`uv-line`、`uv-link`、`uv-overlay`、`uv-safe-bottom`、`uv-status-bar`、`uv-text`、`uv-transition` 等，它们是 uv-ui 组件间的依赖或预留组件。

### 10.3 工具函数

| 文件 | 导出 | 作用 |
|---|---|---|
| `utils/index.js` | `handleLoginFailure`、`parseUrl`、默认插件 | 401 跳登录；把 `{url, query}` 拼成 URL；注入全局属性 |
| `utils/router.js` | `navigateTo`、`replace`、`reLaunch`、`go`、`back`、`switchTab` | 对 uni 路由 API 的统一封装；`back` 在无历史页面时回首页 |
| `utils/cookie.js` | `get/all/set/remove/clearAll/has/CACHE_KEY` | 名为 cookie，实际是 `uni.getStorageSync`/`setStorageSync` 封装 |
| `utils/util.js` | `formatTime`、`formatDateTime`、`formatLocation`、`dateUtils`、`prePage`、`kmUnit`、`isWeixin`、`parseQuery`、`throttle` | 时间格式化、距离格式化（m/km）、上一页实例、微信环境判断、H5/小程序参数解析、节流 |
| `utils/logger.js` | `IS_DEV`、`logger` | 非生产环境才输出 debug/info/warn/error |
| `utils/querystring.js` | `stringify` | 对象转查询串，支持数组 |

### 10.4 hooks

| 文件 | 作用 | 使用情况 |
|---|---|---|
| `hooks/usePage.js` | 列表分页状态（page/limit/loading/loadend/dataList）与触底加载 | 当前页面未使用，且 `onReady` 中的首次加载被注释 |
| `hooks/useGlobalProperties.js` | 返回 `app.config.globalProperties` | 当前页面未使用 |

### 10.5 样式组织

| 文件 | 作用 |
|---|---|
| `uni.scss` | 全局 SCSS 变量：主题色 `$color-primary: #059825`、文字/背景/边框/圆角/间距/字号、uv-ui 主题变量 `$uv-primary: #EE6D46` 等；末尾导入 `static/iconfont/iconfont.scss` |
| `static/style/app.scss` | 全局样式与工具类：reset、按钮重置、Vant/uv-ui 覆盖、iconfont、flex/宽高/内外边距/字号/颜色/边框/圆角/溢出/定位/阴影等；开头 `@import '~@/uni.scss'` |
| 页面 `<style scoped>` | 每个页面顶部定义局部 SCSS token，再通过 CSS 变量（如 `--index-banner-height`）与全局变量组合使用，采用 BEM-like 命名 |
| `static/iconfont/` | 本地图标字体（css/scss/ttf/woff/woff2），`iconfont`、`iconfont-yshop` 类名 |

H5 端多个页面通过 `#ifdef H5` 单独设置 `page { height/min-height }` 或内容区高度，以适配浏览器地址栏和自定义导航栏。

## 11. 代码约定与注意事项

### 11.1 代码约定

- 页面统一使用 `<script setup>` + Vue 组合式 API，页面生命周期从 `@dcloudio/uni-app` 导入。
- 业务接口按域放在 `api/*.js`，页面不直接使用 flyio。
- 金额计算大量使用字符串和 `parseFloat`/`toFixed(2)`，展示时保留两位小数。
- 状态管理只维护跨页共享数据（门店、地址、用户、token、订单类型、选券）；购物车同时保留 store 和本地存储两套，页面以 `storage.cart` 为准。
- 公共页面结构：`uv-navbar` + `container` + `list-cell`/`uv-cell`，导航返回统一调用全局 `$onClickLeft`。
- 平台差异全部使用 `// #ifdef` 条件编译，没有运行时平台判断库。

### 11.2 已确认的缺陷与风险

| 问题 | 位置 | 说明 |
|---|---|---|
| 缺少 `u-picker` 组件 | `pages-checkout/pay/pay.vue:60,82` | 模板使用 `u-picker`，但 `uni_modules` 中没有 `u-picker`（只有 `uv-*` 组件），时间选择器可能无法渲染 |
| 缺少 `u-loadmore` 组件 | `pages-user/balance/bill.vue:31,45` | 项目只有 `uv-load-more`，账单页的加载更多组件未注册 |
| 跳转到不存在的页面 | `pages/mine/mine.vue:176` | `/pages-user/mine/service` 未在 `pages.json` 注册，文件也不存在 |
| 订单详情收货后调用未定义函数 | `pages-order/orders/detail.vue:242-247` | `receive()` 成功后调用 `getOrders(true)`，该文件没有定义 `getOrders`，会抛 `ReferenceError` |
| 购物车页引用未定义变量 | `pages-checkout/cart/cart.vue:97` | `handleCartItemReduce` 在购物车清空时访问 `cartPopupVisible`，该变量未定义 |
| 下拉刷新未重置页码 | `pages/order/order.vue:121-123`、`pages-order/orders/orders.vue:121-123` | `onPullDownRefresh` 调用 `getOrders(false)`，页码不归 1，列表会重复追加 |
| 头像上传鉴权头错误 | `pages-user/mine/userinfo.vue:133-136` | 使用 `member.value.accessToken`，而用户对象通常没有 `accessToken`；应使用 store token 或本地存储 |
| 积分列表重复累积 | `pages-score/scoreproduct/list.vue:101-109` | `changeList` 只向 `list1/list2` push，刷新/重新加载时未清空，可能重复展示 |
| 物流错误分支引用未定义变量 | `pages-score/scoreproduct/orderDetail.vue:155-162` | `data.success == "false"` 时使用 `res.reason`，`res` 未定义 |
| 门店字段命名不一致 | `pages/menu/menu.vue:302-309`、`pay.vue:305` | 前端读 `store.min_price`；后端 `AppStoreShopVO` 为 `minPrice`，存在起送价校验失效风险 |
| 余额字段不一致 | `pages-checkout/pay/pay.vue:601` | 扣减 `member.money`，界面展示 `member.nowMoney` |
| 充值接口无后端实现 | `api/user.js:43-55` | `/recharge/getMoneyList`、`/member/user/recharge` 无 app 控制器；前端也未调用 |
| H5 微信内支付未实现 | `pages-checkout/pay/pay.vue:661-667` | `W-JSAPI` 和 `APP` 分支为空 |
| 硬编码会话键/无效 action | `store/store.js:19,58-64` | `cookieKey` 为硬编码 YSESSID；`setCookie` 使用不存在的 `state.cookie`，且无调用点 |
| 日志可能输出敏感信息 | `utils/cookie.js:14-21` | `set` 会 `console.log` key 和 value，包括 accessToken |
| 未使用的依赖/能力 | 全项目 | `vue-i18n`、`getWechatConfig`、`orderStatus/orderReStatus`、`balanceRecharge/balanceGetMoneyList`、`orderTakeFoods`、`hooks/usePage` 等 |
| 无自动化测试 | `package.json` | `npm test` 固定失败；没有测试目录或测试框架 |
| 无分享能力 | 页面 | 未发现 `onShareAppMessage`/`onShareTimeline` |

### 11.3 二次开发建议

1. 先修复缺失组件：把 `u-picker`/`u-loadmore` 替换为 `uv-ui` 对应组件，或补齐原 uView 组件。
2. 统一门店字段：在 API 层做一层字段归一化（`minPrice → min_price`、补充 `far`），避免页面直接依赖后端命名。
3. 统一分页封装：抽出 `usePage` 并让订单/积分/账单页共用，修复下拉刷新页码未重置的问题。
4. 支付流程补齐：H5 微信内 JSAPI 需要调用 `getWechatConfig` 并 `wx.config`；APP 支付需要补平台依赖和 `uni.requestPayment` 分支。
5. 鉴权统一：所有上传/下载/支付请求都从 `cookie.get('accessToken')` 取 token，而不是从用户对象取。
6. 清理未使用接口与依赖，避免误以为充值、积分抵扣等能力已接入。

---

**关键源码入口索引**

| 主题 | 文件 |
|---|---|
| 应用生命周期/授权 | `App.vue` |
| 应用创建/Pinia | `main.js` |
| 页面与分包 | `pages.json` |
| 请求封装 | `api/api.js` |
| 全局状态 | `store/store.js` |
| 后端地址/常量 | `config/index.js` |
| 构建脚本 | `scripts/uni.js`、`scripts/patch-uni-h5-vue.js` |
| 多端配置 | `manifest.json` |
| 全局样式 | `uni.scss`、`static/style/app.scss` |
