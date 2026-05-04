<template>
  <div class="auth-page">
    <div class="bg-decoration">
      <div class="orb orb-1"></div>
      <div class="orb orb-2"></div>
    </div>

    <div class="auth-content">
      <button class="back-btn" @click="router.back()">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
          <path d="M15 19L8 12l7-7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
      </button>

      <div class="brand">
        <h1 class="brand-title">注册新账号</h1>
        <p class="brand-subtitle">开启你的 AI 滑雪之旅</p>
      </div>

      <div class="auth-form">
        <div class="form-item">
          <label>手机号</label>
          <input
            v-model="form.phone"
            type="tel"
            placeholder="请输入手机号"
            maxlength="11"
            inputmode="numeric"
          />
        </div>

        <div class="form-item">
          <label>密码</label>
          <input
            v-model="form.password"
            type="password"
            placeholder="6-32位密码"
          />
        </div>

        <div class="form-item">
          <label>昵称(可选)</label>
          <input
            v-model="form.nickname"
            type="text"
            placeholder="留空将自动生成"
            maxlength="50"
          />
        </div>

        <button
          class="primary-btn"
          :disabled="loading"
          @click="handleSubmit"
        >
          <span v-if="!loading">注 册</span>
          <span v-else class="loading-dots"><span></span><span></span><span></span></span>
        </button>

        <div class="auth-footer">
          已有账号?<router-link to="/login">立即登录</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { showSuccessToast, showFailToast } from 'vant'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const form = reactive({ phone: '', password: '', nickname: '' })

async function handleSubmit() {
  if (!/^1[3-9]\d{9}$/.test(form.phone)) {
    showFailToast('手机号格式不正确')
    return
  }
  if (form.password.length < 6 || form.password.length > 32) {
    showFailToast('密码长度6-32位')
    return
  }

  loading.value = true
  try {
    await userStore.register({
      phone: form.phone,
      password: form.password,
      nickname: form.nickname || undefined
    })
    showSuccessToast('注册成功')
    router.push('/')
  } catch {} finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.auth-page {
  min-height: 100vh;
  position: relative;
  overflow: hidden;
  background: $bg-base;
}

.bg-decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;

  .orb {
    position: absolute;
    border-radius: 50%;
    filter: blur(60px);
    opacity: 0.2;
  }
  .orb-1 {
    top: -120px; right: -100px;
    width: 320px; height: 320px;
    background: radial-gradient(circle, #8B5CF6 0%, transparent 70%);
  }
  .orb-2 {
    bottom: -80px; left: -100px;
    width: 280px; height: 280px;
    background: radial-gradient(circle, #3B82F6 0%, transparent 70%);
  }
}

.auth-content {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  padding: 20px $space-2xl 40px;
  padding-top: calc(20px + #{$safe-top});
}

.back-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: $text-primary;
  cursor: pointer;
  margin-bottom: $space-2xl;
}

.brand {
  margin-bottom: 40px;

  .brand-title {
    font-size: 28px;
    font-weight: 700;
    background: $gradient-primary;
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
    margin-bottom: $space-sm;
    letter-spacing: -0.5px;
  }

  .brand-subtitle {
    color: $text-secondary;
    font-size: $font-md;
  }
}

.form-item {
  margin-bottom: $space-xl;

  label {
    display: block;
    font-size: $font-sm;
    color: $text-regular;
    margin-bottom: $space-sm;
    font-weight: 500;
  }

  input {
    width: 100%;
    height: 52px;
    padding: 0 $space-lg;
    background: $glass-bg;
    border: 1.5px solid $border-light;
    border-radius: $radius-md;
    color: $text-primary;
    font-size: $font-lg;
    outline: none;
    transition: all 0.2s;

    &::placeholder { color: $text-placeholder; }
    &:focus {
      border-color: $color-primary;
      background: rgba(59, 130, 246, 0.08);
      box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.1);
    }
  }
}

.primary-btn {
  width: 100%;
  height: 52px;
  margin-top: $space-md;
  background: $gradient-primary;
  border-radius: $radius-md;
  color: white;
  font-size: $font-lg;
  font-weight: 600;
  cursor: pointer;
  box-shadow: $shadow-glow;
  transition: transform 0.15s, opacity 0.15s;

  &:active { transform: scale(0.98); }
  &:disabled { opacity: 0.7; }
}

.loading-dots {
  display: inline-flex;
  gap: 4px;

  span {
    width: 6px; height: 6px;
    background: white;
    border-radius: 50%;
    animation: dots 1.4s infinite ease-in-out both;
    &:nth-child(1) { animation-delay: -0.32s; }
    &:nth-child(2) { animation-delay: -0.16s; }
  }
}
@keyframes dots {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

.auth-footer {
  margin-top: $space-2xl;
  text-align: center;
  color: $text-secondary;
  font-size: $font-base;

  a {
    color: $color-primary;
    margin-left: 4px;
    font-weight: 500;
  }
}
</style>
