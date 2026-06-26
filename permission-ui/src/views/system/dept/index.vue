<template>
  <div class="page-container">
    <div class="search-bar">
      <el-input v-model="keyword" placeholder="部门名称" clearable style="width: 220px" @keyup.enter="fetchData" />
      <el-select v-model="statusFilter" placeholder="状态" clearable style="width: 120px">
        <el-option label="启用" :value="1" />
        <el-option label="禁用" :value="0" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="fetchData">搜索</el-button>
      <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
      <div style="flex: 1" />
      <el-button type="primary" :icon="Plus" v-permission="'system:dept:add'" @click="handleAdd(null)">新增部门</el-button>
    </div>

    <div class="table-card">
      <el-table :data="deptList" v-loading="loading" row-key="id" stripe border default-expand-all>
        <el-table-column prop="deptName" label="部门名称" min-width="200" />
        <el-table-column prop="sort" label="排序" width="70" align="center" />
        <el-table-column prop="leader" label="负责人" width="120" />
        <el-table-column prop="phone" label="电话" width="140" />
        <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" v-permission="'system:dept:add'" @click="handleAdd(row)">添加</el-button>
            <el-button link type="primary" size="small" v-permission="'system:dept:edit'" @click="handleEdit(row)">编辑</el-button>
            <el-popconfirm title="确认删除？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button link type="danger" size="small" v-permission="'system:dept:delete'">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="550px" :close-on-click-modal="false" @closed="resetForm">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="80px">
        <el-form-item label="上级部门">
          <el-tree-select
            v-model="formData.parentId"
            :data="deptTreeSelect"
            :props="{ label: 'deptName', children: 'children' }"
            placeholder="顶级部门"
            check-strictly
            clearable
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="部门名称" prop="deptName">
          <el-input v-model="formData.deptName" placeholder="请输入部门名称" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="负责人" prop="leader">
              <el-input v-model="formData.leader" placeholder="请输入负责人" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序" prop="sort">
              <el-input-number v-model="formData.sort" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="电话" prop="phone">
              <el-input v-model="formData.phone" placeholder="请输入电话" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="formData.email" placeholder="请输入邮箱" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="状态">
          <el-radio-group v-model="formData.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确认</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getDeptTree, getDeptTreeSelect, getDeptById, createDept, updateDept, deleteDept } from '@/api/dept'
import type { SysDept } from '@/types'

const loading = ref(false)
const deptList = ref<SysDept[]>([])
const deptTreeSelect = ref<SysDept[]>([])
const keyword = ref('')
const statusFilter = ref<number>()

async function fetchData() {
  loading.value = true
  try {
    const res = await getDeptTree({ keyword: keyword.value, status: statusFilter.value })
    deptList.value = res.data
  } finally { loading.value = false }
}

async function loadTreeSelect() {
  const res = await getDeptTreeSelect()
  deptTreeSelect.value = res.data
}

function resetQuery() { keyword.value = ''; statusFilter.value = undefined; fetchData() }

// Form
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref()
const currentId = ref<number>()

const formData = ref({
  parentId: null as number | null, deptName: '', sort: 0, leader: '', phone: '', email: '', status: 1,
})

const formRules = {
  deptName: [{ required: true, message: '请输入部门名称', trigger: 'blur' }],
}

function resetForm() {
  formData.value = { parentId: null, deptName: '', sort: 0, leader: '', phone: '', email: '', status: 1 }
  currentId.value = undefined
}

function handleAdd(parent: SysDept | null) {
  isEdit.value = false; dialogTitle.value = '新增部门'
  resetForm()
  if (parent) formData.value.parentId = parent.id
  dialogVisible.value = true
}

async function handleEdit(row: SysDept) {
  isEdit.value = true; dialogTitle.value = '编辑部门'
  const res = await getDeptById(row.id)
  Object.assign(formData.value, {
    parentId: res.data.parentId === 0 ? null : res.data.parentId,
    deptName: res.data.deptName, sort: res.data.sort,
    leader: res.data.leader || '', phone: res.data.phone || '',
    email: res.data.email || '', status: res.data.status,
  })
  currentId.value = row.id; dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    const data = { ...formData.value }
    if (data.parentId === null) data.parentId = 0 as any
    if (isEdit.value && currentId.value) {
      await updateDept(currentId.value, data)
      ElMessage.success('修改成功')
    } else {
      await createDept(data)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false; fetchData()
  } finally { submitLoading.value = false }
}

async function handleDelete(id: number) {
  await deleteDept(id); ElMessage.success('删除成功'); fetchData()
}

onMounted(() => { fetchData(); loadTreeSelect() })
</script>
