import request from '@/utils/request'

export function getRoleList() {
  return request.get('/role/list')
}

export function getRolePage(params) {
  return request.get('/role/page', { params })
}

export function createRole(data) {
  return request.post('/role', data)
}

export function updateRole(data) {
  return request.put('/role', data)
}

export function deleteRole(id) {
  return request.delete(`/role/${id}`)
}
