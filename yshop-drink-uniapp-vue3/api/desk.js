import api from './api'

/**
 * 根据二维码令牌获取桌台信息
 * @param {string} token 二维码令牌
 */
export function getDeskByToken(token) {
  return api.get('/store/desk/get', { token }, { login: false })
}

/**
 * 扫码加入/创建桌台共享会话
 */
export function joinDeskSession(token) {
  return api.get('/order/desk/session', { token }, { login: true })
}

/**
 * 获取桌台共享会话详情（轮询购物车）
 */
export function getDeskSession(sessionId) {
  return api.get('/order/desk/session/by-id', { sessionId }, { login: true })
}

/**
 * 添加商品到共享购物车
 */
export function addDeskCartItem(data) {
  return api.post('/order/desk/cart/add', data, { login: true })
}

/**
 * 修改共享购物车商品数量（0 表示删除）
 */
export function updateDeskCartItem(data) {
  return api.post('/order/desk/cart/update', data, { login: true })
}

/**
 * 删除共享购物车商品
 */
export function deleteDeskCartItem(id) {
  return api.delete('/order/desk/cart/delete?id=' + id, {}, { login: true })
}

/**
 * 清空共享购物车
 */
export function clearDeskCart(sessionId) {
  return api.post('/order/desk/cart/clear?sessionId=' + sessionId, {}, { login: true })
}

/**
 * 共享购物车结算，生成一笔订单
 */
export function checkoutDeskCart(data) {
  return api.post('/order/desk/checkout', data, { login: true })
}
