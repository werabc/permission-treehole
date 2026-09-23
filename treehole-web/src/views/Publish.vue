<template>
  <div class="dh-narrow compose">
    <div class="compose__head" v-reveal>
      <h2>写下此刻的心事</h2>
      <p>这里没有熟人，只有愿意听的人。</p>
    </div>

    <div class="dh-card editor" v-reveal>
      <div class="field">
        <label class="dh-label">发布身份</label>
        <div class="dh-seg">
          <button type="button" :class="{ 'is-on': form.isAnonymous === 1 }" @click="form.isAnonymous = 1">匿名发布</button>
          <button type="button" :class="{ 'is-on': form.isAnonymous === 0 }" @click="form.isAnonymous = 0">实名发布</button>
        </div>
        <p class="hint">
          {{ form.isAnonymous === 1 ? '不显示你的昵称与头像，也不会暴露你的身份' : '将以你的昵称与头像显示' }}
        </p>
      </div>

      <div class="field">
        <label class="dh-label">心事分类</label>
        <div class="pills">
          <button
            v-for="cat in categories"
            :key="cat.id"
            type="button"
            :class="['dh-pill', { 'is-on': form.categoryId === cat.id }]"
            @click="form.categoryId = form.categoryId === cat.id ? undefined : cat.id"
          >
            {{ cat.name }}
          </button>
          <span v-if="categories.length === 0" class="hint">暂无可选分类（可不选直接发布）</span>
        </div>
      </div>

      <div class="field">
        <label class="dh-label">正文</label>
        <textarea
          v-model="form.content"
          class="ta"
          placeholder="写下你想说的话…它可以很长，也可以只有一句。"
          maxlength="5000"
        />
        <div class="meter">
          <span>{{ form.content.length }} / 5000</span>
          <span class="meter__bar"><i :style="{ width: meterWidth }" /></span>
          <span>{{ form.isAnonymous === 1 ? '匿名' : '实名' }} · {{ form.categoryId ? '已选分类' : '未选分类' }}</span>
        </div>
      </div>

      <div class="notice">
        <AppIcon name="spark" :size="18" class="notice__ico" />
        <span>
          发布后需经<b> 审核 </b>才会出现在广场；含敏感词的内容会被直接拦下，
          请避免留下联系方式与真实姓名。
        </span>
      </div>

      <div class="editor__acts">
        <button class="dh-btn dh-btn--quiet" type="button" @click="router.back()">取消</button>
        <button class="dh-btn dh-btn--primary dh-btn--lg" type="button" :disabled="submitting" @click="handleSubmit">
          <AppIcon name="spark" :size="16" />
          {{ submitting ? '发布中…' : form.isAnonymous === 1 ? '匿名发布' : '实名发布' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppIcon from '../components/AppIcon.vue'
import { createPost, getCategoryList } from '../api/treehole'
import type { Category } from '../api/treehole'

const router = useRouter()
const categories = ref<Category[]>([])
const submitting = ref(false)

const form = reactive({
  content: '',
  categoryId: undefined as number | undefined,
  isAnonymous: 1,
})

const meterWidth = computed(() => `${Math.min((form.content.length / 5000) * 100, 100)}%`)

async function loadCategories() {
  try {
    const res = await getCategoryList()
    categories.value = res.data || []
  } catch {
    /* 分类加载失败不阻塞发布 */
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
    ElMessage.success('发布成功')
    router.push(`/post/${res.data}`)
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '发布失败')
  } finally {
    submitting.value = false
  }
}

onMounted(loadCategories)
</script>

<style scoped>
.compose { padding: 40px 0 80px; }
.compose__head { text-align: center; margin-bottom: 30px; }
.compose__head h2 { font-size: clamp(26px, 3.4vw, 36px); letter-spacing: -0.03em; font-weight: 700; }
.compose__head p { margin-top: 10px; font-family: var(--font-display); font-style: italic; font-size: 17px; color: var(--text-dim); }

.editor { padding: 30px 32px; border-radius: var(--r-xl); }
.field { margin-bottom: 24px; }
.hint { margin-top: 9px; font-size: 12px; color: var(--text-mute); }
.pills { display: flex; gap: 8px; flex-wrap: wrap; }

.ta {
  width: 100%; min-height: 220px; padding: 20px 22px; font-size: 16px; line-height: 1.85;
  border: 1px solid var(--border); border-radius: var(--r-md); background: var(--surface);
  resize: vertical; transition: border-color 0.35s var(--ease), box-shadow 0.35s var(--ease);
}
.ta:focus { border-color: var(--accent-line); box-shadow: 0 0 0 4px var(--accent-soft); }
.ta::placeholder { color: var(--text-mute); }

.meter { display: flex; align-items: center; justify-content: space-between; margin-top: 10px; font-family: var(--font-mono); font-size: 11px; color: var(--text-mute); gap: 14px; }
.meter__bar { flex: 1; height: 3px; border-radius: 3px; background: var(--surface-2); overflow: hidden; }
.meter__bar i { display: block; height: 100%; background: var(--accent); border-radius: 3px; transition: width 0.4s var(--ease); }

.notice {
  display: flex; gap: 12px; padding: 14px 17px; border-radius: var(--r-md);
  background: var(--accent-soft); border: 1px solid var(--accent-line);
  font-size: 13.5px; color: var(--text-dim);
}
.notice b { color: var(--text); font-weight: 500; }
.notice__ico { color: var(--accent); flex-shrink: 0; margin-top: 2px; }

.editor__acts { display: flex; justify-content: flex-end; gap: 12px; margin-top: 26px; }

@media (max-width: 720px) {
  .editor { padding: 22px; }
  .editor__acts { flex-direction: column-reverse; }
  .editor__acts .dh-btn { width: 100%; }
}
</style>
