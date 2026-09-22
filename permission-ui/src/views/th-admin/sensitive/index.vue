<template>
  <div class="page-container">
    <el-alert
      v-if="stats"
      :title="`词库共 ${stats.wordCount} 条，过滤总开关：${stats.enabled ? '已开启' : '已关闭'}`"
      :type="stats.enabled ? 'success' : 'warning'"
      :closable="false"
      show-icon
      class="stat-bar"
    >
      <template #default>
        <span>等级说明：1-直接拦截（内容不入库）；2-转人工审核。配置项 <code>sensitive_filter_enabled</code> 可在站点配置中开关。</span>
      </template>
    </el-alert>

    <div class="toolbar">
      <el-input
        v-model="query.keyword"
        placeholder="搜索敏感词"
        clearable
        style="width: 200px"
        @keyup.enter="fetchData"
      />
      <el-select v-model="query.level" placeholder="等级" clearable style="width: 130px">
        <el-option label="1-拦截" :value="1" />
        <el-option label="2-转审" :value="2" />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px">
        <el-option label="启用" :value="1" />
        <el-option label="停用" :value="0" />
      </el-select>
      <el-button type="primary" @click="fetchData">查询</el-button>
      <el-button :icon="Plus" type="success" @click="openCreateDialog">新增敏感词</el-button>
      <el-button :icon="Upload" @click="importVisible = true">批量导入</el-button>
      <el-button :icon="Refresh" @click="handleRefresh">刷新词库缓存</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe border row-key="id">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="word" label="敏感词" min-width="160" show-overflow-tooltip />
      <el-table-column label="等级" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="row.level === 1 ? 'danger' : 'warning'" size="small">
            {{ row.level === 1 ? '1-拦截' : '2-转审' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="category" label="分类" width="100" align="center" />
      <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createBy" label="创建人" width="110" align="center" />
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
          <el-button link :type="row.status === 1 ? 'warning' : 'success'" size="small" @click="toggleStatus(row)">
            {{ row.status === 1 ? '停用' : '启用' }}
          </el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      class="pager"
      layout="total, prev, pager, next, sizes"
      :total="total"
      :current-page="query.pageNum"
      :page-size="query.pageSize"
      :page-sizes="[10, 20, 50, 100]"
      @current-change="(p: number) => { query.pageNum = p; fetchData() }"
      @size-change="(s: number) => { query.pageSize = s; query.pageNum = 1; fetchData() }"
    />

    <!-- 新增/编辑 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑敏感词' : '新增敏感词'" width="480px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="敏感词" prop="word">
          <el-input v-model="form.word" placeholder="请输入敏感词" maxlength="50" />
        </el-form-item>
        <el-form-item label="等级" prop="level">
          <el-radio-group v-model="form.level">
            <el-radio :value="1">1-直接拦截</el-radio>
            <el-radio :value="2">2-转人工审核</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" style="width: 100%">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" placeholder="备注说明（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确认</el-button>
      </template>
    </el-dialog>

    <!-- 批量导入 -->
    <el-dialog v-model="importVisible" title="批量导入敏感词" width="540px" :close-on-click-modal="false">
      <el-form label-width="80px">
        <el-form-item label="默认等级">
          <el-radio-group v-model="importForm.level">
            <el-radio :value="1">1-直接拦截</el-radio>
            <el-radio :value="2">2-转人工审核</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="importForm.category" style="width: 100%">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="词条">
          <el-input
            v-model="importForm.text"
            type="textarea"
            :rows="8"
            placeholder="每行一个词，也支持逗号/顿号分隔；已存在的词会自动跳过"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary" :loading="importing" @click="handleImport">导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Plus, Upload, Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  getSensitiveWordPage, getSensitiveWordStats, createSensitiveWord,
  updateSensitiveWord, deleteSensitiveWord, importSensitiveWords, refreshSensitiveWords
} from '@/api/treehole-admin'

const loading = ref(false)
const submitting = ref(false)
const importing = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const stats = ref<{ wordCount: number; enabled: boolean } | null>(null)
const dialogVisible = ref(false)
const importVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()

const categories = ['广告', '辱骂', '色情', '违法', '其他']

const query = reactive({ pageNum: 1, pageSize: 20, keyword: '', level: undefined as number | undefined, status: undefined as number | undefined })
const form = reactive({ id: 0, word: '', level: 1, category: '其他', remark: '' })
const importForm = reactive({ text: '', level: 1, category: '其他' })

const rules: FormRules = {
  word: [{ required: true, message: '请输入敏感词', trigger: 'blur' }],
  level: [{ required: true, message: '请选择等级', trigger: 'change' }]
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getSensitiveWordPage({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      keyword: query.keyword || undefined,
      level: query.level,
      status: query.status
    })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

async function fetchStats() {
  try {
    const res = await getSensitiveWordStats()
    stats.value = res.data
  } catch (e) { /* ignore */ }
}

function openCreateDialog() {
  isEdit.value = false
  Object.assign(form, { id: 0, word: '', level: 1, category: '其他', remark: '' })
  dialogVisible.value = true
}

function openEditDialog(row: any) {
  isEdit.value = true
  Object.assign(form, row)
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value?.validate().catch(() => { throw new Error('validate') })
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateSensitiveWord({ ...form })
      ElMessage.success('修改成功')
    } else {
      await createSensitiveWord({ word: form.word, level: form.level, category: form.category, remark: form.remark })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchData()
    fetchStats()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

async function toggleStatus(row: any) {
  try {
    await updateSensitiveWord({ id: row.id, status: row.status === 1 ? 0 : 1 })
    ElMessage.success('状态已更新')
    fetchData()
  } catch (e: any) {
    ElMessage.error('操作失败')
  }
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确认删除敏感词「${row.word}」？`, '删除')
  } catch { return }
  try {
    await deleteSensitiveWord(row.id)
    ElMessage.success('删除成功')
    fetchData()
    fetchStats()
  } catch (e: any) {
    ElMessage.error('删除失败')
  }
}

async function handleImport() {
  if (!importForm.text.trim()) {
    ElMessage.warning('请输入要导入的词条')
    return
  }
  importing.value = true
  try {
    const res = await importSensitiveWords({ ...importForm })
    ElMessage.success(`成功导入 ${res.data} 条（重复词已跳过）`)
    importVisible.value = false
    importForm.text = ''
    fetchData()
    fetchStats()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '导入失败')
  } finally {
    importing.value = false
  }
}

async function handleRefresh() {
  try {
    const res = await refreshSensitiveWords()
    ElMessage.success(`词库缓存已刷新，当前 ${res.data} 条`)
    fetchStats()
  } catch (e: any) {
    ElMessage.error('刷新失败')
  }
}

onMounted(() => {
  fetchData()
  fetchStats()
})
</script>

<style scoped>
.stat-bar { margin-bottom: 16px; }
.stat-bar code { background: rgba(0, 0, 0, 0.06); padding: 1px 4px; border-radius: 3px; }
.toolbar { margin-bottom: 16px; display: flex; gap: 10px; flex-wrap: wrap; }
.pager { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
