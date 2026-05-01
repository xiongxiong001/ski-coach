# Deploy 部署目录

此目录存放部署相关的所有配置文件和初始化脚本。

## 文件说明

| 文件 | 用途 | 何时使用 |
|------|------|---------|
| `init.sql` | 数据库初始化脚本 | 首次部署时执行 |
| `nginx.conf` | Nginx反向代理配置 | P5阶段添加 |

## 数据库初始化

首次部署时,在MySQL中执行:

```bash
mysql -u root -p < init.sql
```

或在MySQL客户端中:

```sql
SOURCE /path/to/deploy/init.sql;
```

这会创建:
- `ski_coach` 数据库
- 6张业务表(users, videos, analysis_tasks, reports, comparison_reports, admins)
- 1个默认管理员账号(用户名: `admin` / 密码: `admin123`)

> ⚠️ 生产环境部署后,请立即修改默认管理员密码!

## 默认管理员

- 用户名: `admin`
- 密码: `admin123`
- BCrypt哈希值已在 `init.sql` 中

如需重新生成BCrypt哈希,可用任何BCrypt工具,推荐:
```java
new BCryptPasswordEncoder().encode("your_password")
```
