<template>
  <div class="auth-page">
    <!-- 背景装饰 -->
    <div class="bg-decoration">
      <div class="orb orb-1"></div>
      <div class="orb orb-2"></div>
      <div class="orb orb-3"></div>
    </div>

    <div class="auth-content">
      <div class="brand">
        <div class="brand-logo">
          <svg viewBox="0 0 64 64" width="56" height="56">
            <defs>
              <linearGradient id="logoGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stop-color="#3B82F6" />
                <stop offset="100%" stop-color="#8B5CF6" />
              </linearGradient>
            </defs>
            <!-- 雪花/山形组合 -->
            <path d="M32 8 L48 40 L40 40 L32 28 L24 40 L16 40 Z" fill="url(#logoGrad)" />
            <circle cx="32" cy="48" r="4" fill="#06B6D4" />
          </svg>
        </div>
        <h1 class="brand-title">Ski Coach</h1>
        <p class="brand-subtitle">AI 教练 · 让每次滑行都有进步</p>
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
          <div class="password-wrap">
            <input
              v-model="form.password"
              :type="showPwd ? 'text' : 'password'"
              placeholder="请输入密码"
              @keyup.enter="handleSubmit"
            />
            <button class="pwd-toggle" type="button" @click="showPwd = !showPwd">
              {{ showPwd ? '🙈' : '👁' }}
            </button>
          </div>
        </div>

        <button
          class="primary-btn"
          :disabled="loading"
          @click="handleSubmit"
        >
          <span v-if="!loading">登 录</span>
          <span v-else class="loading-dots"><span></span><span></span><span></span></span>
        </button>

        <div class="auth-footer">
          还没有账号?<router-link to="/register">立即注册</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showSuccessToast, showFailToast } from 'vant'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loading = ref(false)
const showPwd = ref(false)
const form = reactive({ phone: '', password: '' })

async function handleSubmit() {
  if (!/^1[3-9]\d{9}$/.test(form.phone)) {
    showFailToast('手机号格式不正确')
    return
  }
  if (form.password.length < 6) {
    showFailToast('密码至少6位')
    return
  }

  loading.value = true
  try {
    await userStore.login(form)
    showSuccessToast('登录成功')
    const redirect = route.query.redirect || '/'
    router.push(redirect)
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

// 背景装饰光球
.bg-decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;

  .orb {
    position: absolute;
    border-radius: 50%;
    filter: blur(60px);
    opacity: 0.5;
  }
  .orb-1 {
    top: -100px; right: -80px;
    width: 280px; height: 280px;
    background: radial-gradient(circle, #3B82F6 0%, transparent 70%);
  }
  .orb-2 {
    bottom: -60px; left: -100px;
    width: 320px; height: 320px;
    background: radial-gradient(circle, #8B5CF6 0%, transparent 70%);
  }
  .orb-3 {
    top: 30%; left: 50%; transform: translateX(-50%);
    width: 200px; height: 200px;
    background: radial-gradient(circle, #06B6D4 0%, transparent 70%);
    opacity: 0.3;
  }
}

.auth-content {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  padding: 60px $space-2xl 40px;
  display: flex;
  flex-direction: column;
  padding-top: calc(60px + #{$safe-top});
}

.brand {
  text-align: center;
  margin-bottom: 50px;

  .brand-logo {
    width: 80px;
    height: 80px;
    margin: 0 auto $space-lg;
    background: $glass-bg;
    border: $glass-border;
    border-radius: $radius-2xl;
    display: flex;
    align-items: center;
    justify-content: center;
    backdrop-filter: $glass-blur;
    box-shadow: $shadow-glow;
  }

  .brand-title {
    font-size: 32px;
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

.auth-form {
  flex: 1;
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

.password-wrap {
  position: relative;

  .pwd-toggle {
    position: absolute;
    right: $space-md;
    top: 50%;
    transform: translateY(-50%);
    cursor: pointer;
    font-size: 18px;
    padding: 4px;
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
  position: relative;
  box-shadow: $shadow-glow;
  transition: transform 0.15s, opacity 0.15s;

  &:active { transform: scale(0.98); }
  &:disabled { opacity: 0.7; }
}

.loading-dots {
  display: inline-flex;
  gap: 4px;

  span {
    width: 6px;
    height: 6px;
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
