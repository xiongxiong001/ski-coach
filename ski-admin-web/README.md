# ski-admin-web

> Ski Coach 管理后台,基于 Vue 3 + Vite + Element Plus + ECharts。

## 风格

**专业冷静** · 信息密度优先

- 浅色背景 + 深色侧边菜单(类 Notion / Linear)
- 蓝色主色,沿用 C 端的 `#3B82F6`
- 大字 KPI + ECharts 趋势图 + 表格优先
- 默认账号:`admin / admin123`(P2.5 后端预置)

## 当前进度

🟢 **P4 完成** - 管理后台全部功能
- ✅ 登录(左侧产品介绍 + 右侧表单)
- ✅ 主布局(深色侧边菜单 + 顶栏 + 用户菜单 + 折叠)
- ✅ 数据总览(4 KPI 卡片 + 实时状态 + ECharts 每日趋势)
- ✅ 用户管理(列表 + 搜索筛选 + 详情抽屉 + 封禁/解封)
- ✅ 任务管理(列表 + 多维度筛选 + 详情抽屉 + 失败重试)
- ✅ 数据分析(LLM 调用次数饼图 + 花费分布饼图 + 详细成本表 + 存储使用环图)

## 快速启动

### 1. 准备环境

需要 **Node.js 18+**。

### 2. 安装依赖

```bash
cd D:\works\ski-coach\ski-admin-web
npm config set registry https://registry.npmmirror.com
npm install
```

### 3. 启动

确保后端 ski-api-server 在 8080 端口运行。

```bash
npm run dev
```

访问 http://localhost:5175(注意是 5175,区别于:
- 用户 PC 版 5173
- 用户移动版 5174)

### 4. 登录

默认账号:
- 用户名:`admin`
- 密码:`admin123`

(P2.5 阶段已在 init.sql 预置,数据库里直接可登录)

## 项目结构

```
ski-admin-web/
├── package.json                 # Vue3 + ElementPlus + ECharts
├── vite.config.js               # 5175 端口,代理 /admin/* 到 8080
└── src/
    ├── main.js
    ├── App.vue
    ├── router/                  # 路由 + 鉴权守卫
    ├── stores/admin.js          # Pinia 管理员态
    ├── api/                     # 4 个模块
    │   ├── http.js              # axios 封装(token、错误处理)
    │   ├── auth.js
    │   ├── users.js
    │   ├── tasks.js
    │   └── stats.js
    ├── views/
    │   ├── login/               # 登录(含产品介绍左栏)
    │   ├── layout/              # MainLayout(侧栏+顶栏)
    │   ├── dashboard/           # 数据总览
    │   ├── users/               # 用户管理
    │   ├── tasks/               # 任务管理
    │   └── analytics/           # 数据分析
    ├── styles/
    │   ├── variables.scss       # 设计 token
    │   └── index.scss
    └── utils/
        ├── format.js
        └── constants.js
```

## 关键设计

### 1. 鉴权(独立 token)

管理员有独立的 token key:`ski_coach_admin_token`,与 C 端用户的 token 完全隔离。这样:
- 用户 token 失效不影响管理员
- 即使一台浏览器同时登了管理员和用户,互不冲突
- 管理员退出后不影响用户登录

### 2. 路由结构

- `/login` 登录页(独立)
- `/` 主布局,带侧边菜单
  - `/dashboard` 数据总览(默认首屏)
  - `/users` 用户管理
  - `/tasks` 任务管理
  - `/analytics` 数据分析

侧边菜单可折叠(顶栏左侧的折叠按钮),折叠后只显示图标。

### 3. 列表页通用模式

每个列表页(用户、任务)都遵循同样模式:
- **筛选区**(card,inline form)
- **列表区**(table + pagination)
- **详情抽屉**(右侧滑出,500-560 宽)

### 4. 任务管理:支持从总览跳转

总览页"进行中任务 / 失败任务"卡片点击可直接跳转到任务列表并自动应用 status 筛选。

### 5. ECharts 集成

3 个图表:
- Dashboard:每日趋势(line + bar 双 y 轴)
- Analytics:调用次数饼图 + 花费饼图 + 存储环形图

通过 `nextTick + ref` 拿到 DOM 后初始化,`resize` 自动响应窗口缩放。

## 与后端的对接

后端管理接口都在 `/admin/*` 路径下(P2.5 已实现):

```
POST /admin/auth/login          → 管理员登录
POST /admin/auth/logout
GET  /admin/users               → 用户列表
GET  /admin/users/{id}
PUT  /admin/users/{id}/status
GET  /admin/tasks               → 任务列表
GET  /admin/tasks/{id}
POST /admin/tasks/{id}/retry    → 失败任务重试
GET  /admin/stats/overview      → 总览统计
GET  /admin/stats/daily         → 每日趋势
GET  /admin/stats/llm-cost      → LLM 成本
GET  /admin/stats/storage       → 存储使用
```

通过 Vite 代理,前端发 `/admin/xxx` 自动转到 `http://localhost:8080/admin/xxx`。

## 常见问题

**Q: 登录提示"密码错误"?**
- 默认密码是 `admin123`,如果之前 init.sql 用过老的哈希值,执行修复 SQL:
  ```sql
  UPDATE ski_coach.admins
  SET password_hash = '$2b$10$RfvwJJ5TP2yZOQ3Zdy6AXOCqUeCLvEcMAkENYj44.TQj/terhckQm'
  WHERE username = 'admin';
  ```

**Q: 数据为空?**
- 总览/分析页需要后端有数据。先在 C 端跑一些视频和报告。

**Q: 图表不显示?**
- 检查 `npm install` 是否完整(echarts 依赖较大)
- 浏览器控制台看是否报错

**Q: 总用户/视频数为 0,但实际有数据?**
- 后端 P2.5 的 AdminStatsMapper.xml 用了 UNION ALL 查询,如果数据库存储路径或表名不一致会有问题
- 测试 `GET /admin/stats/overview` 接口确认后端返回正常
