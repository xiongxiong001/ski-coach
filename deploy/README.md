# Ski Coach 部署指南

> 从零部署到上线的完整步骤。第一次部署预计 30-60 分钟。

## 0. 前置准备

### 服务器要求

**最低配置**(支持 100 个用户):
- 4 核 CPU,8G 内存,100G SSD
- Ubuntu 22.04 / CentOS 7+ / Debian 11+
- 公网 IP

**推荐配置**(1 万用户):
- 8 核 CPU,16G 内存,500G SSD
- 上行带宽 10Mbps+(视频上传)

### 你需要准备

| 项 | 说明 |
|---|---|
| 服务器 | 阿里云 / 腾讯云 ECS,Ubuntu 22.04 |
| 域名 | 例如 `skicoach.com`,2 个子域名:`m.skicoach.com`、`admin.skicoach.com` |
| DeepSeek API Key | https://platform.deepseek.com 注册 + 充值 ¥10 起 |
| SSL 证书 | 用 Let's Encrypt 免费(本指南末尾教 Certbot 自动申请) |

---

## 1. 服务器初始化

### 1.1 SSH 登录服务器

```bash
ssh root@your-server-ip
```

### 1.2 安装 Docker

```bash
# Ubuntu/Debian
curl -fsSL https://get.docker.com | sh

# 启动并设置开机自启
systemctl enable docker
systemctl start docker

# 验证
docker --version
docker compose version
```

如果 `docker compose` 报错(老版本 Docker),装独立版:

```bash
apt install -y docker-compose-plugin
```

### 1.3 (可选)添加普通用户运行 Docker

```bash
useradd -m -s /bin/bash ski
usermod -aG docker ski
su - ski
```

---

## 2. 准备代码与配置

### 2.1 拉取代码

```bash
cd /home/ski   # 或者 /opt
git clone <your-repo-url> ski-coach
cd ski-coach
```

### 2.2 把 application-prod.yml 放到 Java 工程

```bash
cp deploy/application-prod.yml.example ski-api-server/src/main/resources/application-prod.yml
```

### 2.3 配置环境变量(关键)

```bash
cd deploy
cp .env.example .env
```

**用 vim 编辑 `.env`**,把所有 `⚠️ MUST` 标记的值都改成真实值:

```bash
vim .env
```

**重点修改**:

```bash
# 数据库密码:用强密码
MYSQL_ROOT_PASSWORD=YourStrongPassword2026!@#

# JWT 密钥:32位以上随机串,可以用以下命令生成
JWT_USER_SECRET=$(openssl rand -hex 32)
JWT_ADMIN_SECRET=$(openssl rand -hex 32)

# DeepSeek API Key
DEEPSEEK_API_KEY=sk-xxxxxxxxxxxxxxxxxxxxxx

# 视频存储路径:推荐 /data/ski-data
VIDEO_DATA_PATH=/data/ski-data
```

**生成随机 JWT 密钥的快捷方式**:

```bash
echo "JWT_USER_SECRET=$(openssl rand -hex 32)"
echo "JWT_ADMIN_SECRET=$(openssl rand -hex 32)"
```

把输出的两行粘到 `.env` 替换原值。

### 2.4 创建视频存储目录

```bash
sudo mkdir -p /data/ski-data
sudo chown -R 1000:1000 /data/ski-data   # Docker 内部 UID 是 1000
sudo chmod 755 /data/ski-data
```

---

## 3. 构建镜像

### 3.1 全量构建

```bash
cd /home/ski/ski-coach/deploy
chmod +x build.sh
./build.sh all
```

这会:
1. 构建 `ski-api-server:latest`(Java 镜像)
2. 构建 `ski-ai-server:latest`(Python 镜像)
3. 编译两个前端项目并放到 `deploy/web/{mobile,admin}/`

**首次构建慢(15-30 分钟)**:Maven 下依赖、Python 装 MediaPipe(几百 MB)、npm install。

后续只改了某一部分:`./build.sh api` / `./build.sh ai` / `./build.sh web` 单独构建。

### 3.2 验证镜像

```bash
docker images | grep ski
```

应该看到:
```
ski-api-server   latest    xxx   1GB
ski-ai-server    latest    xxx   2GB
```

---

## 4. 启动服务

### 4.1 一键启动

```bash
cd /home/ski/ski-coach/deploy
docker compose up -d
```

第一次启动会拉 MySQL/Redis/Nginx 镜像(~5 分钟)。

### 4.2 查看启动状态

```bash
docker compose ps
```

应该看到 5 个服务都是 `Up (healthy)`:

```
NAME              STATUS
ski-mysql         Up (healthy)
ski-redis         Up (healthy)
ski-api-server    Up
ski-ai-server     Up
ski-nginx         Up
```

### 4.3 看日志(关键!)

```bash
# 看 Java 后端日志(等 30 秒,看到 "Started SkiApiApplication" 才算启动完成)
docker compose logs -f ski-api-server

# 看 Python AI 日志
docker compose logs -f ski-ai-server

# 看 Nginx 日志
docker compose logs -f nginx
```

**Ctrl+C 退出 logs**,但服务还在跑。

---

## 5. 验证部署

### 5.1 后端 API 验证

```bash
# 健康检查
curl http://localhost:8080/actuator/health
# 期望:{"status":"UP"}

# 用户注册
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"phone":"13800138000","password":"test1234"}'
# 期望:{"code":0,"data":{"token":"...","userId":1,...}}
```

### 5.2 通过 Nginx 访问前端

```bash
# 访问用户端
curl -I http://localhost
# 期望:HTTP/1.1 200 OK

# 访问管理后台(本机用 admin.localhost,服务器上用域名)
curl -I http://admin.localhost
```

### 5.3 浏览器访问

如果服务器有公网 IP `1.2.3.4`:

- 用户端 H5:http://1.2.3.4
- 管理后台:http://1.2.3.4 (但 Nginx 配的是按域名分流,需要先配域名)

**先配 hosts 临时测试**(本地电脑):

```
1.2.3.4 m.skicoach.com
1.2.3.4 admin.skicoach.com
```

然后浏览器访问 `http://m.skicoach.com` 和 `http://admin.skicoach.com`。

---

## 6. 配置域名 + HTTPS

### 6.1 解析 DNS

去你的域名服务商(阿里云/腾讯云/Cloudflare 后台):

```
A 记录  m       ->  1.2.3.4
A 记录  admin   ->  1.2.3.4
```

等 5-10 分钟生效。

### 6.2 申请 HTTPS 证书(Let's Encrypt)

```bash
# 装 certbot
apt install -y certbot

# 先停 nginx 释放 80 端口
docker compose stop nginx

# 申请证书
certbot certonly --standalone \
  -d m.skicoach.com \
  -d admin.skicoach.com \
  -m your-email@example.com \
  --agree-tos --no-eff-email

# 证书生成在:
# /etc/letsencrypt/live/m.skicoach.com/fullchain.pem
# /etc/letsencrypt/live/m.skicoach.com/privkey.pem
```

### 6.3 启用 HTTPS

```bash
mkdir -p /home/ski/ski-coach/deploy/certs

# 复制证书到 deploy/certs(权限给 nginx 容器读)
cp /etc/letsencrypt/live/m.skicoach.com/fullchain.pem deploy/certs/
cp /etc/letsencrypt/live/m.skicoach.com/privkey.pem deploy/certs/
chmod 644 deploy/certs/*
```

编辑 `deploy/nginx/nginx.conf`,**取消注释 HTTPS 部分**,把域名改成你自己的。

修改 `deploy/docker-compose.yml`,取消注释:

```yaml
volumes:
  - ./certs:/etc/nginx/certs:ro
ports:
  - "443:443"
```

重启:

```bash
docker compose up -d nginx
```

访问 https://m.skicoach.com 应该看到🔒安全标志。

### 6.4 自动续证书(Let's Encrypt 90 天到期)

```bash
crontab -e
```

添加:

```
0 3 1 * * certbot renew --quiet --deploy-hook 'docker compose -f /home/ski/ski-coach/deploy/docker-compose.yml restart nginx'
```

每月 1 号凌晨 3 点检查,到期前自动续。

---

## 7. 日常运维

### 启动 / 停止 / 重启

```bash
cd /home/ski/ski-coach/deploy

docker compose up -d            # 启动(后台)
docker compose stop             # 停止
docker compose restart          # 重启所有
docker compose restart ski-api-server  # 只重启 Java
```

### 看日志

```bash
docker compose logs -f --tail=200 ski-api-server
docker compose logs -f --tail=200 ski-ai-server
docker compose logs -f nginx
```

### 更新代码

```bash
cd /home/ski/ski-coach
git pull

cd deploy
./build.sh all          # 重新构建
docker compose up -d    # 滚动更新
```

### 备份数据库

```bash
docker exec ski-mysql mysqldump -uroot -p${MYSQL_ROOT_PASSWORD} ski_coach > backup-$(date +%Y%m%d).sql
```

建议加 cron:

```
0 2 * * * /home/ski/scripts/backup-db.sh
```

### 备份视频文件

```bash
rsync -av /data/ski-data/ /backup/ski-data/
```

也建议加 cron 每周一次。

### 修改默认管理员密码

```bash
# 进 MySQL 容器
docker exec -it ski-mysql mysql -uroot -p${MYSQL_ROOT_PASSWORD} ski_coach

# 在 MySQL 里执行
mysql> UPDATE admins SET password_hash = '<新密码的BCrypt值>' WHERE username = 'admin';
```

生成 BCrypt 密码:

```bash
docker run --rm python:3.11-slim sh -c "pip install bcrypt -q && python -c \"import bcrypt; print(bcrypt.hashpw(b'你的新密码', bcrypt.gensalt(rounds=10)).decode())\""
```

---

## 8. 监控

### 看资源占用

```bash
docker stats
```

### 关键指标

| 指标 | 怎么看 | 异常阈值 |
|---|---|---|
| Redis 队列长度 | `docker exec ski-redis redis-cli LLEN ski_coach:task:queue` | 积压 > 50 需扩容 worker |
| 失败任务 | 管理后台 → 任务管理 → 状态 = failed | 持续增加要查原因 |
| 磁盘 | `df -h /data/ski-data` | 用了 80%+ 该清理 |
| MySQL 连接 | `docker exec ski-mysql mysqladmin -uroot -p... status` | 连接数接近 max_connections |

---

## 9. 故障排查

### Q1: 服务起不来

```bash
docker compose ps          # 看是哪个状态异常
docker compose logs <name> # 看具体错误
```

最常见的几种:
- MySQL 启动失败:数据卷权限问题,`docker compose down -v` 然后重启(注意:会删数据!)
- Java 启动失败:大概率是数据库连不上,看 Java 日志找 "Communications link failure"
- Python OOM:MediaPipe 吃内存,容器内存限制太低

### Q2: 上传视频后一直 "分析中"

```bash
# 看 Java 日志,任务有没有入队
docker compose logs ski-api-server | grep -i "任务入队\|Worker"

# 看 Python 日志,有没有收到调用
docker compose logs ski-ai-server

# 看 Redis 队列
docker exec ski-redis redis-cli LLEN ski_coach:task:queue
```

### Q3: 前端访问 502

Nginx 转发到 ski-api-server 失败。检查:

```bash
docker compose ps             # 确认 ski-api-server 是 Up
docker exec ski-nginx nginx -t   # 配置语法检查
```

### Q4: 视频上传 413

`client_max_body_size` 太小。在 `nginx.conf` 调大,默认我配了 100M。

---

## 10. 安全清单

部署前**务必**确认:

- [ ] `.env` 里所有密码都改成强密码
- [ ] JWT 密钥用 `openssl rand -hex 32` 生成的随机串
- [ ] 默认管理员密码 `admin123` **已修改**
- [ ] 数据库 / Redis 端口**没有暴露到公网**(只在容器内网通信)
  - 检查 `docker-compose.yml` 里的 `ports: - "3306:3306"` 改成 `- "127.0.0.1:3306:3306"` 只本地可访问
- [ ] 防火墙只开 80 / 443 / 22(SSH)
- [ ] HTTPS 已启用(否则 iOS 上一些功能失效)
- [ ] 管理后台子域名**有 IP 白名单**(可选,推荐)
- [ ] DeepSeek 余额充足,且**已设置每日消费上限**

---

## 11. 成本预估(参考)

按 100 用户、每人每月 5 个视频:
- 服务器:¥80-120/月(阿里云 2 核 4G,够 100 人)
- DeepSeek:0.005¥/次 × 500 次/月 = ¥2.5/月(忽略不计)
- 域名:¥60/年
- 总计:**约 ¥100/月,扛得住前 100 个种子用户**

到 1000 用户:服务器升 4 核 8G(约 ¥300/月)。
到 1 万用户:加 CDN + 视频对象存储,约 ¥1500-3000/月。

---

如果这份指南没解决你的问题,把日志(`docker compose logs`)发给我,我帮你看。
