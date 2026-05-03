<template>
  <div class="layout">
    <!-- 侧边菜单 -->
    <aside class="sider" :class="{ collapsed: isCollapsed }">
      <div class="sider-logo">
        <span class="logo-icon">⛷</span>
        <span v-if="!isCollapsed" class="logo-text">Ski Coach</span>
      </div>

      <el-menu
        :default-active="route.path"
        class="sider-menu"
        :collapse="isCollapsed"
        :collapse-transition="false"
        background-color="#1F2937"
        text-color="#D1D5DB"
        active-text-color="#FFFFFF"
        router
      >
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>
      </el-menu>

      <div class="sider-footer">
        <span v-if="!isCollapsed" class="version">v1.0.0</span>
      </div>
    </aside>

    <!-- 主区域 -->
    <div class="main">
      <!-- 顶栏 -->
      <header class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="isCollapsed = !isCollapsed">
            <Fold v-if="!isCollapsed" />
            <Expand v-else />
          </el-icon>
          <span class="page-title">{{ route.meta?.title || '' }}</span>
        </div>

        <div class="header-right">
          <el-dropdown trigger="click" @command="handleCommand">
            <span class="user-trigger">
              <el-avatar :size="28" class="avatar">{{ avatarText }}</el-avatar>
              <span class="username">{{ adminStore.realName }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  <el-icon><User /></el-icon>
                  {{ adminStore.username }}
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- 内容区 -->
      <main class="content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  DataAnalysis, User, Cpu, TrendCharts, Fold, Expand,
  ArrowDown, SwitchButton
} from '@element-plus/icons-vue'
import { useAdminStore } from '@/stores/admin'

const route = useRoute()
const router = useRouter()
const adminStore = useAdminStore()

const isCollapsed = ref(false)

const menuItems = [
  { path: '/dashboard', title: '数据总览', icon: DataAnalysis },
  { path: '/users',     title: '用户管理', icon: User },
  { path: '/tasks',     title: '任务管理', icon: Cpu },
  { path: '/analytics', title: '数据分析', icon: TrendCharts }
]

const avatarText = computed(() => {
  const name = adminStore.realName || adminStore.username || 'A'
  return name.charAt(0).toUpperCase()
})

async function handleCommand(cmd) {
  if (cmd === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗?', '提示', {
        confirmButtonText: '退出',
        cancelButtonText: '取消',
        type: 'warning'
      })
    } catch { return }
    await adminStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}
</script>

<style lang="scss" scoped>
.layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

// ====== 侧边菜单 ======
.sider {
  width: 220px;
  background: $bg-sider-dark;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  transition: width 0.2s;

  &.collapsed {
    width: 64px;
  }
}

.sider-logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: $space-sm;
  border-bottom: 1px solid rgba(255,255,255,0.06);
  color: white;

  .logo-icon { font-size: 28px; }
  .logo-text {
    font-size: $font-xl;
    font-weight: 600;
    letter-spacing: -0.5px;
  }
}

.sider-menu {
  flex: 1;
  border-right: none !important;

  :deep(.el-menu-item) {
    height: 48px;
    line-height: 48px;
    margin: 4px 8px;
    border-radius: $radius-md;

    &:hover {
      background: rgba(255,255,255,0.06) !important;
    }
    &.is-active {
      background: $color-primary !important;
      color: white !important;

      &::before {
        display: none;  // 去掉默认的左侧高亮线
      }
    }
  }
}

.sider-footer {
  padding: $space-md;
  border-top: 1px solid rgba(255,255,255,0.06);
  text-align: center;

  .version {
    font-size: $font-xs;
    color: rgba(255,255,255,0.4);
  }
}

// ====== 主区域 ======
.main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

// ====== 顶栏 ======
.header {
  height: 60px;
  background: $bg-header;
  border-bottom: 1px solid $border-light;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 $space-lg;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: $space-md;
}

.collapse-btn {
  font-size: 20px;
  color: $text-regular;
  cursor: pointer;
  padding: 6px;
  border-radius: $radius-sm;

  &:hover {
    background: $bg-hover;
    color: $color-primary;
  }
}

.page-title {
  font-size: $font-lg;
  font-weight: 500;
  color: $text-primary;
}

.header-right {
  display: flex;
  align-items: center;
  gap: $space-md;
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: $space-sm;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: $radius-md;
  outline: none;

  &:hover { background: $bg-hover; }

  .avatar {
    background: $color-primary;
    color: white;
    font-size: $font-sm;
    font-weight: 600;
  }
  .username {
    font-size: $font-base;
    color: $text-primary;
  }
}

// ====== 内容区 ======
.content {
  flex: 1;
  overflow: auto;
  background: $bg-page;
}

// 路由切换动画
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.15s;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
</style>
