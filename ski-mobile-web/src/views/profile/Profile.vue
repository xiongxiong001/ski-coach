<template>
  <div class="profile-page">
    <!-- 顶部背景 -->
    <header class="profile-hero">
      <div class="hero-bg">
        <div class="orb orb-1"></div>
        <div class="orb orb-2"></div>
      </div>
      <div class="hero-content">
        <div class="avatar-large">
          {{ avatarText }}
        </div>
        <div class="nick-row">
          <span class="nickname">{{ userStore.nickname }}</span>
          <button class="edit-btn" @click="showEditDialog = true">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
              <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7M18.5 2.5a2.121 2.121 0 113 3L12 15l-4 1 1-4 9.5-9.5z"
                    stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
          </button>
        </div>
        <div class="phone">{{ userStore.userInfo?.phone || '' }}</div>
      </div>
    </header>

    <!-- 菜单卡片 -->
    <div class="menu-section">
      <div class="menu-card">
        <div class="menu-item" @click="router.push('/videos')">
          <div class="menu-icon thumb-blue">🎬</div>
          <div class="menu-info">
            <div class="menu-title">我的视频</div>
            <div class="menu-desc">查看上传的所有视频</div>
          </div>
          <div class="menu-arrow">›</div>
        </div>
        <div class="menu-item" @click="router.push('/comparisons')">
          <div class="menu-icon thumb-purple">📊</div>
          <div class="menu-info">
            <div class="menu-title">对比报告</div>
            <div class="menu-desc">看到自己的进步轨迹</div>
          </div>
          <div class="menu-arrow">›</div>
        </div>
      </div>

      <div class="menu-card">
        <div class="menu-item" @click="router.push('/feedback')">
          <div class="menu-icon thumb-green">💬</div>
          <div class="menu-info">
            <div class="menu-title">用户反馈</div>
            <div class="menu-desc">帮助我们做得更好</div>
          </div>
          <div class="menu-arrow">›</div>
        </div>
      </div>

      <div class="menu-card">
        <div class="menu-item" @click="showAbout">
          <div class="menu-icon thumb-cyan">ℹ️</div>
          <div class="menu-info">
            <div class="menu-title">关于我们</div>
            <div class="menu-desc">让 AI 成为你的滑雪教练</div>
          </div>
          <div class="menu-arrow">›</div>
        </div>
      </div>

      <button class="logout-btn" @click="handleLogout">
        退出登录
      </button>
    </div>

    <!-- 修改昵称弹窗 -->
    <van-dialog
      v-model:show="showEditDialog"
      title="修改昵称"
      show-cancel-button
      confirm-button-color="#2563EB"
      :before-close="handleSaveNickname"
    >
      <div class="dialog-input-wrap">
        <input
          v-model="newNickname"
          class="dialog-input"
          placeholder="请输入昵称"
          maxlength="50"
        />
      </div>
    </van-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { showSuccessToast, showFailToast, showConfirmDialog, showDialog } from 'vant'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const showEditDialog = ref(false)
const newNickname = ref('')

const avatarText = computed(() => {
  return userStore.nickname?.charAt(0) || '雪'
})

watch(showEditDialog, (val) => {
  if (val) newNickname.value = userStore.userInfo?.nickname || ''
})

async function handleSaveNickname(action) {
  if (action === 'cancel') return true
  if (!newNickname.value.trim()) {
    showFailToast('昵称不能为空')
    return false
  }
  try {
    await userStore.updateNickname(newNickname.value.trim())
    showSuccessToast('已更新')
    return true
  } catch {
    return false
  }
}

function showAbout() {
  showDialog({
    title: 'Ski Coach',
    message: 'AI 滑雪教练\n基于姿态识别和大语言模型,\n为每个雪友提供个性化点评\n\nv1.0.0',
    confirmButtonText: '知道了',
    confirmButtonColor: '#2563EB'
  })
}

async function handleLogout() {
  try {
    await showConfirmDialog({
      title: '退出登录',
      message: '确定要退出当前账号吗?',
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      confirmButtonColor: '#EF4444'
    })
  } catch {
    return
  }
  await userStore.logout()
  showSuccessToast('已退出登录')
  router.push('/login')
}
</script>

<style lang="scss" scoped>
.profile-page {
  min-height: 100vh;
  background: $bg-base;
}

.profile-hero {
  position: relative;
  padding: calc(40px + #{$safe-top}) $space-lg $space-3xl;
  background: linear-gradient(180deg, #DBEAFE 0%, $bg-base 100%);
  overflow: hidden;
}

.hero-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;

  .orb {
    position: absolute;
    border-radius: 50%;
    filter: blur(60px);
  }
  .orb-1 {
    top: -50px; right: -50px;
    width: 200px; height: 200px;
    background: radial-gradient(circle, rgba(139,92,246,0.4) 0%, transparent 70%);
  }
  .orb-2 {
    bottom: -50px; left: -50px;
    width: 220px; height: 220px;
    background: radial-gradient(circle, rgba(59,130,246,0.4) 0%, transparent 70%);
  }
}

.hero-content {
  position: relative;
  z-index: 1;
  text-align: center;
}

.avatar-large {
  width: 80px;
  height: 80px;
  margin: 0 auto $space-md;
  border-radius: 50%;
  background: $gradient-primary;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  font-weight: 600;
  color: white;
  box-shadow: $shadow-glow;
  border: 3px solid rgba(255,255,255,0.1);
}

.nick-row {
  display: inline-flex;
  align-items: center;
  gap: $space-sm;
  margin-bottom: 4px;
}

.nickname {
  font-size: $font-2xl;
  font-weight: 600;
  color: $text-primary;
}

.edit-btn {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: rgba(255,255,255,0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  color: $text-secondary;
  cursor: pointer;
  transition: all 0.15s;

  &:active {
    background: rgba(255,255,255,0.15);
    color: $text-primary;
  }
}

.phone {
  font-size: $font-sm;
  color: $text-secondary;
}

.menu-section {
  padding: 0 $space-lg $space-2xl;
  margin-top: -$space-lg;
  position: relative;
  z-index: 2;
}

.menu-card {
  background: $bg-card;
  border: 1px solid $border-light;
  border-radius: $radius-lg;
  margin-bottom: $space-md;
  overflow: hidden;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: $space-md $space-lg;
  cursor: pointer;
  transition: background 0.15s;
  border-bottom: 1px solid $border-light;

  &:last-child { border-bottom: none; }
  &:active { background: rgba(255,255,255,0.02); }
}

.menu-icon {
  width: 40px;
  height: 40px;
  border-radius: $radius-md;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  margin-right: $space-md;
  flex-shrink: 0;

  &.thumb-blue { background: linear-gradient(135deg, #3B82F6, #1e40af); }
  &.thumb-purple { background: linear-gradient(135deg, #8B5CF6, #6d28d9); }
  &.thumb-cyan { background: linear-gradient(135deg, #06B6D4, #0e7490); }
  &.thumb-green { background: linear-gradient(135deg, #10B981, #047857); }
}

.menu-info {
  flex: 1;
  min-width: 0;
}

.menu-title {
  font-size: $font-md;
  font-weight: 500;
  color: $text-primary;
  margin-bottom: 2px;
}

.menu-desc {
  font-size: $font-xs;
  color: $text-secondary;
}

.menu-arrow {
  font-size: 22px;
  color: $text-secondary;
  line-height: 1;
}

.logout-btn {
  width: 100%;
  height: 48px;
  margin-top: $space-2xl;
  background: $bg-card;
  border: 1px solid rgba(239, 68, 68, 0.3);
  border-radius: $radius-md;
  color: $color-danger;
  font-size: $font-md;
  font-weight: 500;
  cursor: pointer;

  &:active {
    background: rgba(239, 68, 68, 0.05);
  }
}

.dialog-input-wrap {
  padding: $space-md $space-lg;
}

.dialog-input {
  width: 100%;
  height: 44px;
  padding: 0 $space-md;
  background: $bg-elevated;
  border: 1px solid $border-light;
  border-radius: $radius-md;
  color: $text-primary;
  font-size: $font-base;
  outline: none;

  &:focus {
    border-color: $color-primary;
  }
}
</style>
