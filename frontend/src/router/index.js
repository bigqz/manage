import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/user',
    children: [
      {
        path: 'user',
        name: 'UserList',
        component: () => import('@/views/user/list.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'role',
        name: 'RoleList',
        component: () => import('@/views/role/list.vue'),
        meta: { title: '角色管理' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to) => {
  const userStore = useUserStore()
  if (to.meta.public) {
    if (userStore.token) {
      return '/'
    }
    return true
  }
  if (!userStore.token) {
    return '/login'
  }
  if (!userStore.userInfo) {
    try {
      await userStore.fetchUserInfo()
    } catch {
      userStore.logout()
      return '/login'
    }
  }
  return true
})

export default router
