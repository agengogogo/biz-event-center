import { defineStore } from 'pinia'
import { login as loginRequest, storeSession } from '@/api'
import { PARK_KEY, ROLES_KEY, TOKEN_KEY } from '@/api/http'

interface AuthState {
  token: string | null
  parkId: string | null
  roles: string[]
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: localStorage.getItem(TOKEN_KEY),
    parkId: localStorage.getItem(PARK_KEY),
    roles: JSON.parse(localStorage.getItem(ROLES_KEY) ?? '[]') as string[]
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.token)
  },
  actions: {
    async login(username: string, password: string) {
      const session = await loginRequest(username, password)
      storeSession(session)
      this.token = session.token
      this.parkId = session.parkId
      this.roles = session.roles
    },
    logout() {
      this.token = null
      this.parkId = null
      this.roles = []
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(PARK_KEY)
      localStorage.removeItem(ROLES_KEY)
    },
    hasRole(role: string) {
      return this.roles.includes(role)
    }
  }
})
