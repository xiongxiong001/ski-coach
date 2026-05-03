import { defineStore } from 'pinia'
import { TOKEN_KEY, ADMIN_INFO_KEY } from '@/utils/constants'
import * as authApi from '@/api/auth'

export const useAdminStore = defineStore('admin', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    adminInfo: JSON.parse(localStorage.getItem(ADMIN_INFO_KEY) || 'null')
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    username: (state) => state.adminInfo?.username || '',
    realName: (state) => state.adminInfo?.realName || state.adminInfo?.username || '管理员'
  },

  actions: {
    async login(form) {
      const data = await authApi.login(form)
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
      const info = {
        adminId: loginResp.adminId,
        username: loginResp.username,
        realName: loginResp.realName
      }
      this.adminInfo = info
      localStorage.setItem(TOKEN_KEY, loginResp.token)
      localStorage.setItem(ADMIN_INFO_KEY, JSON.stringify(info))
    },

    clearAuth() {
      this.token = ''
      this.adminInfo = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(ADMIN_INFO_KEY)
    }
  }
})
