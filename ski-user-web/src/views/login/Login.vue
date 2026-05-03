<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="brand">
        <div class="brand-icon">⛷</div>
        <h1 class="brand-title">Ski Coach</h1>
        <p class="brand-subtitle">AI 滑雪教练 · 让每次滑行都有进步</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        size="large"
        @submit.prevent="handleSubmit"
      >
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="11" />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            show-password
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

        <div class="auth-footer">
          还没有账号?<router-link to="/register">立即注册</router-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({
  phone: '',
  password: ''
})

const rules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度6-32位', trigger: 'blur' }
  ]
}

async function handleSubmit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    await userStore.login(form)
    ElMessage.success('登录成功')
    const redirect = route.query.redirect || '/'
    router.push(redirect)
  } catch (e) {
    // 错误已在 http 拦截器里 toast,这里不再重复
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #2563eb 0%, #1e40af 100%);
  padding: $space-md;
}

.auth-card {
  width: 100%;
  max-width: 420px;
  background: #fff;
  border-radius: $radius-xl;
  padding: $space-2xl $space-xl;
  box-shadow: $shadow-lg;
}

.brand {
  text-align: center;
  margin-bottom: $space-xl;

  .brand-icon {
    font-size: 56px;
    margin-bottom: $space-sm;
  }
  .brand-title {
    font-size: 28px;
    font-weight: 600;
    color: $color-primary;
    margin-bottom: $space-xs;
  }
  .brand-subtitle {
    color: $text-secondary;
    font-size: 14px;
  }
}

.auth-footer {
  margin-top: $space-lg;
  text-align: center;
  color: $text-secondary;
  font-size: 14px;
}
</style>
