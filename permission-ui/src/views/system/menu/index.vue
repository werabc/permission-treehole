<template>
  <div class="page-container">
    <div class="search-bar">
      <el-input v-model="keyword" placeholder="菜单名称" clearable style="width: 220px" @keyup.enter="fetchData" />
      <el-select v-model="statusFilter" placeholder="状态" clearable style="width: 120px">
        <el-option label="启用" :value="1" />
        <el-option label="禁用" :value="0" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="fetchData">搜索</el-button>
      <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
      <div style="flex: 1" />
      <el-button type="primary" :icon="Plus" v-permission="'system:menu:add'" @click="handleAdd(null)">根菜单</el-button>
    </div>

    <div class="table-card">
      <el-table :data="menuList" v-loading="loading" row-key="id" stripe border default-expand-all>
        <el-table-column prop="menuName" label="菜单名称" min-width="200" />
        <el-table-column prop="icon" label="图标" width="80" align="center">
          <template #default="{ row }">
            <el-icon v-if="row.icon" :size="18"><component :is="row.icon" /></el-icon>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.menuType)" size="small">{{ typeLabel(row.menuType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="路由路径" min-width="160" show-overflow-tooltip />
        <el-table-column prop="component" label="组件路径" min-width="180" show-overflow-tooltip />
        <el-table-column prop="permission" label="权限标识" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag v-if="row.permission" type="warning" size="small">{{ row.permission }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="60" align="center" />
        <el-table-column label="状态" width="70" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" v-permission="'system:menu:add'" v-if="row.menuType !== 'BUTTON'" @click="handleAdd(row)">添加</el-button>
            <el-button link type="primary" size="small" v-permission="'system:menu:edit'" @click="handleEdit(row)">编辑</el-button>
            <el-popconfirm title="确认删除此菜单？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button link type="danger" size="small" v-permission="'system:menu:delete'">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="650px" :close-on-click-modal="false" @closed="resetForm">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="80px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="上级菜单">
              <el-tree-select
                v-model="formData.parentId"
                :data="menuTreeSelect"
                :props="{ label: 'menuName', children: 'children' }"
                placeholder="顶级菜单"
                check-strictly
                clearable
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="菜单类型" prop="menuType">
              <el-select v-model="formData.menuType" style="width: 100%" :disabled="isEdit">
                <el-option label="目录" value="CATALOG" />
                <el-option label="菜单" value="MENU" />
                <el-option label="按钮" value="BUTTON" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="菜单名称" prop="menuName">
              <el-input v-model="formData.menuName" placeholder="请输入菜单名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序" prop="sort">
              <el-input-number v-model="formData.sort" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20" v-if="formData.menuType !== 'BUTTON'">
          <el-col :span="12">
            <el-form-item label="路由路径" prop="path" v-if="formData.menuType !== 'BUTTON'">
              <el-input v-model="formData.path" placeholder="如 /system/user" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="图标" v-if="formData.menuType !== 'BUTTON'">
              <el-input v-model="formData.icon" placeholder="Element Plus 图标名" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="组件路径" prop="component" v-if="formData.menuType === 'MENU'">
          <el-input v-model="formData.component" placeholder="如 system/user/index" />
        </el-form-item>
        <el-form-item label="权限标识" prop="permission" v-if="formData.menuType === 'BUTTON'">
          <el-input v-model="formData.permission" placeholder="如 system:user:list" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="formData.status">
                <el-radio :value="1">启用</el-radio>
                <el-radio :value="0">禁用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="可见" v-if="formData.menuType !== 'BUTTON'">
              <el-radio-group v-model="formData.visible">
                <el-radio :value="1">可见</el-radio>
                <el-radio :value="0">隐藏</el-radio>
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getMenuTree, getMenuTreeSelect, getMenuById, createMenu, updateMenu, deleteMenu } from '@/api/menu'
import type { SysMenu } from '@/types'

const loading = ref(false)
const menuList = ref<SysMenu[]>([])
const menuTreeSelect = ref<SysMenu[]>([])
const keyword = ref('')
const statusFilter = ref<number>()

async function fetchData() {
  loading.value = true
  try {
    const res = await getMenuTree({ keyword: keyword.value, status: statusFilter.value })
    menuList.value = res.data
  } finally { loading.value = false }
}

async function loadTreeSelect() {
  const res = await getMenuTreeSelect()
  menuTreeSelect.value = res.data
}

function resetQuery() { keyword.value = ''; statusFilter.value = undefined; fetchData() }

function typeLabel(type: string) {
  const map: Record<string, string> = { CATALOG: '目录', MENU: '菜单', BUTTON: '按钮' }
  return map[type] || type
}

function typeTag(type: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' {
  const map: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    CATALOG: 'primary', MENU: 'success', BUTTON: 'warning',
  }
  return map[type] || 'info'
}

// Form
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref()
const currentId = ref<number>()

const formData = ref({
  parentId: null as number | null, menuName: '', menuType: 'MENU', path: '', component: '',
  icon: '', permission: '', sort: 0, status: 1, visible: 1,
})

const formRules = {
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  menuType: [{ required: true, message: '请选择菜单类型', trigger: 'change' }],
}

function resetForm() {
  formData.value = {
    parentId: null, menuName: '', menuType: 'MENU', path: '', component: '',
    icon: '', permission: '', sort: 0, status: 1, visible: 1,
  }
  currentId.value = undefined
}

function handleAdd(parent: SysMenu | null) {
  isEdit.value = false; dialogTitle.value = '新增菜单'
  resetForm()
  if (parent) {
    formData.value.parentId = parent.id
    if (parent.menuType === 'MENU') {
      formData.value.menuType = 'BUTTON'
    }
  }
  dialogVisible.value = true
}

async function handleEdit(row: SysMenu) {
  isEdit.value = true; dialogTitle.value = '编辑菜单'
  const res = await getMenuById(row.id)
  Object.assign(formData.value, {
    parentId: res.data.parentId === 0 ? null : res.data.parentId,
    menuName: res.data.menuName, menuType: res.data.menuType,
    path: res.data.path || '', component: res.data.component || '',
    icon: res.data.icon || '', permission: res.data.permission || '',
    sort: res.data.sort, status: res.data.status, visible: res.data.visible,
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
      await updateMenu(currentId.value, data)
      ElMessage.success('修改成功')
    } else {
      await createMenu(data)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false; fetchData()
  } finally { submitLoading.value = false }
}

async function handleDelete(id: number) {
  await deleteMenu(id); ElMessage.success('删除成功'); fetchData()
}

onMounted(() => { fetchData(); loadTreeSelect() })
</script>
