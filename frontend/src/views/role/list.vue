<template>
  <div class="page">
    <div class="toolbar">
      <el-input
        v-model="query.roleName"
        placeholder="搜索角色名称"
        clearable
        style="width: 220px"
        @keyup.enter="loadData"
      />
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button type="primary" @click="openCreate">新增角色</el-button>
    </div>

    <el-table v-loading="loading" :data="tableData" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="roleName" label="角色名称" min-width="140" />
      <el-table-column prop="roleCode" label="角色编码" min-width="140" />
      <el-table-column prop="remark" label="备注" min-width="180" />
      <el-table-column prop="createTime" label="创建时间" min-width="170" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" />
        </el-form-item>
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="form.roleCode" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createRole, deleteRole, getRolePage, updateRole } from '@/api/role'

const loading = ref(false)
const saving = ref(false)
const tableData = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const formRef = ref()

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  roleName: ''
})

const form = reactive({
  id: null,
  roleName: '',
  roleCode: '',
  remark: ''
})

const rules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }]
}

const dialogTitle = computed(() => (form.id ? '编辑角色' : '新增角色'))

async function loadData() {
  loading.value = true
  try {
    const res = await getRolePage(query)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.id = null
  form.roleName = ''
  form.roleCode = ''
  form.remark = ''
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

function openEdit(row) {
  form.id = row.id
  form.roleName = row.roleName
  form.roleCode = row.roleCode
  form.remark = row.remark
  dialogVisible.value = true
}

async function onSave() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.id) {
      await updateRole({ ...form })
      ElMessage.success('更新成功')
    } else {
      await createRole({ ...form })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

async function onDelete(row) {
  await ElMessageBox.confirm(`确认删除角色「${row.roleName}」吗？`, '提示', { type: 'warning' })
  await deleteRole(row.id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)
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
