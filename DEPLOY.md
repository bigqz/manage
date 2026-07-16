# Manage 后台管理系统 - 部署指南

本文档介绍如何将本项目部署到 **Linux 生产服务器**（推荐 CentOS / Ubuntu + Nginx）。

> 本地开发启动说明见 [README.md](./README.md)

---

## 目录

- [部署架构](#部署架构)
- [服务器要求](#服务器要求)
- [部署前准备](#部署前准备)
- [第一步：部署 MySQL](#第一步部署-mysql)
- [第二步：打包后端](#第二步打包后端)
- [第三步：部署后端](#第三步部署后端)
- [第四步：打包前端](#第四步打包前端)
- [第五步：部署前端（Nginx）](#第五步部署前端nginx)
- [第六步：配置 HTTPS（推荐）](#第六步配置-https推荐)
- [生产环境配置建议](#生产环境配置建议)
- [验证部署](#验证部署)
- [常见问题](#常见问题)
- [Windows 服务器部署（简要）](#windows-服务器部署简要)

---

## 部署架构

```
用户浏览器
    │
    ▼
┌─────────────────────────────────────┐
│  Nginx (80/443)                     │
│  ├── /          → 前端静态文件 dist │
│  └── /api/*     → 反向代理到后端     │
└─────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────┐
│  Spring Boot JAR (8080)             │
│  ManageApplication                  │
└─────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────┐
│  MySQL (3306)                       │
│  数据库: manage                      │
└─────────────────────────────────────┘
```

**请求流向：**

1. 用户访问 `https://your-domain.com` → Nginx 返回前端页面
2. 前端请求 `/api/auth/login` → Nginx 转发到 `http://127.0.0.1:8080/api/auth/login`
3. 后端连接 MySQL 读写数据

---

## 服务器要求

| 项目 | 最低配置 | 推荐配置 |
|------|----------|----------|
| CPU | 1 核 | 2 核+ |
| 内存 | 2 GB | 4 GB+ |
| 磁盘 | 20 GB | 40 GB+ |
| 系统 | CentOS 7+ / Ubuntu 20.04+ | Ubuntu 22.04 LTS |

**需要安装的软件：**

| 软件 | 用途 | 版本 |
|------|------|------|
| JDK | 运行后端 | 17+ |
| MySQL | 数据库 | 8.0+ |
| Nginx | 静态资源 + 反向代理 | 1.20+ |
| Git | 拉取代码（可选） | 最新 |

---

## 部署前准备

### 1. 拉取代码

```bash
cd /opt
git clone https://codeup.aliyun.com/69bba359706afd34aa5fa377/manage.git
cd manage
```

也可以在本机打包好后，用 `scp` / FTP 上传到服务器。

### 2. 开放端口

| 端口 | 用途 | 是否对外开放 |
|------|------|--------------|
| 80 | HTTP | 是 |
| 443 | HTTPS | 是 |
| 8080 | 后端（内部） | **否**，仅本机访问 |
| 3306 | MySQL（内部） | **否**，仅本机访问 |

```bash
# CentOS (firewalld)
firewall-cmd --permanent --add-service=http
firewall-cmd --permanent --add-service=https
firewall-cmd --reload

# Ubuntu (ufw)
ufw allow 80
ufw allow 443
ufw enable
```

### 3. 生产环境必改项

部署前务必修改以下内容：

| 配置项 | 文件 | 说明 |
|--------|------|------|
| 数据库密码 | `application-prod.yml` 或启动参数 | 不要用 `123456` |
| JWT 密钥 | `application-prod.yml` | 换成随机长字符串 |
| MySQL 地址 | `application-prod.yml` | 生产库地址 |
| 关闭 SQL 日志 | `application-prod.yml` | 去掉 `StdOutImpl` |

---

## 第一步：部署 MySQL

### 1. 安装 MySQL 8

**Ubuntu：**

```bash
sudo apt update
sudo apt install -y mysql-server
sudo systemctl start mysql
sudo systemctl enable mysql
```

**CentOS：**

```bash
sudo yum install -y mysql-server
sudo systemctl start mysqld
sudo systemctl enable mysqld
```

### 2. 创建数据库和用户

```bash
mysql -u root -p
```

```sql
-- 创建数据库
CREATE DATABASE IF NOT EXISTS `manage` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建专用账号（不要用 root）
CREATE USER 'manage_user'@'localhost' IDENTIFIED BY '你的强密码';
GRANT ALL PRIVILEGES ON manage.* TO 'manage_user'@'localhost';
FLUSH PRIVILEGES;
```

### 3. 导入初始化脚本

```bash
mysql -u manage_user -p manage < /opt/manage/sql/init.sql
```

### 4. 验证

```sql
USE manage;
SHOW TABLES;
SELECT * FROM sys_user;
```

应看到 `sys_user`、`sys_role`、`sys_user_role` 三张表，以及 `admin` 用户。

---

## 第二步：打包后端

### 在开发机或服务器上执行

```bash
cd /opt/manage/backend
mvn clean package -DskipTests
```

打包成功后生成：

```
backend/target/manage-backend-1.0.0.jar
```

### 创建生产配置文件

在服务器创建配置目录：

```bash
mkdir -p /opt/manage/config
```

创建 `/opt/manage/config/application-prod.yml`：

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/manage?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: manage_user
    password: 你的强密码

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    # 生产环境不要打印 SQL
  global-config:
    db-config:
      id-type: auto

jwt:
  secret: 请替换为至少32位随机字符串
  expiration: 86400000
```

> 不要把生产密码提交到 Git，配置文件放在服务器本地。

---

## 第三步：部署后端

### 1. 创建运行目录

```bash
mkdir -p /opt/manage/app
mkdir -p /opt/manage/logs

cp backend/target/manage-backend-1.0.0.jar /opt/manage/app/
```

### 2. 手动启动测试

```bash
cd /opt/manage/app

java -jar manage-backend-1.0.0.jar \
  --spring.profiles.active=prod \
  --spring.config.additional-location=/opt/manage/config/application-prod.yml
```

看到 `Started ManageApplication` 表示启动成功。`Ctrl+C` 停止。

### 3. 配置 systemd 开机自启

创建服务文件 `/etc/systemd/system/manage-backend.service`：

```ini
[Unit]
Description=Manage Backend Service
After=network.target mysql.service

[Service]
Type=simple
User=root
WorkingDirectory=/opt/manage/app
ExecStart=/usr/bin/java -jar /opt/manage/app/manage-backend-1.0.0.jar --spring.profiles.active=prod --spring.config.additional-location=/opt/manage/config/application-prod.yml
Restart=on-failure
RestartSec=10
StandardOutput=append:/opt/manage/logs/backend.log
StandardError=append:/opt/manage/logs/backend-error.log

[Install]
WantedBy=multi-user.target
```

> `ExecStart` 中的 `java` 路径可用 `which java` 查看，JDK 17 建议用绝对路径。

启用服务：

```bash
sudo systemctl daemon-reload
sudo systemctl start manage-backend
sudo systemctl enable manage-backend
sudo systemctl status manage-backend
```

### 4. 查看日志

```bash
# 实时日志
tail -f /opt/manage/logs/backend.log

# 服务状态
systemctl status manage-backend
```

### 5. 验证后端

```bash
curl -X POST http://127.0.0.1:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

返回 `code: 200` 和 `token` 即正常。

---

## 第四步：打包前端

### 1. 安装依赖并构建

```bash
cd /opt/manage/frontend
pnpm install
pnpm build
```

构建产物在：

```
frontend/dist/
├── index.html
├── assets/
│   ├── index-xxx.js
│   └── index-xxx.css
└── ...
```

### 2. 部署静态文件

```bash
mkdir -p /var/www/manage
cp -r dist/* /var/www/manage/
```

---

## 第五步：部署前端（Nginx）

### 1. 安装 Nginx

**Ubuntu：**

```bash
sudo apt install -y nginx
sudo systemctl start nginx
sudo systemctl enable nginx
```

**CentOS：**

```bash
sudo yum install -y nginx
sudo systemctl start nginx
sudo systemctl enable nginx
```

### 2. 创建站点配置

创建 `/etc/nginx/conf.d/manage.conf`：

```nginx
server {
    listen 80;
    server_name your-domain.com;   # 改成你的域名或服务器 IP

    # 前端静态资源
    root /var/www/manage;
    index index.html;

    # Vue Router history 模式
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 后端 API 反向代理
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # 超时设置
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2)$ {
        expires 7d;
        add_header Cache-Control "public, immutable";
    }

    # 禁止访问隐藏文件
    location ~ /\. {
        deny all;
    }
}
```

### 3. 检查并重载 Nginx

```bash
sudo nginx -t
sudo systemctl reload nginx
```

### 4. 没有域名时

`server_name` 可以直接写服务器 IP，例如：

```nginx
server_name 192.168.1.100;
```

浏览器访问 `http://192.168.1.100` 即可。

---

## 第六步：配置 HTTPS（推荐）

使用 Let's Encrypt 免费证书（需要域名已解析到服务器）：

```bash
# Ubuntu
sudo apt install -y certbot python3-certbot-nginx

# 自动配置 HTTPS
sudo certbot --nginx -d your-domain.com
```

按提示操作后，Nginx 会自动加上 443 端口和证书续期任务。

---

## 生产环境配置建议

### 1. 使用 Spring Profile 隔离环境

```
application.yml          # 公共配置
application-prod.yml     # 生产配置（放服务器，不进 Git）
```

启动时指定：

```bash
java -jar manage-backend-1.0.0.jar --spring.profiles.active=prod
```

### 2. 安全清单

| 项目 | 建议 |
|------|------|
| MySQL | 使用专用账号，禁止 root 远程登录 |
| JWT secret | 至少 32 位随机字符串 |
| 后台默认密码 | 首次登录后修改 `admin` 密码 |
| 8080 端口 | 不对外开放，仅 Nginx 内网转发 |
| HTTPS | 生产环境务必启用 |
| 数据库备份 | 配置每日自动备份 |

### 3. MySQL 自动备份（示例）

```bash
# 创建备份脚本 /opt/manage/scripts/backup-db.sh
#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)
mysqldump -u manage_user -p'你的密码' manage > /opt/manage/backup/manage_$DATE.sql
# 保留最近 7 天
find /opt/manage/backup -name "*.sql" -mtime +7 -delete
```

```bash
chmod +x /opt/manage/scripts/backup-db.sh

# 每天凌晨 2 点备份
crontab -e
# 添加：
0 2 * * * /opt/manage/scripts/backup-db.sh
```

---

## 验证部署

按顺序检查：

```bash
# 1. MySQL 是否正常
mysql -u manage_user -p -e "USE manage; SHOW TABLES;"

# 2. 后端是否运行
systemctl status manage-backend
curl http://127.0.0.1:8080/api/auth/login -X POST -H "Content-Type: application/json" -d '{"username":"admin","password":"admin123"}'

# 3. Nginx 是否正常
systemctl status nginx
curl -I http://your-domain.com

# 4. 前端页面
# 浏览器打开 https://your-domain.com
# 用 admin / admin123 登录
```

---

## 更新部署（发新版）

### 更新后端

```bash
cd /opt/manage
git pull

cd backend
mvn clean package -DskipTests

cp target/manage-backend-1.0.0.jar /opt/manage/app/
systemctl restart manage-backend
```

### 更新前端

```bash
cd /opt/manage/frontend
pnpm install
pnpm build
cp -r dist/* /var/www/manage/
```

> 前端更新后建议强制刷新浏览器缓存（Ctrl+Shift+R）。

---

## 常见问题

### 1. 页面能开，登录报网络错误

**原因：** 后端没启动，或 Nginx 代理配置错误。

```bash
systemctl status manage-backend
curl http://127.0.0.1:8080/api/auth/login -X POST -H "Content-Type: application/json" -d '{"username":"admin","password":"admin123"}'
```

### 2. 刷新页面 404

**原因：** Nginx 没配 `try_files` 回退到 `index.html`。

确认配置中有：

```nginx
location / {
    try_files $uri $uri/ /index.html;
}
```

### 3. 后端启动报数据库连接失败

- 检查 MySQL 是否运行：`systemctl status mysql`
- 检查 `application-prod.yml` 中的用户名密码
- 检查数据库是否已创建并导入 `init.sql`

### 4. 接口 502 Bad Gateway

- 后端进程挂了：`systemctl restart manage-backend`
- 查看日志：`tail -100 /opt/manage/logs/backend-error.log`

### 5. 跨域问题

生产环境前后端同域（都走 Nginx），**不需要额外配 CORS**。  
如果前后端分域名部署，需在后端 `CorsConfig.java` 中指定允许的前端域名。

---

## Windows 服务器部署（简要）

如果在 Windows Server 上部署：

### 后端

1. 安装 JDK 17
2. 打包：`mvn clean package -DskipTests`
3. 运行：

```powershell
java -jar manage-backend-1.0.0.jar --spring.profiles.active=prod --spring.config.additional-location=D:\manage\config\application-prod.yml
```

4. 可用 **NSSM** 注册为 Windows 服务实现开机自启

### 前端

1. `pnpm build` 生成 `dist`
2. 将 `dist` 放到 IIS 或 Nginx for Windows 的网站目录
3. 配置反向代理 `/api` → `http://127.0.0.1:8080`

### MySQL

与 Linux 步骤相同，用 Navicat 执行 `sql/init.sql` 即可。

---

## 部署检查清单

上线前逐项确认：

- [ ] MySQL 已安装，数据库 `manage` 已创建
- [ ] `init.sql` 已执行
- [ ] 生产数据库密码已修改（非 `123456`）
- [ ] JWT secret 已修改
- [ ] 后端 JAR 打包成功
- [ ] `application-prod.yml` 配置正确
- [ ] systemd 服务已配置并启动
- [ ] 前端 `pnpm build` 成功
- [ ] Nginx 配置正确，`nginx -t` 通过
- [ ] 80/443 端口已开放
- [ ] HTTPS 已配置（推荐）
- [ ] 浏览器可正常登录 `admin / admin123`
- [ ] 用户管理、角色管理功能正常

---

## 相关文档

- [README.md](./README.md) - 本地开发配置与启动
- [sql/init.sql](./sql/init.sql) - 数据库初始化脚本
