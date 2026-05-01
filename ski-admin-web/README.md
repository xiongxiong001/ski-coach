# ski-admin-web

> Ski Coach 管理后台Web应用 - 基于 Vue 3 + Vite + Element Plus 实现

## 状态

🚧 **待开发(P4阶段)** - 此目录目前为空,P3完成后开始填充。

## 计划技术栈

与 `ski-user-web` 完全一致(Vue 3 + Vite + Element Plus + Pinia + Axios)。

## 核心页面(待P4完成后实现)

- 管理员登录
- 用户管理(列表/详情/封禁)
- 任务管理(列表/详情/失败重试)
- 数据统计(总览/每日数据/LLM成本/存储使用)

## 启动方式(待P4完成后填充)

```bash
npm install
npm run dev
```

开发服务器默认端口: `5174`(避免与user-web冲突)

## 与 ski-user-web 的关系

两个前端工程是**完全独立**的项目:
- 各自有独立的 `package.json` 和 `node_modules`
- 各自独立部署(Nginx不同的vhost或路径)
- 但共用同一份 axios配置 / 类型定义 / 工具函数(可考虑P5阶段抽出共享)

## IDE打开

推荐用 **VSCode** 或 **WebStorm** 打开此目录(`ski-coach/ski-admin-web`)。
