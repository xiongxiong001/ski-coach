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
                <stop offset="0%" stop-color="#2563EB" />
                <stop offset="100%" stop-color="#8B5CF6" />
              </linearGradient>
            </defs>
            <path d="M32 8 L48 40 L40 40 L32 28 L24 40 L16 40 Z" fill="url(#logoGrad)" />
            <circle cx="32" cy="48" r="4" fill="#06B6D4" />
          </svg>
        </div>
        <h1 class="brand-title">Ski Coach</h1>
        <p class="brand-subtitle">AI 教练 · 让每次滑行都有进步</p>
      </div>

      <!-- 登录方式切换 -->
      <div class="segment">
        <button
          :class="['segment-item', { active: loginMode === 'sms' }]"
          @click="loginMode = 'sms'"
        >验证码登录注册</button>
        <button
          :class="['segment-item', { active: loginMode === 'password' }]"
          @click="loginMode = 'password'"
        >密码登录</button>
      </div>

      <!-- 验证码登录注册 -->
      <div v-show="loginMode === 'sms'" class="auth-form">
        <div class="form-item">
          <label>手机号</label>
          <input
            v-model="smsForm.phone"
            type="tel"
            placeholder="请输入手机号"
            maxlength="11"
            inputmode="numeric"
          />
        </div>

        <div class="form-item">
          <label>验证码</label>
          <div class="sms-row">
            <input
              v-model="smsForm.code"
              type="tel"
              placeholder="请输入验证码"
              maxlength="6"
              inputmode="numeric"
            />
            <button
              class="sms-btn"
              :disabled="!!countdown || smsForm.phone.length !== 11"
              @click="handleSendCode"
            >
              {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
            </button>
          </div>
        </div>

        <p class="sms-hint">未注册手机号验证后自动创建账号</p>

        <button
          class="primary-btn"
          :disabled="smsLoading"
          @click="handleSmsLogin"
        >
          <span v-if="!smsLoading">登录 / 注册</span>
          <span v-else class="loading-dots"><span></span><span></span><span></span></span>
        </button>
      </div>

      <!-- 密码登录 -->
      <div v-show="loginMode === 'password'" class="auth-form">
        <div class="form-item">
          <label>手机号</label>
          <input
            v-model="passwordForm.phone"
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
              v-model="passwordForm.password"
              :type="showPwd ? 'text' : 'password'"
              placeholder="请输入密码"
              @keyup.enter="handlePasswordLogin"
            />
            <button class="pwd-toggle" type="button" @click="showPwd = !showPwd">
              {{ showPwd ? '🙈' : '👁' }}
            </button>
          </div>
        </div>

        <button
          class="primary-btn"
          :disabled="passwordLoading"
          @click="handlePasswordLogin"
        >
          <span v-if="!passwordLoading">登 录</span>
          <span v-else class="loading-dots"><span></span><span></span><span></span></span>
        </button>

        <p class="switch-hint">
          还没有账号？<a @click="loginMode = 'sms'">验证码登录注册</a>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showSuccessToast, showFailToast } from 'vant'
import { useUserStore } from '@/stores/user'
import { sendSmsCode } from '@/api/auth'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loginMode = ref('sms')

// ==================== 验证码登录注册 ====================
const smsLoading = ref(false)
const countdown = ref(0)
let countdownTimer = null
const smsForm = reactive({ phone: '', code: '' })

async function handleSendCode() {
  if (!/^1[3-9]\d{9}$/.test(smsForm.phone)) {
    showFailToast('手机号格式不正确')
    return
  }

  try {
    await sendSmsCode(smsForm.phone)
    showSuccessToast('验证码已发送')
    countdown.value = 60
    countdownTimer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(countdownTimer)
        countdownTimer = null
      }
    }, 1000)
  } catch {}
}

async function handleSmsLogin() {
  if (!/^1[3-9]\d{9}$/.test(smsForm.phone)) {
    showFailToast('手机号格式不正确')
    return
  }
  if (!smsForm.code) {
    showFailToast('请输入验证码')
    return
  }

  smsLoading.value = true
  try {
    await userStore.smsLogin({ phone: smsForm.phone, code: smsForm.code })
    showSuccessToast('登录成功')
    const redirect = route.query.redirect || '/'
    router.push(redirect)
  } catch {} finally {
    smsLoading.value = false
  }
}

// ==================== 密码登录 ====================
const passwordLoading = ref(false)
const showPwd = ref(false)
const passwordForm = reactive({ phone: '', password: '' })

async function handlePasswordLogin() {
  if (!/^1[3-9]\d{9}$/.test(passwordForm.phone)) {
    showFailToast('手机号格式不正确')
    return
  }
  if (passwordForm.password.length < 6) {
    showFailToast('密码至少6位')
    return
  }

  passwordLoading.value = true
  try {
    await userStore.login(passwordForm)
    showSuccessToast('登录成功')
    const redirect = route.query.redirect || '/'
    router.push(redirect)
  } catch {} finally {
    passwordLoading.value = false
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
    opacity: 0.3;
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
  margin-bottom: 40px;

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

// 分段切换
.segment {
  display: flex;
  background: $glass-bg;
  border: $glass-border;
  border-radius: $radius-md;
  padding: 4px;
  margin-bottom: $space-2xl;

  .segment-item {
    flex: 1;
    height: 38px;
    border-radius: calc(#{$radius-md} - 2px);
    font-size: $font-sm;
    font-weight: 500;
    color: $text-secondary;
    cursor: pointer;
    transition: all 0.25s;
    white-space: nowrap;

    &.active {
      background: white;
      color: $color-primary;
      box-shadow: 0 2px 8px rgba(0,0,0,0.08);
    }
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

.sms-row {
  display: flex;
  gap: 12px;

  input {
    flex: 1;
  }

  .sms-btn {
    height: 52px;
    padding: 0 $space-lg;
    white-space: nowrap;
    background: $glass-bg;
    border: 1.5px solid $border-light;
    border-radius: $radius-md;
    color: $color-primary;
    font-size: $font-base;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s;

    &:disabled {
      color: $text-placeholder;
    }
  }
}

.sms-hint {
  text-align: center;
  color: $text-placeholder;
  font-size: $font-xs;
  margin-top: -$space-sm;
  margin-bottom: $space-lg;
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

.switch-hint {
  margin-top: $space-xl;
  text-align: center;
  color: $text-secondary;
  font-size: $font-base;

  a {
    color: $color-primary;
    margin-left: 4px;
    font-weight: 500;
    cursor: pointer;
  }
}
</style>
