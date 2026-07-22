# Aurora Admin 管理系统

Aurora Admin（亦称 EasyAdmin）是一个基于 Spring Boot 3 + Sa-Token 的企业级后台管理系统，采用前后端分离架构，提供完整的 RBAC 权限管理、定时任务、文件管理、聚合邮箱等功能模块。

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
├── aurora-app/          # 后端 Maven 多模块工程
│   ├── aurora-common/   # 公共模块（实体类、VO/DTO、工具类、配置、注解、AOP）
│   ├── aurora-admin/    # 核心业务模块（Controller、Service、Mapper）
│   └── aurora-server/   # 启动模块（入口类、配置文件、MyBatis XML）
├── aurora-web/          # 前端 Vite + Vue 3 工程
└── aurora-admin.sql     # MySQL 初始化脚本
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

## 安装部署

### 环境要求

- JDK 21+
- Node.js 18+
- MySQL 8.0+
- Redis 6.0+

### 后端部署

```bash
# 克隆项目
git clone https://gitee.com/wuhobin/aurora-admin.git
cd aurora-admin

# 导入数据库
mysql -u root -p
CREATE DATABASE easyadmin CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE easyadmin;
SOURCE aurora-admin.sql;

# 构建并启动
cd aurora-app
mvn clean install -DskipTests
cd aurora-server
mvn spring-boot:run
```

后端服务默认运行在 `http://localhost:8800`

数据库/Redis 等敏感配置通过环境变量覆盖：

- `MYSQL_HOST`、`MYSQL_USER`、`MYSQL_PASSWORD`
- `REDIS_HOST`、`REDIS_PASSWORD`
- `mail.username`、`mail.password`

### 前端部署

```bash
cd aurora-web
npm install
npm run dev        # 开发服，默认 :3000
npm run build      # 生产构建
```

前端开发时通过 Vite 代理将 `/api` 转发到后端。

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
