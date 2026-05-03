<template>
  <div class="page-container">
    <h2 class="mb-lg">个人中心</h2>

    <div class="card">
      <h3 class="mb-md">基本信息</h3>

      <div class="info-row">
        <div class="info-label">手机号</div>
        <div class="info-value">{{ userStore.userInfo?.phone }}</div>
      </div>

      <div class="info-row">
        <div class="info-label">昵称</div>
        <div class="info-value">
          <span v-if="!editing">{{ userStore.userInfo?.nickname }}</span>
          <el-input v-else v-model="newNickname" maxlength="50" style="max-width: 240px" />
        </div>
        <div class="info-actions">
          <template v-if="!editing">
            <el-button link type="primary" @click="startEdit">修改</el-button>
          </template>
          <template v-else>
            <el-button :loading="saving" type="primary" size="small" @click="handleSave">保存</el-button>
            <el-button size="small" @click="cancelEdit">取消</el-button>
          </template>
        </div>
      </div>
    </div>

    <div class="card mt-lg">
      <h3 class="mb-md">账号操作</h3>
      <el-button type="danger" plain @click="handleLogout">退出登录</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const editing = ref(false)
const saving = ref(false)
const newNickname = ref('')

function startEdit() {
  newNickname.value = userStore.userInfo?.nickname || ''
  editing.value = true
}

function cancelEdit() {
  editing.value = false
}

async function handleSave() {
  if (!newNickname.value.trim()) {
    ElMessage.warning('昵称不能为空')
    return
  }
  saving.value = true
  try {
    await userStore.updateNickname(newNickname.value.trim())
    ElMessage.success('昵称已更新')
    editing.value = false
  } finally {
    saving.value = false
  }
}

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗?', '提示', {
      confirmButtonText: '退出',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<style lang="scss" scoped>
.info-row {
  display: flex;
  align-items: center;
  padding: $space-md 0;
  border-bottom: 1px solid $border-light;

  &:last-child { border-bottom: none; }

  .info-label {
    width: 100px;
    color: $text-secondary;
    font-size: 14px;
  }

  .info-value {
    flex: 1;
    color: $text-primary;
  }

  .info-actions {
    display: flex;
    gap: $space-sm;
  }
}
</style>
