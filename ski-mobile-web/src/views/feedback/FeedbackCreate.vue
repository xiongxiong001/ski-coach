<template>
  <div class="page">
    <header class="nav-bar">
      <button class="back-btn" @click="router.back()">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
          <path d="M15 19L8 12l7-7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
      </button>
      <h1 class="nav-title">用户反馈</h1>
      <span class="my-feedback-link" @click="router.push('/feedback/list')">我的反馈</span>
    </header>

    <!-- 引导提示 -->
    <div class="guide-banner">
      <div class="guide-card">
        <div class="guide-emoji">👋</div>
        <div class="guide-text">告诉我们你的想法，每条反馈我们都会亲自看——<br>平均回复时长<span class="highlight"> 4 小时</span></div>
      </div>
    </div>

    <div class="content">
      <!-- 反馈类型 -->
      <div class="section-label">反馈类型</div>
      <div class="type-grid">
        <div
          v-for="t in feedbackTypes"
          :key="t.value"
          class="type-card"
          :class="[t.value, { active: form.type === t.value }]"
          @click="form.type = t.value"
        >
          <span class="type-emoji">{{ t.emoji }}</span>
          <span class="type-name">{{ t.label }}</span>
          <div v-if="form.type === t.value" class="check-mark">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
              <path d="M5 13l4 4L19 7"/>
            </svg>
          </div>
        </div>
      </div>

      <!-- 详细描述 -->
      <div class="section-label">详细描述</div>

      <div class="textarea-wrapper">
        <textarea
          v-model="form.content"
          class="content-input"
          maxlength="2000"
          rows="5"
        ></textarea>
        <p v-show="!form.content" class="section-hint">比如：在第几步遇到的问题？发生了什么？预期是什么？</p>
        <div class="quick-tags">
          <span
            v-for="tag in quickTags"
            :key="tag"
            class="quick-tag"
            @click="appendContent(tag)"
          >+ {{ tag }}</span>
        </div>
        <div class="char-count" :class="{ over: form.content.length >= 2000 }">
          {{ form.content.length }} / 2000
        </div>
      </div>

      <!-- 截图 -->
      <div class="section-label">截图：<span class="optional-text">选填，最多{{ MAX_IMAGES }}张</span></div>
      <div class="images-row">
        <div
          v-for="(img, i) in previewImages"
          :key="i"
          class="image-thumb"
          @click="removeImage(i)"
        >
          <img :src="img.url" :alt="'图片' + (i + 1)" />
          <div class="image-remove">×</div>
        </div>
        <div
          v-if="previewImages.length < MAX_IMAGES"
          class="image-add"
          @click="openPicker"
        >
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <path d="M12 5v14M5 12h14" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
          </svg>
        </div>
      </div>
      <input
        ref="fileInputRef"
        type="file"
        accept="image/*"
        multiple
        class="file-input-hidden"
        @change="onFilesSelected"
      />

      <!-- 联系方式 -->
      <div class="section-label">联系方式：<span class="optional-text">选填</span></div>
      <input
        v-model="form.contact"
        class="contact-input"
        placeholder="手机号、邮箱或微信号，便于我们跟进"
        maxlength="64"
      />

      <!-- 提交按钮 -->
      <button
        class="submit-btn"
        :class="{ submitting: submitting }"
        :disabled="submitting"
        @click="handleSubmit"
      >
        {{ submitting ? '提交中...' : '提交反馈' }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { showSuccessToast, showFailToast } from 'vant'
import { submitFeedback } from '@/api/feedback'
import { compressImage } from '@/utils/imageCompress'

const router = useRouter()

const MAX_IMAGES = 5

const feedbackTypes = [
  { value: 'bug', label: 'BUG反馈', emoji: '🐛' },
  { value: 'feature', label: '功能建议', emoji: '💡' },
  { value: 'performance', label: '测速反馈', emoji: '⚡' },
  { value: 'other', label: '其他', emoji: '🎯' }
]

const quickTags = ['卡顿', '上传失败', 'AI 不准']

const form = reactive({
  type: '',
  content: '',
  contact: ''
})

const fileInputRef = ref(null)
const previewImages = ref([])
const compressedFiles = ref([])
const submitting = ref(false)

function openPicker() {
  fileInputRef.value?.click()
}

async function onFilesSelected(e) {
  const files = Array.from(e.target.files || [])
  e.target.value = ''

  const remaining = MAX_IMAGES - previewImages.value.length
  if (files.length > remaining) {
    showFailToast(`最多还能添加 ${remaining} 张图片`)
    return
  }

  for (const file of files) {
    const compressed = await compressImage(file)
    compressedFiles.value.push(compressed)
    previewImages.value.push({
      url: URL.createObjectURL(compressed),
      name: file.name
    })
  }
}

function removeImage(index) {
  URL.revokeObjectURL(previewImages.value[index].url)
  previewImages.value.splice(index, 1)
  compressedFiles.value.splice(index, 1)
}

function appendContent(tag) {
  if (form.content.length >= 2000) return
  const prefix = form.content && !form.content.endsWith('\n') ? '\n' : ''
  form.content += prefix + tag
}

async function handleSubmit() {
  if (!form.type) {
    showFailToast('请选择反馈类型')
    return
  }
  if (!form.content.trim()) {
    showFailToast('请填写详细描述')
    return
  }

  submitting.value = true
  try {
    const formData = new FormData()
    formData.append('type', form.type)
    formData.append('content', form.content.trim())
    if (form.contact.trim()) {
      formData.append('contact', form.contact.trim())
    }
    formData.append('appVersion', '1.0.0')
    compressedFiles.value.forEach((file) => {
      formData.append('images', file, file.name || 'image.jpg')
    })

    await submitFeedback(formData)
    showSuccessToast('感谢反馈！我们会尽快处理')
    router.back()
  } catch {
    // 错误已在拦截器中 toast
  } finally {
    submitting.value = false
  }
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: $bg-base;
  padding-bottom: $space-3xl;
}

// ====== 顶栏 ======
.nav-bar {
  position: sticky;
  top: 0;
  z-index: $z-navbar;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px $space-md;
  padding-top: calc(12px + #{$safe-top});
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid $border-light;

  .back-btn {
    width: 40px; height: 40px;
    display: flex; align-items: center; justify-content: center;
    color: $text-primary;
  }
  .nav-title {
    font-size: $font-md;
    font-weight: 600;
    color: $text-primary;
  }
}

.my-feedback-link {
  font-size: $font-sm;
  font-weight: 400;
  color: $color-primary;
  cursor: pointer;

  &:active { opacity: 0.7; }
}

// ====== 引导提示 ======
.guide-banner {
  padding: 8px $space-lg $space-lg;
}

.guide-card {
  background: #F2F2F5;
  border-radius: $radius-lg;
  padding: $space-md $space-lg;
  display: flex;
  align-items: center;
  gap: $space-sm;

  .guide-emoji {
    font-size: 28px;
    flex-shrink: 0;
  }

  .guide-text {
    font-size: $font-xs;
    color: $text-primary;
    line-height: 1.6;
  }

  .highlight {
    color: #FF9500;
    font-weight: 600;
  }
}

// ====== 内容区 ======
.content {
  padding: 0 $space-lg;
}

.section-label {
  font-size: $font-sm;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: $space-md;

  .optional-text {
    font-weight: normal;
    color: $text-secondary;
  }
  margin-top: $space-lg;

  &:first-child { margin-top: 0; }
}

.section-hint {
  margin: -4px 0 $space-sm;
  font-size: $font-sm;
  color: $text-secondary;
  line-height: 1.4;
}

// ====== 类型卡片 2×2 ======
.type-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: $space-md;
}

.type-card {
  height: 64px;
  border-radius: $radius-lg;
  display: flex;
  align-items: center;
  padding: 0 $space-md;
  cursor: pointer;
  transition: all 0.15s;
  border: 1px solid #E5E5E5;
  background: #FFFFFF;
  position: relative;
  user-select: none;

  &.bug {
    &.active { 
      border-color: #D70015; 
      background: #FFF5F5;
      .check-mark { color: #D70015; }
    }
  }
  &.feature {
    &.active { 
      border-color: #FF9500; 
      background: #FFF8F0;
      .check-mark { color: #FF9500; }
    }
  }
  &.performance {
    &.active { 
      border-color: #007AFF; 
      background: #F0F7FF;
      .check-mark { color: #007AFF; }
    }
  }
  &.other {
    &.active { 
      border-color: #9C27B0; 
      background: #FDF5FF;
      .check-mark { color: #9C27B0; }
    }
  }

  &:active { transform: scale(0.98); }
}

.type-emoji {
  font-size: 20px;
  margin-right: $space-sm;
}

.type-name {
  font-size: $font-sm;
  font-weight: 500;
  color: $text-primary;
  flex: 1;
}

.check-mark {
  position: absolute;
  top: 8px;
  right: 8px;
}

// ====== 文本域容器 ======
.textarea-wrapper {
  position: relative;
}

// ====== 快捷标签 ======
.quick-tags {
  position: absolute;
  bottom: $space-md;
  left: $space-md;
  display: flex;
  gap: $space-xs;
  flex-wrap: wrap;
}

.quick-tag {
  padding: 4px 10px;
  background: #F5F5F5;
  border-radius: 12px;
  font-size: $font-xs;
  color: #666666;
  cursor: pointer;
  user-select: none;
  transition: all 0.15s;
  backdrop-filter: blur(4px);

  &:active { background: rgba(37, 99, 235, 0.1); }
}

.section-hint {
  position: absolute;
  top: $space-md;
  left: $space-md;
  margin: 0;
  pointer-events: none;
  opacity: 1;
}

// ====== 文本域 ======
.content-input {
  width: 100%;
  min-height: 140px;
  padding: $space-md;
  padding-bottom: 40px;
  background: $bg-card;
  border: 1px solid $border-light;
  border-radius: $radius-lg;
  font-size: $font-base;
  color: $text-primary;
  outline: none;
  resize: vertical;
  font-family: inherit;
  line-height: 1.6;
  transition: border-color 0.15s;

  &:focus { border-color: $color-primary; }
}

.char-count {
  position: absolute;
  bottom: $space-md;
  right: $space-md;
  text-align: right;
  font-size: $font-xs;
  color: $text-secondary;
  margin-top: $space-xs;

  &.over { color: #D70015; }
}

// ====== 截图 ======
.images-row {
  display: flex;
  flex-wrap: wrap;
  gap: $space-sm;
}

.image-thumb {
  width: 80px;
  height: 80px;
  border-radius: $radius-md;
  overflow: hidden;
  position: relative;
  cursor: pointer;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  .image-remove {
    position: absolute;
    top: 4px; right: 4px;
    width: 20px; height: 20px;
    background: rgba(0, 0, 0, 0.5);
    color: white;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    line-height: 1;
  }
}

.image-add {
  width: 80px;
  height: 80px;
  border: 1.5px dashed $border-base;
  border-radius: $radius-md;
  display: flex;
  align-items: center;
  justify-content: center;
  color: $text-secondary;
  cursor: pointer;
  transition: all 0.15s;

  &:active { border-color: $color-primary; color: $color-primary; }
}

.file-input-hidden { display: none; }

// ====== 联系方式 ======
.contact-input {
  width: 100%;
  height: 48px;
  padding: 0 $space-md;
  background: $bg-card;
  border: 1px solid $border-light;
  border-radius: $radius-lg;
  font-size: $font-base;
  color: $text-primary;
  outline: none;
  transition: border-color 0.15s;

  &:focus { border-color: $color-primary; }
  &::placeholder { color: $text-placeholder; }
}

// ====== 提交按钮 ======
.submit-btn {
  width: 100%;
  height: 52px;
  margin-top: $space-2xl;
  background: $gradient-primary;
  color: white;
  border-radius: $radius-md;
  font-size: $font-lg;
  font-weight: 600;
  cursor: pointer;
  box-shadow: $shadow-glow;
  transition: all 0.15s;

  &:active { transform: scale(0.98); }
  &.submitting { opacity: 0.7; pointer-events: none; }
}
</style>