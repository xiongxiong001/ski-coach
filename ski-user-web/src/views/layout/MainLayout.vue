<template>
  <div class="layout">
    <!-- 顶部导航栏 -->
    <header class="header">
      <div class="header-inner">
        <div class="logo" @click="router.push('/')">
          <span class="logo-icon">⛷</span>
          <span class="logo-text">Ski Coach</span>
        </div>

        <nav class="nav">
          <router-link to="/" exact-active-class="active">首页</router-link>
          <router-link to="/videos" active-class="active">我的视频</router-link>
          <router-link to="/comparisons" active-class="active">对比报告</router-link>
        </nav>

        <el-dropdown trigger="click" @command="handleCommand">
          <span class="user-trigger">
            {{ userStore.nickname }}
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <!-- 内容区 -->
    <main class="main">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
  </div>
</template>

<script setup>
import { ArrowDown } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

async function handleCommand(cmd) {
  if (cmd === 'profile') {
    router.push('/profile')
  } else if (cmd === 'logout') {
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
}
</script>

<style lang="scss" scoped>
.layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.header {
  background: #fff;
  border-bottom: 1px solid $border-light;
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: $shadow-sm;
}

.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 $space-lg;
  height: 60px;
  display: flex;
  align-items: center;
  gap: $space-xl;
}

.logo {
  display: flex;
  align-items: center;
  gap: $space-sm;
  cursor: pointer;
  font-size: 18px;
  font-weight: 600;
  color: $color-primary;

  .logo-icon { font-size: 24px; }
}

.nav {
  flex: 1;
  display: flex;
  gap: $space-lg;

  a {
    color: $text-regular;
    font-size: 15px;
    padding: $space-sm 0;
    text-decoration: none;
    transition: color .15s;
    border-bottom: 2px solid transparent;

    &:hover { color: $color-primary; }
    &.active {
      color: $color-primary;
      border-bottom-color: $color-primary;
    }
  }
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  color: $text-regular;
  outline: none;

  &:hover { color: $color-primary; }
}

.main {
  flex: 1;
  background: $bg-page;
}

// 路由切换动画
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.15s ease;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
</style>
