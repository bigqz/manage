<template>
  <div class="login-page">
    <div class="panel">
      <div class="brand">
        <div class="mark" />
        <div class="name">Manage</div>
      </div>
      <p class="desc">简单后台管理系统</p>

      <el-form ref="formRef" :model="form" :rules="rules" @submit.prevent>
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            size="large"
            show-password
            @keyup.enter="onSubmit"
          />
        </el-form-item>
        <el-button class="submit" type="primary" size="large" :loading="loading" @click="onSubmit">
          登录
        </el-button>
      </el-form>

      <p class="hint">默认账号：admin / admin123</p>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({
  username: 'admin',
  password: 'admin123'
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function onSubmit() {
  await formRef.value.validate()
  loading.value = true
  try {
    await userStore.login(form)
    ElMessage.success('登录成功')
    router.push('/')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background:
    radial-gradient(900px 600px at 15% 20%, rgba(0, 186, 199, 0.18), transparent 60%),
    radial-gradient(700px 500px at 85% 10%, rgba(62, 124, 255, 0.16), transparent 55%),
    linear-gradient(180deg, #0b1220 0%, #070b14 100%);
}
.panel {
  width: min(420px, 100%);
  padding: 36px 32px 28px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(10, 16, 29, 0.72);
  backdrop-filter: blur(12px);
  animation: rise 0.5s ease-out;
}
.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}
.mark {
  width: 22px;
  height: 22px;
  border-radius: 7px;
  background: linear-gradient(135deg, #00bac7, #3e7cff);
}
.name {
  font-size: 28px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 0.8px;
}
.desc {
  margin: 0 0 28px;
  color: rgba(255, 255, 255, 0.62);
  font-size: 14px;
}
.submit {
  width: 100%;
  margin-top: 4px;
}
.hint {
  margin: 18px 0 0;
  text-align: center;
  color: rgba(255, 255, 255, 0.45);
  font-size: 12px;
}
@keyframes rise {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
