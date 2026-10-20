# 扫码点餐二维码（本地测试）

> 二维码使用不可猜测的 `token`，桌号不再明文暴露；令牌可在管理端「门店管理 → 桌台管理」中重置。
> 当前地址指向开发机 `192.168.0.6:5173`，正式环境请替换为已备案的 HTTPS 域名。

## 二维码

| 文件 | 桌号 | 二维码名称 | 门店 |
|---|---|---|---|
| `desk-A01.png` | A01 | A01 | yshop（shopId=3） |
| `desk-A01-Left.png` | A01 | A01-Left | yshop（shopId=3） |
| `desk-A02.png` | A02 | A02 | yshop（shopId=3） |
| `desk-A03.png` | A03 | A03 | yshop（shopId=3） |

> `desk-A01.png` 和 `desk-A01-Left.png` 是**同一张桌的两个码**：
> 扫同一个码的人共享购物车/合并一单；扫不同码的人各自独立下单。

## 对应链接

```
http://192.168.0.6:5173/h5/#/pages/scan/scan?token=<A01 的 qr_token>
http://192.168.0.6:5173/h5/#/pages/scan/scan?token=<A02 的 qr_token>
http://192.168.0.6:5173/h5/#/pages/scan/scan?token=<A03 的 qr_token>
```

令牌保存在数据库 `yshop_store_desk.qr_token`，可通过管理端查看/重置。

## 使用步骤

1. 启动 H5：`npm run dev:h5`（监听 `0.0.0.0:5173`）
2. 手机与电脑连接同一 Wi-Fi
3. 微信「扫一扫」对应二维码
4. 扫码页会调用 `/app-api/store/desk/get?token=...` 校验桌台，成功后进入点餐页
5. 顶部显示「桌号 A01 · 堂食 N 人」，就餐人数可在结算页调整
6. 提交订单后，管理端「订单管理」可看到「堂食 / 桌号」

## 管理端生成二维码

1. 登录管理端 `http://localhost:80`
2. 进入「门店管理 → 桌台管理」
3. 新增/批量创建桌台
4. 点击「二维码」查看并下载

生产环境需在 `yshop-drink-vue3/.env.prod` 配置：

```
VITE_SCAN_H5_URL='https://your-domain.com/h5'
```

## 小程序码

当前二维码是 H5 链接。若要使用微信小程序码：

1. 使用微信 `wxacode.getUnlimited` 生成小程序码，`scene` 传 `token=<qr_token>`
2. 小程序冷启动时 `App.vue` 会自动解析 `scene` 并跳转扫码页
3. 需要已认证小程序 AppID、HTTPS 域名与 request 合法域名配置

## 数据库

```sql
SELECT id, shop_id, desk_number, area, capacity, status, qr_token
FROM yshop_store_desk WHERE deleted = 0;
```
