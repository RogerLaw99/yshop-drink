<template>
  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form
      class="-mb-15px"
      :model="queryParams"
      ref="queryFormRef"
      :inline="true"
      label-width="78px"
    >
      <el-form-item label="门店" prop="shopId">
        <el-select v-model="queryParams.shopId" placeholder="全部门店" clearable class="!w-200px">
          <el-option v-for="item in shopList" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="桌号" prop="deskNumber">
        <el-input
          v-model="queryParams.deskNumber"
          placeholder="请输入桌号"
          clearable
          @keyup.enter="handleQuery"
          class="!w-200px"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable class="!w-160px">
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" @click="openForm('create')" v-hasPermi="['store:desk:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
        <el-button type="success" @click="openBatchForm" v-hasPermi="['store:desk:create']">
          <Icon icon="ep:files" class="mr-5px" /> 批量创建
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="编号" align="center" prop="id" width="80" />
      <el-table-column label="门店" align="center" width="160">
        <template #default="scope">{{ getShopName(scope.row.shopId) }}</template>
      </el-table-column>
      <el-table-column label="桌号" align="center" prop="deskNumber" width="120" />
      <el-table-column label="区域/楼层" align="center" prop="area" />
      <el-table-column label="座位数" align="center" prop="capacity" width="90" />
      <el-table-column label="排序" align="center" prop="sort" width="80" />
      <el-table-column label="状态" align="center" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
            {{ scope.row.status === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="170">
        <template #default="scope">{{ formatDate(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="300" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openQrDialog(scope.row)">二维码</el-button>
          <el-button
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
            v-hasPermi="['store:desk:update']"
          >
            编辑
          </el-button>
          <el-button
            link
            type="warning"
            @click="handleResetToken(scope.row)"
            v-hasPermi="['store:desk:update']"
          >
            重置令牌
          </el-button>
          <el-button
            link
            type="danger"
            @click="handleDelete(scope.row.id)"
            v-hasPermi="['store:desk:delete']"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>

  <!-- 新增/编辑弹窗 -->
  <Dialog :title="dialogTitle" v-model="dialogVisible" width="520px">
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="90px" v-loading="formLoading">
      <el-form-item label="门店" prop="shopId">
        <el-select v-model="formData.shopId" placeholder="请选择门店" class="w-full">
          <el-option v-for="item in shopList" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="桌号" prop="deskNumber">
        <el-input v-model="formData.deskNumber" placeholder="如 A01" maxlength="32" />
      </el-form-item>
      <el-form-item label="区域/楼层" prop="area">
        <el-input v-model="formData.area" placeholder="如 大厅、二楼" maxlength="64" />
      </el-form-item>
      <el-form-item label="座位数" prop="capacity">
        <el-input-number v-model="formData.capacity" :min="1" :max="100" />
      </el-form-item>
      <el-form-item label="排序" prop="sort">
        <el-input-number v-model="formData.sort" :min="0" :max="9999" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="formData.status">
          <el-radio :label="1">启用</el-radio>
          <el-radio :label="0">停用</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>

  <!-- 批量创建弹窗 -->
  <Dialog title="批量创建桌台" v-model="batchDialogVisible" width="520px">
    <el-form ref="batchFormRef" :model="batchFormData" :rules="batchFormRules" label-width="90px" v-loading="batchLoading">
      <el-form-item label="门店" prop="shopId">
        <el-select v-model="batchFormData.shopId" placeholder="请选择门店" class="w-full">
          <el-option v-for="item in shopList" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="桌号前缀" prop="prefix">
        <el-input v-model="batchFormData.prefix" placeholder="如 A" maxlength="16" />
      </el-form-item>
      <el-form-item label="起始序号" prop="startNumber">
        <el-input-number v-model="batchFormData.startNumber" :min="1" :max="9999" />
      </el-form-item>
      <el-form-item label="结束序号" prop="endNumber">
        <el-input-number v-model="batchFormData.endNumber" :min="1" :max="9999" />
      </el-form-item>
      <el-form-item label="区域/楼层" prop="area">
        <el-input v-model="batchFormData.area" placeholder="如 大厅" maxlength="64" />
      </el-form-item>
      <el-form-item label="座位数" prop="capacity">
        <el-input-number v-model="batchFormData.capacity" :min="1" :max="100" />
      </el-form-item>
      <el-alert type="info" :closable="false" show-icon>
        示例：前缀 A，序号 1~10，将生成 A01 ~ A10，已存在的桌号会自动跳过。
      </el-alert>
    </el-form>
    <template #footer>
      <el-button @click="submitBatchForm" type="primary" :disabled="batchLoading">确 定</el-button>
      <el-button @click="batchDialogVisible = false">取 消</el-button>
    </template>
  </Dialog>

  <!-- 二维码弹窗：一桌多码 -->
  <Dialog title="桌台二维码" v-model="qrDialogVisible" width="860px">
    <div class="mb-10px text-16px font-bold">
      {{ currentDesk?.deskNumber }}（{{ getShopName(currentDesk?.shopId) }}）
    </div>
    <el-form :inline="true" :model="qrFormData" class="mb-10px">
      <el-form-item label="名称">
        <el-input v-model="qrFormData.qrName" placeholder="如 A01-左" class="!w-180px" maxlength="64" />
      </el-form-item>
      <el-form-item label="排序">
        <el-input-number v-model="qrFormData.sort" :min="0" :max="9999" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="addQr">新增二维码</el-button>
      </el-form-item>
    </el-form>
    <el-table :data="qrList" v-loading="qrLoading" size="small">
      <el-table-column label="预览" width="120" align="center">
        <template #default="scope">
          <img v-if="qrImageMap[scope.row.id]" :src="qrImageMap[scope.row.id]" class="w-90px h-90px" />
        </template>
      </el-table-column>
      <el-table-column label="名称" prop="qrName" min-width="130" />
      <el-table-column label="令牌" min-width="180">
        <template #default="scope">
          <span class="text-12px break-all">{{ scope.row.qrToken }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80" align="center">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
            {{ scope.row.status === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" align="center">
        <template #default="scope">
          <el-button link type="primary" @click="downloadQr(scope.row)">下载</el-button>
          <el-button link type="warning" @click="handleResetQrToken(scope.row)">重置</el-button>
          <el-button link type="danger" @click="handleDeleteQr(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="mt-10px text-12px text-orange-500">
      同一桌台可配置多个二维码：扫同一个码的人共享购物车，不同码的订单互相独立。
      正式环境请把 VITE_SCAN_H5_URL 配置为已备案的 HTTPS 域名。
    </div>
    <template #footer>
      <el-button @click="qrDialogVisible = false">关 闭</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts" name="StoreDesk">
import QRCode from 'qrcode'
import { formatDate } from '@/utils/formatTime'
import * as DeskApi from '@/api/mall/store/desk'
import * as ShopApi from '@/api/mall/store/shop'

const message = useMessage()

const loading = ref(true)
const total = ref(0)
const list = ref<DeskApi.DeskVO[]>([])
const shopList = ref<any[]>([])
const queryParams = reactive<DeskApi.DeskPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  shopId: undefined,
  deskNumber: undefined,
  status: undefined
})
const queryFormRef = ref()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formData = ref<DeskApi.DeskVO>({
  shopId: undefined as any,
  deskNumber: '',
  area: '',
  capacity: 4,
  status: 1,
  sort: 0
})
const formRules = reactive({
  shopId: [{ required: true, message: '请选择门店', trigger: 'change' }],
  deskNumber: [{ required: true, message: '桌号不能为空', trigger: 'blur' }]
})
const formRef = ref()

const batchDialogVisible = ref(false)
const batchLoading = ref(false)
const batchFormRef = ref()
const batchFormData = ref<DeskApi.DeskBatchCreateReqVO>({
  shopId: undefined as any,
  prefix: 'A',
  startNumber: 1,
  endNumber: 10,
  area: '',
  capacity: 4
})
const batchFormRules = reactive({
  shopId: [{ required: true, message: '请选择门店', trigger: 'change' }],
  prefix: [{ required: true, message: '桌号前缀不能为空', trigger: 'blur' }]
})

const qrDialogVisible = ref(false)
const currentDesk = ref<DeskApi.DeskVO>()
const qrLoading = ref(false)
const qrList = ref<DeskApi.DeskQrVO[]>([])
const qrImageMap = ref<Record<number, string>>({})
const qrFormData = ref({ qrName: '', sort: 0 })
const scanBase = (import.meta.env.VITE_SCAN_H5_URL as string) || 'http://192.168.0.6:5173/h5'
const qrContentFor = (qr: DeskApi.DeskQrVO) => `${scanBase}/#/pages/scan/scan?token=${qr.qrToken}`

const getShopName = (shopId?: number) => {
  const shop = shopList.value.find((item) => item.id === Number(shopId))
  return shop ? shop.name : shopId ?? '-'
}

const getShopList = async () => {
  shopList.value = (await ShopApi.getShopList()) || []
}

const getList = async () => {
  loading.value = true
  try {
    const data = await DeskApi.getDeskPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryFormRef.value?.resetFields()
  handleQuery()
}

const resetForm = () => {
  formData.value = {
    shopId: shopList.value.length > 0 ? shopList.value[0].id : (undefined as any),
    deskNumber: '',
    area: '',
    capacity: 4,
    status: 1,
    sort: 0
  }
  formRef.value?.resetFields()
}

const openForm = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = type === 'create' ? '新增桌台' : '编辑桌台'
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await DeskApi.getDesk(id)
    } finally {
      formLoading.value = false
    }
  }
}

const submitForm = async () => {
  await formRef.value?.validate()
  formLoading.value = true
  try {
    if (formType.value === 'create') {
      await DeskApi.createDesk(formData.value)
      message.success('新增成功')
    } else {
      await DeskApi.updateDesk(formData.value)
      message.success('修改成功')
    }
    dialogVisible.value = false
    await getList()
  } finally {
    formLoading.value = false
  }
}

const openBatchForm = () => {
  batchDialogVisible.value = true
  batchFormData.value = {
    shopId: shopList.value.length > 0 ? shopList.value[0].id : (undefined as any),
    prefix: 'A',
    startNumber: 1,
    endNumber: 10,
    area: '',
    capacity: 4
  }
}

const submitBatchForm = async () => {
  await batchFormRef.value?.validate()
  if (batchFormData.value.endNumber < batchFormData.value.startNumber) {
    message.error('结束序号不能小于起始序号')
    return
  }
  batchLoading.value = true
  try {
    const ids = await DeskApi.batchCreateDesk(batchFormData.value)
    message.success(`成功创建 ${ids?.length || 0} 个桌台`)
    batchDialogVisible.value = false
    await getList()
  } finally {
    batchLoading.value = false
  }
}

const handleDelete = async (id: number) => {
  await message.delConfirm()
  await DeskApi.deleteDesk(id)
  message.success('删除成功')
  await getList()
}

const handleResetToken = async (row: DeskApi.DeskVO) => {
  await message.confirm(`确定重置桌号 ${row.deskNumber} 的二维码令牌吗？旧二维码将立即失效。`)
  await DeskApi.resetDeskToken(row.id!)
  message.success('令牌已重置，请重新下载二维码')
  await getList()
}

const loadQrList = async () => {
  if (!currentDesk.value?.id) return
  qrLoading.value = true
  try {
    qrList.value = (await DeskApi.getDeskQrList(currentDesk.value.id)) || []
    const map: Record<number, string> = {}
    for (const qr of qrList.value) {
      if (qr.id && qr.qrToken) {
        map[qr.id] = await QRCode.toDataURL(qrContentFor(qr), { width: 300, margin: 2 })
      }
    }
    qrImageMap.value = map
  } finally {
    qrLoading.value = false
  }
}

const openQrDialog = async (row: DeskApi.DeskVO) => {
  currentDesk.value = row
  qrDialogVisible.value = true
  qrFormData.value = { qrName: `${row.deskNumber}-`, sort: qrList.value.length }
  await loadQrList()
}

const addQr = async () => {
  if (!currentDesk.value?.id) return
  const name = qrFormData.value.qrName.trim()
  if (!name) {
    message.warning('请输入二维码名称')
    return
  }
  await DeskApi.createDeskQr({
    deskId: currentDesk.value.id,
    qrName: name,
    status: 1,
    sort: qrFormData.value.sort
  })
  message.success('新增成功')
  qrFormData.value.qrName = `${currentDesk.value.deskNumber}-`
  await loadQrList()
}

const handleResetQrToken = async (qr: DeskApi.DeskQrVO) => {
  await message.confirm(`确定重置「${qr.qrName}」的二维码令牌吗？旧二维码将立即失效。`)
  await DeskApi.resetDeskQrToken(qr.id!)
  message.success('令牌已重置')
  await loadQrList()
}

const handleDeleteQr = async (qr: DeskApi.DeskQrVO) => {
  await message.delConfirm(`确定删除二维码「${qr.qrName}」吗？`)
  await DeskApi.deleteDeskQr(qr.id!)
  message.success('删除成功')
  await loadQrList()
}

const downloadQr = (qr: DeskApi.DeskQrVO) => {
  const dataUrl = qrImageMap.value[qr.id!]
  if (!dataUrl) return
  const link = document.createElement('a')
  link.href = dataUrl
  link.download = `desk-${currentDesk.value?.deskNumber || ''}-${qr.qrName}.png`
  link.click()
}

onMounted(async () => {
  await getShopList()
  await getList()
})
</script>
