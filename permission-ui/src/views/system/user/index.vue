<template>
  <div class="page-container">
    <div class="search-bar">
      <el-input v-model="queryParams.keyword" placeholder="用户名/昵称/手机号" clearable style="width: 220px" @keyup.enter="fetchData" />
      <el-tree-select
        v-model="queryParams.deptId"
        :data="deptTree"
        :props="{ label: 'deptName', children: 'children' }"
        placeholder="选择部门"
        clearable
        check-strictly
        style="width: 200px"
      />
      <el-select v-model="queryParams.status" placeholder="状态" clearable style="width: 120px">
        <el-option label="启用" :value="1" />
        <el-option label="禁用" :value="0" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="fetchData">搜索</el-button>
      <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
      <div style="flex: 1" />
      <el-button type="success" :icon="Download" @click="handleExport" v-permission="'system:user:list'">导出</el-button>
      <el-dropdown @command="handleBatchCommand" v-permission="'system:user:edit'">
        <el-button type="warning">
          批量操作<el-icon class="el-icon--right"><ArrowDown /></el-icon>
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="enable">批量启用</el-dropdown-item>
            <el-dropdown-item command="disable">批量禁用</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
      <el-button type="primary" :icon="Plus" v-permission="'system:user:add'" @click="handleAdd">新增用户</el-button>
    </div>

    <div class="table-card">
      <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%"
                @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column prop="deptName" label="部门" min-width="130" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="最后登录" width="170" />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" v-permission="'system:user:query'" @click="handleView(row)">查看</el-button>
            <el-button link type="primary" size="small" v-permission="'system:user:edit'" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="primary" size="small" v-permission="'system:user:edit'" @click="handleAssignRole(row)">角色</el-button>
            <el-button link type="warning" size="small" v-permission="'system:user:reset-pwd'" @click="handleResetPwd(row)">重置</el-button>
            <el-popconfirm title="确认删除此用户？" @confirm="handleDelete([row.id])">
              <template #reference>
                <el-button link type="danger" size="small" v-permission="'system:user:delete'">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" :close-on-click-modal="false" @closed="resetForm">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="80px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="formData.username" :disabled="isEdit" placeholder="请输入用户名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="formData.nickname" placeholder="请输入昵称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20" v-if="!isEdit">
          <el-col :span="12">
            <el-form-item label="密码" prop="password">
              <el-input v-model="formData.password" type="password" show-password placeholder="请输入密码" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="formData.phone" placeholder="请输入手机号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="formData.email" placeholder="请输入邮箱" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="部门" prop="deptId">
              <el-tree-select
                v-model="formData.deptId"
                :data="deptTree"
                :props="{ label: 'deptName', children: 'children' }"
                placeholder="请选择部门"
                check-strictly
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="formData.status">
                <el-radio :value="1">启用</el-radio>
                <el-radio :value="0">禁用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确认</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 分配角色对话框 -->
    <el-dialog v-model="roleDialogVisible" title="分配角色" width="500px" :close-on-click-modal="false">
      <el-checkbox-group v-model="selectedRoleIds">
        <el-checkbox v-for="role in allRoles" :key="role.id" :value="role.id" :label="role.id" style="margin: 8px 16px 8px 0">
          {{ role.roleName }}
          <el-tag size="small" type="info" style="margin-left: 4px">{{ role.roleCode }}</el-tag>
        </el-checkbox>
      </el-checkbox-group>
      <el-empty v-if="allRoles.length === 0" description="暂无可分配角色" />
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="roleDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="roleLoading" @click="handleRoleSubmit">确认</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Search, Refresh, Plus, Download, ArrowDown } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getUserPage, getUserById, createUser, updateUser, deleteUsers,
  assignRoles, getUserRoleIds, resetPassword, exportUsers, batchUpdateStatus,
} from '@/api/user'
import { getDeptTreeSelect } from '@/api/dept'
import { getAllRoles } from '@/api/role'
import type { SysUser, SysDept, SysRole } from '@/types'

const loading = ref(false)
const tableData = ref<SysUser[]>([])
const total = ref(0)
const deptTree = ref<SysDept[]>([])
const selectedRows = ref<SysUser[]>([])
const allRoles = ref<SysRole[]>([])

const queryParams = reactive({
  pageNum: 1, pageSize: 10, keyword: '', deptId: undefined as number | undefined, status: undefined as number | undefined,
})

async function fetchData() {
  loading.value = true
  try {
    const res = await getUserPage(queryParams)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function loadDeptTree() {
  const res = await getDeptTreeSelect()
  deptTree.value = res.data
}

function resetQuery() {
  queryParams.keyword = ''
  queryParams.deptId = undefined
  queryParams.status = undefined
  queryParams.pageNum = 1
  fetchData()
}

function handleSelectionChange(rows: SysUser[]) {
  selectedRows.value = rows
}

async function handleExport() {
  try {
    await exportUsers()
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  }
}

async function handleBatchCommand(command: string) {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择用户')
    return
  }
  const status = command === 'enable' ? 1 : 0
  const action = status === 1 ? '启用' : '禁用'
  await ElMessageBox.confirm(`确定要批量${action}选中的 ${selectedRows.value.length} 个用户吗？`, '批量操作')
  const ids = selectedRows.value.map(r => r.id)
  await batchUpdateStatus(ids, status)
  ElMessage.success(`批量${action}成功`)
  fetchData()
}

// Form
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref()
const currentId = ref<number>()
const formRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, message: '密码至少6位', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
}

const formData = reactive({
  username: '', nickname: '', password: '', phone: '', email: '', deptId: null as number | null, status: 1,
})

function resetForm() {
  Object.assign(formData, { username: '', nickname: '', password: '', phone: '', email: '', deptId: null, status: 1 })
  currentId.value = undefined
}

function handleAdd() {
  isEdit.value = false
  dialogTitle.value = '新增用户'
  resetForm()
  dialogVisible.value = true
}

async function handleView(row: SysUser) {
  const res = await getUserById(row.id)
  ElMessageBox.alert(JSON.stringify(res.data, null, 2), '用户详情')
}

async function handleEdit(row: SysUser) {
  isEdit.value = true
  dialogTitle.value = '编辑用户'
  const res = await getUserById(row.id)
  Object.assign(formData, {
    username: res.data.username,
    nickname: res.data.nickname,
    password: '',
    phone: res.data.phone || '',
    email: res.data.email || '',
    deptId: res.data.deptId,
    status: res.data.status,
  })
  currentId.value = row.id
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    if (isEdit.value && currentId.value) {
      await updateUser(currentId.value, formData)
      ElMessage.success('修改成功')
    } else {
      await createUser(formData)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchData()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(ids: number[]) {
  await deleteUsers(ids)
  ElMessage.success('删除成功')
  fetchData()
}

async function handleResetPwd(row: SysUser) {
  try {
    await ElMessageBox.prompt('请输入新密码', '重置密码', { inputType: 'password', inputValidator: (v) => v && v.length >= 6 ? true : '密码至少6位' })
    const newPwd = (document.querySelector('.el-message-box__input input') as HTMLInputElement)?.value
    await resetPassword(row.id, newPwd)
    ElMessage.success('密码重置成功')
  } catch { /* cancel */ }
}

// Role assignment
const roleDialogVisible = ref(false)
const roleLoading = ref(false)
const selectedRoleIds = ref<number[]>([])
const currentRoleUserId = ref<number>()

async function handleAssignRole(row: SysUser) {
  currentRoleUserId.value = row.id
  try {
    const [rolesRes, idsRes] = await Promise.all([getAllRoles(), getUserRoleIds(row.id)])
    allRoles.value = rolesRes.data
    selectedRoleIds.value = idsRes.data
    roleDialogVisible.value = true
  } catch { /* ignore */ }
}

async function handleRoleSubmit() {
  roleLoading.value = true
  try {
    await assignRoles(currentRoleUserId.value!, selectedRoleIds.value)
    ElMessage.success('角色分配成功')
    roleDialogVisible.value = false
  } finally {
    roleLoading.value = false
  }
}

onMounted(() => {
  loadDeptTree()
  fetchData()
})
</script>

<style scoped>
.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
