<template>
  <div class="page-container">
    <div class="search-bar">
      <el-input v-model="keyword" placeholder="角色名称/编码" clearable style="width: 240px" @keyup.enter="fetchData" />
      <el-button type="primary" :icon="Search" @click="fetchData">搜索</el-button>
      <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
      <div style="flex: 1" />
      <el-button type="primary" :icon="Plus" v-permission="'system:role:add'" @click="handleAdd">新增角色</el-button>
    </div>

    <div class="table-card">
      <el-table :data="tableData" v-loading="loading" stripe border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="roleName" label="角色名称" min-width="140" />
        <el-table-column prop="roleCode" label="角色编码" min-width="140" />
        <el-table-column prop="roleDesc" label="描述" min-width="180" show-overflow-tooltip />
        <el-table-column label="数据范围" width="140" align="center">
          <template #default="{ row }">
            <el-tag :type="dataScopeType(row.dataScope)" size="small">{{ dataScopeLabel(row.dataScope) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" v-permission="'system:role:edit'" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="primary" size="small" v-permission="'system:role:edit'" @click="handleAssignMenu(row)">权限</el-button>
            <el-popconfirm title="确认删除此角色？" @confirm="handleDelete([row.id])">
              <template #reference>
                <el-button link type="danger" size="small" v-permission="'system:role:delete'">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pageNum" v-model:page-size="pageSize" :page-sizes="[10, 20, 50]"
          :total="total" layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchData" @current-change="fetchData"
        />
      </div>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="550px" :close-on-click-modal="false" @closed="resetForm">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="80px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="formData.roleName" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="formData.roleCode" placeholder="请输入角色编码" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="描述" prop="roleDesc">
          <el-input v-model="formData.roleDesc" type="textarea" :rows="2" placeholder="请输入角色描述" />
        </el-form-item>
        <el-form-item label="数据范围" prop="dataScope">
          <el-select v-model="formData.dataScope" style="width: 100%" @change="onScopeChange">
            <el-option
              v-for="opt in dataScopeOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <div class="scope-tip">{{ currentScopeTip }}</div>
        </el-form-item>
        <el-form-item v-if="formData.dataScope === 5" label="层级深度">
          <el-input-number v-model="formData.dataScopeLevel" :min="1" :max="10" />
          <span class="scope-tip" style="margin-left: 10px">本部门往下 N 级为止</span>
        </el-form-item>
        <el-form-item v-if="formData.dataScope === 7" label="指定部门">
          <el-tree
            ref="deptTreeRef"
            :data="deptTree"
            :props="{ label: 'deptName', children: 'children' }"
            node-key="id"
            show-checkbox
            check-strictly
            default-expand-all
            class="dept-tree"
            :default-checked-keys="checkedDeptIds"
          />
        </el-form-item>
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

    <!-- 分配权限对话框 -->
    <el-dialog v-model="menuDialogVisible" title="分配菜单权限" width="500px" :close-on-click-modal="false">
      <el-tree
        ref="menuTreeRef"
        :data="menuTree"
        :props="{ label: 'menuName', children: 'children' }"
        node-key="id"
        show-checkbox
        default-expand-all
        :default-checked-keys="checkedMenuIds"
      />
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="menuDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="menuLoading" @click="handleMenuSubmit">确认</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, onMounted } from 'vue'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getRolePage, getRoleById, createRole, updateRole, deleteRoles, assignMenus, getRoleMenuIds } from '@/api/role'
import { getMenuTreeSelect } from '@/api/menu'
import { getDeptTree } from '@/api/dept'
import type { SysRole, SysMenu } from '@/types'

const loading = ref(false)
const tableData = ref<SysRole[]>([])
const total = ref(0)
const keyword = ref('')
const pageNum = ref(1)
const pageSize = ref(10)

async function fetchData() {
  loading.value = true
  try {
    const res = await getRolePage({ pageNum: pageNum.value, pageSize: pageSize.value, keyword: keyword.value })
    tableData.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

function resetQuery() { keyword.value = ''; pageNum.value = 1; fetchData() }

/** 数据范围选项：与后端 DataScope 枚举一一对应（8 级） */
const dataScopeOptions = [
  { value: 1, label: '1-全部数据', tip: '不受组织限制，可见全部数据（如超管、审计岗）' },
  { value: 2, label: '2-本集团及以下', tip: '以组织树根节点(集团)为边界，覆盖全部下级公司' },
  { value: 3, label: '3-本公司及以下', tip: '以所属公司为边界，不跨公司' },
  { value: 4, label: '4-本部门及以下', tip: '以所属部门为边界，含全部子部门' },
  { value: 5, label: '5-本部门及以下(限N级)', tip: '限定向下钻取层数，避免越权看到过深的组织数据' },
  { value: 6, label: '6-本部门', tip: '仅本部门，不含子部门' },
  { value: 7, label: '7-自定义部门', tip: '按下方勾选的部门生效（不含子部门，需要子部门请用第 4/5 项）' },
  { value: 8, label: '8-仅本人', tip: '只能看到自己创建 / 属于自己的数据' },
]
const currentScopeTip = computed(() =>
  dataScopeOptions.find(o => o.value === formData.value.dataScope)?.tip || '')

function dataScopeLabel(scope: number) {
  return dataScopeOptions.find(o => o.value === scope)?.label.replace(/^\d-/, '') || '未知'
}

function dataScopeType(scope: number): 'primary' | 'success' | 'warning' | 'info' | 'danger' {
  const map: Record<number, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    1: 'danger', 2: 'warning', 3: 'warning', 4: 'primary',
    5: 'primary', 6: 'success', 7: 'info', 8: 'info',
  }
  return map[scope] || 'info'
}

/** 部门树（自定义数据范围勾选用） */
const deptTree = ref<any[]>([])
const deptTreeRef = ref()
const checkedDeptIds = ref<number[]>([])

async function loadDeptTree() {
  if (deptTree.value.length > 0) return
  try {
    const res = await getDeptTree()
    deptTree.value = res.data || []
  } catch (e) { /* ignore */ }
}

function onScopeChange(value: number) {
  if (value === 7) {
    checkedDeptIds.value = []
    loadDeptTree()
  }
}

// Form
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref()
const currentId = ref<number>()
const formRules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  dataScope: [{ required: true, message: '请选择数据范围', trigger: 'change' }],
}

const formData = ref({ roleName: '', roleCode: '', roleDesc: '', dataScope: 8, dataScopeLevel: 1, status: 1, deptIds: '' })

function resetForm() {
  formData.value = { roleName: '', roleCode: '', roleDesc: '', dataScope: 8, dataScopeLevel: 1, status: 1, deptIds: '' }
  checkedDeptIds.value = []
  currentId.value = undefined
}

function handleAdd() { isEdit.value = false; dialogTitle.value = '新增角色'; resetForm(); dialogVisible.value = true }

async function handleEdit(row: SysRole) {
  isEdit.value = true; dialogTitle.value = '编辑角色'
  const res = await getRoleById(row.id)
  formData.value = {
    roleName: res.data.roleName,
    roleCode: res.data.roleCode,
    roleDesc: res.data.roleDesc || '',
    dataScope: res.data.dataScope,
    dataScopeLevel: (res.data as any).dataScopeLevel || 1,
    status: res.data.status,
    deptIds: (res.data as any).deptIds || '',
  }
  // 自定义数据范围：回显已选部门
  if (res.data.dataScope === 7) {
    checkedDeptIds.value = formData.value.deptIds
      ? formData.value.deptIds.split(',').map((s: string) => Number(s)).filter((n: number) => !Number.isNaN(n))
      : []
    await loadDeptTree()
    // 等树渲染完成再回填勾选
    nextTick(() => deptTreeRef.value?.setCheckedKeys(checkedDeptIds.value))
  }
  currentId.value = row.id; dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  const payload: any = { ...formData.value }
  if (payload.dataScope === 7) {
    const checked = deptTreeRef.value?.getCheckedKeys() || []
    if (checked.length === 0) {
      ElMessage.warning('自定义数据范围至少需要勾选一个部门')
      return
    }
    payload.deptIds = checked.join(',')
  } else {
    payload.deptIds = ''
  }
  submitLoading.value = true
  try {
    if (isEdit.value && currentId.value) {
      await updateRole(currentId.value, payload)
      ElMessage.success('修改成功')
    } else {
      await createRole(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false; fetchData()
  } finally { submitLoading.value = false }
}

async function handleDelete(ids: number[]) {
  await deleteRoles(ids); ElMessage.success('删除成功'); fetchData()
}

// Menu assignment
const menuDialogVisible = ref(false)
const menuTree = ref<SysMenu[]>([])
const checkedMenuIds = ref<number[]>([])
const menuLoading = ref(false)
const currentMenuRoleId = ref<number>()
const menuTreeRef = ref()

async function handleAssignMenu(row: SysRole) {
  currentMenuRoleId.value = row.id
  const [treeRes, idsRes] = await Promise.all([getMenuTreeSelect(), getRoleMenuIds(row.id)])
  menuTree.value = treeRes.data
  checkedMenuIds.value = idsRes.data
  menuDialogVisible.value = true
}

async function handleMenuSubmit() {
  menuLoading.value = true
  try {
    const keys = menuTreeRef.value!.getCheckedKeys() as number[]
    const halfKeys = menuTreeRef.value!.getHalfCheckedKeys() as number[]
    await assignMenus(currentMenuRoleId.value!, [...keys, ...halfKeys])
    ElMessage.success('权限分配成功')
    menuDialogVisible.value = false
  } finally { menuLoading.value = false }
}

onMounted(fetchData)
</script>

<style scoped>
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
.scope-tip { color: #909399; font-size: 12px; line-height: 1.6; margin-top: 4px; }
.dept-tree {
  width: 100%;
  max-height: 220px;
  overflow-y: auto;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 6px;
}
</style>
