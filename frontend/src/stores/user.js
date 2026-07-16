import { defineStore } from 'pinia'
import { getUserInfo, login as loginApi } from '@/api/auth'

const TOKEN_KEY = 'manage_token'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    userInfo: null,
    roles: []
  }),
  actions: {
    async login(form) {
      const res = await loginApi(form)
      this.token = res.data.token
      this.userInfo = res.data.userInfo
      this.roles = res.data.roles || []
      localStorage.setItem(TOKEN_KEY, this.token)
    },
    async fetchUserInfo() {
      const res = await getUserInfo()
      this.userInfo = res.data.userInfo
      this.roles = res.data.roles || []
    },
    logout() {
      this.token = ''
      this.userInfo = null
      this.roles = []
      localStorage.removeItem(TOKEN_KEY)
    }
  }
})
