import request from '@/utils/request'

export function getUserPage(params) {
  return request.get('/user/page', { params })
}

export function createUser(data) {
  return request.post('/user', data)
}

export function updateUser(data) {
  return request.put('/user', data)
}

export function deleteUser(id) {
  return request.delete(`/user/${id}`)
}

export function assignUserRoles(id, roleIds) {
  return request.put(`/user/${id}/roles`, { roleIds })
}
