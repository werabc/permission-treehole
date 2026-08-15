import axios from 'axios'

const service = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

service.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 200) {
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  (error) => {
    return Promise.reject(error)
  }
)

export default service
