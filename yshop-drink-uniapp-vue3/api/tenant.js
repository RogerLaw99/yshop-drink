import api from './api'

/**
 * 获取可用的商户（租户）列表
 */
export function getTenantList() {
  return api.get('/tenant/list', {}, { login: false })
}
