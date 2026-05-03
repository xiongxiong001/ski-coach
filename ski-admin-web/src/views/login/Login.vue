<template>
  <div class="login-page">
    <!-- 左侧:产品介绍 -->
    <div class="login-left">
      <div class="brand">
        <div class="brand-icon">⛷</div>
        <h1>Ski Coach</h1>
        <p>管理后台 · Admin Console</p>
      </div>
      <div class="features">
        <div class="feature">
          <span class="feature-icon">📊</span>
          <span>实时数据看板</span>
        </div>
        <div class="feature">
          <span class="feature-icon">👥</span>
          <span>用户管理</span>
        </div>
        <div class="feature">
          <span class="feature-icon">⚙️</span>
          <span>AI 任务监控</span>
        </div>
        <div class="feature">
          <span class="feature-icon">💰</span>
          <span>成本分析</span>
        </div>
      </div>
    </div>

    <!-- 右侧:登录表单 -->
    <div class="login-right">
      <div class="login-card">
        <h2>管理员登录</h2>
        <p class="subtitle">请使用管理员账号登录</p>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          size="large"
          @submit.prevent="handleSubmit"
        >
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名" prefix-icon="User" />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              show-password
              prefix-icon="Lock"
              @keyup.enter="handleSubmit"
            />
          </el-form-item>

          <el-button
            type="primary"
            :loading="loading"
            style="width: 100%"
            size="large"
            @click="handleSubmit"
          >
            登 录
          </el-button>
        </el-form>

        <div class="hint">
          <span class="hint-text">默认账号:admin / admin123</span>
          <span class="hint-warning">⚠ 生产环境请务必修改</span>
        </div>
      </div>

      <div class="footer">
        © {{ new Date().getFullYear() }} Ski Coach · AI 滑雪教练
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAdminStore } from '@/stores/admin'

const router = useRouter()
const route = useRoute()
const adminStore = useAdminStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleSubmit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    await adminStore.login(form)
    ElMessage.success('登录成功')
    const redirect = route.query.redirect || '/dashboard'
    router.push(redirect)
  } catch {} finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  display: flex;
}

// 左侧
.login-left {
  flex: 1;
  background: linear-gradient(135deg, $color-primary-dark 0%, #6d28d9 100%);
  color: white;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: $space-2xl $space-2xl;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: -100px; right: -100px;
    width: 400px; height: 400px;
    background: radial-gradient(circle, rgba(255,255,255,0.1), transparent 70%);
    border-radius: 50%;
  }
  &::after {
    content: '';
    position: absolute;
    bottom: -80px; left: -80px;
    width: 300px; height: 300px;
    background: radial-gradient(circle, rgba(139,92,246,0.3), transparent 70%);
    border-radius: 50%;
  }

  @media (max-width: 768px) {
    display: none;
  }
}

.brand {
  position: relative;
  z-index: 1;
  margin-bottom: $space-2xl;

  .brand-icon { font-size: 56px; margin-bottom: $space-md; }
  h1 {
    font-size: 36px;
    font-weight: 700;
    margin-bottom: $space-sm;
    letter-spacing: -1px;
  }
  p {
    font-size: $font-md;
    opacity: 0.85;
  }
}

.features {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: $space-md;
  max-width: 380px;

  .feature {
    background: rgba(255, 255, 255, 0.1);
    backdrop-filter: blur(10px);
    border: 1px solid rgba(255, 255, 255, 0.15);
    border-radius: $radius-md;
    padding: $space-md;
    display: flex;
    align-items: center;
    gap: $space-sm;
    font-size: $font-sm;

    .feature-icon { font-size: 18px; }
  }
}

// 右侧
.login-right {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: $space-2xl;
  background: $bg-page;
}

.login-card {
  width: 100%;
  max-width: 400px;
  background: white;
  border-radius: $radius-lg;
  padding: $space-2xl;
  box-shadow: $shadow-md;

  h2 {
    font-size: $font-3xl;
    font-weight: 600;
    margin-bottom: $space-xs;
    color: $text-primary;
  }
  .subtitle {
    color: $text-secondary;
    margin-bottom: $space-xl;
    font-size: $font-base;
  }
}

.hint {
  margin-top: $space-md;
  padding: $space-md;
  background: #FEF3C7;
  border: 1px solid #FCD34D;
  border-radius: $radius-md;
  font-size: $font-xs;
  color: #92400E;
  display: flex;
  flex-direction: column;
  gap: 4px;

  .hint-warning { font-weight: 500; }
}

.footer {
  margin-top: $space-xl;
  font-size: $font-xs;
  color: $text-placeholder;
}
</style>
