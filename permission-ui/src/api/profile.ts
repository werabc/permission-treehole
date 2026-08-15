import request from './index'

export interface ProfileForm {
  nickname: string
  email: string
  phone: string
  sex: number
}

export function updateProfile(data: ProfileForm) {
  return request.put('/user/profile', data)
}
