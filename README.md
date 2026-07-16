# Manage 后台管理系统

Vue3 + Element Plus + Spring Boot 3 + MyBatis-Plus + MySQL 的简单后台管理系统。

## 项目结构

```
manage/
├── frontend/          # Vue3 前端
├── backend/           # Spring Boot 后端
├── sql/init.sql       # 数据库初始化脚本
└── README.md
```

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3、Vite、Element Plus、Vue Router、Pinia、Axios |
| 后端 | Spring Boot 3、MyBatis-Plus、JWT |
| 数据库 | MySQL 8 |

## 功能模块

- 登录 / 退出（JWT 鉴权）
- 用户管理（增删改查、分配角色）
- 角色管理（增删改查）

默认后台账号：`admin / admin123`

---

## 环境要求

| 工具 | 版本要求 |
|------|----------|
| JDK | 17+（推荐 17，避免 JDK 24 编译兼容问题） |
| Maven | 3.8+ |
| MySQL | 8.0+ |
| Node.js | 18+ |
| pnpm | 最新版 |

---

## 一、数据库配置

### 1. 启动 MySQL

确保本机 MySQL 服务已启动。

### 2. 执行初始化脚本

在 Navicat / DBeaver / MySQL Workbench 中执行：

```
sql/init.sql
```

或在命令行执行：

```bash
mysql -u root -p < sql/init.sql
```

执行成功后会创建：

- 数据库：`manage`
- 表：`sys_user`、`sys_role`、`sys_user_role`
- 默认管理员：`admin / admin123`

### 3. 确认数据库账号

本项目默认 MySQL 配置：

| 项 | 值 |
|---|---|
| 地址 | `localhost:3306` |
| 数据库 | `manage` |
| 用户名 | `root` |
| 密码 | `123456` |

> 若你本机 MySQL 密码不同，请同步修改后端配置文件。

---

## 二、后端配置与启动

### 1. 修改数据库连接

编辑 `backend/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/manage?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: 123456   # 改成你的 MySQL 密码
```

### 2. IDEA 启动（推荐）

1. 用 IDEA 打开 `backend` 目录
2. 配置 Project SDK 为 **JDK 17+**
3. 安装并启用 **Lombok 插件**，开启 **注解处理（Annotation Processing）**
4. 右侧 Maven 面板点击 **Reload All Maven Projects**
5. 运行 `com.manage.ManageApplication`

启动成功后，后端地址：`http://localhost:8080`

### 3. 命令行启动

```bash
cd backend
mvn spring-boot:run
```

### 4. 后端常见问题

| 问题 | 解决方案 |
|------|----------|
| `未定义模块 SDK` | File → Project Structure → 配置 JDK 17 |
| `TypeTag :: UNKNOWN` | 升级 Lombok 或换 JDK 17；勾选 Delegate IDE build to Maven |
| `Lombok 要求启用注解处理` | Settings → Annotation Processors → 勾选启用 |
| 数据库连接失败 | 检查 MySQL 是否启动、密码是否正确、是否已执行 init.sql |

---

## 三、前端配置与启动

### 1. 安装依赖

```bash
cd frontend
pnpm install
```

### 2. 启动开发服务

```bash
pnpm dev
```

启动成功后访问：`http://localhost:5173`

### 3. 接口代理

前端通过 Vite 代理转发 API 请求到后端，配置见 `frontend/vite.config.js`：

```js
proxy: {
  '/api': {
    target: 'http://localhost:8080',
    changeOrigin: true
  }
}
```

### 4. 生产构建

```bash
pnpm build
```

构建产物在 `frontend/dist/` 目录。

---

## 四、启动顺序

```
1. 启动 MySQL
2. 执行 sql/init.sql（首次）
3. 启动后端（IDEA 运行 ManageApplication 或 mvn spring-boot:run）
4. 启动前端（pnpm dev）
5. 浏览器访问 http://localhost:5173
6. 使用 admin / admin123 登录
```

---

## 五、接口说明

### 认证

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 登录 |
| GET | `/api/auth/info` | 获取当前用户信息 |
| POST | `/api/auth/logout` | 退出 |

### 用户

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/user/page` | 分页查询 |
| POST | `/api/user` | 新增 |
| PUT | `/api/user` | 修改 |
| DELETE | `/api/user/{id}` | 删除 |
| PUT | `/api/user/{id}/roles` | 分配角色 |

### 角色

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/role/list` | 全部角色 |
| GET | `/api/role/page` | 分页查询 |
| POST | `/api/role` | 新增 |
| PUT | `/api/role` | 修改 |
| DELETE | `/api/role/{id}` | 删除 |

统一响应格式：

```json
{
  "code": 200,
  "msg": "ok",
  "data": {}
}
```

---

## 六、端口一览

| 服务 | 端口 |
|------|------|
| 前端 | 5173 |
| 后端 | 8080 |
| MySQL | 3306 |

---

## 七、Git 仓库

```bash
git clone https://codeup.aliyun.com/69bba359706afd34aa5fa377/manage.git
```

---

## 八、注意事项

1. **MySQL 密码** 和 **后台登录密码** 不是同一个：
   - MySQL：`root / 123456`（按你本机实际配置）
   - 后台系统：`admin / admin123`
2. 后端必须在 JDK 17+ 环境运行，JDK 24 可能出现 Lombok 编译问题
3. `node_modules`、`target`、`.idea` 等目录已在 `.gitignore` 中忽略，克隆后需重新安装依赖
