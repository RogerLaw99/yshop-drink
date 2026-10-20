import { defineStore } from 'pinia'

import cookie from '@/utils/cookie'
import { navigateTo } from '@/utils/router'

export const useMainStore = defineStore('main', {
  state: () => ({
	store: {},
	cart: [],
	orderType: 'takein',
	// 扫码点餐信息（桌号、人数、门店）
	desk: {
		deskNumber: '',
		deskPeople: 1,
		shopId: null,
		deskId: null,
		qrId: null,
		qrName: '',
		activeOrderId: null,
		orderCount: 0,
		orderTotal: 0,
		// shared：一起点（共享购物车，一人支付）；separate：分开点（各自下单）
		mode: 'shared',
		sessionId: null,
		cartCount: 0,
		cartTotal: 0
	},
	address: {},
	addresses: {},
	member: {

	},
	openid:"",
	token:"",
	// 当前商户（租户）
	tenant: { id: null, name: '' },
	lang: 'zh-cn',
	cookieKey:'YSESSID=yshop-e4dk4o2utr3c0n95tp42p745ai',
	// 默认地为你为北京地址
	location: {},
	mycoupon: {}
  }),
  getters: {
    isLogin(state) {
      return !!(state.token || cookie.get('accessToken'))
    },
  },
  actions: {
	DEL_COUPON() {
	    	this.mycoupon = {}
	},
	SET_COUPON(coupon) {
	  	this.mycoupon = coupon
	},
	SET_ORDER_TYPE(type) {
	  	this.orderType = type
	},
	SET_DESK(desk) {
		this.desk = { ...this.desk, ...desk }
	},
	CLEAR_DESK() {
		this.desk = {
			deskNumber: '',
			deskPeople: 1,
			shopId: null,
			deskId: null,
			qrId: null,
			qrName: '',
			activeOrderId: null,
			orderCount: 0,
			orderTotal: 0,
			mode: 'shared',
			sessionId: null,
			cartCount: 0,
			cartTotal: 0
		}
	},
	SET_MEMBER(member) {
		this.member = member
		cookie.set('userinfo', member)
	},
	SET_ADDRESS(address) {
		this.address = address
	},
	SET_ADDRESSES(addresses) {
		this.addresses = addresses
	},
	SET_STORE(store) {
		this.store = store
	},
	SET_CART(cart) {
		this.cart = cart
	},
	REMOVE_CART(state) {
		this.cart = []
	},
	setCookie(state, provider) {
		state.cookie = provider;
		uni.setStorage({
			key: 'cookieKey',
			data: provider
		});
	},
	SET_LOCATION(location) {
		this.location = location;
	},
	SET_OPENID(openid) {
		this.openid = openid;
	},
	SET_TOKEN(token) {
		this.token = token;
		cookie.set('accessToken', token)
	},
	SET_TENANT(tenant) {
		this.tenant = { ...this.tenant, ...tenant }
		if (tenant.id) {
			uni.setStorageSync('tenantId', tenant.id)
		}
		if (tenant.name) {
			uni.setStorageSync('tenantName', tenant.name)
		}
	},
	  
    setAccessToken(user) {
      cookie.set('accessToken', user)
      // return getUserInfo()
    },
    setSelectAddress(id) {
      const list = Array.isArray(this.addresses) ? this.addresses : []
      this.address = list.find(item => item.id == id) || {}
    },
    init() {
      const accessToken = cookie.get('accessToken')
      const userinfo = cookie.get('userinfo')
      if (accessToken) {
        this.token = accessToken
      }
      if (userinfo && typeof userinfo === 'object') {
        this.member = userinfo
      }
      this.tenant = {
        id: uni.getStorageSync('tenantId') || null,
        name: uni.getStorageSync('tenantName') || ''
      }
    },
    logout() {
      this.member = {}
      this.cart = []
      this.token = ''
      cookie.remove('accessToken')
      cookie.remove('userinfo')
      navigateTo('/pages-user/login/login')
    },
  },
})
