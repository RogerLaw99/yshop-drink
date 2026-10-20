<script setup>
import { onHide,onLaunch,onShow } from '@dcloudio/uni-app'
import { storeToRefs } from 'pinia'
import { useMainStore } from '@/store/store'
const main = useMainStore()
import { isWeixin,parseQuery } from '@/utils/util'
import cookie from '@/utils/cookie'
import {
  userAuthSession,
  wechatAuth
} from '@/api/auth'
import { APP_ID } from '@/config'

onLaunch((options = {}) => {
	console.log('App Launch')
	// 冷启动从本地恢复会话，避免 Pinia 空态导致登录态丢失
	main.init()
	// #ifdef MP-WEIXIN
	// 扫小程序码进入：scene 为编码后的 query（如 token=xxx）
	const scene = options && options.query && options.query.scene
	if (scene) {
		setTimeout(() => {
			uni.reLaunch({
				url: '/pages/scan/scan?scene=' + encodeURIComponent(scene)
			})
		}, 50)
		return
	}
	// 未确定商户时先选择商户
	if (!uni.getStorageSync('tenantId')) {
		setTimeout(() => {
			uni.reLaunch({ url: '/pages/tenant/tenant' })
		}, 50)
		return
	}
	// 冷启动静默登录一次；已有 token 则跳过
	if (!main.isLogin) {
		wechatMiniLogin()
	}
	// #endif
	// #ifdef H5
	const currentHash = (typeof location !== 'undefined' && location.hash) ? location.hash : ''
	if (!uni.getStorageSync('tenantId')
		&& currentHash.indexOf('/pages/scan/scan') === -1
		&& currentHash.indexOf('/pages/tenant/tenant') === -1) {
		setTimeout(() => {
			uni.reLaunch({ url: '/pages/tenant/tenant' })
		}, 50)
	}
	// #endif
})

onShow(() => {
    console.log('App Show')
   
	// #ifdef H5
	if(isWeixin()){
		oAuth()
	}
	// #endif
	// #ifdef MP-WEIXIN
	// 仅在已确定商户且 token 失效/未登录时再静默登录
	if (!main.isLogin && uni.getStorageSync('tenantId')) {
		wechatMiniLogin()
	}
	// #endif
})

onHide(() => {
   console.log('App Hide')
})



const wechatMiniLogin = () => {
	//this.$u.toast('登录中');
	uni.login({
		provider: 'weixin'
	}).then(async (res) => {
		let data = await userAuthSession({
			code: res.code
		});
		if (data) {
			main.SET_OPENID(data.openId)
			if (data.hasOwnProperty('userInfo') && data.accessToken && data.accessToken != '') {
				main.SET_MEMBER(data.userInfo);
				main.SET_TOKEN(data.accessToken);
			}
		}
	});
}

const getAuthUrl = (appId) => {
	  // #ifdef H5
	  // #endif
	  cookie.set('redirect', window.location.href)
	  const url = `${location.origin}/h5/#/pages/index/index`
	  cookie.set('index_url',url)
	  let redirect_uri = encodeURIComponent(url)
	
	  const state = 'STATE'
	  return `https://open.weixin.qq.com/connect/oauth2/authorize?appid=${appId}&redirect_uri=${redirect_uri}&response_type=code&scope=snsapi_userinfo&state=STATE#wechat_redirect`
 }
 
const oAuth = async() => {
	  const WX_AUTH = 'wx_auth'
	  return new Promise((resolve, reject) => {
		     
		const accessToken = uni.getStorageSync('accessToken');
		if (cookie.has('wx_auth') && accessToken && main.isLogin) {
		  reject()
		  return
		}
		const { code } = parseQuery()
		if (!code) {
		  //公众号appid
		  const appid = APP_ID;
		  location.href = getAuthUrl(appid)
		  return
		} else {
		 auth(code)
		}
		resolve()
	  }).catch(error => {
		console.log(error)
	  })
}

const auth = async(code) => {
	console.log('获取微信授权:',code)
	let data = await wechatAuth({'code':code})
	cookie.set('wx_auth', code)
	if (data) {
		main.SET_OPENID(data.openId)
		main.SET_MEMBER(data.userInfo);
		main.SET_TOKEN(data.accessToken);
		
	
	}

}



</script>

<style lang="scss">
@import '~@/static/style/app.scss';
</style>
