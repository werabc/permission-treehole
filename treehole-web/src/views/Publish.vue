<template>
  <div class="publish">
    <div class="publish-card">
      <h2 class="title">发布树洞</h2>
      <p class="subtitle">匿名分享你的故事，安全保密</p>

      <div class="form-group">
        <label>分类</label>
        <select v-model="form.categoryId" class="select">
          <option :value="undefined">选择分类</option>
          <option v-for="cat in categories" :key="cat.id" :value="cat.id">
            {{ cat.icon }} {{ cat.name }}
          </option>
        </select>
      </div>

      <div class="form-group">
        <label>内容</label>
        <textarea
          v-model="form.content"
          class="textarea"
          placeholder="写下你想说的话..."
          maxlength="5000"
          rows="8"
        ></textarea>
        <span class="char-count">{{ form.content.length }}/5000</span>
      </div>

      <div class="form-group">
        <label>发布方式</label>
        <div class="radio-group">
          <label class="radio-label">
            <input type="radio" v-model="form.isAnonymous" :value="0" />
            <span>实名发布（显示昵称）</span>
          </label>
          <label class="radio-label">
            <input type="radio" v-model="form.isAnonymous" :value="1" />
            <span>匿名发布（不显示身份）</span>
          </label>
        </div>
      </div>

      <div class="notice">
        <el-alert type="info" :closable="false" show-icon>
          <template #title>
            发布的内容需要审核通过后才会显示在列表中
          </template>
        </el-alert>
      </div>

      <div class="actions">
        <button class="th-btn th-btn-ghost" @click="$router.back()">取消</button>
        <button class="th-btn th-btn-primary" :loading="submitting" @click="handleSubmit">
          {{ submitting ? '发布中...' : '发布' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElAlert } from 'element-plus'
import { createPost, getCategoryList } from '../api/treehole'
import type { Category } from '../api/treehole'

const router = useRouter()
const categories = ref<Category[]>([])
const submitting = ref(false)

const form = reactive({
  content: '',
  categoryId: undefined as number | undefined,
  isAnonymous: 1, // 默认匿名
})

async function loadCategories() {
  try {
    const res = await getCategoryList()
    categories.value = res.data
  } catch (e) {
    console.error('加载分类失败:', e)
  }
}

async function handleSubmit() {
  if (!form.content.trim()) {
    ElMessage.warning('请输入内容')
    return
  }
  submitting.value = true
  try {
    const res = await createPost({
      content: form.content,
      categoryId: form.categoryId,
      isAnonymous: form.isAnonymous,
    })
    ElMessage.success('发布成功！等待审核')
    router.push(`/post/${res.data}`)
  } catch (e: any) {
    ElMessage.error(e.message || '发布失败')
  } finally {
    submitting.value = false
  }
}

onMounted(loadCategories)
</script>

<style scoped>
.publish {
  max-width: 600px;
  margin: 0 auto;
}

.publish-card {
  background: #fff;
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.title {
  font-size: 22px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 4px;
}

.subtitle {
  font-size: 14px;
  color: #94a3b8;
  margin-bottom: 24px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #334155;
  margin-bottom: 6px;
}

.select {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  background: #fff;
}

.textarea {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 15px;
  line-height: 1.6;
  resize: vertical;
  transition: border-color 0.2s;
  font-family: inherit;
}

.textarea:focus {
  border-color: #3b82f6;
  outline: none;
}

.char-count {
  display: block;
  text-align: right;
  font-size: 12px;
  color: #94a3b8;
  margin-top: 4px;
}

.radio-group {
  display: flex;
  gap: 24px;
}

.radio-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #334155;
  cursor: pointer;
}

.radio-label input[type="radio"] {
  accent-color: #3b82f6;
}

.notice {
  margin: 16px 0;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
}
</style>
