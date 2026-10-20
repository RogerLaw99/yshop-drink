import request from '@/config/axios'

export interface DeskVO {
  id?: number
  shopId: number
  deskNumber: string
  area?: string
  capacity?: number
  status?: number
  sort?: number
  qrToken?: string
  createTime?: Date
}

export interface DeskPageReqVO {
  pageNo: number
  pageSize: number
  shopId?: number
  deskNumber?: string
  area?: string
  status?: number
}

export interface DeskQrVO {
  id?: number
  deskId: number
  shopId?: number
  qrName: string
  qrToken?: string
  status?: number
  sort?: number
  createTime?: Date
}

export interface DeskBatchCreateReqVO {
  shopId: number
  prefix: string
  startNumber: number
  endNumber: number
  area?: string
  capacity?: number
}

// 查询桌台分页
export const getDeskPage = async (params: DeskPageReqVO) => {
  return await request.get({ url: `/store/desk/page`, params })
}

// 查询桌台详情
export const getDesk = async (id: number) => {
  return await request.get({ url: `/store/desk/get?id=` + id })
}

// 查询门店下的桌台
export const getDeskListByShop = async (shopId: number) => {
  return await request.get({ url: `/store/desk/list-by-shop?shopId=` + shopId })
}

// 新增桌台
export const createDesk = async (data: DeskVO) => {
  return await request.post({ url: `/store/desk/create`, data })
}

// 修改桌台
export const updateDesk = async (data: DeskVO) => {
  return await request.put({ url: `/store/desk/update`, data })
}

// 删除桌台
export const deleteDesk = async (id: number) => {
  return await request.delete({ url: `/store/desk/delete?id=` + id })
}

// 批量创建桌台
export const batchCreateDesk = async (data: DeskBatchCreateReqVO) => {
  return await request.post({ url: `/store/desk/batch-create`, data })
}

// 重置桌台默认二维码令牌
export const resetDeskToken = async (id: number) => {
  return await request.post({ url: `/store/desk/reset-token?id=` + id })
}

// ========== 一桌多码 ==========

// 查询桌台下的二维码列表
export const getDeskQrList = async (deskId: number) => {
  return await request.get({ url: `/store/desk/qr/list?deskId=` + deskId })
}

// 新增二维码
export const createDeskQr = async (data: DeskQrVO) => {
  return await request.post({ url: `/store/desk/qr/create`, data })
}

// 修改二维码
export const updateDeskQr = async (data: DeskQrVO) => {
  return await request.put({ url: `/store/desk/qr/update`, data })
}

// 删除二维码
export const deleteDeskQr = async (id: number) => {
  return await request.delete({ url: `/store/desk/qr/delete?id=` + id })
}

// 重置二维码令牌
export const resetDeskQrToken = async (id: number) => {
  return await request.post({ url: `/store/desk/qr/reset-token?id=` + id })
}
