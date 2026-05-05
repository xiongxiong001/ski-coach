<template>
  <div class="page">
    <header class="nav-bar">
      <button class="back-btn" @click="router.back()">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
          <path d="M15 19L8 12l7-7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
      </button>
      <h1 class="nav-title">用户反馈</h1>
      <div class="nav-spacer"></div>
    </header>

    <div class="content">
      <!-- 反馈类型 -->
      <div class="section-label">
        反馈类型
        <span class="my-feedback-link" @click="router.push('/feedback/list')">我的反馈 </span>
      </div>
      <div class="type-grid">
        <div
          v-for="t in feedbackTypes"
          :key="t.value"
          class="type-card"
          :class="{ active: form.type === t.value }"
          @click="form.type = t.value"
        >
          <div class="type-emoji">{{ t.emoji }}</div>
          <div class="type-name">{{ t.label }}</div>
        </div>
      </div>

      <!-- 详细描述 -->
      <div class="section-label">详细描述</div>
      <textarea
        v-model="form.content"
        class="content-input"
        placeholder="请详细描述您遇到的问题或建议..."
        maxlength="2000"
        rows="6"
      ></textarea>
      <div class="char-count">{{ form.content.length }}/2000</div>

      <!-- 联系方式 -->
      <div class="section-label">联系方式（选填）</div>
      <input
        v-model="form.contact"
        class="contact-input"
        placeholder="手机号或邮箱，方便我们联系您"
        maxlength="64"
      />

      <!-- 截图 -->
      <div class="section-label">
        截图/照片（选填，最多{{ MAX_IMAGES }}张）
        <span class="label-hint">上传前会自动压缩</span>
      </div>
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
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none">
            <path d="M12 5v14M5 12h14" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
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
  padding-bottom: $space-2xl;
}

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
  .nav-spacer { width: 40px; }
}

.content {
  padding: $space-lg;
}

.section-label {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  font-size: $font-sm;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: $space-md;
  margin-top: $space-lg;

  &:first-child { margin-top: 0; }

  .label-hint {
    font-weight: 400;
    color: $text-secondary;
    font-size: $font-xs;
    margin-left: $space-sm;
  }
}

.my-feedback-link {
  font-size: $font-xs;
  font-weight: 400;
  color: $text-secondary;
  cursor: pointer;

  &:active { color: $color-primary; }
}

.type-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: $space-sm;
}

.type-card {
  background: $bg-card;
  border: 1.5px solid $border-light;
  border-radius: $radius-md;
  padding: $space-md $space-sm;
  text-align: center;
  cursor: pointer;
  transition: all 0.15s;

  .type-emoji { font-size: 22px; margin-bottom: 2px; }
  .type-name {
    font-size: $font-xs;
    font-weight: 500;
    color: $text-primary;
  }

  &.active {
    border-color: $color-primary;
    background: rgba(37, 99, 235, 0.06);
    box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.08);
  }

  &:active { transform: scale(0.97); }
}

.content-input {
  width: 100%;
  padding: $space-md;
  background: $bg-card;
  border: 1px solid $border-light;
  border-radius: $radius-lg;
  font-size: $font-base;
  color: $text-primary;
  outline: none;
  resize: vertical;
  font-family: inherit;
  line-height: 1.6;

  &:focus { border-color: $color-primary; }
  &::placeholder { color: $text-placeholder; }
}

.char-count {
  text-align: right;
  font-size: $font-xs;
  color: $text-secondary;
  margin-top: $space-xs;
}

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

  &:focus { border-color: $color-primary; }
  &::placeholder { color: $text-placeholder; }
}

.images-row {
  display: flex;
  flex-wrap: wrap;
  gap: $space-md;
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
    top: 2px; right: 2px;
    width: 22px; height: 22px;
    background: rgba(0, 0, 0, 0.55);
    color: white;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 14px;
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

.file-input-hidden {
  display: none;
}

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