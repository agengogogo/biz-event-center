import axios from 'axios'

export const TOKEN_KEY = 'bec_token'
export const PARK_KEY = 'bec_park'
export const ROLES_KEY = 'bec_roles'

const http = axios.create({ baseURL: '/api', timeout: 15000 })

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && window.location.pathname !== '/login') {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(PARK_KEY)
      localStorage.removeItem(ROLES_KEY)
      window.location.assign('/login')
    }
    return Promise.reject(error)
  }
)

export default http
