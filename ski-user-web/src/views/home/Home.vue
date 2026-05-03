<template>
  <div class="page-container">
    <!-- 欢迎横幅 -->
    <div class="hero">
      <div class="hero-content">
        <h1>👋 你好,{{ userStore.nickname }}</h1>
        <p>上传你的滑雪视频,AI 教练为你点评每一次滑行</p>
        <el-button type="primary" size="large" @click="router.push('/videos')">
          <el-icon><Upload /></el-icon>&nbsp;开始上传视频
        </el-button>
      </div>
      <div class="hero-icon">⛷️</div>
    </div>

    <!-- 三个核心功能入口 -->
    <div class="feature-grid">
      <div class="feature-card" @click="router.push('/videos')">
        <div class="feature-icon">🎬</div>
        <h3>我的视频</h3>
        <p>上传滑雪视频,自动获得 AI 教练点评。每个视频独立分析,支持回看。</p>
      </div>

      <div class="feature-card" @click="router.push('/comparison/create')">
        <div class="feature-icon">📊</div>
        <h3>进步对比</h3>
        <p>选两个视频,看你这次比上次哪里进步、哪里需要继续努力。</p>
      </div>

      <div class="feature-card" @click="router.push('/comparisons')">
        <div class="feature-icon">📜</div>
        <h3>历史报告</h3>
        <p>所有 AI 教练的点评记录都在这里,按时间随时回顾。</p>
      </div>
    </div>

    <!-- 使用提示 -->
    <div class="tips card mt-lg">
      <h3>📌 怎么拍才能拿到好的分析报告?</h3>
      <ul>
        <li><strong>视角:</strong>第三人称(让朋友帮拍),全身入镜</li>
        <li><strong>时长:</strong>建议 20 秒到 3 分钟,不超过 100MB</li>
        <li><strong>清晰度:</strong>1080p 以上,光线充足,雪场背景对比明显</li>
        <li><strong>动作:</strong>包含完整的转弯动作(至少 2-3 个连续转弯)</li>
        <li><strong>格式:</strong>支持 mp4、mov、m4v</li>
      </ul>
    </div>
  </div>
</template>

<script setup>
import { Upload } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
</script>

<style lang="scss" scoped>
.hero {
  background: linear-gradient(135deg, $color-primary 0%, $color-primary-dark 100%);
  color: #fff;
  border-radius: $radius-xl;
  padding: $space-2xl;
  display: flex;
  align-items: center;
  margin-bottom: $space-lg;

  .hero-content {
    flex: 1;
    h1 {
      font-size: 28px;
      margin-bottom: $space-sm;
      font-weight: 600;
    }
    p {
      font-size: 16px;
      opacity: 0.9;
      margin-bottom: $space-lg;
    }
  }

  .hero-icon {
    font-size: 100px;
    opacity: 0.85;
  }
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: $space-lg;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
}

.feature-card {
  background: $bg-card;
  border-radius: $radius-lg;
  padding: $space-lg;
  border: 1px solid $border-light;
  cursor: pointer;
  transition: all .2s;

  &:hover {
    transform: translateY(-2px);
    box-shadow: $shadow-md;
    border-color: $color-primary-light;
  }

  .feature-icon {
    font-size: 40px;
    margin-bottom: $space-sm;
  }

  h3 {
    font-size: 18px;
    margin-bottom: $space-sm;
    color: $text-primary;
  }

  p {
    color: $text-secondary;
    font-size: 14px;
    line-height: 1.6;
  }
}

.tips {
  h3 {
    font-size: 16px;
    margin-bottom: $space-md;
  }

  ul {
    padding-left: $space-lg;
    line-height: 2;
    color: $text-regular;

    strong {
      color: $color-primary-dark;
      margin-right: $space-xs;
    }
  }
}
</style>
