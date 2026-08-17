<template>
  <div class="page-container">
    <el-tabs v-model="activeTab" tab-position="left" class="settings-tabs">
      <!-- 基本设置 -->
      <el-tab-pane label="基本设置" name="basic">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>站点基本配置</span>
              <el-button type="primary" :loading="saving" @click="saveSettings">保存配置</el-button>
            </div>
          </template>
          <el-form :model="settings" label-width="140px" style="max-width: 600px">
            <el-form-item label="站点名称">
              <el-input v-model="settings.site_name" placeholder="请输入站点名称" />
            </el-form-item>
            <el-form-item label="站点Logo">
              <el-input v-model="settings.site_logo" placeholder="Logo URL" />
            </el-form-item>
            <el-form-item label="站点描述">
              <el-input v-model="settings.site_description" type="textarea" rows="3" placeholder="站点描述" />
            </el-form-item>
            <el-form-item label="站点关键词">
              <el-input v-model="settings.site_keywords" placeholder="SEO关键词，逗号分隔" />
            </el-form-item>
            <el-form-item label="站点ICP备案号">
              <el-input v-model="settings.icp_number" placeholder="ICP备案号" />
            </el-form-item>
            <el-form-item label="联系邮箱">
              <el-input v-model="settings.contact_email" placeholder="联系邮箱" />
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>

      <!-- 注册设置 -->
      <el-tab-pane label="注册设置" name="registration">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>用户注册配置</span>
              <el-button type="primary" :loading="saving" @click="saveSettings">保存配置</el-button>
            </div>
          </template>
          <el-form :model="settings" label-width="140px" style="max-width: 600px">
            <el-form-item label="允许注册">
              <el-switch v-model="settings.register_enabled" :active-value="'1'" :inactive-value="'0'" active-text="开启" inactive-text="关闭" />
            </el-form-item>
            <el-form-item label="允许匿名发帖">
              <el-switch v-model="settings.anonymous_enabled" :active-value="'1'" :inactive-value="'0'" active-text="开启" inactive-text="关闭" />
            </el-form-item>
            <el-form-item label="注册验证方式">
              <el-radio-group v-model="settings.register_verify_type">
                <el-radio value="NONE">无需验证</el-radio>
                <el-radio value="EMAIL">邮箱验证</el-radio>
                <el-radio value="PHONE">手机验证</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="邀请码注册">
              <el-switch v-model="settings.invite_required" :active-value="'1'" :inactive-value="'0'" active-text="开启" inactive-text="关闭" />
            </el-form-item>
            <el-form-item label="注册赠送积分">
              <el-input-number v-model="settings.register_credits" :min="0" :max="1000" />
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>

      <!-- 内容策略 -->
      <el-tab-pane label="内容策略" name="content">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>内容审核配置</span>
              <el-button type="primary" :loading="saving" @click="saveSettings">保存配置</el-button>
            </div>
          </template>
          <el-form :model="settings" label-width="140px" style="max-width: 600px">
            <el-form-item label="内容审核">
              <el-switch v-model="settings.audit_enabled" :active-value="'1'" :inactive-value="'0'" active-text="开启" inactive-text="关闭" />
            </el-form-item>
            <el-form-item label="帖子最大字数">
              <el-input-number v-model="settings.post_max_length" :min="100" :max="10000" :step="100" />
            </el-form-item>
            <el-form-item label="评论最大字数">
              <el-input-number v-model="settings.comment_max_length" :min="100" :max="5000" :step="50" />
            </el-form-item>
            <el-form-item label="每日发帖限制">
              <el-input-number v-model="settings.post_daily_limit" :min="1" :max="100" />
            </el-form-item>
            <el-form-item label="每日评论限制">
              <el-input-number v-model="settings.comment_daily_limit" :min="1" :max="200" />
            </el-form-item>
            <el-form-item label="发帖间隔(秒)">
              <el-input-number v-model="settings.post_interval" :min="0" :max="3600" />
            </el-form-item>
            <el-form-item label="敏感词过滤">
              <el-switch v-model="settings.sensitive_word_enabled" :active-value="'1'" :inactive-value="'0'" active-text="开启" inactive-text="关闭" />
            </el-form-item>
            <el-form-item label="敏感词列表">
              <el-input v-model="settings.sensitive_words" type="textarea" rows="4" placeholder="每行一个敏感词" :disabled="settings.sensitive_word_enabled !== '1'" />
            </el-form-item>
            <el-form-item label="内容策略说明">
              <el-input v-model="settings.content_policy" type="textarea" rows="6" placeholder="用户内容策略说明" />
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>

      <!-- 安全设置 -->
      <el-tab-pane label="安全设置" name="security">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>安全配置</span>
              <el-button type="primary" :loading="saving" @click="saveSettings">保存配置</el-button>
            </div>
          </template>
          <el-form :model="settings" label-width="140px" style="max-width: 600px">
            <el-form-item label="JWT过期时间(小时)">
              <el-input-number v-model="settings.jwt_expire_hours" :min="1" :max="168" />
            </el-form-item>
            <el-form-item label="Refresh Token过期(天)">
              <el-input-number v-model="settings.refresh_token_expire_days" :min="1" :max="90" />
            </el-form-item>
            <el-form-item label="登录失败锁定">
              <el-switch v-model="settings.login_lock_enabled" :active-value="'1'" :inactive-value="'0'" active-text="开启" inactive-text="关闭" />
            </el-form-item>
            <el-form-item label="登录失败次数">
              <el-input-number v-model="settings.login_max_fail_count" :min="3" :max="20" :disabled="settings.login_lock_enabled !== '1'" />
            </el-form-item>
            <el-form-item label="锁定时长(分钟)">
              <el-input-number v-model="settings.login_lock_duration" :min="5" :max="1440" :disabled="settings.login_lock_enabled !== '1'" />
            </el-form-item>
            <el-form-item label="同IP注册限制">
              <el-switch v-model="settings.ip_register_limit" :active-value="'1'" :inactive-value="'0'" active-text="开启" inactive-text="关闭" />
            </el-form-item>
            <el-form-item label="同IP每日注册数">
              <el-input-number v-model="settings.ip_daily_register_limit" :min="1" :max="50" :disabled="settings.ip_register_limit !== '1'" />
            </el-form-item>
            <el-form-item label="强制HTTPS">
              <el-switch v-model="settings.force_https" :active-value="'1'" :inactive-value="'0'" active-text="开启" inactive-text="关闭" />
            </el-form-item>
            <el-form-item label="API限流">
              <el-switch v-model="settings.rate_limit_enabled" :active-value="'1'" :inactive-value="'0'" active-text="开启" inactive-text="关闭" />
            </el-form-item>
            <el-form-item label="每分钟请求数">
              <el-input-number v-model="settings.rate_limit_per_minute" :min="10" :max="1000" :disabled="settings.rate_limit_enabled !== '1'" />
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>

      <!-- 通知设置 -->
      <el-tab-pane label="通知设置" name="notification">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>通知配置</span>
              <el-button type="primary" :loading="saving" @click="saveSettings">保存配置</el-button>
            </div>
          </template>
          <el-form :model="settings" label-width="140px" style="max-width: 600px">
            <el-form-item label="邮件通知">
              <el-switch v-model="settings.email_notification" :active-value="'1'" :inactive-value="'0'" active-text="开启" inactive-text="关闭" />
            </el-form-item>
            <el-form-item label="SMTP服务器">
              <el-input v-model="settings.smtp_host" placeholder="SMTP服务器地址" />
            </el-form-item>
            <el-form-item label="SMTP端口">
              <el-input-number v-model="settings.smtp_port" :min="1" :max="65535" />
            </el-form-item>
            <el-form-item label="发件人邮箱">
              <el-input v-model="settings.smtp_from" placeholder="发件人邮箱" />
            </el-form-item>
            <el-form-item label="系统通知">
              <el-switch v-model="settings.system_notification" :active-value="'1'" :inactive-value="'0'" active-text="开启" inactive-text="关闭" />
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getThSettings, updateThSettings } from '@/api/treehole-admin'

const activeTab = ref('basic')
const settings = ref<any>({})
const saving = ref(false)

async function fetchSettings() {
  const res = await getThSettings()
  settings.value = res.data
}

async function saveSettings() {
  saving.value = true
  try {
    await updateThSettings(settings.value)
    ElMessage.success('保存成功')
  } finally { saving.value = false }
}

onMounted(fetchSettings)
</script>

<style scoped>
.settings-tabs { min-height: 500px; }
.settings-tabs :deep(.el-tabs__item) { padding: 0 20px; height: 45px; line-height: 45px; }
.card-header { font-weight: 600; font-size: 15px; display: flex; justify-content: space-between; align-items: center; }
</style>
