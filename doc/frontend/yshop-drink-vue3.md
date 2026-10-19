# yshop-drink-vue3 商家后台管理端

> 本文基于 `yshop-drink-vue3` 当前源码（`package.json` 版本 `3.0.0`）逐文件分析整理。文中接口路径均为去掉 axios `baseURL` 后的相对路径，实际请求地址为 `VITE_BASE_URL + VITE_API_URL + 接口路径`。

## 1. 项目定位

`yshop-drink-vue3` 是 yshop-drink 多门店在线点餐系统的**商家后台管理端**，与 `yshop-drink-boot3` 后端、`yshop-drink-uniapp-vue3` 用户端组成一套完整系统。

| 维度 | 说明 |
|---|---|
| 使用对象 | 平台/商家管理员、门店运营人员 |
| 业务范围 | 门店、商品、订单、会员、优惠券、积分商城、微信公众号/小程序、支付商户、快递、系统管理、基础设施管理 |
| 前后端交互 | 统一走后端 `/admin-api` 前缀，响应结构为 `CommonResult<T>`（`code`、`data`、`msg`） |
| 权限模型 | 后端菜单 + 角色 + 按钮权限标识（RBAC），前端根据登录返回的菜单动态生成路由 |
| 路由形态 | 静态基础路由 + 后端 `system_menu` 菜单驱动的动态路由 |
| 工程类型 | Vue 3 SPA，Vite 构建，TypeScript，Element Plus |

项目同时包含两类模块：

1. **yshop 点餐业务模块**：`mall`（门店、商品、订单、会员、优惠券）、`score`（积分商城）、`express`（快递）、`message`（微信模板消息）、`pay`（支付商户）。
2. **通用后台基础模块**（沿用 yudao 系后台能力）：`system`（用户、角色、菜单、字典、邮件、短信、通知、租户、OAuth2、社交登录）、`infra`（代码生成、文件、定时任务、日志、Redis、监控）、`mp`（微信公众号/小程序管理）。

## 2. 技术栈

| 分类 | 依赖/工具 | 版本 | 用途 |
|---|---|---|---|
| 框架 | `vue` | 3.4.21 | 组合式 API、`<script setup>` |
| 构建 | `vite` | 5.1.4 | 开发服务器与生产构建 |
| 构建 | `typescript` | 5.3.3 | 类型系统；`vue-tsc` 做类型检查 |
| UI | `element-plus` | 2.6.1 | 后台组件库 |
| UI 图标 | `@element-plus/icons-vue`、`@iconify/iconify`、`@purge-icons/generated` | - | Element Plus 图标、Iconify 在线/离线图标 |
| 路由 | `vue-router` | 4.3.x | History 模式、动态路由 |
| 状态 | `pinia` + `pinia-plugin-persistedstate` | 2.1.7 / 3.2.1 | 全局状态与持久化 |
| 请求 | `axios` | 1.6.8 | HTTP 请求、拦截器、Token 刷新 |
| 国际化 | `vue-i18n` | 9.10.2 | 中英文文案 |
| 样式 | `sass`、`unocss`、`animate.css` | - | SCSS、原子化 CSS、动画 |
| 图表 | `echarts`、`echarts-wordcloud` | 5.5.x | 仪表盘/公众号统计图表 |
| 表单设计 | `@form-create/designer`、`@form-create/element-ui` | 3.1.x | 低代码表单设计器（`infra/build`） |
| 富文本 | `@wangeditor/editor`、`@wangeditor/editor-for-vue`、`vue-ueditor-wrap` | - | 公告、商品详情、公众号文章编辑 |
| 文件/图片 | `cropperjs`、`qrcode`、`vue-baidu-map-3x` | - | 头像裁剪、二维码、百度地图选点 |
| 辅助 | `lodash-es`、`dayjs`、`qs`、`crypto-js`、`jsencrypt`、`@vueuse/core` | - | 工具函数、日期、查询串、加密、组合式工具 |
| 编码规范 | ESLint 8、Prettier 3、Stylelint 16 | - | 代码检查与格式化 |
| 运行环境 | Node.js、pnpm | `node >= 16`、`pnpm >= 8.6` | 仓库约定使用 pnpm 8.x（lockfile v6） |

> `@vitejs/plugin-legacy` 出现在 `devDependencies` 中，但 `build/vite/index.ts` 并未注册该插件，当前构建不生成 legacy 兼容包。

## 3. 目录结构

```text
yshop-drink-vue3/
├── build/vite/                 # Vite 插件配置
│   ├── index.ts                # 插件集合：Vue、JSX、UnoCSS、自动导入、SVG、gzip 等
│   └── optimize.ts             # optimizeDeps 的 include/exclude
├── public/                     # 静态资源（favicon、logo.gif 等）
├── types/                      # 全局类型声明
│   ├── router.d.ts             # 路由 meta / AppRouteRecordRaw 扩展
│   ├── env.d.ts                # import.meta.env 类型
│   ├── global.d.ts             # Recordable、PageParam 等全局类型
│   └── custom-types.d.ts
├── src/
│   ├── api/                    # 接口定义，按后端业务域分目录
│   ├── assets/                 # 图片、SVG、样式资源
│   ├── components/             # 公共组件（Table、Form、Dialog、UploadFile 等）
│   ├── config/axios/           # axios 实例、请求封装、错误码
│   ├── directives/             # v-hasPermi / v-hasRole
│   ├── hooks/                  # 组合式函数（web/、表单项等）
│   ├── layout/                 # 主框架、菜单、标签页、头部、设置
│   ├── locales/                # zh-CN.ts、en.ts 国际化文案
│   ├── plugins/                # Element Plus、i18n、SVG、UnoCSS、form-create、统计
│   ├── router/                 # 路由实例与静态路由
│   ├── store/                  # Pinia 实例与 modules
│   ├── styles/                 # 全局 SCSS、变量
│   ├── types/                  # 自动生成的类型声明
│   ├── utils/                  # 路由工具、鉴权、字典、树、下载等
│   ├── views/                  # 业务页面
│   ├── App.vue                 # 根组件
│   ├── main.ts                 # 应用入口
│   └── permission.ts           # 全局路由守卫
├── .env / .env.dev / .env.local / .env.test / .env.stage / .env.prod
├── index.html                  # HTML 模板，EJS 注入 VITE_APP_TITLE
├── package.json
├── tsconfig.json
├── vite.config.ts
├── uno.config.ts
├── .eslintrc.js / prettier.config.js / stylelint.config.js
└── pnpm-lock.yaml
```

`src` 下关键约定：

| 目录 | 约定 |
|---|---|
| `api/<domain>/index.ts` | 一个业务域一个文件或目录，导出 `getXxxPage/getXxx/createXxx/updateXxx/deleteXxx` 等方法 |
| `views/<module>/<page>/index.vue` | 列表页；`XxxForm.vue` 为新增/编辑弹窗 |
| `store/modules/*.ts` | 一个 Pinia store 一个文件，同时导出 `useXxxStoreWithOut()` 供非组件环境使用 |
| `components/<Name>/index.ts` | 组件出口；部分组件通过 `unplugin-vue-components` 自动注册 |
| `router/modules/remaining.ts` | 不依赖后端菜单的静态路由 |

## 4. 启动、构建与环境配置

### 4.1 常用命令

| 命令 | 实际执行 | 说明 |
|---|---|---|
| `pnpm install` / `pnpm i` | `pnpm install` | 安装依赖（建议 pnpm 8.x） |
| `pnpm dev` | `vite` | 本地开发，端口取 `VITE_PORT`（默认 80），`VITE_OPEN=true` 时自动打开浏览器 |
| `pnpm dev-server` | `vite --mode dev` | 以 `dev` 模式启动，加载 `.env.dev` |
| `pnpm build:local` | `vite build` | 默认 production 模式，加载 `.env` + `.env.local`，**不会加载 `.env.prod`** |
| `pnpm build:dev` | `vite build --mode dev` | 输出目录取 `VITE_OUT_DIR`（默认 `dist`） |
| `pnpm build:test` | `vite build --mode test` | 加载 `.env.test`，输出 `dist-test` |
| `pnpm build:stage` | `vite build --mode stage` | 加载 `.env.stage`，输出 `dist-stage` |
| `pnpm build:prod` | `vite build --mode prod` | 加载 `.env.prod`，输出 `dist-prod` |
| `pnpm serve:dev` / `pnpm serve:prod` | `vite preview --mode ...` | 预览对应构建产物 |
| `pnpm preview` | `pnpm build:local && vite preview` | 本地构建并预览 |
| `pnpm ts:check` | `vue-tsc --noEmit` | TypeScript 类型检查 |
| `pnpm lint:eslint` | `eslint --fix --ext .js,.ts,.vue ./src` | ESLint 修复 |
| `pnpm lint:format` | `prettier --write ...` | Prettier 格式化 |
| `pnpm lint:style` | `stylelint --fix ...` | 样式检查 |
| `pnpm clean` / `pnpm clean:cache` | `rimraf node_modules` / `rimraf node_modules/.cache` | 清理依赖与缓存 |

### 4.2 环境文件加载规则

Vite 只会加载以 `VITE_` 开头的变量到客户端。项目环境文件如下：

| 文件 | 何时加载 | 主要内容 |
|---|---|---|
| `.env` | 所有模式 | 公共配置：标题、端口、租户/验证码开关、百度统计、百度地图 AK |
| `.env.local` | 所有模式，优先级高于 `.env`；通常被 git 忽略 | 本机覆盖：本机后端地址 `http://127.0.0.1:48081` |
| `.env.dev` | `--mode dev`（`pnpm dev-server`、`pnpm build:dev`） | 远程开发环境地址、上传地址、API 前缀、构建开关 |
| `.env.test` | `--mode test` | 测试环境地址，基址 `/admin-ui-vue3/`，输出 `dist-test` |
| `.env.stage` | `--mode stage` | 预发布环境，输出 `dist-stage` |
| `.env.prod` | `--mode prod`（`pnpm build:prod`） | 生产环境，输出 `dist-prod` |

注意：

- `pnpm dev` 未显式传 `--mode`，`vite.config.ts` 中 `loadEnv` 实际只读取 `.env` 与 `.env.local`，因此不会加载 `.env.dev`；需要开发环境配置时使用 `pnpm dev-server`。
- `.env.local` 对所有模式生效。仓库当前存在 `.env.local` 且指向 `127.0.0.1:48081`，因此即使执行 `build:prod`，本地也会被它覆盖。生产打包前应移除或调整该文件。
- `vite.config.ts` 中本地代理 `proxy` 已被注释，跨域由后端 CORS 支持，前端直接请求 `VITE_BASE_URL`。

### 4.3 环境变量

| 变量 | 示例/默认值 | 作用 |
|---|---|---|
| `NODE_ENV` | `production`（dev/test/stage/prod 文件中） | 构建模式标记 |
| `VITE_APP_TITLE` | `意象点餐管理系统` | 浏览器标题、登录页标题、`index.html` EJS 注入 |
| `VITE_PORT` | `80` | 开发服务器端口 |
| `VITE_OPEN` | `true` | `vite` 启动后是否自动打开浏览器 |
| `VITE_APP_TENANT_ENABLE` | `false` | 是否启用多租户；为 `true` 时登录请求会先按租户名换租户 ID，请求头携带 `tenant-id` |
| `VITE_APP_CAPTCHA_ENABLE` | `true`（`.env`、`.env.dev`） | 登录是否弹出滑块/点选验证码 |
| `VITE_APP_DOCALERT_ENABLE` | `true` | 是否展示 `DocAlert` 文档提示 |
| `VITE_APP_BAIDU_CODE` | `a1ff...` | 百度统计站点 ID；为空则不注入统计脚本 |
| `VITE_BAIDU_MAP_AK` | `m1YR...` | 百度地图 JS API AK（门店地图选点） |
| `VITE_DEV` | `true`/`false` | 环境标记；当前 `src` 中未直接使用 |
| `VITE_BASE_URL` | `http://127.0.0.1:48081`（local）/远程域名 | 后端服务根地址 |
| `VITE_API_URL` | `/admin-api` | 管理端 API 前缀 |
| `VITE_UPLOAD_URL` | `http://127.0.0.1:48081/admin-api/infra/file/upload` | 文件上传地址 |
| `VITE_UPLOAD_TYPE` | `server` | `server` 后端上传；`client` 前端直传（S3/MinIO 预签名） |
| `VITE_DROP_DEBUGGER` | `false`/`true` | 构建时是否移除 `debugger` |
| `VITE_DROP_CONSOLE` | `false`/`true` | 构建时是否移除 `console` |
| `VITE_SOURCEMAP` | `true`/`false` | 为 `true` 时构建内联 sourcemap |
| `VITE_BASE_PATH` | `/` | 应用部署基址（Vite `base`） |
| `VITE_OUT_DIR` | `dist` / `dist-test` / `dist-stage` / `dist-prod` | 构建输出目录 |
| `VITE_MALL_H5_DOMAIN` | `http://mall.yudao.yixiang.co` 等 | 商城 H5 域名；当前 `src` 中未检索到使用 |

**请求基址计算**：

```text
axios baseURL = VITE_BASE_URL + VITE_API_URL
例如：http://127.0.0.1:48081 + /admin-api = http://127.0.0.1:48081/admin-api
```

因此在 `src/api` 中写 `/system/user/page`，最终请求为 `http://127.0.0.1:48081/admin-api/system/user/page`。

**已知环境配置问题**：

- `.env.prod` 的 `VITE_BASE_URL` 是 `http://localhost:48081`，正式部署必须改为真实后端地址。
- `.env.stage` 的 `VITE_BASE_PATH` 被写成了完整域名 `https://apidc.dayouqiantu.cn`，它不是合法的路径基址；部署到子路径时应改为 `/xxx/`。
- `VITE_MALL_H5_DOMAIN`、`VITE_DEV` 在当前 `src` 中没有实际引用。

## 5. 路由体系

### 5.1 路由实例

`src/router/index.ts`：

- 使用 `createWebHistory()`（URL 不带 `#`）。
- `strict: true`。
- 初始路由为 `remaining.ts` 中的静态路由。
- `scrollBehavior` 始终回到左上角。
- `resetRouter()` 会移除除 `Redirect`、`Login`、`NoFind`、`Root` 外的所有路由。当前静态首页路由名为 `Home`，不在保留名单中，因此 `resetRouter` 会把它一并移除；由于随后会整页刷新重建，实际影响有限，但命名不一致值得注意。

### 5.2 静态路由

文件：`src/router/modules/remaining.ts`。

| 路径 | 名称 | 组件 | 说明 |
|---|---|---|---|
| `/redirect/:path(.*)` | `Redirect1` | `views/Redirect/Redirect.vue` | 重定向中转页，支持 `path` 或 `name` |
| `/` → `/index` | `Home` | `views/Home/Index.vue` | 首页仪表盘，`affix: true` 固定标签页 |
| `/user/profile` | `Profile` | `views/Profile/Index.vue` | 个人中心 |
| `/user/notify-message` | `MyNotifyMessage` | `views/system/notify/my/index.vue` | 我的站内信 |
| `/dict/type/data/:dictType` | `SystemDictData` | `views/system/dict/data/index.vue` | 字典数据（隐藏页，高亮 `/system/dict`） |
| `/codegen/edit` | `InfraCodegenEditTable` | `views/infra/codegen/EditTable.vue` | 修改代码生成配置（隐藏页） |
| `/job/job-log` | `InfraJobLog` | `views/infra/job/logger/index.vue` | 定时任务调度日志（隐藏页） |
| `/login` | `Login` | `views/Login/Login.vue` | 登录页 |
| `/sso` | `SSOLogin` | `views/Login/Login.vue` | SSO 登录入口 |
| `/social-login` | `SocialLogin` | `views/Login/SocialLogin.vue` | 社交登录回调/登录页 |
| `/403` | `NoAccess` | `views/Error/403.vue` | 无权限 |
| `/404` | `NoFound` | `views/Error/404.vue` | 未找到 |
| `/500` | `Error` | `views/Error/500.vue` | 服务错误 |
| `/yshop/materia/index` | `EditorMaterials` | `components/Materials/src/editorMaterials.vue` | 素材库编辑弹层路由 |

白名单（`src/permission.ts`）：`/login`、`/social-login`、`/auth-redirect`、`/bind`、`/register`、`/oauthLogin/gitee`。其中 `/auth-redirect`、`/bind`、`/register`、`/oauthLogin/gitee` 当前没有在静态路由中注册。

### 5.3 后端菜单驱动的动态路由流程

动态路由**不从前端配置文件读取**，而是完全由登录用户权限信息中的 `menus` 生成。

```text
访问任意受保护路由
  ↓
permission.ts beforeEach 检查 accessToken
  ↓
无 token → /login?redirect=<fullPath>
  ↓
有 token →
  1. dictStore.getIsSetDict 为 false 时，加载字典
  2. userStore.getIsSetUser 为 false 时：
     a. GET /system/auth/get-permission-info
     b. 写入 permissions / roles / user
     c. 将 menus 写入 localStorage 的 roleRouters
  3. permissionStore.generateRoutes()
     a. 从 localStorage 读取 roleRouters
     b. generateRoute() 递归转换为 vue-router 路由
     c. 追加 /:path(.*) → /404
  4. router.addRoute() 逐条注册
  5. 重新执行 next()，跳转目标页
```

关键点：

- `permissionStore.generateRoutes()` **自身不发请求**，只读取 `setUserInfoAction()` 已缓存的 `menus`。如果 token 存在但 `roleRouters` 缓存被清空，动态路由会为空。
- `menus` 来自后端 `AuthPermissionInfoRespVO.menus`，后端已剔除按钮类型菜单并按 `sort` 排序，形成树。
- 动态路由 404 兜底必须放在最后，代码中通过 `routerMap.concat([...404])` 保证。
- `addRouters` 是注册到 router 的扁平化路由；`routers` 是用于侧边栏渲染的“静态路由 + 动态菜单路由”。

### 5.4 菜单字段到路由字段的映射

`src/utils/routerHelper.ts` 的 `generateRoute()`：

| 后端菜单字段 | 前端路由字段 | 规则 |
|---|---|---|
| `name` | `meta.title` | 菜单显示名称 |
| `icon` | `meta.icon` | 图标标识（Iconify/SVG） |
| `visible` | `meta.hidden` | `hidden = !visible` |
| `keepAlive` | `meta.noCache` | `noCache = !keepAlive` |
| `alwaysShow` | `meta.alwaysShow` | 有且仅有一个子菜单时，默认 `true` |
| `path` | `path` | 若含 `?`，只取问号前部分；参数解析后放入 `meta.query` |
| `componentName` | `name` | 优先使用；为空时由 `path` 转首字母大写驼峰 |
| `component` | `component` | 与 `import.meta.glob` 的模块路径做包含匹配 |
| `redirect` | `redirect` | 原样赋值；注意后端菜单 VO 当前未返回该字段，实际通常为空 |
| `children` | `children` | 递归生成 |

其他生成规则：

1. **顶级非目录菜单**（`parentId == 0`、无 `children`、有 `component`）：自动包一层 `Layout`，父路由名称为 `XxxParent`，子路由 `path: ''`。
2. **目录菜单**（有 `children`）：`component = Layout`，并根据第一个子菜单计算 `redirect`。
3. **外链菜单**（`path` 以 `http(s)://` 开头）：包一层 `/external-link` 的 `Layout`，菜单点击时 `window.open`。
4. **普通菜单**：直接解析组件。
5. **层级降级**：`getAddRouters()` 调用 `flatMultiLevelRoutes()`，把三层以上路由提升为两层，避免 `keep-alive`/菜单渲染问题。

### 5.5 页面组件解析规则

`src/utils/routerHelper.ts` 顶部：

```ts
const modules = import.meta.glob('../views/**/*.{vue,tsx}')
```

即所有 `src/views` 下的 `.vue` 与 `.tsx` 页面都会被 Vite 收集。解析逻辑：

1. 若菜单配置了 `component`（例如 `system/user/index`），在模块 key 中查找包含该字符串的项。
2. 若未配置 `component`，则用菜单 `path` 做包含匹配。
3. 命中后使用 `modules[key]` 作为异步组件。

因此：

- 后端菜单的 `component` 必须与 `src/views` 下的相对路径片段一致，例如 `system/user/index` 对应 `src/views/system/user/index.vue`。
- 若找不到匹配项，`data.component` 为 `undefined`，路由虽然注册成功但页面无法渲染。
- 菜单类型为按钮时不会出现在菜单树中，但仍会进入 `permissions` 权限数组，供 `v-hasPermi` 使用。
- `component` 可带查询参数，例如 `xxx/index?foo=bar`，前端会拆分参数放入 `meta.query`。
- `componentName` 需要与页面内 `defineOptions({ name: 'Xxx' })` 或 `<script setup name="Xxx">` 一致，否则 `keep-alive` 缓存失效。

**已知问题**：`getRedirect()` 在“目录的直接子菜单是叶子页面”这一常见场景下只计算了路径但没有 `return path`，因此目录路由的 `redirect` 为 `undefined`。访问目录根路径时可能只渲染空 `Layout`，通常需要点击具体子菜单。新增菜单时建议直接访问叶子路径或修复该函数。

### 5.6 布局与标签页

`src/layout/Layout.vue` + `components/useRenderLayout.tsx` 支持四种布局：

| 布局值 | 结构 |
|---|---|
| `classic` | 左侧 Logo + 竖向菜单，顶部工具栏 + 标签页，内容区 |
| `topLeft` | 顶部 Logo/工具栏，左侧菜单，内容区 |
| `top` | 顶部 Logo + 横向菜单 + 工具栏，内容区 |
| `cutMenu` | 顶部 Logo/工具栏，左侧一级分类 TabMenu + 二级菜单 |

- 布局、主题、暗黑模式、组件尺寸等保存在 `useAppStore`，其中部分字段写入 localStorage。
- `AppView.vue` 使用 `<keep-alive :include="getCachedViews">` 缓存页面，缓存名单来自 `tagsView` store，依据路由 `meta.noCache` 计算。
- `AppView` 还通过 `provide('reload')` 提供页面无感刷新能力。
- 顶部工具栏包含折叠、面包屑、全屏、菜单搜索、尺寸切换、语言切换、站内信、用户菜单。
- 标签页支持关闭、关闭其他、关闭左侧/右侧、全部关闭等操作。

## 6. API 层与请求封装

### 6.1 目录与分层

```text
src/api/<module>/index.ts     业务接口
src/config/axios/index.ts     统一请求方法（get/post/put/delete/download/upload）
src/config/axios/service.ts   axios 实例、拦截器、Token 刷新
src/config/axios/config.ts    baseURL、超时、成功码
src/config/axios/errorCode.ts HTTP 错误码文案
```

### 6.2 axios 实例

`src/config/axios/config.ts`：

| 配置 | 值 |
|---|---|
| `base_url` | `import.meta.env.VITE_BASE_URL + import.meta.env.VITE_API_URL` |
| `result_code` | `200` |
| `request_timeout` | `30000` ms |
| 默认 Content-Type | `application/json` |

`service.ts` 创建实例时 `withCredentials: false`（不携带 Cookie），认证完全依赖 `Authorization` 请求头。

### 6.3 统一请求方法

`src/config/axios/index.ts`：

| 方法 | 说明 | 返回值 |
|---|---|---|
| `get` | GET，参数走 `params` | `res.data`（业务 payload） |
| `post` | POST，JSON body | `res.data` |
| `postOriginal` | POST，返回完整 `CommonResult` | 完整响应对象 |
| `put` | PUT | `res.data` |
| `delete` | DELETE | `res.data` |
| `download` | GET + `responseType: 'blob'` | Blob |
| `upload` | POST + `multipart/form-data` | 完整响应对象 |

响应拦截器返回的是后端的 `CommonResult`（`{ code, data, msg }`），因此普通方法再取一层 `res.data` 得到业务数据；`download` 与 `upload` 保留完整响应。

### 6.4 请求拦截器

`src/config/axios/service.ts` 的 request 拦截器依次处理：

1. **白名单判断**：`whiteList = ['/login', '/refresh-token']`。
2. **Token 注入**：存在 accessToken 时添加 `Authorization: Bearer <accessToken>`。
3. **租户注入**：`VITE_APP_TENANT_ENABLE === 'true'` 且存在 `tenantId` 时添加 `tenant-id` 请求头。
4. **表单编码**：POST 且 `Content-Type: application/x-www-form-urlencoded` 时用 `qs.stringify` 处理 body。
5. **GET 参数编码**：使用 `qs.stringify(params, { allowDots: true })` 拼到 URL 后，并清空 `config.params`，以支持嵌套对象/数组参数。

> **已发现的实现缺陷**：白名单回调对任意存在 `url` 的请求都会执行 `return (isToken = false)`，并没有真正判断 `config.url.indexOf(v) > -1`。结果是 `isToken` 恒为 `false`，白名单没有实际作用——登录/刷新令牌请求只要本地存在 token 也会带上 `Authorization`。目前不影响主流程，但与代码意图不一致。

### 6.5 响应拦截器与错误处理

处理顺序：

| 场景 | 处理 |
|---|---|
| 响应为 `blob` / `arraybuffer` | 若类型不是 JSON，直接返回二进制；若是 JSON（通常是导出失败），解析为 JSON 后继续走错误判断 |
| `code === 401` | 触发无感刷新令牌流程 |
| `code === 500` | `ElMessage.error` 国际化错误提示，`Promise.reject` |
| `code === 901` | 特殊提示（演示/授权到期类），附带官网地址 |
| 其他非成功码 | `ElNotification.error` 提示后端 `msg`；`无效的刷新令牌` 不提示 |
| HTTP 网络错误 | `Network Error`、超时、HTTP 状态码分别转换为中文提示 |
| 成功 | 返回完整 `CommonResult` |

**成功码判断细节**：后端 `GlobalErrorCodeConstants.SUCCESS` 为 `0`，但拦截器使用 `const code = data.code || result_code`。由于 `0` 是假值，实际会被替换为 `200`，随后 `code !== 200` 为假，因此成功响应能正常返回。这是一种“隐式兼容”，若后端改为其他成功码需要同步调整。

`errorCode.ts` 仅包含：

| code | 文案 |
|---|---|
| `401` | 认证失败，无法访问系统资源 |
| `403` | 当前操作没有权限 |
| `404` | 访问资源不存在 |
| `default` | 系统未知错误，请反馈给管理员 |

### 6.6 无感刷新 Token

关键变量：

- `isRefreshToken`：是否正在刷新。
- `requestList`：等待刷新完成的请求队列。
- `isRelogin.show`：是否已弹出重新登录框。
- `ignoreMsgs`：`无效的刷新令牌`、`刷新令牌已过期`，避免重复弹窗。

流程：

1. 任意请求返回 `code === 401`。
2. 若当前没有在刷新：
   - 取不到 refreshToken → 直接 `handleAuthorized()`。
   - 取到 refreshToken → 使用**原生 axios**（绕过拦截器）请求 `POST {baseURL}/system/auth/refresh-token?refreshToken=...`。
3. 刷新成功：`setToken()` 保存新的 access/refresh token，更新当前请求头，依次执行队列回调，然后重放当前请求。
4. 刷新失败：执行队列回调，进入 `handleAuthorized()`，不重放当前请求，避免递归。
5. `finally` 清空队列并重置 `isRefreshToken`。
6. 若 401 时已经在刷新：把当前请求包装成 Promise 放入 `requestList`，等待刷新完成后携带新 token 重放。

`handleAuthorized()`：

- 若当前 URL 已包含 `login?redirect=`，不再弹窗。
- 否则弹出不可取消的确认框，用户确认后：
  1. `resetRouter()` 重置路由；
  2. `deleteUserCache()` 删除用户与菜单缓存；
  3. `removeToken()` 删除 token；
  4. 整页重新加载，让路由守卫重新走登录流程。

### 6.7 `src/api` 业务模块

所有路径均省略 `/admin-api` 前缀。

#### 登录与认证

文件：`src/api/login/index.ts`、`src/api/login/oauth2/index.ts`。

| 方法 | 接口 |
|---|---|
| `login` | `POST /system/auth/login` |
| `refreshToken` | `POST /system/auth/refresh-token` |
| `loginOut` | `POST /system/auth/logout` |
| `getInfo` | `GET /system/auth/get-permission-info` |
| `sendSmsCode` / `smsLogin` | `POST /system/auth/send-sms-code`、`/system/auth/sms-login` |
| `socialLogin` / `socialAuthRedirect` | `POST /system/auth/social-login`、`GET /system/auth/social-auth-redirect` |
| `getTenantIdByName` / `getTenantByWebsite` | `/system/tenant/get-id-by-name`、`/system/tenant/get-by-website` |
| `getCode` / `reqCheck` | `system/captcha/get`、`system/captcha/check`（点选/滑块验证码） |
| `getAuthorize` / `authorize` | `/system/oauth2/authorize` |

#### 系统管理 `system`

| 文件 | 接口前缀 | 代表方法 |
|---|---|---|
| `system/user/index.ts` | `/system/user` | `getUserPage`、`getUser`、`createUser`、`updateUser`、`deleteUser`、`exportUser`、`importUserTemplate`、`resetUserPwd`、`updateUserStatus`、`getSimpleUserList` |
| `system/user/profile.ts` | `/system/user/profile` | `getUserProfile`、`updateUserProfile`、`updateUserPassword`、`uploadAvatar` |
| `system/user/socialUser.ts` | `/system/social-user` | `socialBind`、`socialUnbind`、`socialAuthRedirect` |
| `system/role/index.ts` | `/system/role` | `getRolePage`、`getSimpleRoleList`、`getRole`、`createRole`、`updateRole`、`updateRoleStatus`、`deleteRole`、`exportRole` |
| `system/menu/index.ts` | `/system/menu` | `getSimpleMenusList`、`getMenuList`、`getMenu`、`createMenu`、`updateMenu`、`deleteMenu` |
| `system/permission/index.ts` | `/system/permission` | `getRoleMenuList`、`assignRoleMenu`、`assignRoleDataScope`、`getUserRoleList`、`assignUserRole` |
| `system/dept/index.ts` | `/system/dept` | `getSimpleDeptList`、`getDeptPage`、`getDept`、`createDept`、`updateDept`、`deleteDept` |
| `system/post/index.ts` | `/system/post` | `getPostPage`、`getSimplePostList`、`createPost`、`updatePost`、`deletePost`、`exportPost` |
| `system/dict/dict.type.ts` | `/system/dict-type` | `getSimpleDictTypeList`、`getDictTypePage`、`createDictType`、`updateDictType`、`deleteDictType` |
| `system/dict/dict.data.ts` | `/system/dict-data` | `getSimpleDictDataList`、`getDictDataPage`、`createDictData`、`updateDictData`、`deleteDictData` |
| `system/notice/index.ts` | `/system/notice` | `getNoticePage`、`getNotice`、`createNotice`、`updateNotice`、`deleteNotice`、`pushNotice` |
| `system/notify/message/index.ts` | `/system/notify-message` | `getNotifyMessagePage`、`getMyNotifyMessagePage`、`updateNotifyMessageRead`、`updateAllNotifyMessageRead`、`getUnreadNotifyMessageList`、`getUnreadNotifyMessageCount` |
| `system/notify/template/index.ts` | `/system/notify-template` | `getNotifyTemplatePage`、`createNotifyTemplate`、`updateNotifyTemplate`、`deleteNotifyTemplate`、`sendNotify` |
| `system/mail/account/index.ts` | `/system/mail-account` | `getMailAccountPage`、`createMailAccount`、`updateMailAccount`、`deleteMailAccount`、`getSimpleMailAccountList` |
| `system/mail/template/index.ts` | `/system/mail-template` | `getMailTemplatePage`、`createMailTemplate`、`updateMailTemplate`、`deleteMailTemplate`、`sendMail` |
| `system/mail/log/index.ts` | `/system/mail-log` | `getMailLogPage`、`getMailLog` |
| `system/sms/smsChannel/index.ts` | `/system/sms-channel` | `getSmsChannelPage`、`getSimpleSmsChannelList`、`createSmsChannel`、`updateSmsChannel`、`deleteSmsChannel` |
| `system/sms/smsTemplate/index.ts` | `/system/sms-template` | `getSmsTemplatePage`、`createSmsTemplate`、`updateSmsTemplate`、`deleteSmsTemplate`、`sendSms` |
| `system/sms/smsLog/index.ts` | `/system/sms-log` | `getSmsLogPage`、`exportSmsLog` |
| `system/social/client/index.ts` | `/system/social-client` | `getSocialClientPage`、`getSocialClient`、`createSocialClient`、`updateSocialClient`、`deleteSocialClient` |
| `system/social/user/index.ts` | `/system/social-user` | `getSocialUserPage`、`getSocialUser` |
| `system/oauth2/client.ts` | `/system/oauth2-client` | `getOAuth2ClientPage`、`getOAuth2Client`、`createOAuth2Client`、`updateOAuth2Client`、`deleteOAuth2Client` |
| `system/oauth2/token.ts` | `/system/oauth2-token` | `getAccessTokenPage`、`deleteAccessToken` |
| `system/tenant/index.ts` | `/system/tenant` | `getTenantPage`、`getTenant`、`createTenant`、`updateTenant`、`deleteTenant`、`exportTenant` |
| `system/tenantPackage/index.ts` | `/system/tenant-package` | `getTenantPackagePage`、`getTenantPackageList`、`createTenantPackage`、`updateTenantPackage`、`deleteTenantPackage` |
| `system/area/index.ts` | `/system/area` | `getAreaTree`、`getAreaByIp` |
| `system/loginLog/index.ts` | `/system/login-log` | `getLoginLogPage`、`exportLoginLog` |
| `system/operatelog/index.ts` | `/system/operate-log` | `getOperateLogPage`、`exportOperateLog` |

#### 基础设施 `infra`

| 文件 | 接口前缀 | 代表方法 |
|---|---|---|
| `infra/config/index.ts` | `/infra/config` | `getConfigPage`、`getConfig`、`getConfigKey`、`createConfig`、`updateConfig`、`deleteConfig`、`exportConfig` |
| `infra/file/index.ts` | `/infra/file` | `getFilePage`、`deleteFile`、`getFilePresignedUrl`、`createFile`、`updateFile`（上传） |
| `infra/fileConfig/index.ts` | `/infra/file-config` | `getFileConfigPage`、`updateFileConfigMaster`、`createFileConfig`、`updateFileConfig`、`deleteFileConfig`、`testFileConfig` |
| `infra/job/index.ts` | `/infra/job` | `getJobPage`、`createJob`、`updateJob`、`deleteJob`、`updateJobStatus`、`runJob`、`getJobNextTimes`、`exportJob` |
| `infra/jobLog/index.ts` | `/infra/job-log` | `getJobLogPage`、`getJobLog`、`exportJobLog` |
| `infra/apiAccessLog/index.ts` | `/infra/api-access-log` | `getApiAccessLogPage`、`exportApiAccessLog` |
| `infra/apiErrorLog/index.ts` | `/infra/api-error-log` | `getApiErrorLogPage`、`updateApiErrorLogPage`（处理状态）、`exportApiErrorLog` |
| `infra/codegen/index.ts` | `/infra/codegen` | `getCodegenTablePage`、`getCodegenTable`、`createCodegenTable`、`updateCodegenTable`、`syncCodegenFromDB`、`previewCodegen`、`downloadCodegen`、`createCodegenList`、`deleteCodegenTable` |
| `infra/dataSourceConfig/index.ts` | `/infra/data-source-config` | `createDataSourceConfig`、`updateDataSourceConfig`、`deleteDataSourceConfig`、`getDataSourceConfig`、`getDataSourceConfigList` |
| `infra/redis/index.ts` | `/infra/redis` | `getCache`（Redis 监控信息） |
| `infra/demo/demo01` | `/infra/demo01-contact` | 单表 CRUD 示例 |
| `infra/demo/demo02` | `/infra/demo02-category` | 树表 CRUD 示例 |
| `infra/demo/demo03/{normal,inner,erp}` | `/infra/demo03-student` | 主子表示例（普通/内嵌/ERP） |

#### 商城业务 `mall`

| 文件 | 接口前缀 | 代表方法 |
|---|---|---|
| `mall/store/shop/index.ts` | `/store/shop` | `getShopList`、`getShopPage`、`getShop`、`createShop`、`updateShop`、`deleteShop`、`exportShop` |
| `mall/product/product.ts` | `/product/store-product` | `getStoreProductPage`、`getStoreProduct`、`getStoreProductInfo`、`createStoreProduct`、`updateStoreProduct`、`deleteStoreProduct`、`exportStoreProduct`、`isFormatAttr`、`saleStoreProduct` |
| `mall/product/category.ts` | `/product/category` | `createCategory`、`updateCategory`、`deleteCategory`、`getCategory`、`getCategoryList` |
| `mall/product/brand.ts` | `/product/brand` | `createBrand`、`updateBrand`、`deleteBrand`、`getBrand`、`getBrandParam` |
| `mall/product/storeProductRelation/index.ts` | `/product/store-product-relation` | 商品关联 CRUD/导出 |
| `mall/product/storeProductReply/index.ts` | `/product/store-product-reply` | 商品评论 CRUD/导出 |
| `mall/shop/ads/index.ts` | `/shop/ads` | 广告位 CRUD/导出 |
| `mall/shop/recharge/index.ts` | `/shop/recharge` | 充值配置 CRUD/导出 |
| `mall/shop/service/index.ts` | `/shop/service` | 客服/服务配置 CRUD/导出 |
| `mall/shop/storeProductRule/index.ts` | `/product/store-product-rule` | 商品规格规则 CRUD/导出 |
| `mall/shop/materialGroup/index.ts` | `/shop/material-group` | 素材分组 CRUD/导出 |
| `mall/order/storeOrder/index.ts` | `/order/store-order` | `getStoreOrderPage`、`getStoreOrder`、`createStoreOrder`、`updateStoreOrder`、`deleteStoreOrder`、`payStoreOrder`、`takeStoreOrder`、`rufundStoreOrder`、`getStoreOrderRecordList`、`exportStoreOrder`、`getLogistic`、`getOrderHtml`、`getShopCount`、`orderNoticeUrl` |
| `mall/coupon/index.ts` | `/coupon` | `getCouponList`、`getCouponPage`、`getCoupon`、`createCoupon`、`updateCoupon`、`deleteCoupon`、`exportCoupon` |
| `mall/coupon/user/index.ts` | `/coupon/user` | `getUserList`、`getUserPage`、`getUser`、`createUser`、`updateUser`、`deleteUser`、`exportUser` |

#### 会员 `member`

| 文件 | 接口前缀 | 代表方法 |
|---|---|---|
| `member/user/index.ts` | `/member/user` | `getUserPage`、`getUser`、`createUser`、`updateUser`、`updateMony`（余额调整）、`deleteUser`、`exportUser` |
| `member/userAddress/index.ts` | `/member/user-address` | `getUserAddressPage`、`getUserAddress`、`createUserAddress`、`updateUserAddress`、`deleteUserAddress`、`exportUserAddress` |
| `member/userBill/index.ts` | `/member/user-bill` | `getUserBillPage`、`getUserBill`、`createUserBill`、`updateUserBill`、`deleteUserBill`、`exportUserBill` |

#### 积分商城 `score`

| 文件 | 接口前缀 | 代表方法 |
|---|---|---|
| `score/product/index.ts` | `/score/product` | `getProductPage`、`getProduct`、`createProduct`、`updateProduct`、`deleteProduct`、`exportProduct` |
| `score/order/index.ts` | `/score/order` | `getOrderPage`、`getOrder`、`createOrder`、`updateOrder`、`deleteOrder`、`exportOrder`、`getLogistic`、`takeStoreOrder` |

#### 微信公众号/小程序 `mp`

| 文件 | 接口前缀 | 代表方法 |
|---|---|---|
| `mp/account/index.ts` | `/mp/account` | 公众号账号增删改查、`getSimpleAccountList`、`generateAccountQrCode`、`clearAccountQuota`、`setAccountMain` |
| `mp/account2/index.ts` | `/ma/account` | 小程序账号增删改查、`setAccountMain` |
| `mp/autoReply/index.ts` | `/mp/auto-reply` | 自动回复 CRUD |
| `mp/draft/index.ts` | `/mp/draft` | 草稿箱分页、创建、更新、删除 |
| `mp/freePublish/index.ts` | `/mp/free-publish` | 发布记录分页、删除、提交发布 |
| `mp/material/index.ts` | `/mp/material` | 素材分页、删除永久素材 |
| `mp/menu/index.ts` | `/mp/menu` | 自定义菜单查询、保存、删除 |
| `mp/message/index.ts` | `/mp/message` | 消息分页、发送消息 |
| `mp/statistics/index.ts` | `/mp/statistics` | 上行消息、用户增减、累计用户、接口统计 |
| `mp/tag/index.ts` | `/mp/tag` | 标签 CRUD、`getSimpleTagList`、`syncTag` |
| `mp/user/index.ts` | `/mp/user` | 粉丝分页、详情、更新、同步 |

#### 其他业务域

| 文件 | 接口前缀 | 代表方法 |
|---|---|---|
| `pay/merchantDetails/index.ts` | `/pay/merchant-details` | 支付商户参数 CRUD/导出 |
| `message/wechatTemplate/index.ts` | `/message/wechat-template` | 微信模板消息配置 CRUD/导出 |
| `express/index.ts` | `/order/express` | `getExpressList`、`getExpressPage`、`getExpress`、`getExpressSet`、`postExpressSet`、`createExpress`、`updateExpress`、`deleteExpress`、`exportExpress` |
| `tools/material.js` | `/shop/material` | 素材分页、新增、详情、删除、修改（供素材库组件使用） |
| `tools/materialgroup.js` | `/shop/material-group` | 素材分组分页、列表、新增、详情、删除、修改 |

> `api/mall/product/brand.ts` 目前没有对应页面，也未在 `src` 中被引用。

## 7. 状态管理

`src/store/index.ts` 创建 Pinia 并注册 `pinia-plugin-persistedstate`。

| Store | 文件 | 关键 state | 关键 getter/action | 持久化 |
|---|---|---|---|---|
| `app` | `store/modules/app.ts` | 布局 `layout`、主题 `theme`、暗黑 `isDark`、组件尺寸、菜单折叠、面包屑/标签页/页脚开关、`mobile`、`pageLoading` | `setLayout`、`setIsDark`、`setTheme`、`setCssVarTheme`、`setCollapse`、`setPageLoading`、`setMobile` | `persist: false`，但 layout/theme/isDark/currentSize/fixedMenu 单独写入 localStorage |
| `admin-user` | `store/modules/user.ts` | `permissions`、`roles`、`isSetUser`、`user {id, avatar, nickname, deptId}` | `setUserInfoAction`（读取/请求用户信息并写缓存）、`loginOut`、`resetState`、`setUserAvatarAction`、`setUserNicknameAction` | `persist: false`，用户信息写入 localStorage |
| `permission` | `store/modules/permission.ts` | `routers`、`addRouters`、`menuTabRouters` | `generateRoutes`（读取缓存菜单生成路由）、`setMenuTabRouters`；getter `getAddRouters` 做多级路由扁平化 | `persist: false` |
| `dict` | `store/modules/dict.ts` | `dictMap: Map<string, DictValueType[]>`、`isSetDict` | `setDictMap`、`getDictByType`、`resetDict` | 使用 **sessionStorage**，缓存键 `dictCache`，60 秒过期 |
| `locales` | `store/modules/locale.ts` | `currentLocale`、`localeMap`（zh-CN / en） | `setCurrentLocale` | localStorage 保存 `lang` |
| `tagsView` | `store/modules/tagsView.ts` | `visitedViews`、`cachedViews: Set<string>` | `addView`、`delView`、`delAllViews`、`delOthersViews`、`delLeftViews`、`delRightViews`、`updateVisitedView` | `persist: false` |
| `lock` | `store/modules/lock.ts` | `lockInfo {isLock, password}` | `setLockInfo`、`unLock`、`resetLockInfo` | `persist: true` |
| `simpleWorkflow` | `store/modules/simpleWorkflow.ts` | 审批流设计器的抽屉开关、审批人/抄送人/条件配置 | 各类 `setXxx` action | 未声明持久化 |

**缓存键（`src/hooks/web/useCache.ts`）**：

| 键 | 内容 | 存储 |
|---|---|---|
| `user`（`CACHE_KEY.USER`） | 用户信息、permissions、roles | localStorage |
| `roleRouters`（`CACHE_KEY.ROLE_ROUTERS`） | 后端返回的菜单树 | localStorage |
| `isDark` / `theme` / `layout` / `lang` / `fixedMenu` / `currentSize` | UI 偏好 | localStorage |
| `dictCache` | 字典数据 | sessionStorage |
| `loginForm` | “记住我”登录表单（密码经 RSA 加密） | localStorage，30 天 |
| `tenantId` | 当前租户编号 | localStorage |
| `ACCESS_TOKEN` / `REFRESH_TOKEN` | 令牌 | localStorage |

> `v-hasPermi`、`v-hasRole`、首页快捷入口等直接从 localStorage 读取 `user` 缓存，而不是从 Pinia 读取。缓存被清空但页面未刷新时可能出现异常。

## 8. 页面与功能模块

> 以下页面均真实存在于 `src/views`。最终是否出现在侧边栏，取决于后端 `system_menu` 是否配置了对应菜单记录。

### 8.1 首页、登录、个人中心、错误页

| 页面 | 文件 | 功能 |
|---|---|---|
| 首页仪表盘 | `views/Home/Index.vue` | 展示会员总数、今日订单数、订单总数、昨日订单数、总金额、近七天订单数、商品总数、本月订单数；调用 `/order/store-order/count` 与公告分页；包含快捷入口 `PanelGroupT` |
| 快捷入口 | `views/Home/PanelGroupT.vue` | 商品、会员、订单、优惠券、打印机、门店、支付、消息等入口；当 `user.shopId > 0`（门店账号）时隐藏 |
| 备用仪表盘 | `views/Home/Index2.vue` | 通用统计卡片模板，当前静态路由未引用 |
| 登录页 | `views/Login/Login.vue` | 左右分栏登录；当前只挂载账号密码登录 `LoginForm` |
| 账号登录 | `views/Login/components/LoginForm.vue` | 租户名（可选）、用户名、密码、记住我、滑块/点选验证码、登录 |
| 其他登录表单 | `views/Login/components/MobileForm.vue`、`QrCodeForm.vue`、`RegisterForm.vue`、`SSOLogin.vue` | 手机号、二维码、注册、SSO 表单文件存在，但当前 `Login.vue` 中已注释，未启用 |
| 社交登录 | `views/Login/SocialLogin.vue` | 社交授权登录/回调页，路由 `/social-login` |
| 个人中心 | `views/Profile/Index.vue` | 左侧用户卡片；右侧“基本信息 / 修改密码 / 社交账号绑定”三个标签页 |
| 错误页 | `views/Error/403.vue`、`404.vue`、`500.vue` | 无权限、未找到、服务错误 |
| 重定向页 | `views/Redirect/Redirect.vue` | 根据 `path` 或 `name` 参数执行 `replace` |

### 8.2 商城业务 `views/mall`

| 目录 | 页面文件 | 功能 |
|---|---|---|
| `store/shop` | `index.vue`、`ShopForm.vue`、`map.vue` | 门店列表、新增/编辑门店（名称、电话、图片、经纬度、配送距离、起送价、配送费、营业状态、营业时间等）；`map.vue` 使用百度地图选点 |
| `product/storeProduct` | `index.vue`、`StoreProductForm.vue`、`CateTree.vue` | 商品列表（左侧分类树、按门店/名称筛选、上下架、编辑、删除）；商品表单含基本信息、单/多规格、库存、图片、详情等 |
| `product/category` | `index.vue`、`CategoryForm.vue` | 商品分类树/列表与新增编辑 |
| `product/storeProductRelation` | `index.vue` | 商品关联关系管理 |
| `product/storeProductReply` | `index.vue` | 商品评论管理 |
| `shop/ads` | `index.vue`、`AdsForm.vue` | 广告/轮播图管理（图片、可用状态、权重、展示门店） |
| `shop/recharge` | `index.vue`、`RechargeForm.vue` | 充值规则/充值配置管理 |
| `shop/service` | `index.vue`、`ServiceForm.vue` | 客服/服务入口配置（标题、图标、类型、内容等） |
| `shop/storeProductRule` | `index.vue`、`StoreProductRuleForm.vue` | 商品规格规则（SKU 规格模板）管理 |
| `order/storeOrder` | `index.vue`、`OrderDetail.vue`、`OrderRecord.vue`、`OrderSend.vue`、`OrderSendInfo.vue`、`StoreOrderForm.vue`、`StoreOrderRefund.vue`、`StoreOrderRemark.vue`、`work.vue` | 订单列表（全部/外卖/自取标签，未支付/制作中/待收货/已收货/退款/已删除状态，按订单号、姓名、电话、时间筛选）；订单详情、操作记录、发货、发货信息、退款、备注、订单打印/工作台等 |
| `coupon` | `index.vue`、`Form.vue`、`user/OrderRecord.vue` | 优惠券列表与新增编辑；`user/OrderRecord.vue` 查看某优惠券的领取/使用记录 |
| `member/user` | `index.vue`、`UserDetail.vue`、`UserForm.vue`、`yue.vue` | 会员列表（微信小程序/公众号/H5 标签）、会员详情（含账单 `userBill`）、新增编辑、余额调整 |
| `member/userAddress` | `index.vue`、`UserAddressForm.vue` | 会员收货地址管理 |

### 8.3 积分商城 `views/score`

| 目录 | 页面文件 | 功能 |
|---|---|---|
| `product` | `index.vue`、`ProductForm.vue` | 积分商品列表（标题、主图、消耗积分、权重、库存、销量、上下架）与新增编辑 |
| `order` | `index.vue`、`OrderDetail.vue`、`OrderForm.vue` | 积分订单列表、详情、新增/编辑；支持物流查询、确认收货 |

### 8.4 快递与支付

| 目录 | 页面文件 | 功能 |
|---|---|---|
| `views/express` | `index.vue`、`ExpressForm.vue`、`ExpressSet.vue` | 快递公司列表与 CRUD；快递查询/打印相关配置 |
| `views/pay/merchantDetails` | `index.vue`、`MerchantDetailsForm.vue` | 支付商户参数（支付渠道、appid、mchId、回调地址、签名方式、测试环境）管理 |

### 8.5 消息与微信公众号/小程序 `views/mp`

| 目录 | 页面文件 | 功能 |
|---|---|---|
| `message/wechatTemplate` | `index.vue`、`WechatTemplateForm.vue` | 微信模板消息配置（模板编号、模板名等） |
| `mp/account` | `index.vue`、`AccountForm.vue` | 微信公众号账号管理：授权、二维码、清空配额、设为主账号 |
| `mp/account2` | `index.vue`、`AccountForm.vue` | 微信小程序账号管理：appId、主账号设置 |
| `mp/autoReply` | `index.vue`、`components/ReplyForm.vue`、`ReplyTable.vue` | 公众号自动回复规则（文本/图片/图文等） |
| `mp/draft` | `index.vue`、`components/NewsForm.vue`、`DraftTable.vue`、`CoverSelect.vue` | 公众号草稿箱与图文素材编辑 |
| `mp/freePublish` | `index.vue` | 公众号发布记录与发布操作 |
| `mp/material` | `index.vue`、`components/ImageTable.vue`、`VideoTable.vue`、`VoiceTable.vue`、`UploadFile.vue`、`UploadVideo.vue` | 公众号永久素材管理（图片/视频/语音） |
| `mp/menu` | `index.vue`、`components/MenuEditor.vue`、`MenuPreviewer.vue` | 公众号自定义菜单编辑与预览 |
| `mp/message` | `index.vue`、`MessageTable.vue` | 公众号消息列表与发送 |
| `mp/statistics` | `index.vue` | 用户增减、累计用户、接口调用、上行消息等 ECharts 统计 |
| `mp/tag` | `index.vue`、`TagForm.vue` | 公众号用户标签管理与同步 |
| `mp/user` | `index.vue`、`UserForm.vue` | 公众号粉丝列表、详情、同步 |
| `mp/components` | `wx-*` 组件 | 消息编辑器、素材选择、位置选择、语音/视频播放等复用组件 |

### 8.6 系统管理 `views/system`

| 目录 | 页面文件 | 功能 |
|---|---|---|
| `user` | `index.vue`、`UserForm.vue`、`UserImportForm.vue`、`UserAssignRoleForm.vue`、`DeptTree.vue` | 用户列表（部门树过滤、搜索、状态切换）、新增编辑、Excel 导入、分配角色 |
| `role` | `index.vue`、`RoleForm.vue`、`RoleAssignMenuForm.vue`、`RoleDataPermissionForm.vue` | 角色 CRUD、分配菜单权限、分配数据权限 |
| `menu` | `index.vue`、`MenuForm.vue` | 菜单树管理；表单含类型（目录/菜单/按钮）、路由地址、组件地址、组件名、权限标识、排序、显示/隐藏、缓存、总是显示 |
| `dept` | `index.vue`、`DeptForm.vue` | 部门树管理 |
| `post` | `index.vue`、`PostForm.vue` | 岗位管理 |
| `dict` | `index.vue`、`DictTypeForm.vue`、`data/index.vue`、`data/DictDataForm.vue` | 字典类型与字典数据管理 |
| `notice` | `index.vue`、`NoticeForm.vue` | 通知公告管理、推送 |
| `notify/message` | `index.vue`、`NotifyMessageDetail.vue` | 站内信消息列表与详情 |
| `notify/my` | `index.vue`、`MyNotifyMessageDetail.vue` | 我的站内信（静态路由 `/user/notify-message`） |
| `notify/template` | `index.vue`、`NotifyTemplateForm.vue`、`NotifyTemplateSendForm.vue` | 站内信模板管理与发送 |
| `mail/account` | `index.vue`、`MailAccountForm.vue`、`MailAccountDetail.vue` | 邮箱账号管理 |
| `mail/template` | `index.vue`、`MailTemplateForm.vue`、`MailTemplateSendForm.vue` | 邮件模板与发送 |
| `mail/log` | `index.vue`、`MailLogDetail.vue` | 邮件发送日志 |
| `sms/channel` | `index.vue`、`SmsChannelForm.vue` | 短信渠道管理 |
| `sms/template` | `index.vue`、`SmsTemplateForm.vue`、`SmsTemplateSendForm.vue` | 短信模板与发送 |
| `sms/log` | `index.vue`、`SmsLogDetail.vue` | 短信发送日志 |
| `social/client` | `index.vue`、`SocialClientForm.vue` | 社交登录客户端（钉钉、企业微信等） |
| `social/user` | `index.vue`、`SocialUserDetail.vue` | 社交用户绑定关系 |
| `oauth2/client` | `index.vue`、`ClientForm.vue` | OAuth2 客户端管理 |
| `oauth2/token` | `index.vue` | 访问令牌列表与强制下线 |
| `tenant` | `index.vue`、`TenantForm.vue` | 租户管理 |
| `tenantPackage` | `index.vue`、`TenantPackageForm.vue` | 租户套餐管理 |
| `area` | `index.vue`、`AreaForm.vue` | 地区树、IP 归属地查询（虚拟化表格） |
| `loginlog` | `index.vue`、`LoginLogDetail.vue` | 登录日志 |
| `operatelog` | `index.vue`、`OperateLogDetail.vue` | 操作日志 |

### 8.7 基础设施 `views/infra`

| 目录 | 页面文件 | 功能 |
|---|---|---|
| `config` | `index.vue`、`ConfigForm.vue` | 参数配置管理 |
| `file` | `index.vue`、`FileForm.vue` | 文件列表、上传、删除 |
| `fileConfig` | `index.vue`、`FileConfigForm.vue` | 文件存储配置（本地、S3、MinIO 等）、设为主配置、测试 |
| `job` | `index.vue`、`JobForm.vue`、`JobDetail.vue`、`logger/index.vue`、`logger/JobLogDetail.vue` | 定时任务 CRUD、启停、立即执行、下次执行时间、调度日志 |
| `apiAccessLog` | `index.vue`、`ApiAccessLogDetail.vue` | API 访问日志与详情 |
| `apiErrorLog` | `index.vue`、`ApiErrorLogDetail.vue` | API 异常日志、处理状态 |
| `codegen` | `index.vue`、`ImportTable.vue`、`EditTable.vue`、`PreviewCode.vue`、`components/*Form.vue` | 数据库表导入、代码生成配置、预览与下载 |
| `dataSourceConfig` | `index.vue`、`DataSourceConfigForm.vue` | 代码生成数据源配置 |
| `build` | `index.vue` | `@form-create/designer` 表单设计器，可生成 JSON / Options / 组件代码 |
| `redis` | `index.vue` | Redis 基本信息、命令统计、内存/Key 监控 |
| `server` | `index.vue` | 内嵌 Spring Boot Admin（`VITE_BASE_URL + /admin/applications`） |
| `druid` | `index.vue` | 内嵌 Druid 监控页（可通过配置键 `url.druid` 覆盖） |
| `swagger` | `index.vue` | 内嵌 Knife4j / Swagger 文档（配置键 `url.swagger`） |
| `skywalking` | `index.vue` | 内嵌 SkyWalking（配置键 `url.skywalking`） |
| `webSocket` | `index.vue` | WebSocket 连接测试、选择用户发送消息、查看消息记录 |
| `demo/demo01` | `index.vue`、`Demo01ContactForm.vue` | 单表 CRUD 示例 |
| `demo/demo02` | `index.vue`、`Demo02CategoryForm.vue` | 树表 CRUD 示例 |
| `demo/demo03/normal`、`inner`、`erp` | 各自 `index.vue` 与表单组件 | 主子表 CRUD 示例（普通/内嵌/ERP 三种交互） |

> `infra/build`、`infra/demo/*`、`infra/druid`、`infra/server`、`infra/skywalking`、`infra/swagger`、`infra/webSocket` 属于通用后台能力，是否展示由后端菜单决定。

## 9. 权限控制

### 9.1 登录鉴权

- 令牌保存在 localStorage：`ACCESS_TOKEN`、`REFRESH_TOKEN`。
- 每个请求在请求拦截器中注入 `Authorization: Bearer <accessToken>`。
- 路由守卫 `src/permission.ts`：
  - 没有 token 且目标不在白名单 → 跳转 `/login?redirect=<fullPath>`。
  - 有 token 且访问 `/login` → 跳转 `/`。
  - 有 token 且首次进入 → 拉取用户权限信息、生成并注册动态路由。
- 登录成功后保存 token，然后 `router.push` 到 `redirect` 或首页；路由守卫补充用户信息与动态路由。

### 9.2 用户信息与菜单权限

`GET /system/auth/get-permission-info` 返回：

| 字段 | 含义 | 前端用途 |
|---|---|---|
| `user` | 管理员基础信息（id、nickname、avatar、deptId、shopId） | 顶部用户信息、门店账号隐藏快捷入口 |
| `roles` | 角色标识数组（如 `admin`、`common`） | `v-hasRole` |
| `permissions` | 操作权限标识数组（如 `system:user:create`） | `v-hasPermi` |
| `menus` | 菜单树（已剔除按钮类型，按 sort 排序） | 动态路由与侧边栏 |

后端角色-菜单关系通过 `system/permission` 接口维护：

- `GET /system/permission/list-role-menus?roleId=`
- `POST /system/permission/assign-role-menu`
- `POST /system/permission/assign-role-data-scope`
- `GET /system/permission/list-user-roles?userId=`
- `POST /system/permission/assign-user-role`

### 9.3 按钮级权限指令

定义：`src/directives/permission/hasPermi.ts`、`hasRole.ts`，在 `src/directives/index.ts` 中注册为 `v-hasPermi`、`v-hasRole`。

| 指令 | 用法 | 规则 |
|---|---|---|
| `v-hasPermi` | `v-hasPermi="['system:user:create']"` | 从 localStorage `user.permissions` 读取；拥有 `*:*:*` 或数组中任一权限则保留元素，否则从 DOM 移除 |
| `v-hasRole` | `v-hasRole="['admin']"` | 从 localStorage `user.roles` 读取；拥有 `admin` 或数组中任一角色则保留，否则移除 |

实现细节与限制：

- 绑定值**必须是数组且非空**，否则直接 `throw new Error`。
- 指令只读取 localStorage 中的 `user` 缓存，不读取 Pinia；若缓存缺失会抛异常。
- 指令是“移除 DOM”的一次性判断，不会响应权限变化。
- 权限字符串必须与后端 `@PreAuthorize("@ss.hasPermission('...')")` 完全一致。

项目中实际使用的权限标识示例：

| 页面 | 权限标识 |
|---|---|
| 系统用户 | `system:user:create`、`system:user:import`、`system:user:export` |
| 系统角色 | `system:role:create`、`system:role:export` |
| 门店 | `store:shop:create` |
| 商品 | `shop:store-product:create/update/delete` |
| 订单快递 | `order:express:create/export` |
| 支付商户 | `pay:merchant-details:create/update` |
| 积分商品 | `score:product:create` |
| 优惠券 | `coupon::create`（源码中即为双冒号，配置菜单权限时需与之一致） |
| 公众号 | `mp:account:create` |
| 小程序 | `ma:account:create` |

> 权限前缀并不完全统一（`store:shop:*`、`shop:store-product:*`、`coupon::*` 等并存），新增页面时应以对应后端 Controller 的权限字符串为准，不要按目录名臆测。

### 9.4 菜单权限

- 侧边栏只渲染 `meta.hidden !== true` 的路由。
- 后端 `visible=false` 的菜单仍会生成路由并可通过 URL 访问，但不会出现在侧边栏（与菜单表单提示一致）。
- 按钮类型菜单不进入路由树，只贡献 `permissions`。
- `v-hasPermi` 控制页面内按钮是否可见。

## 10. 核心业务流程

### 10.1 登录流程

```text
用户打开 /login
  ↓
LoginForm 读取“记住我”表单（localStorage，密码 RSA 解密）
  ↓
用户输入租户名（可选）、用户名、密码
  ↓
点击登录：
  1. 若 VITE_APP_TENANT_ENABLE=true：
     GET /system/tenant/get-id-by-name?name=<tenantName>
     将租户 ID 写入 localStorage
  2. 表单校验
  3. 若 VITE_APP_CAPTCHA_ENABLE=true：
     弹出 Verify 组件（默认 blockPuzzle 滑块）
     验证成功后回调 handleLogin({ captchaVerification })
  4. POST /system/auth/login
  5. 保存 accessToken / refreshToken
  6. 记住我则保存表单，否则清除
  7. 跳转 redirect 参数指定路径，否则跳转首页
  8. 路由守卫发现 isSetUser=false：
     GET /system/auth/get-permission-info
     写入 permissions / roles / user / menus
     生成动态路由并 addRoute
  9. 再次进入目标页
```

验证码未开启时（`VITE_APP_CAPTCHA_ENABLE === 'false'`）直接调用 `handleLogin({})`。

### 10.2 动态菜单加载流程

```text
登录成功 / 刷新页面
  ↓
permission.ts beforeEach
  ↓
dictStore.getIsSetDict?
  ├─ 否 → sessionStorage 有 dictCache 则复用，否则 GET /system/dict-data/simple-list 并缓存 60s
  ↓
userStore.getIsSetUser?
  ├─ 是 → 直接 next()
  └─ 否 →
       GET /system/auth/get-permission-info
       userStore.permissions / roles / user ← 响应
       localStorage.user ← 响应
       localStorage.roleRouters ← response.menus
       permissionStore.generateRoutes()
         generateRoute(menus) 递归转换
         追加 404 兜底
         addRouters = 待注册路由
         routers = remainingRouter + 动态路由
       forEach router.addRoute()
       计算 redirect（from.query.redirect 或 to.path）
       next(redirect)
```

### 10.3 Token 刷新流程

```text
任意业务请求
  ↓
响应 code=401
  ↓
isRefreshToken?
  ├─ 否 →
  │   refreshToken 存在?
  │   ├─ 否 → handleAuthorized()（确认后清 token、清缓存、重置路由、整页刷新）
  │   └─ 是 → POST {baseURL}/system/auth/refresh-token?refreshToken=...
  │            ├─ 成功 → setToken(新 token)
  │            │         更新当前请求 Authorization
  │            │         执行 requestList 队列
  │            │         重放当前请求
  │            └─ 失败 → 执行队列回调
  │                      handleAuthorized()
  └─ 是 → 当前请求加入 requestList
         等待刷新完成后携带新 token 重放
```

### 10.4 退出登录流程

顶部用户菜单 → 退出登录：

1. `POST /system/auth/logout`。
2. 删除 `ACCESS_TOKEN`、`REFRESH_TOKEN`。
3. 删除 `user`、`roleRouters` 缓存（保留“记住我”表单）。
4. 重置 user store。
5. 清空标签页。
6. 跳转 `/login?redirect=/index`。

### 10.5 字典加载流程

`dictStore.setDictMap()`：

1. 优先读取 sessionStorage `dictCache`。
2. 无缓存时请求 `GET /system/dict-data/simple-list`。
3. 按 `dictType` 聚合成 `Map<dictType, {value,label,colorType,cssClass}[]>`。
4. 写入 sessionStorage，有效期 60 秒。
5. 页面通过 `getDictOptions`、`getIntDictOptions`、`getStrDictOptions`、`getBoolDictOptions`、`getDictLabel` 或 `DictTag` 组件使用。

### 10.6 文件上传流程

`src/components/UploadFile/src/useUpload.ts`：

| `VITE_UPLOAD_TYPE` | 流程 |
|---|---|
| `server` | 调用 `POST /infra/file/upload`（`multipart/form-data`），后端保存并返回文件信息；成功判断 `res.code === 0` |
| `client` | 1. 用 SHA-256 生成文件名；2. `GET /infra/file/presigned-url?path=` 获取预签名地址；3. 直接 `axios.put` 到对象存储；4. 异步 `POST /infra/file/create` 登记文件信息 |

## 11. 代码约定与注意事项

### 11.1 开发约定

1. **路径别名**：`@/` → `src/`（`vite.config.ts` 与 `tsconfig.json` 均已配置）。
2. **自动导入**：`unplugin-auto-import` 自动导入 Vue、Vue Router 及项目内 `useI18n`、`useMessage`、`useTable`、`useCrudSchemas`、`required`、`DICT_TYPE`；`unplugin-vue-components` 自动导入 Element Plus 与 `src/components` 下的组件。新增公共组件时通常无需手动 `import`。
3. **页面命名**：为支持 `keep-alive`，页面应通过 `defineOptions({ name: 'Xxx' })`（或 `<script setup name="Xxx">`）声明名称，并与菜单 `componentName` / 路由 `name` 保持一致。
4. **接口组织**：按后端业务域在 `src/api` 下建目录；方法名统一使用 `getXxxPage`、`getXxx`、`createXxx`、`updateXxx`、`deleteXxx`、`exportXxx`。
5. **列表页模式**：页面自行维护 `queryParams`、`list`、`loading`、`total`，使用 `ContentWrap` + `el-form` + `el-table` + `Pagination`；通用 `useTable` hook 也已提供，但多数业务页未使用。
6. **表单弹窗**：列表页与 `XxxForm.vue` 通过 `ref` 的 `open(type, id)` 方法交互。
7. **字典**：优先使用 `DICT_TYPE` 常量与 `getIntDictOptions` / `DictTag`，不要硬编码枚举值。
8. **国际化**：文案通过 `useI18n()` 的 `t()`；当前主要页面以中文硬编码为主，`locales/zh-CN.ts` 与 `en.ts` 覆盖通用文案。
9. **样式**：SCSS 变量在 `src/styles/variables.scss` 中全局注入；原子类使用 UnoCSS。
10. **提交前检查**：`pnpm lint:eslint`、`pnpm lint:style`、`pnpm ts:check`。

### 11.2 新增后台页面的完整步骤

1. 在 `src/views/<module>/<page>/index.vue` 创建页面（可附 `XxxForm.vue`）。
2. 在 `src/api/<module>/` 添加接口方法。
3. 在系统菜单中新增菜单记录：
   - 类型：菜单（`type=2`）；
   - 路由地址：与父级路径组合后的相对路径；
   - 组件地址：`<module>/<page>/index`，必须能匹配 `src/views/**` 下的文件；
   - 组件名：与页面 `name` 一致，用于 `keep-alive`；
   - 权限标识：与后端 `@PreAuthorize` 一致；
   - 是否缓存、是否可见、排序、图标。
4. 为按钮配置按钮类型菜单及权限标识，并在页面按钮上添加 `v-hasPermi`。
5. 给角色分配菜单权限后重新登录（或清空 `roleRouters` 缓存），动态路由才会更新。

### 11.3 已知问题与风险点

| 位置 | 问题 | 影响/建议 |
|---|---|---|
| `src/config/axios/service.ts` | 白名单回调对任意有 `url` 的请求都执行 `isToken = false`，未真正判断 URL 是否命中白名单 | 白名单失效，登录/刷新请求可能携带旧 token；不建议依赖该白名单行为 |
| `src/config/axios/service.ts` | 成功码使用 `data.code || result_code`，后端成功码 `0` 被替换成 `200` | 当前可正常工作；若后端调整成功码需同步修改 |
| `src/utils/routerHelper.ts` `getRedirect` | 目录的直接子菜单为叶子页面时没有 `return path`，`redirect` 为 `undefined` | 访问目录根路径可能显示空白内容区；建议直接访问叶子路由或修复函数 |
| `src/router/index.ts` `resetRouter` | 保留名单包含 `Root`，但静态首页名称为 `Home` | 重置时首页路由被移除；当前靠整页刷新恢复 |
| `src/store/modules/permission.ts` | 动态路由只读 `roleRouters` 缓存，缓存丢失且 token 仍有效时动态路由为空 | 清理浏览器缓存或重新登录；可考虑在 `generateRoutes` 中兜底重新拉取 |
| `src/directives/permission/*` | 指令直接读 localStorage 的 `user`；绑定值必须是数组 | 缓存缺失时报错；非数组绑定会抛异常 |
| `src/views/Home/PanelGroupT.vue` | 直接读取 `user.user.shopId` | 依赖后端返回 `shopId`；若缓存结构变化会报错 |
| `src/views/mall/coupon/index.vue` | 权限标识为 `coupon::create`（双冒号） | 配置菜单权限时需保持一致，否则按钮不显示 |
| `.env.prod` | `VITE_BASE_URL` 指向 `http://localhost:48081` | 生产部署前必须改为真实地址 |
| `.env.stage` | `VITE_BASE_PATH` 写成完整域名 | Vite `base` 应为路径（如 `/admin-ui-vue3/`） |
| `.env.local` | 对所有模式生效且当前指向本机后端 | 生产构建前移除或调整 |
| `src/api/mall/product/brand.ts` | 有接口无页面、无引用 | 如需品牌管理需新增页面与菜单 |
| `src/views/Home/Index2.vue` | 文件存在但静态路由未引用 | 如需启用应配置菜单或改为首页组件 |
| `Login.vue` | 手机/二维码/注册/SSO 表单已注释 | 当前仅账号密码登录可用 |

### 11.4 与后端的关键契约

| 契约 | 说明 |
|---|---|
| API 前缀 | 所有管理端接口位于 `/admin-api` 下 |
| 响应结构 | `{ code, data, msg }`；成功码为 `0`（前端做了 `0 -> 200` 的兼容） |
| 分页参数 | `pageNo`、`pageSize`（从 1 开始） |
| 分页响应 | `{ list, total }` |
| 权限信息 | `GET /system/auth/get-permission-info` 返回 `user/roles/permissions/menus` |
| 菜单字段 | `id/parentId/name/path/component/componentName/icon/visible/keepAlive/alwaysShow/children` |
| 按钮权限 | 菜单表 `permission` 字段与后端 `@ss.hasPermission(...)` 字符串一致 |
| Token | 请求头 `Authorization: Bearer <accessToken>` |
| 租户 | 启用时请求头 `tenant-id` |
| 文件上传 | 默认 `VITE_UPLOAD_URL` 指向 `/admin-api/infra/file/upload` |
