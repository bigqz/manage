<template>
  <el-container class="layout">
    <el-aside class="aside" :width="collapsed ? '64px' : '220px'">
      <div class="brand" :class="{ collapsed }">
        <div class="mark" />
        <div v-if="!collapsed" class="name">Manage</div>
      </div>
      <el-menu
        :default-active="active"
        class="menu"
        :collapse="collapsed"
        :collapse-transition="false"
        router
      >
        <el-menu-item index="/user">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/role">
          <el-icon><Key /></el-icon>
          <span>角色管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="left">
          <el-button text class="icon-btn" @click="collapsed = !collapsed">
            <el-icon><Fold v-if="!collapsed" /><Expand v-else /></el-icon>
          </el-button>
          <div class="page-title">{{ pageTitle }}</div>
        </div>
        <div class="right">
          <div class="user">
            <span class="nickname">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</span>
          </div>
          <el-button text @click="onLogout">退出</el-button>
        </div>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const collapsed = ref(false)
const active = computed(() => route.path)
const pageTitle = computed(() => route.meta?.title || '后台管理')

function onLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout {
  min-height: 100vh;
  background: radial-gradient(1200px 800px at 20% 10%, rgba(0, 186, 199, 0.10), transparent 60%),
    radial-gradient(900px 700px at 90% 0%, rgba(62, 124, 255, 0.10), transparent 55%),
    linear-gradient(180deg, #0b1220 0%, #070b14 100%);
}
.aside {
  border-right: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(10, 16, 29, 0.6);
  backdrop-filter: blur(10px);
}
.brand {
  height: 56px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 14px;
  color: rgba(255, 255, 255, 0.92);
  user-select: none;
}
.brand.collapsed {
  justify-content: center;
}
.mark {
  width: 18px;
  height: 18px;
  border-radius: 6px;
  background: linear-gradient(135deg, #00bac7, #3e7cff);
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.12) inset;
}
.name {
  font-size: 16px;
  letter-spacing: 0.6px;
  font-weight: 650;
}
.menu {
  border-right: none;
  background: transparent;
}
.header {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(10, 16, 29, 0.45);
  backdrop-filter: blur(10px);
}
.left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.icon-btn {
  color: rgba(255, 255, 255, 0.88);
}
.page-title {
  color: rgba(255, 255, 255, 0.92);
  font-size: 14px;
  letter-spacing: 0.2px;
}
.right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.nickname {
  color: rgba(255, 255, 255, 0.86);
  font-size: 13px;
}
.main {
  padding: 16px;
}
:deep(.el-menu) {
  --el-menu-bg-color: transparent;
  --el-menu-text-color: rgba(255, 255, 255, 0.78);
  --el-menu-active-color: #00bac7;
  --el-menu-hover-bg-color: rgba(255, 255, 255, 0.06);
  --el-menu-item-height: 44px;
}
</style>

