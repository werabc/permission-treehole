<template>
  <div class="author-page">
    <div class="page-header">
      <h2 class="page-title">作者中心</h2>
      <el-button type="primary" :icon="Plus" @click="showNovelDialog(null)">新建小说</el-button>
    </div>

    <!-- Novel List -->
    <div class="novel-table-card" v-loading="loading">
      <el-table :data="novels" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="title" label="小说名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="categoryName" label="分类" width="100" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 2 ? 'success' : 'info'" size="small">
              {{ row.status === 2 ? '已完结' : '连载中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="wordCount" label="总字数" width="100" align="center">
          <template #default="{ row }">{{ formatCount(row.wordCount) }}</template>
        </el-table-column>
        <el-table-column prop="likeCount" label="收藏" width="80" align="center" />
        <el-table-column prop="clickCount" label="点击" width="80" align="center" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="showChapterDialog(row)">章节</el-button>
            <el-button link type="primary" size="small" @click="showNovelDialog(row)">编辑</el-button>
            <el-popconfirm title="确认删除此小说？" @confirm="handleDeleteNovel(row.id)">
              <template #reference>
                <el-button link type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap" v-if="total > pageSize">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20]"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="fetchNovels"
        />
      </div>
    </div>

    <!-- Novel Dialog -->
    <el-dialog
      v-model="novelDialogVisible"
      :title="editingNovel ? '编辑小说' : '新建小说'"
      width="550px"
      :close-on-click-modal="false"
      @closed="resetNovelForm"
    >
      <el-form ref="novelFormRef" :model="novelForm" :rules="novelRules" label-width="70px">
        <el-form-item label="书名" prop="title">
          <el-input v-model="novelForm.title" placeholder="请输入小说名称" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="novelForm.categoryId" placeholder="请选择分类" style="width: 100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.categoryName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="简介" prop="intro">
          <el-input v-model="novelForm.intro" type="textarea" :rows="4" placeholder="请输入小说简介" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="novelForm.status">
            <el-radio :value="1">连载中</el-radio>
            <el-radio :value="2">已完结</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="novelDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="novelSubmitting" @click="handleNovelSubmit">确认</el-button>
      </template>
    </el-dialog>

    <!-- Chapter Dialog -->
    <el-dialog
      v-model="chapterDialogVisible"
      :title="currentNovel ? `管理章节 - ${currentNovel.title}` : '章节管理'"
      width="700px"
      :close-on-click-modal="false"
    >
      <div class="chapter-list-inner">
        <div class="chapter-list-item" v-for="ch in chapters" :key="ch.id">
          <span class="ch-num">第{{ ch.chapterNum }}章</span>
          <span class="ch-title">{{ ch.chapterTitle }}</span>
          <span class="ch-words">{{ ch.wordCount }}字</span>
          <div class="ch-actions">
            <el-button link type="primary" size="small" @click="editChapter(ch)">编辑</el-button>
            <el-popconfirm title="确认删除此章节？" @confirm="handleDeleteChapter(ch.id)">
              <template #reference>
                <el-button link type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </div>
        </div>
        <el-empty v-if="!chapters.length" description="暂无章节" :image-size="50" />
      </div>

      <!-- Chapter Editor (inline expand) -->
      <div class="chapter-editor" v-if="editingChapter !== undefined">
        <el-divider />
        <el-form label-width="70px">
          <el-form-item label="章节标题">
            <el-input v-model="chapterForm.chapterTitle" placeholder="请输入章节标题" />
          </el-form-item>
          <el-form-item label="章节内容">
            <el-input
              v-model="chapterForm.content"
              type="textarea"
              :rows="12"
              placeholder="请输入章节内容..."
            />
          </el-form-item>
        </el-form>
        <div class="chapter-editor-footer">
          <span class="word-hint">{{ chapterForm.content?.length || 0 }} 字</span>
          <div>
            <el-button @click="editingChapter = undefined">取消</el-button>
            <el-button type="primary" :loading="chapterSubmitting" @click="handleChapterSubmit">
              {{ chapterForm.id ? '保存修改' : '新增章节' }}
            </el-button>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="chapter-dialog-footer">
          <el-button @click="startNewChapter" :icon="Plus">新增章节</el-button>
          <el-button @click="chapterDialogVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getMyNovels, createNovel, updateNovel, deleteNovels, getChaptersByNovel, createChapter, updateChapter, deleteChapter } from '@/api/novel'
import { getCategoryList } from '@/api/novel'
import type { Novel, NovelCategory, NovelChapter } from '@/types'

// Novels
const loading = ref(false)
const novels = ref<Novel[]>([])
const categories = ref<NovelCategory[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

async function fetchNovels() {
  loading.value = true
  try {
    const res = await getMyNovels({ pageNum: pageNum.value, pageSize: pageSize.value })
    novels.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

// Novel form
const novelDialogVisible = ref(false)
const novelFormRef = ref()
const editingNovel = ref<Novel | null>(null)
const novelSubmitting = ref(false)
const novelForm = ref({ title: '', categoryId: undefined as number | undefined, intro: '', status: 1 })
const novelRules = {
  title: [{ required: true, message: '请输入书名', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  intro: [{ required: true, message: '请输入简介', trigger: 'blur' }],
}

function resetNovelForm() {
  novelForm.value = { title: '', categoryId: undefined, intro: '', status: 1 }
  editingNovel.value = null
}

function showNovelDialog(row: Novel | null) {
  if (row) {
    editingNovel.value = row
    novelForm.value = { title: row.title, categoryId: row.categoryId, intro: row.intro, status: row.status }
  } else {
    editingNovel.value = null
    resetNovelForm()
  }
  novelDialogVisible.value = true
}

async function handleNovelSubmit() {
  const valid = await novelFormRef.value?.validate().catch(() => false)
  if (!valid) return
  novelSubmitting.value = true
  try {
    if (editingNovel.value) {
      await updateNovel(editingNovel.value.id, novelForm.value)
      ElMessage.success('修改成功')
    } else {
      await createNovel({ ...novelForm.value, categoryId: novelForm.value.categoryId! })
      ElMessage.success('创建成功')
    }
    novelDialogVisible.value = false
    fetchNovels()
  } finally { novelSubmitting.value = false }
}

async function handleDeleteNovel(id: number) {
  await deleteNovels([id])
  ElMessage.success('删除成功')
  fetchNovels()
}

// Chapters
const chapterDialogVisible = ref(false)
const currentNovel = ref<Novel | null>(null)
const chapters = ref<NovelChapter[]>([])
const editingChapter = ref<any>(undefined)
const chapterSubmitting = ref(false)
const chapterForm = ref({ id: undefined as number | undefined, chapterTitle: '', content: '' })

async function showChapterDialog(novel: Novel) {
  currentNovel.value = novel
  const res = await getChaptersByNovel(novel.id)
  chapters.value = res.data
  editingChapter.value = undefined
  chapterDialogVisible.value = true
}

function startNewChapter() {
  editingChapter.value = { isNew: true }
  chapterForm.value = { id: undefined, chapterTitle: '', content: '' }
}

function editChapter(ch: NovelChapter) {
  editingChapter.value = { isNew: false }
  chapterForm.value = { id: ch.id, chapterTitle: ch.chapterTitle, content: ch.content || '' }
}

async function handleChapterSubmit() {
  chapterSubmitting.value = true
  try {
    if (chapterForm.value.id) {
      await updateChapter(chapterForm.value.id, {
        chapterTitle: chapterForm.value.chapterTitle,
        content: chapterForm.value.content,
      })
      ElMessage.success('章节更新成功')
    } else {
      await createChapter({
        novelId: currentNovel.value!.id,
        chapterTitle: chapterForm.value.chapterTitle,
        content: chapterForm.value.content,
      })
      ElMessage.success('章节新增成功')
    }
    editingChapter.value = undefined
    // Refresh chapters
    const res = await getChaptersByNovel(currentNovel.value!.id)
    chapters.value = res.data
  } finally { chapterSubmitting.value = false }
}

async function handleDeleteChapter(id: number) {
  await deleteChapter(id)
  ElMessage.success('章节已删除')
  const res = await getChaptersByNovel(currentNovel.value!.id)
  chapters.value = res.data
}

function formatCount(n: number) {
  if (!n) return '0'
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  return String(n)
}

onMounted(async () => {
  const res = await getCategoryList()
  categories.value = res.data
  fetchNovels()
})
</script>

<style scoped>
.author-page {
  margin: -20px;
  min-height: calc(100vh - 60px);
  background: #f5f7fa;
  padding: 24px 28px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  margin: 0;
}

.novel-table-card {
  background: #fff;
  border-radius: 10px;
  padding: 20px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}

.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

/* Chapter list in dialog */
.chapter-list-inner {
  max-height: 300px;
  overflow-y: auto;
  margin-bottom: 8px;
}

.chapter-list-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #f2f3f5;
}

.ch-num { font-size: 13px; color: #909399; flex-shrink: 0; }
.ch-title { flex: 1; font-size: 14px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ch-words { font-size: 12px; color: #c0c4cc; flex-shrink: 0; }
.ch-actions { display: flex; gap: 4px; flex-shrink: 0; }

.chapter-editor-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.word-hint {
  font-size: 13px;
  color: #909399;
}

.chapter-dialog-footer {
  display: flex;
  justify-content: space-between;
  width: 100%;
}
</style>
