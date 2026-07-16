# Manage 后台管理系统 - 阿里云部署指南

本文档介绍如何将本项目部署到 **阿里云**，采用最常见的生产方案：

```
阿里云 ECS（应用服务器） + RDS MySQL（云数据库） + Nginx + 域名 + SSL
```

> 通用 Linux 部署步骤见 [DEPLOY.md](./DEPLOY.md)  
> 本地开发启动见 [README.md](./README.md)

---

## 目录

- [部署架构](#部署架构)
- [需要购买的阿里云服务](#需要购买的阿里云服务)
- [整体流程](#整体流程)
- [第一步：购买并配置 ECS](#第一步购买并配置-ecs)
- [第二步：购买并配置 RDS MySQL](#第二步购买并配置-rds-mysql)
- [第三步：配置安全组](#第三步配置安全组)
- [第四步：连接服务器并安装环境](#第四步连接服务器并安装环境)
- [第五步：初始化数据库](#第五步初始化数据库)
- [第六步：部署后端](#第六步部署后端)
- [第七步：部署前端](#第七步部署前端)
- [第八步：配置 Nginx](#第八步配置-nginx)
- [第九步：域名解析](#第九步域名解析)
- [第十步：配置 HTTPS 证书](#第十步配置-https-证书)
- [从 Codeup 拉代码部署](#从-codeup-拉代码部署)
- [更新发版](#更新发版)
- [费用参考](#费用参考)
- [常见问题](#常见问题)
- [方案二：单机部署（ECS 自带 MySQL）](#方案二单机部署ecs-自带-mysql)

---

## 部署架构

```
                    用户浏览器
                        │
                        ▼
              ┌─────────────────┐
              │  阿里云 DNS      │
              │  admin.xxx.com  │
              └────────┬────────┘
                       │
                       ▼
              ┌─────────────────┐
              │  阿里云 ECS      │
              │  Nginx (80/443) │
              │  ├── 前端 dist   │
              │  └── /api 代理    │
              │  Spring Boot     │
              │  (127.0.0.1:8080)│
              └────────┬────────┘
                       │ 内网连接
                       ▼
              ┌─────────────────┐
              │  阿里云 RDS      │
              │  MySQL 8.0      │
              │  数据库: manage  │
              └─────────────────┘
```

**为什么推荐 ECS + RDS：**

| 对比 | ECS 自建 MySQL | 阿里云 RDS |
|------|----------------|------------|
| 运维 | 自己备份、自己维护 | 自动备份、监控告警 |
| 稳定性 | 一般 | 高可用可选 |
| 安全性 | 自己管 | 白名单、审计 |
| 适合 | 测试/学习 | **生产推荐** |

---

## 需要购买的阿里云服务

| 服务 | 用途 | 是否必须 |
|------|------|----------|
| ECS 云服务器 | 跑 Nginx + 后端 JAR | 必须 |
| RDS MySQL | 云数据库 | 强烈推荐 |
| 域名 | 绑定访问地址 | 推荐 |
| SSL 证书 | HTTPS | 推荐 |
| Codeup | 代码仓库（你已有） | 已有 |

### 推荐配置（小型项目）

| 服务 | 规格 | 参考价 |
|------|------|--------|
| ECS | 2核4G，40G 系统盘，CentOS 7.9 / Ubuntu 22.04 | ~60-100 元/月 |
| RDS | MySQL 8.0，1核2G，20G 存储 | ~50-80 元/月 |
| 域名 | `.com` / `.cn` | ~30-60 元/年 |
| SSL | 免费 DV 证书 | 0 元 |

---

## 整体流程

```
1. 买 ECS + RDS
2. 配安全组（放行 22/80/443）
3. RDS 建库 + 导入 init.sql
4. ECS 装 JDK + Nginx + Git
5. 拉代码 → 打包后端 → 启动 JAR
6. 打包前端 → 放到 Nginx
7. 配 Nginx 反向代理
8. 域名解析到 ECS 公网 IP
9. 申请 SSL → 开启 HTTPS
10. 浏览器访问测试
```

---

## 第一步：购买并配置 ECS

### 1. 购买 ECS

登录 [阿里云控制台](https://ecs.console.aliyun.com/) → **创建实例**

| 配置项 | 推荐选择 |
|--------|----------|
| 地域 | 选离你用户近的（如华东1-杭州） |
| 镜像 | **Ubuntu 22.04** 或 CentOS 7.9 |
| 规格 | 2核4G（ecs.c6.large 或同类） |
| 系统盘 | 40GB ESSD |
| 网络 | 默认 VPC |
| 公网 IP | **分配公网 IPv4 地址** |
| 带宽 | 按固定带宽 3-5 Mbps |
| 安全组 | 新建（下一步配置） |
| 登录凭证 | 密钥对 或 自定义密码 |

### 2. 记录关键信息

购买完成后记下：

| 信息 | 示例 |
|------|------|
| 公网 IP | `47.96.xxx.xxx` |
| 内网 IP | `172.16.x.x` |
| 登录用户名 | `root`（Linux） |
| 登录密码/密钥 | 你设置的 |

### 3. 远程连接 ECS

**方式一：阿里云控制台**

ECS 控制台 → 实例 → **远程连接** → Workbench

**方式二：本地 SSH**

```bash
ssh root@47.96.xxx.xxx
```

---

## 第二步：购买并配置 RDS MySQL

### 1. 购买 RDS

登录 [RDS 控制台](https://rdsnext.console.aliyun.com/) → **创建实例**

| 配置项 | 推荐选择 |
|--------|----------|
| 数据库类型 | MySQL |
| 版本 | **8.0** |
| 系列 | 基础版（够用）/ 高可用版（生产） |
| 规格 | 1核2G 或 2核4G |
| 存储 | 20GB 起 |
| 地域 | **与 ECS 相同地域**（重要！） |
| 网络类型 | 专有网络 VPC（与 ECS 同一 VPC） |
| 白名单 | 先设为 `0.0.0.0/0`（后面收紧） |

### 2. 创建数据库账号

RDS 控制台 → 你的实例 → **账号管理** → **创建账号**

| 项 | 值 |
|---|---|
| 账号 | `manage_user` |
| 密码 | 强密码（自己记好） |
| 授权数据库 | `manage`（先创建数据库） |

### 3. 创建数据库

RDS 控制台 → **数据库管理** → **创建数据库**

| 项 | 值 |
|---|---|
| 数据库名 | `manage` |
| 字符集 | `utf8mb4` |

### 4. 获取连接地址

RDS 控制台 → 实例详情 → **数据库连接**

记下：

| 信息 | 示例 |
|------|------|
| 内网地址 | `rm-xxx.mysql.rds.aliyuncs.com` |
| 端口 | `3306` |

> **ECS 连接 RDS 必须用内网地址**，速度快且免流量费。

### 5. 配置白名单

RDS 控制台 → **白名单设置**

添加 ECS 的内网 IP 段，例如：

```
172.16.0.0/16
```

或直接加 ECS 的内网 IP：`172.16.x.x`

---

## 第三步：配置安全组

### ECS 安全组规则

ECS 控制台 → 实例 → **安全组** → **配置规则** → **入方向**

| 协议 | 端口 | 授权对象 | 说明 |
|------|------|----------|------|
| TCP | 22 | 你的办公 IP/32 | SSH（不要对 0.0.0.0/0 开放） |
| TCP | 80 | 0.0.0.0/0 | HTTP |
| TCP | 443 | 0.0.0.0/0 | HTTPS |

> **不要**对外开放 8080 和 3306，后端和数据库只走内网。

### RDS 安全组

RDS 通过白名单控制，不需要在 ECS 安全组开放 3306 到公网。

---

## 第四步：连接服务器并安装环境

SSH 登录 ECS 后执行：

### Ubuntu 22.04

```bash
# 更新系统
apt update && apt upgrade -y

# 安装 JDK 17
apt install -y openjdk-17-jdk

# 安装 Nginx
apt install -y nginx

# 安装 Git
apt install -y git

# 安装 Maven（用于服务器打包）
apt install -y maven

# 验证
java -version
nginx -v
git --version
mvn -version
```

### CentOS 7.9

```bash
yum update -y
yum install -y java-17-openjdk java-17-openjdk-devel
yum install -y nginx git maven

java -version
nginx -v
```

---

## 第五步：初始化数据库

### 1. 上传 SQL 文件到 ECS

**方式一：Git 拉取（推荐，后面统一拉代码）**

**方式二：本地上传**

```bash
# 在你本地电脑执行
scp D:\work\manage\sql\init.sql root@47.96.xxx.xxx:/tmp/
```

### 2. 安装 MySQL 客户端（用于连接 RDS）

```bash
# Ubuntu
apt install -y mysql-client

# CentOS
yum install -y mysql
```

### 3. 导入数据到 RDS

```bash
mysql -h rm-xxx.mysql.rds.aliyuncs.com -u manage_user -p manage < /tmp/init.sql
```

输入 RDS 账号密码。

### 4. 验证

```bash
mysql -h rm-xxx.mysql.rds.aliyuncs.com -u manage_user -p -e "USE manage; SHOW TABLES; SELECT username FROM sys_user;"
```

应看到 3 张表和 `admin` 用户。

---

## 第六步：部署后端

### 1. 拉取代码

```bash
cd /opt
git clone https://codeup.aliyun.com/69bba359706afd34aa5fa377/manage.git
cd manage
```

> 如果是私有仓库，需配置 Codeup 的 SSH 密钥或 HTTPS 账号。

### 2. 创建生产配置

```bash
mkdir -p /opt/manage/config
```

创建 `/opt/manage/config/application-prod.yml`：

```yaml
server:
  port: 8080

spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://rm-xxx.mysql.rds.aliyuncs.com:3306/manage?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: manage_user
    password: 你的RDS密码

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      id-type: auto

jwt:
  secret: 替换为至少32位随机字符串_阿里云生产环境
  expiration: 86400000
```

> `rm-xxx.mysql.rds.aliyuncs.com` 换成你的 RDS 内网地址。

### 3. 打包

```bash
cd /opt/manage/backend
mvn clean package -DskipTests
```

### 4. 部署 JAR

```bash
mkdir -p /opt/manage/app
mkdir -p /opt/manage/logs
cp target/manage-backend-1.0.0.jar /opt/manage/app/
```

### 5. 测试启动

```bash
cd /opt/manage/app
java -jar manage-backend-1.0.0.jar \
  --spring.profiles.active=prod \
  --spring.config.additional-location=/opt/manage/config/application-prod.yml
```

看到 `Started ManageApplication` 后 `Ctrl+C` 停止。

### 6. 配置 systemd 开机自启

```bash
cat > /etc/systemd/system/manage-backend.service << 'EOF'
[Unit]
Description=Manage Backend Service
After=network.target

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
EOF

systemctl daemon-reload
systemctl start manage-backend
systemctl enable manage-backend
systemctl status manage-backend
```

### 7. 验证后端

```bash
curl -X POST http://127.0.0.1:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

---

## 第七步：部署前端

### 1. 安装 Node.js 和 pnpm

```bash
# 安装 Node.js 18
curl -fsSL https://deb.nodesource.com/setup_18.x | bash -
apt install -y nodejs

# 安装 pnpm
npm install -g pnpm

node -v
pnpm -v
```

### 2. 构建前端

```bash
cd /opt/manage/frontend
pnpm install
pnpm build
```

### 3. 部署静态文件

```bash
mkdir -p /var/www/manage
cp -r dist/* /var/www/manage/
```

---

## 第八步：配置 Nginx

### 1. 创建站点配置

```bash
cat > /etc/nginx/conf.d/manage.conf << 'EOF'
server {
    listen 80;
    server_name admin.yourdomain.com;   # 改成你的域名，或先用公网 IP

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
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2)$ {
        expires 7d;
        add_header Cache-Control "public, immutable";
    }

    location ~ /\. {
        deny all;
    }
}
EOF
```

### 2. 检查并重载

```bash
nginx -t
systemctl reload nginx
```

### 3. 暂时用 IP 访问

如果还没配域名，`server_name` 改成 `_` 或你的公网 IP：

```nginx
server_name _;
```

浏览器访问 `http://47.96.xxx.xxx` 测试。

---

## 第九步：域名解析

### 1. 购买域名（如果还没有）

[阿里云域名控制台](https://dc.console.aliyun.com/) → 注册域名

### 2. 添加解析记录

域名控制台 → 你的域名 → **解析设置** → **添加记录**

| 记录类型 | 主机记录 | 记录值 | TTL |
|----------|----------|--------|-----|
| A | `admin` | ECS 公网 IP | 10 分钟 |

效果：`admin.yourdomain.com` → `47.96.xxx.xxx`

### 3. 等待生效

通常 10 分钟内生效，可用以下命令检查：

```bash
ping admin.yourdomain.com
```

### 4. 更新 Nginx 配置

把 `server_name` 改为你的域名：

```nginx
server_name admin.yourdomain.com;
```

```bash
nginx -t && systemctl reload nginx
```

---

## 第十步：配置 HTTPS 证书

### 方式一：阿里云免费 SSL 证书（推荐）

1. 打开 [SSL 证书控制台](https://yundun.console.aliyun.com/?p=cas)
2. **购买证书** → 选择 **免费证书（DV）** → 0 元购买
3. **证书申请** → 填写域名 `admin.yourdomain.com`
4. 按提示完成 DNS 验证（自动或手动添加 TXT 记录）
5. 证书签发后 → **下载** → 选择 **Nginx** 格式
6. 上传到服务器：

```bash
mkdir -p /etc/nginx/ssl
# 上传 cert.pem 和 key.pem 到 /etc/nginx/ssl/
```

7. 修改 Nginx 配置：

```nginx
server {
    listen 80;
    server_name admin.yourdomain.com;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl;
    server_name admin.yourdomain.com;

    ssl_certificate     /etc/nginx/ssl/cert.pem;
    ssl_certificate_key /etc/nginx/ssl/key.pem;
    ssl_protocols       TLSv1.2 TLSv1.3;

    root /var/www/manage;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2)$ {
        expires 7d;
        add_header Cache-Control "public, immutable";
    }
}
```

```bash
nginx -t && systemctl reload nginx
```

### 方式二：Let's Encrypt（免费自动续期）

```bash
apt install -y certbot python3-certbot-nginx
certbot --nginx -d admin.yourdomain.com
```

---

## 从 Codeup 拉代码部署

你的代码仓库：

```
https://codeup.aliyun.com/69bba359706afd34aa5fa377/manage.git
```

### 配置 SSH 密钥（推荐）

**在 ECS 上：**

```bash
ssh-keygen -t ed25519 -C "ecs-deploy"
cat ~/.ssh/id_ed25519.pub
```

复制公钥 → Codeup 仓库 → **设置** → **部署密钥** → 添加

**然后：**

```bash
cd /opt
git clone git@codeup.aliyun.com:69bba359706afd34aa5fa377/manage.git
```

### HTTPS 方式

```bash
git clone https://codeup.aliyun.com/69bba359706afd34aa5fa377/manage.git
# 输入 Codeup 用户名和密码/Token
```

---

## 更新发版

```bash
# 1. 拉最新代码
cd /opt/manage
git pull

# 2. 更新后端
cd backend
mvn clean package -DskipTests
cp target/manage-backend-1.0.0.jar /opt/manage/app/
systemctl restart manage-backend

# 3. 更新前端
cd /opt/manage/frontend
pnpm install
pnpm build
cp -r dist/* /var/www/manage/

# 4. 验证
systemctl status manage-backend
curl -I https://admin.yourdomain.com
```

---

## 费用参考

按最小配置估算（华东1地域）：

| 服务 | 规格 | 月费（约） |
|------|------|-----------|
| ECS | 2核4G，3M 带宽 | 80-120 元 |
| RDS | MySQL 1核2G，20G | 50-80 元 |
| 域名 | .com | 分摊约 5 元/月 |
| SSL | 免费 DV | 0 元 |
| **合计** | | **约 135-205 元/月** |

> 新用户通常有免费试用/优惠券，实际可能更低。

### 省钱方案

- 测试环境：只用一台 ECS，MySQL 装在 ECS 上（见下方方案二），约 60-80 元/月
- 包年包月比按量付费便宜
- 学生/新用户优惠

---

## 常见问题

### 1. ECS 连不上 RDS

| 检查项 | 操作 |
|--------|------|
| 同一地域 | ECS 和 RDS 必须在同一地域 |
| 同一 VPC | 网络类型必须都是 VPC 且同一 VPC |
| 白名单 | RDS 白名单加入 ECS 内网 IP |
| 地址 | 用 RDS **内网地址**，不是外网地址 |
| 账号权限 | `manage_user` 已授权 `manage` 库 |

测试连接：

```bash
mysql -h rm-xxx.mysql.rds.aliyuncs.com -u manage_user -p -e "SELECT 1"
```

### 2. 外网访问不了

| 检查项 | 操作 |
|--------|------|
| 安全组 | ECS 入方向放行 80/443 |
| Nginx | `systemctl status nginx` |
| 后端 | `systemctl status manage-backend` |
| 域名解析 | `ping admin.yourdomain.com` 是否指向 ECS IP |

### 3. 登录报网络错误

```bash
# 在 ECS 上测试
curl http://127.0.0.1:8080/api/auth/login -X POST \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

- 后端无响应 → 查 `tail -f /opt/manage/logs/backend.log`
- 后端正常但浏览器失败 → 查 Nginx 配置中 `/api/` 代理

### 4. HTTPS 证书申请失败

- 域名解析必须已生效
- 安全组必须放行 80 端口（验证用）
- 免费证书每域名每年 20 个名额

### 5. Codeup 拉代码失败

- 私有仓库需配置 SSH 密钥或 HTTPS Token
- 检查 ECS 是否能访问外网：`ping codeup.aliyun.com`

---

## 方案二：单机部署（ECS 自带 MySQL）

预算有限或测试环境，可以只用一台 ECS，不购买 RDS：

```
ECS（Nginx + 后端 + MySQL 全在一台）
```

### 与 RDS 方案的区别

| 步骤 | RDS 方案 | 单机方案 |
|------|----------|----------|
| 数据库 | 阿里云 RDS | ECS 上 `apt install mysql-server` |
| 连接地址 | `rm-xxx.mysql.rds.aliyuncs.com` | `127.0.0.1` |
| 初始化 | 远程导入 init.sql | 本地导入 init.sql |
| application-prod.yml | RDS 内网地址 | `jdbc:mysql://127.0.0.1:3306/manage` |

### 单机 MySQL 安装

```bash
apt install -y mysql-server
systemctl start mysql
systemctl enable mysql

mysql -u root -e "CREATE DATABASE manage DEFAULT CHARACTER SET utf8mb4;"
mysql -u root manage < /opt/manage/sql/init.sql
```

其余 Nginx、后端、前端部署步骤相同。

---

## 上线检查清单

- [ ] ECS 已购买，公网 IP 可 SSH 登录
- [ ] RDS 已购买（或 ECS 自建 MySQL）
- [ ] ECS 与 RDS 同地域、同 VPC
- [ ] RDS 白名单已加 ECS 内网 IP
- [ ] 安全组放行 80/443，22 仅办公 IP
- [ ] `init.sql` 已导入
- [ ] `application-prod.yml` 使用 RDS 内网地址
- [ ] JWT secret 和数据库密码已修改
- [ ] 后端 systemd 服务运行中
- [ ] 前端 dist 已部署到 Nginx
- [ ] 域名已解析到 ECS 公网 IP
- [ ] HTTPS 证书已配置
- [ ] 浏览器可访问并登录 `admin / admin123`

---

## 相关文档

- [README.md](./README.md) - 本地开发配置与启动
- [DEPLOY.md](./DEPLOY.md) - 通用 Linux 部署指南
- [sql/init.sql](./sql/init.sql) - 数据库初始化脚本
