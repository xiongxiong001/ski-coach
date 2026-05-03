# ski-mobile-web

> Ski Coach 移动端 H5,基于 Vue 3 + Vite + Vant。

## 设计理念

**冰雪冷调 · 深色高级感**

- 深空背景(`#0A0E27`)+ 玻璃态卡片
- 蓝紫渐变(`#3B82F6 → #8B5CF6`)主色
- 4 个底部 Tab(首页 / 我的视频 / 对比 / 我的)
- 数据可视化:进度环、统计大数字、动态背景
- 适配 iOS 安全区域(顶部刘海、底部小白条)

## 当前进度

🟢 **P3-Mobile.1 完成** - 项目骨架 + 核心页面
🟢 **P3-Mobile.2 完成** - 对比报告完整流程
- ✅ 对比创建页(VS 双栏选择 + 步骤引导 + 粘性底部按钮)
- ✅ 对比等待页(三层 ring 脉冲动画 + 实时计时 + 自动跳转)
- ✅ 对比详情(渐变 hero + 进步统计 + VS 双视频卡 + 完整 Markdown)
- ✅ 对比列表(渐变缩略卡片 + 三色统计指标 + 浮动创建按钮)
- ✅ 视频详情→对比的快捷路径(带 currVideo 上下文)
- ✅ 分享(复制链接,可粘贴到微信)

⚪ **P4** - 管理后台 PC 版(Vue + Element Plus)

## 快速启动

### 1. 准备环境

需要 **Node.js 18+**(推荐 20)。

### 2. 安装依赖

```bash
cd D:\works\ski-coach\ski-mobile-web
npm config set registry https://registry.npmmirror.com
npm install
```

### 3. 启动开发服务器

确保后端 ski-api-server 在 8080 端口跑着。

```bash
npm run dev
```

启动后访问 http://localhost:5174 (注意是 5174,区别于 PC 版的 5173)

### 4. 在手机上预览(强烈推荐)

启动后看终端输出:

```
➜  Local:   http://localhost:5174/
➜  Network: http://192.168.x.x:5174/   ← 用这个
```

**让你的手机和电脑连同一个 WiFi**,然后:
- 在手机浏览器打开那个 Network 地址,**这才是真实体验**
- 或者把这个地址生成二维码,扫码打开更快

### 5. 用 Chrome 模拟手机

如果手机不方便:Chrome F12 → 切换到设备模拟模式 → 选 iPhone 14 Pro 或 Pixel 7

## 项目结构

```
ski-mobile-web/
├── package.json
├── vite.config.js          # Vant 自动按需引入
├── index.html              # 移动端 viewport + 主题色
└── src/
    ├── main.js
    ├── App.vue
    ├── router/             # 含 noTab 元属性区分布局
    ├── stores/user.js
    ├── api/                # 与 PC 版完全相同(直接复用)
    ├── views/
    │   ├── login/          # Login + Register(沉浸式)
    │   ├── layout/         # MainLayout(底部TabBar)
    │   ├── home/           # 首页
    │   ├── video/          # 列表 + 详情
    │   ├── report/         # AI 教练报告
    │   ├── comparison/     # 对比(占位,P3-Mobile.2实现)
    │   └── profile/        # 个人中心
    ├── components/
    │   └── MarkdownView.vue
    ├── styles/
    │   ├── variables.scss  # ★ 设计系统(颜色、字号、阴影、渐变)
    │   └── index.scss      # ★ 全局样式 + Vant 深色主题覆盖
    └── utils/
```

## 关键技术点

### 1. Vant 深色主题

通过 CSS 变量定制 Vant 组件颜色:

```scss
:root {
  --van-background: #0A0E27;
  --van-text-color: rgba(255,255,255,0.92);
  --van-cell-background: #141937;
  // ...
}
```

### 2. iOS 安全区域适配

```scss
$safe-top:    env(safe-area-inset-top, 0px);
$safe-bottom: env(safe-area-inset-bottom, 0px);

.tab-bar { padding-bottom: $safe-bottom; }
.nav-bar { padding-top: calc(12px + #{$safe-top}); }
```

### 3. 玻璃态(Glass Morphism)

```scss
$glass-bg:     rgba(255, 255, 255, 0.04);
$glass-border: 1px solid rgba(255, 255, 255, 0.08);
$glass-blur:   blur(20px);

.card {
  background: $glass-bg;
  backdrop-filter: $glass-blur;
  -webkit-backdrop-filter: $glass-blur;
  border: $glass-border;
}
```

### 4. 渐变文字 / 渐变背景

```scss
.text-gradient {
  background: linear-gradient(135deg, #3B82F6, #8B5CF6);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}
```

### 5. 上传进度环

SVG 圆形进度条,通过 stroke-dashoffset 实现:

```html
<svg viewBox="0 0 100 100">
  <circle cx="50" cy="50" r="42"
          :stroke-dasharray="263.89"
          :stroke-dashoffset="263.89 - (263.89 * progress / 100)" />
</svg>
```

## 常见问题

**Q: 启动后白屏?**
- 检查 Node.js 版本(需要 18+)
- `npm install` 是否完整
- 看浏览器控制台报错

**Q: 手机访问不到?**
- 电脑和手机必须在同一 WiFi
- Windows 防火墙可能阻止,允许 Vite 进程通过
- 看终端输出的 Network 地址(用 IP)

**Q: Vant 组件没样式?**
- 检查 `main.js` 是否有 `import 'vant/lib/index.css'`(已配)

**Q: 上传 100MB 视频很慢?**
- 移动端 4G 网络确实慢,这是正常的
- 雪友实际场景:连酒店 WiFi 上传

**Q: iOS Safari 上 backdrop-filter 不生效?**
- Safari 需要 `-webkit-backdrop-filter`(已配)
- iOS 9+ 都支持
