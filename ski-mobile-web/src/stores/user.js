import { defineStore } from 'pinia'
import { TOKEN_KEY, USER_INFO_KEY } from '@/utils/constants'
import * as authApi from '@/api/auth'
import * as userApi from '@/api/user'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    userInfo: JSON.parse(localStorage.getItem(USER_INFO_KEY) || 'null')
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    nickname: (state) => state.userInfo?.nickname || '雪友'
  },

  actions: {
    async login(form) {
      const data = await authApi.login(form)
      this.setAuth(data)
      return data
    },

    async register(form) {
      const data = await authApi.register(form)
      this.setAuth(data)
      return data
    },

    async smsLogin(form) {
      const data = await authApi.smsLogin(form)
      this.setAuth(data)
      return data
    },

    async logout() {
      try {
        if (this.token) await authApi.logout()
      } catch (e) {}
      this.clearAuth()
    },

    setAuth(loginResp) {
      this.token = loginResp.token
      const userInfo = {
        userId: loginResp.userId,
        phone: loginResp.phone,
        nickname: loginResp.nickname
      }
      this.userInfo = userInfo
      localStorage.setItem(TOKEN_KEY, loginResp.token)
      localStorage.setItem(USER_INFO_KEY, JSON.stringify(userInfo))
    },

    clearAuth() {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_INFO_KEY)
    },

    async fetchProfile() {
      const data = await userApi.getProfile()
      this.userInfo = { ...this.userInfo, ...data }
      localStorage.setItem(USER_INFO_KEY, JSON.stringify(this.userInfo))
      return data
    },

    async updateNickname(nickname) {
      await userApi.updateProfile({ nickname })
      this.userInfo = { ...this.userInfo, nickname }
      localStorage.setItem(USER_INFO_KEY, JSON.stringify(this.userInfo))
    }
  }
})
