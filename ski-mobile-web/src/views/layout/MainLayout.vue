<template>
  <div class="layout">
    <main class="layout-main">
      <router-view v-slot="{ Component }">
        <transition name="page" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>

    <!-- 底部 TabBar -->
    <nav class="tab-bar">
      <div
        v-for="tab in tabs"
        :key="tab.key"
        class="tab-item"
        :class="{ active: activeTab === tab.key }"
        @click="navigateTo(tab)"
      >
        <div class="tab-icon-wrap">
          <svg class="tab-icon" viewBox="0 0 24 24" fill="none">
            <path :d="tab.icon" :stroke="activeTab === tab.key ? 'url(#tabGradient)' : 'currentColor'"
                  stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </div>
        <div class="tab-label">{{ tab.label }}</div>
        <div v-if="activeTab === tab.key" class="tab-indicator"></div>
      </div>

      <!-- SVG渐变定义 -->
      <svg width="0" height="0" style="position:absolute">
        <defs>
          <linearGradient id="tabGradient" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stop-color="#2563EB" />
            <stop offset="100%" stop-color="#8B5CF6" />
          </linearGradient>
        </defs>
      </svg>
    </nav>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

// 用 SVG path 自绘图标,不依赖图标库,完全可控
const tabs = [
  {
    key: 'home',
    label: '首页',
    path: '/',
    icon: 'M3 12L12 4l9 8M5 10v10h14V10'
  },
  {
    key: 'videos',
    label: '我的视频',
    path: '/videos',
    icon: 'M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z'
  },
  {
    key: 'comparisons',
    label: '对比',
    path: '/comparisons',
    icon: 'M3 4h6v16H3V4zm12 0h6v8h-6V4zm0 12h6v4h-6v-4z'
  },
  {
    key: 'profile',
    label: '我的',
    path: '/profile',
    icon: 'M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z'
  }
]

const activeTab = computed(() => route.meta.tab || 'home')

function navigateTo(tab) {
  if (tab.path !== route.path) {
    router.push(tab.path)
  }
}
</script>

<style lang="scss" scoped>
.layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: $bg-base;
}

.layout-main {
  flex: 1;
  // 给底部TabBar让出空间
  padding-bottom: calc(64px + #{$safe-bottom});
}

.tab-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: $z-tabbar;
  display: flex;
  height: 64px;
  padding-bottom: $safe-bottom;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-top: 1px solid $border-light;
}

.tab-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
  cursor: pointer;
  color: $text-secondary;
  transition: color 0.2s;
  padding: 8px 0;

  &.active {
    color: $color-primary;

    .tab-icon-wrap {
      transform: translateY(-2px);
    }
  }
}

.tab-icon-wrap {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.tab-icon {
  width: 24px;
  height: 24px;
}

.tab-label {
  font-size: 11px;
  margin-top: 2px;
  font-weight: 500;
}

.tab-indicator {
  position: absolute;
  top: 6px;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: $gradient-primary;
  box-shadow: 0 0 8px rgba(59, 130, 246, 0.6);
  animation: indicator-in 0.3s ease;
}

@keyframes indicator-in {
  from { opacity: 0; transform: scale(0); }
  to { opacity: 1; transform: scale(1); }
}

// 路由切换动画
.page-enter-active, .page-leave-active {
  transition: opacity 0.18s, transform 0.18s;
}
.page-enter-from { opacity: 0; transform: translateY(8px); }
.page-leave-to { opacity: 0; }
</style>
