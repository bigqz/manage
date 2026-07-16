<template>
  <div class="page">
    <div class="toolbar">
      <el-input
        v-model="query.username"
        placeholder="搜索用户名"
        clearable
        style="width: 220px"
        @keyup.enter="loadData"
      />
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button type="primary" @click="openCreate">新增用户</el-button>
    </div>

    <el-table v-loading="loading" :data="tableData" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" min-width="120" />
      <el-table-column prop="nickname" label="昵称" min-width="120" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '正常' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="角色" min-width="180">
        <template #default="{ row }">
          <el-tag v-for="role in row.roles" :key="role.id" style="margin-right: 6px">
            {{ role.roleName }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" min-width="170" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="primary" @click="openAssign(row)">分配角色</el-button>
          <el-button link type="danger" :disabled="row.id === 1" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadData"
        @size-change="loadData"
      />
    </div>

    <el-drawer v-model="drawerVisible" :title="drawerTitle" size="420px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password :placeholder="form.id ? '不填则不修改' : ''" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="drawerVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-drawer>

    <el-dialog v-model="roleDialogVisible" title="分配角色" width="420px">
      <el-select v-model="selectedRoleIds" multiple placeholder="选择角色" style="width: 100%">
        <el-option v-for="role in roleOptions" :key="role.id" :label="role.roleName" :value="role.id" />
      </el-select>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="assigning" @click="onAssign">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { assignUserRoles, createUser, deleteUser, getUserPage, updateUser } from '@/api/user'
import { getRoleList } from '@/api/role'

const loading = ref(false)
const saving = ref(false)
const assigning = ref(false)
const tableData = ref([])
const total = ref(0)
const roleOptions = ref([])

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  username: ''
})

const drawerVisible = ref(false)
const roleDialogVisible = ref(false)
const formRef = ref()
const currentUserId = ref(null)
const selectedRoleIds = ref([])

const form = reactive({
  id: null,
  username: '',
  password: '',
  nickname: '',
  status: 1
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const drawerTitle = computed(() => (form.id ? '编辑用户' : '新增用户'))

async function loadData() {
  loading.value = true
  try {
    const res = await getUserPage(query)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function loadRoles() {
  const res = await getRoleList()
  roleOptions.value = res.data
}

function resetForm() {
  form.id = null
  form.username = ''
  form.password = ''
  form.nickname = ''
  form.status = 1
}

function openCreate() {
  resetForm()
  drawerVisible.value = true
}

function openEdit(row) {
  form.id = row.id
  form.username = row.username
  form.password = ''
  form.nickname = row.nickname
  form.status = row.status
  drawerVisible.value = true
}

function openAssign(row) {
  currentUserId.value = row.id
  selectedRoleIds.value = (row.roles || []).map((item) => item.id)
  roleDialogVisible.value = true
}

async function onSave() {
  await formRef.value.validate()
  if (!form.id && !form.password) {
    ElMessage.error('新增用户时密码不能为空')
    return
  }
  saving.value = true
  try {
    if (form.id) {
      await updateUser({ ...form })
      ElMessage.success('更新成功')
    } else {
      await createUser({ ...form })
      ElMessage.success('创建成功')
    }
    drawerVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

async function onAssign() {
  assigning.value = true
  try {
    await assignUserRoles(currentUserId.value, selectedRoleIds.value)
    ElMessage.success('分配成功')
    roleDialogVisible.value = false
    loadData()
  } finally {
    assigning.value = false
  }
}

async function onDelete(row) {
  await ElMessageBox.confirm(`确认删除用户「${row.username}」吗？`, '提示', { type: 'warning' })
  await deleteUser(row.id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(async () => {
  await Promise.all([loadData(), loadRoles()])
})
</script>

<style scoped>
.page {
  padding: 16px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(10, 16, 29, 0.55);
}
.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
