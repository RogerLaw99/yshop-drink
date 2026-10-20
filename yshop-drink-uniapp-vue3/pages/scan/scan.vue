<template>
	<view class="scan-page">
		<view class="scan-page__title">{{ loading ? '正在进入扫码点餐…' : errorMsg || '正在进入扫码点餐…' }}</view>
		<view class="scan-page__desk" v-if="deskNumber">
			桌号：{{ deskNumber }}
			<text v-if="deskPeople"> · {{ deskPeople }}人</text>
		</view>
		<view class="scan-page__tip" v-if="!loading && errorMsg">请联系店员获取正确的桌台二维码</view>
	</view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useMainStore } from '@/store/store'
import { getDeskByToken, joinDeskSession } from '@/api/desk'
import { getActiveDeskOrders, getDeskOrderSummary } from '@/api/order'

const main = useMainStore()
const deskNumber = ref('')
const deskPeople = ref(1)
const loading = ref(true)
const errorMsg = ref('')

const safeDecode = (value) => {
	if (!value) return ''
	try {
		return decodeURIComponent(value)
	} catch (e) {
		return value
	}
}

// 解析小程序码 scene，例如 "token=abc&deskPeople=2"
const parseScene = (scene) => {
	const result = {}
	if (!scene) return result
	scene.split('&').forEach((pair) => {
		const index = pair.indexOf('=')
		if (index > 0) {
			result[pair.substring(0, index)] = pair.substring(index + 1)
		}
	})
	return result
}

const goMenu = () => {
	setTimeout(() => {
		uni.switchTab({ url: '/pages/menu/menu' })
	}, 300)
}

const applyDesk = async (desk, people, token) => {
	deskNumber.value = desk.deskNumber
	deskPeople.value = people
	main.SET_ORDER_TYPE('desk')
	main.SET_DESK({
		deskNumber: desk.deskNumber,
		deskPeople: people,
		shopId: desk.shopId,
		deskId: desk.id,
		qrId: desk.qrId || null,
		qrName: desk.qrName || '',
		activeOrderId: null,
		orderCount: 0,
		orderTotal: 0,
		mode: 'shared',
		sessionId: null,
		cartCount: 0,
		cartTotal: 0
	})
	// 已登录时：加入共享会话；同时兼容“分开点”模式下的本人订单与全桌汇总
	if (main.isLogin && desk.id) {
		try {
			const [session, orders, summary] = await Promise.all([
				token ? joinDeskSession(token) : Promise.resolve(null),
				getActiveDeskOrders({ deskId: desk.id, qrId: desk.qrId }),
				getDeskOrderSummary({ deskId: desk.id, qrId: desk.qrId })
			])
			main.SET_DESK({
				sessionId: session ? session.sessionId : null,
				cartCount: session ? session.totalNumber : 0,
				cartTotal: session ? session.totalAmount : 0,
				activeOrderId: orders && orders.length > 0 ? orders[0].orderId : null,
				orderCount: summary ? summary.orderCount : 0,
				orderTotal: summary ? summary.totalAmount : 0
			})
		} catch (e) {
			console.log('查询桌台会话失败', e)
		}
	}
	goMenu()
}

const loadByToken = async (token, people) => {
	loading.value = true
	errorMsg.value = ''
	try {
		const desk = await getDeskByToken(token)
		if (!desk) {
			errorMsg.value = '桌台不存在或已停用'
			loading.value = false
			return
		}
		// 扫码确定当前商户（租户），后续请求都会携带 tenant-id
		if (desk.tenantId) {
			main.SET_TENANT({ id: desk.tenantId })
		}
		// 换桌时清空上一桌的购物车，避免串单
		const lastToken = uni.getStorageSync('lastDeskToken')
		if (lastToken && lastToken !== token) {
			main.REMOVE_CART()
			uni.removeStorageSync('cart')
		}
		uni.setStorageSync('lastDeskToken', token)
		await applyDesk(desk, Number(people || desk.capacity || 1) || 1, token)
	} catch (e) {
		errorMsg.value = (e && e.msg) || '桌台信息加载失败'
		loading.value = false
	}
}

onLoad((options = {}) => {
	// 小程序码：options.scene 是编码后的 query
	const scene = safeDecode(options.scene || '')
	const sceneParams = parseScene(scene)

	const token = options.token || sceneParams.token || ''
	const people = options.deskPeople || options.people || sceneParams.deskPeople || sceneParams.people
	const deskNumberParam = safeDecode(options.deskNumber || options.desk || sceneParams.deskNumber || '')

	if (token) {
		loadByToken(token, people)
		return
	}

	// 兼容旧的明文参数（仅测试用，生产请使用 token）
	if (deskNumberParam) {
		deskNumber.value = deskNumberParam
		deskPeople.value = Number(people || 1) || 1
		main.SET_ORDER_TYPE('desk')
		main.SET_DESK({
			deskNumber: deskNumberParam,
			deskPeople: deskPeople.value,
			shopId: (options.shopId || sceneParams.shopId) ? Number(options.shopId || sceneParams.shopId) : null,
			deskId: (options.deskId || sceneParams.deskId) ? Number(options.deskId || sceneParams.deskId) : null
		})
		goMenu()
		return
	}

	loading.value = false
	errorMsg.value = '未识别到桌号'
	uni.showToast({ title: '缺少桌台参数', icon: 'none' })
})
</script>

<style lang="scss" scoped>
.scan-page {
	min-height: 100vh;
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	background: #f5f5f5;
	color: #333;

	&__title {
		font-size: 34rpx;
		font-weight: 600;
	}

	&__desk {
		margin-top: 24rpx;
		font-size: 30rpx;
		color: #09b4f1;
	}

	&__tip {
		margin-top: 24rpx;
		font-size: 26rpx;
		color: #999;
	}
}
</style>
