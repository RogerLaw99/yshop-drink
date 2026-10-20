<template>
	<view class="tenant-page">
		<view class="tenant-page__title">请选择商家</view>
		<view class="tenant-page__tip">也可以直接扫描餐桌上的二维码进入</view>
		<view class="tenant-page__list">
			<view
				v-for="item in list"
				:key="item.id"
				class="tenant-page__item"
				@tap="select(item)"
			>
				<view class="tenant-page__name">{{ item.name }}</view>
				<view class="tenant-page__arrow">›</view>
			</view>
			<view v-if="!loading && list.length === 0" class="tenant-page__empty">
				暂无可用的商家
			</view>
		</view>
	</view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getTenantList } from '@/api/tenant'
import { useMainStore } from '@/store/store'

const main = useMainStore()
const list = ref([])
const loading = ref(true)

onLoad(async () => {
	try {
		list.value = (await getTenantList()) || []
	} catch (e) {
		console.log('获取商户列表失败', e)
	} finally {
		loading.value = false
	}
})

const select = (item) => {
	main.SET_TENANT({ id: item.id, name: item.name })
	uni.switchTab({ url: '/pages/index/index' })
}
</script>

<style lang="scss" scoped>
.tenant-page {
	min-height: 100vh;
	background: #f5f5f5;
	padding: 80rpx 40rpx;

	&__title {
		font-size: 44rpx;
		font-weight: 600;
		color: #333;
	}

	&__tip {
		margin-top: 16rpx;
		font-size: 26rpx;
		color: #999;
	}

	&__list {
		margin-top: 60rpx;
		background: #fff;
		border-radius: 16rpx;
		overflow: hidden;
	}

	&__item {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 36rpx 32rpx;
		border-bottom: 1rpx solid #f0f0f0;

		&:last-child {
			border-bottom: none;
		}
	}

	&__name {
		font-size: 32rpx;
		color: #333;
	}

	&__arrow {
		font-size: 40rpx;
		color: #bbb;
	}

	&__empty {
		padding: 60rpx;
		text-align: center;
		color: #999;
		font-size: 28rpx;
	}
}
</style>
