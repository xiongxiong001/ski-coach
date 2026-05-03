# ski-user-web

> Ski Coach 用户端 Web 应用,基于 Vue 3 + Vite + Element Plus。

## 当前进度

🟢 **P3.1 完成** - 项目骨架 + 核心功能
- ✅ Vite + Vue 3 (Composition API + `<script setup>`)
- ✅ Element Plus(自动按需引入)+ Pinia + Vue Router 4
- ✅ Axios 封装(token 自动注入、统一错误处理、登录态过期跳转)
- ✅ 登录/注册页(蓝色雪山主题)
- ✅ 主布局(顶栏导航 + 用户菜单)
- ✅ 首页(欢迎横幅 + 三大功能入口 + 拍摄技巧)
- ✅ 我的视频(列表 + 上传 + 删除 + 状态筛选 + 分页)
- ✅ 视频详情(分析中自动轮询 + 已完成显示统计 + 跳转报告)
- ✅ 报告详情(Markdown 渲染)
- ✅ 个人中心(改昵称 + 退出登录)

⚪ **P3.2** - 对比报告完整流程 + 报告列表 + 细节优化

## 快速启动

### 1. 准备环境

需要 **Node.js 18+**(推荐 20)。

### 2. 安装依赖

```bash
npm install
# 或用 pnpm 更快:
pnpm install
```

> 国内可用 `npm install --registry=https://registry.npmmirror.com`

### 3. 确认后端在跑

确保 `ski-api-server`(Java) 在 8080 端口运行。

如果你 Java 改了端口,修改 `vite.config.js` 里的 `proxy.target`。

### 4. 启动开发服务器

```bash
npm run dev
```

服务启动后访问: http://localhost:5173

### 5. 生产构建

```bash
npm run build
```

构建产物在 `dist/` 目录,可直接用 Nginx 托管。

## 项目结构

```
ski-user-web/
├── package.json
├── vite.config.js
├── index.html
├── public/
└── src/
    ├── main.js                # 应用入口
    ├── App.vue
    ├── router/                # Vue Router 配置 + 守卫
    ├── stores/                # Pinia 状态(user)
    ├── api/                   # axios 封装 + 各模块API
    ├── views/                 # 页面组件
    │   ├── login/             # 登录、注册
    │   ├── layout/            # 主布局
    │   ├── home/              # 首页
    │   ├── video/             # 视频列表/详情
    │   ├── report/            # 报告详情
    │   ├── comparison/        # 对比报告(P3.2)
    │   └── profile/           # 个人中心
    ├── components/            # 通用组件
    │   ├── TaskProgress.vue   # 任务进度(轮询)
    │   ├── MarkdownView.vue   # Markdown 渲染
    │   └── EmptyState.vue
    ├── styles/                # 全局样式 + SCSS 变量
    └── utils/                 # 格式化、常量
```

## 与后端的对接

后端默认在 `http://localhost:8080`,本前端通过 Vite 代理转发:

```js
// vite.config.js
proxy: {
  '/api': { target: 'http://localhost:8080', changeOrigin: true }
}
```

请求都走 `/api/...` 前缀,不需要写完整 URL。

## 开发提示

### Token 存储

登录成功后 token 存到 `localStorage`,key = `ski_coach_user_token`。

axios 拦截器自动加 `Authorization: Bearer xxx` 到每个请求。

收到 4010/4011/4012 错误码时,自动清除 token 并跳到登录页。

### 任务轮询机制

视频上传后会进入 `pending`/`analyzing` 状态。前端用 `TaskProgress` 组件每 3 秒轮询一次任务状态,直到 `success` 或 `failed`。

视频详情页也有同样的轮询逻辑——只要视频还在分析中,页面会自动刷新。

### Markdown 渲染

教练报告是 Markdown 格式。`MarkdownView` 组件用 `marked` 解析、`DOMPurify` 清洗 XSS。样式在 `styles/index.scss` 的 `.markdown-content` 类下。

## 常见问题

**Q: 启动后白屏?**
- 看浏览器控制台的报错。最常见的是 `npm install` 没装完整。

**Q: 上传视频报 4010?**
- token 失效,前端会自动跳到登录页。重新登录即可。

**Q: 上传后视频一直在"分析中"?**
- 检查后端 ski-api-server 是否在跑、Python ski-ai-server 是否在跑、两者的视频存储路径是否一致。

**Q: 报告详情页没内容?**
- 检查接口 `/api/reports/{id}` 是否能返回 `reportMarkdown`。
