# Nexora Admin 管理系统

Nexora Admin 是一个基于 Spring Boot 3 + Sa-Token 的企业级后台管理系统，采用前后端分离架构，提供完整的 RBAC 权限管理、定时任务、文件管理、聚合邮箱等功能模块。

## 功能特性

### 系统管理

- **用户管理**：用户列表查询、新增用户、修改用户、删除用户、重置密码、个人信息管理
- **角色权限**：角色列表、新增角色、修改角色、删除角色、权限分配
- **菜单管理**：菜单树结构展示、新增菜单、修改菜单、删除菜单、按钮级权限控制
- **字典管理**：字典类型管理、字典数据维护，支持系统数据的标准化配置
- **操作日志**：记录所有用户操作日志，支持按条件查询和删除

### 定时任务

- **任务管理**：创建、编辑、删除定时任务，Cron 表达式可视化编辑
- **任务执行**：支持立即执行任务、启用/停用任务
- **调度日志**：查看定时任务执行日志，支持清空

### 文件管理

- **文件上传**：支持 OSS 文件上传
- **文件列表**：文件查询、下载、删除，图片预览

### 聚合邮箱

- **邮箱账户管理**：多邮箱账户（QQ/163/126/Yeah）的增删改查与连接测试
- **聚合收件箱**：多账户邮件统一阅读，支持附件下载、自动刷新、新邮件通知
- **安全加密**：授权码 AES-256-GCM 加密存储

## 项目架构

```
.
├── nexora-server/        # 后端 Maven 多模块工程
│   ├── nexora-common/    # 公共模块（实体类、VO/DTO、工具类、配置、注解、AOP）
│   ├── nexora-system/    # 核心业务模块（Controller、Service、Mapper）
│   └── nexora-boot/      # 启动模块（入口类、配置文件、MyBatis XML）
├── nexora-web/           # 前端 Vite + Vue 3 工程
└── nexora-admin.sql      # MySQL 初始化脚本
```

## 技术栈

### 后端

- **核心框架**：Spring Boot 3.1.x
- **ORM 框架**：MyBatis Plus
- **权限认证**：Sa-Token（RBAC 模型）
- **缓存**：Redis
- **任务调度**：Quartz
- **对象存储**：OSS（七牛 Kodo）
- **API 文档**：Knife4j（Swagger）
- **Java 版本**：JDK 21

### 前端

- **核心框架**：Vue 3（Composition API + `<script setup>`）
- **UI 组件库**：Element Plus
- **构建工具**：Vite
- **状态管理**：Pinia
- **路由管理**：Vue Router 4（动态路由 + 权限守卫）
- **HTTP 客户端**：Axios
- **编辑器**：Mavon Editor
- **图表库**：ECharts 5

### 数据库

- **数据库**：MySQL 8.0+
- **缓存**：Redis 6.0+

## Ubuntu 生产部署

本节采用以下部署结构：后端 JAR 由 Docker Compose 管理，MySQL 和 Redis 使用已有的外部服务，前端静态文件由宿主机 Nginx 托管。Docker Compose、Nginx、MySQL 和 Redis 的安装不在本文范围内。

```text
浏览器
  └─> Nginx :80
        ├─> /      -> /opt/nexora/web
        └─> /api/  -> 127.0.0.1:8800（Docker 后端）

/opt/nexora/
├── backend/
│   ├── nexora-admin.jar
│   ├── Dockerfile
│   └── .dockerignore
├── logs/
├── web/
├── .env
└── compose.yml
```

### 1. 部署前检查

- 本地构建环境：JDK 21、Maven、Node.js 18+、npm。
- Ubuntu 服务器：Docker Engine、Docker Compose 插件、Nginx。
- 外部服务：MySQL 8.0+、Redis 6.0+，且允许后端容器所在服务器访问。
- 服务器需放行 Nginx 使用的 `80` 端口。后端 `8800` 只绑定 `127.0.0.1`，不需要向公网放行。
- 已准备七牛 Kodo 的 AK、SK、Bucket 和访问域名。

部署模板位于 [`deploy`](deploy) 目录。以下命令中的 `SERVER_USER` 和 `SERVER_IP` 需替换为实际 SSH 用户与服务器 IP。

### 2. 初始化数据库

首次部署时，在能够连接 MySQL 的机器上执行：

```bash
mysql -h MYSQL_HOST -P 3306 -u root -p -e \
  'CREATE DATABASE IF NOT EXISTS `nexora-admin` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;'
mysql -h MYSQL_HOST -P 3306 -u root -p nexora-admin < nexora-admin.sql
```

生产环境建议创建只拥有 `nexora-admin` 库权限的独立账号，不要让应用使用 MySQL `root` 账号。

### 3. 本地构建后端 JAR

在项目根目录执行：

```bash
cd nexora-server
mvn clean package -DskipTests
cd ..
```

构建产物为：

```text
nexora-server/nexora-boot/target/nexora-admin.jar
```

### 4. 创建服务器目录并上传后端

需要把打包好的jar包上传到服务器


### 5. 配置生产环境变量

登录服务器，复制环境变量模板：

```bash
cd /opt/nexora
cp .env.example .env
chmod 600 .env
vi .env
```

`.env` 会同时用于 Compose 变量替换和后端容器环境变量注入。至少需要检查以下配置：

| 变量 | 说明 |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | 固定为 `prod`，否则项目默认启动 `dev` 配置 |
| `BACKEND_PORT` / `SERVER_PORT` | 宿主机回环端口和容器内服务端口，默认均为 `8800` |
| `MYSQL_HOST` / `MYSQL_USER` / `MYSQL_PASSWORD` | MySQL 地址和账号 |
| `REDIS_HOST` / `REDIS_PASSWORD` | Redis 地址和密码 |
| `MAIL_CREDENTIAL_SECRET` | 邮箱授权码加密密钥，至少 16 位；保存邮箱账号后不可随意更换 |
| `MAIL_VERIFICATION_ENABLED` / `SMTP_*` | 邮箱换绑验证码开关与 SMTP 发件配置；配置完成后再启用 |
| `OSS_QINIU_*` | 七牛 Kodo 的 AK、SK、Bucket 和访问域名 |
| `JAVA_TOOL_OPTIONS` | JVM 内存、编码和时区参数，按服务器内存调整 |
| `LOG_PATH` | 容器日志目录，保持 `/app/logs` 即可 |
| `LOG_RETENTION` | 压缩日志保留时间，默认 `7d`；过期日志在每日滚动时自动清理 |
| `KNIFE4J_*` / `SPRINGDOC_*` | 生产 API 文档开关，模板默认关闭 |

不要把真实 `.env` 上传到 Git、聊天记录或工单。模板中的所有 `CHANGE_ME` 都必须替换。

MySQL 或 Redis 位于 Docker 宿主机时，地址填写 `host.docker.internal`；`compose.yml` 已通过 `host-gateway` 提供该域名。服务还必须监听 Docker 网桥可访问的地址，不能只监听宿主机 `127.0.0.1`。如果 MySQL 或 Redis 位于其他机器，直接填写其内网 IP 或域名。

> **Redis 密码注意事项：** 当前 `application-prod.yml` 中 `spring.data.redis.password` 仍被注释。使用带密码的 Redis 前，需要先恢复 `password: ${redis.password}` 并重新构建 JAR，否则 `.env` 中的 `REDIS_PASSWORD` 不会生效。

### 6. 构建并启动后端容器

在服务器执行：

```bash
cd /opt/nexora

# 展开并校验 Compose 配置。输出包含敏感配置，不要复制到外部。
docker compose config >/dev/null

# 首次构建并启动
docker compose build --pull nexora-admin
docker compose up -d

# 查看容器状态和启动日志
docker compose ps
docker compose logs --tail=200 nexora-admin
```

确认日志中使用 `prod` Profile，且 MySQL、Redis 和七牛配置没有连接或占位符错误。后端端口仅监听本机：

```bash
curl -i http://127.0.0.1:8800/
```

根路径可能返回 `401` 或 `404`，但应能收到 Spring Boot 的 HTTP 响应；不能出现连接拒绝或超时。

### 7. 本地构建并上传前端

项目的 `nexora-web/.env.production` 已将生产 API 前缀配置为 `/api`。在本地项目根目录执行：

```bash
cd nexora-web
npm ci
npm run build
cd ..

scp -r nexora-web/dist/* SERVER_USER@SERVER_IP:/opt/nexora/web/
```

如需清理服务器上已经失效的旧静态资源，应先备份 `/opt/nexora/web`，再由管理员清理并重新上传；不要直接覆盖不同版本后留下的旧哈希文件。

### 8. 配置宿主机 Nginx

在服务器创建 `/etc/nginx/sites-available/nexora`：

```nginx
server {
    listen 80;
    server_name _;

    root /opt/nexora/web;
    index index.html;
    client_max_body_size 50m;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8800/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 10s;
        proxy_read_timeout 60s;
    }
}
```

`proxy_pass` 末尾的 `/` 不能省略：它负责将浏览器请求中的 `/api` 前缀去掉，再转发给实际不带 `/api` 前缀的后端接口。

启用并检查站点：

```bash
sudo ln -sfn /etc/nginx/sites-available/nexora /etc/nginx/sites-enabled/nexora
sudo chmod -R a+rX /opt/nexora/web
sudo nginx -t
sudo systemctl reload nginx
```

如果 Nginx 默认站点同样监听 `80` 且抢先匹配请求，请停用 `/etc/nginx/sites-enabled/default` 后再次执行 `nginx -t` 和 reload。

### 9. 验证部署

```bash
# 容器应显示为 Up
cd /opt/nexora
docker compose ps

# 检查后端容器日志
docker compose logs --tail=100 nexora-admin

# 验证 Nginx 首页与 API 代理
curl -I http://127.0.0.1/
curl -i -X POST http://127.0.0.1/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{}'
```

最后在浏览器访问 `http://SERVER_IP/`，确认页面加载、登录请求、动态路由和文件资源均正常。浏览器开发者工具中的接口地址应以 `/api/` 开头，不能再出现旧域名。

### 10. 日常运维

```bash
cd /opt/nexora

# 状态、实时日志、重启、停止和启动
docker compose ps
docker compose logs -f --tail=200 nexora-admin
docker compose restart nexora-admin
docker compose stop nexora-admin
docker compose start nexora-admin

# 查看持久化的应用日志
tail -f logs/info.log
tail -f logs/error.log
```

不要使用 `docker compose down -v`，以免误删 Compose 管理的卷。当前配置使用宿主机 `./logs` 绑定目录，执行普通 `docker compose down` 不会删除日志文件。

### 11. 发布新版本与回滚

发布前先在服务器备份当前 JAR：

```bash
cd /opt/nexora
cp backend/nexora-admin.jar \
  "backend/nexora-admin.jar.$(date +%Y%m%d-%H%M%S).bak"
```

本地重新构建并上传 JAR 后，在服务器重建容器：

```bash
cd /opt/nexora
docker compose up -d --build --force-recreate nexora-admin
docker compose logs --tail=200 nexora-admin
```

前端发布前同样备份 `web` 目录，然后上传新的 `dist` 内容并执行 `sudo nginx -t && sudo systemctl reload nginx`。

后端需要回滚时，将对应的 `.bak` 文件复制回 `backend/nexora-admin.jar`，再执行：

```bash
docker compose up -d --build --force-recreate nexora-admin
```

### 12. 常见问题

- **启动后使用了开发配置**：确认 `.env` 中 `SPRING_PROFILES_ACTIVE=prod`，再强制重建容器。
- **MySQL/Redis 连接拒绝**：容器中的 `127.0.0.1` 指向容器自身；宿主机服务应使用 `host.docker.internal`，远程服务使用内网 IP 或域名。
- **Redis 提示未认证**：确认已在 `application-prod.yml` 启用密码属性，并检查 `REDIS_PASSWORD` 后重新构建 JAR。
- **后端提示七牛属性无法解析**：检查 `OSS_QINIU_ACCESS_KEY` 和 `OSS_QINIU_SECRET_KEY`，旧项目使用的 `file.qiniu.*` 属性名不适用于当前配置。
- **Nginx API 返回 502**：先检查 `docker compose ps` 和容器日志，再从宿主机访问 `http://127.0.0.1:8800`。
- **刷新前端页面返回 404**：确认 Nginx 的 `location /` 中存在 `try_files $uri $uri/ /index.html`。
- **上传文件返回 413**：确认 Nginx 的 `client_max_body_size` 不小于后端 `50MB` 上传限制。

## API 文档

启动后端后访问 Knife4j 文档：

```
http://localhost:8800/doc.html
```

### 核心接口

**认证授权：**

```http
POST /auth/login          # 登录
POST /auth/logout         # 登出
GET  /auth/info           # 获取当前用户信息
```

**用户管理：**

```http
GET    /sys/user              # 用户列表
POST   /sys/user              # 新增用户
PUT    /sys/user              # 修改用户
DELETE /sys/user/delete/{ids} # 删除用户
PUT    /sys/user/reset        # 重置密码
POST   /sys/user/profile/email/sendCode # 发送换绑邮箱验证码
PUT    /sys/user/profile/changeEmail    # 修改当前用户邮箱
```

**角色权限：**

```http
GET    /sys/role              # 角色列表
POST   /sys/role              # 新增角色
PUT    /sys/role              # 修改角色
DELETE /sys/role/delete/{ids} # 删除角色
```

**菜单管理：**

```http
GET    /sys/menu/tree    # 菜单树
POST   /sys/menu         # 新增菜单
PUT    /sys/menu         # 修改菜单
DELETE /sys/menu/{id}    # 删除菜单
```

**定时任务：**

```http
GET    /monitor/job/list     # 任务列表
POST   /monitor/job          # 新增任务
PUT    /monitor/job          # 修改任务
DELETE /monitor/job/delete/{id} # 删除任务
```

**聚合邮箱：**

```http
GET    /mail/account/list          # 邮箱账户列表
POST   /mail/account               # 新增账户
PUT    /mail/account               # 修改账户
DELETE /mail/account/{id}          # 删除账户
POST   /mail/account/{id}/test     # 测试连接
GET    /mail/inbox/list            # 邮件列表
GET    /mail/inbox/detail          # 邮件详情
GET    /mail/inbox/attachment      # 附件下载
```

## 贡献指南

1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

## 许可证

本项目采用 GNU Affero General Public License v3.0 许可证。
