<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="brand">
        <div class="brand-icon">⛷</div>
        <h1 class="brand-title">注册新账号</h1>
        <p class="brand-subtitle">开始你的 AI 滑雪教练之旅</p>
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
            placeholder="6-32位密码"
            show-password
          />
        </el-form-item>

        <el-form-item label="昵称(可选)" prop="nickname">
          <el-input v-model="form.nickname" placeholder="留空将自动生成" maxlength="50" />
        </el-form-item>

        <el-button
          type="primary"
          :loading="loading"
          style="width: 100%"
          size="large"
          @click="handleSubmit"
        >
          注 册
        </el-button>

        <div class="auth-footer">
          已有账号?<router-link to="/login">立即登录</router-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({
  phone: '',
  password: '',
  nickname: ''
})

const rules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度6-32位', trigger: 'blur' }
  ],
  nickname: [
    { max: 50, message: '昵称不超过50个字符', trigger: 'blur' }
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
    await userStore.register({
      phone: form.phone,
      password: form.password,
      nickname: form.nickname || undefined
    })
    ElMessage.success('注册成功')
    router.push('/')
  } catch {
    // 错误已在 http 拦截器 toast
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

  .brand-icon { font-size: 56px; margin-bottom: $space-sm; }
  .brand-title { font-size: 24px; font-weight: 600; color: $color-primary; margin-bottom: $space-xs; }
  .brand-subtitle { color: $text-secondary; font-size: 14px; }
}

.auth-footer {
  margin-top: $space-lg;
  text-align: center;
  color: $text-secondary;
  font-size: 14px;
}
</style>
