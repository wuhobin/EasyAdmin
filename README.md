# Aurora Admin 管理系统

Aurora Admin 是一个基于 Spring Boot 和 Sa-Token 的企业级后台管理系统，采用前后端分离架构，提供完整的权限管理、系统监控等功能模块，支持流程引擎扩展，适用于各类中后台管理场景。

## 项目简介

Aurora Admin 是一个现代化的后台管理解决方案，拥有灵活的权限控制体系和丰富的业务功能模块。项目采用模块化设计，易于集成和扩展，适合快速搭建企业级应用。

## 功能特性

### 系统管理

- **用户管理**：用户列表查询、新增用户、修改用户、删除用户、重置密码、个人信息管理
- **角色权限**：角色列表、新增角色、修改角色、删除角色、权限分配、角色用户关联
- **菜单管理**：菜单树结构展示、新增菜单、修改菜单、删除菜单、菜单排序
- **字典管理**：字典类型管理、字典数据维护，支持系统数据的标准化配置

### 系统监控

- **服务器监控**：实时监控 CPU、内存、磁盘、网络等服务器资源使用情况
- **缓存监控**：查看 Redis 缓存信息，执行缓存清理操作
- **在线用户**：查看当前在线用户列表，支持强制用户下线

### 日志管理

- **操作日志**：记录所有用户操作日志，支持按条件查询和删除

### 定时任务

- **任务管理**：创建、编辑、删除定时任务
- **任务执行**：支持立即执行任务、暂停/恢复任务
- **执行日志**：查看定时任务执行日志

### 工具集

- **文件管理**：文件上传、删除等文件操作功能

### 流程引擎

- **业务流程**：内置流程引擎支持，实现业务流程的可配置化管理
- **流程模板**：支持流程模板定义和复用

## 项目架构

### 后端模块

```
admin-app/
├── aurora-common/        # 公共模块（实体类、工具类、配置等）
├── aurora-auth/          # 认证授权模块（Sa-Token 配置、登录逻辑）
├── aurora-admin/         # 核心业务模块（各类 Controller 和 Service）
├── aurora-quartz/        # 定时任务模块（Quartz 任务调度）
├── aurora-file/          # 文件服务模块（文件上传下载）
└── aurora-server/        # 启动模块（主程序入口、配置文件）
```

### 前端模块

```
admin-ui/
├── api/                  # API 请求接口定义
├── assets/               # 静态资源文件
├── components/           # 公共组件库
├── layouts/              # 页面布局组件
├── router/               # 路由配置
├── store/                # Pinia 状态管理
├── views/                # 页面视图组件
└── types/                # TypeScript 类型定义
```

## 技术栈

### 后端技术

- **核心框架**：Spring Boot 3.1.0
- **ORM 框架**：MyBatis Plus 3.5.5
- **权限认证**：Sa-Token 1.44.0
- **缓存**：Redis
- **任务调度**：Quartz
- **API 文档**：Knife4j 4.5.0
- **工具库**：Hutool 5.8.26
- **Java 版本**：JDK 17

### 前端技术

- **核心框架**：Vue 3.2.47
- **UI 组件库**：Element Plus 2.3.0
- **开发语言**：TypeScript 5.4.5
- **构建工具**：Vite 4.2.0
- **状态管理**：Pinia 2.0.33
- **路由管理**：Vue Router 4.1.6
- **HTTP 客户端**：Axios 1.3.4
- **编辑器**：Mavon Editor 3.0.1
- **图表库**：ECharts 5.5.1

### 数据库

- **数据库**：MySQL 8.0+
- **缓存**：Redis

## 安装部署

### 环境要求

- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Redis 6.0+

### 后端部署

1. 克隆项目

```bash
git clone https://gitee.com/wuhobin/aurora-admin.git
cd aurora-admin
```

2. 导入数据库

```bash
# 登录 MySQL
mysql -u root -p

# 创建数据库并导入
CREATE DATABASE aurora_admin CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE aurora_admin;
SOURCE aurora-admin.sql;
```

3. 修改配置文件
   编辑 `admin-app/aurora-server/src/main/resources/application.properties`：

```properties
# 数据库配置
mysql.address=${MYSQL_HOST:127.0.0.1}
mysql.username=${MYSQL_USER:root}
mysql.password=${MYSQL_PASSWORD:your-password}
# Redis 配置
redis.address=${REDIS_HOST:127.0.0.1}
redis.password=${REDIS_PASSWORD:your-password}
```

4. 启动后端服务

```bash
cd admin-app
mvn clean install
cd aurora-server
mvn spring-boot:run
```

或者直接打包运行：

```bash
cd admin-app
mvn clean package
java -jar aurora-server/target/aurora-blog.jar
```

后端服务默认运行在 `http://localhost:8080`

### 前端部署

1. 安装依赖

```bash
cd admin-ui
npm install
```

2. 修改环境配置（可选）
   编辑 `.env.development` 或 `.env.production` 文件，配置后端接口地址

3. 启动开发服务器

```bash
npm run dev
```

4. 构建生产版本

```bash
npm run build
```

前端服务默认运行在 `http://localhost:5173`

## 接口文档

项目集成了 Knife4j API 文档，启动后端服务后访问：

```
http://localhost:8080/doc.html
```

## 核心接口

### 认证授权

```http
# 登录
POST /auth/login

# 登出
POST /auth/logout

# 获取当前用户信息
GET /auth/info
```

### 用户管理

```http
# 获取用户列表
GET /sys/user

# 新增用户
POST /sys/user

# 修改用户
PUT /sys/user

# 删除用户
DELETE /sys/user/delete/{ids}

# 重置密码
PUT /sys/user/resetPassword
```

### 角色权限

```http
# 获取角色列表
GET /sys/role

# 新增角色
POST /sys/role/add

# 修改角色
PUT /sys/role/update

# 删除角色
DELETE /sys/role/delete/{ids}
```

### 菜单管理

```http
# 获取菜单树
GET /sys/menu/tree

# 新增菜单
POST /sys/menu

# 修改菜单
PUT /sys/menu

# 删除菜单
DELETE /sys/menu/{id}
```

### 系统监控

```http
# 获取服务器信息
GET /monitor/server

# 获取缓存信息
GET /monitor/cache/info

# 获取在线用户列表
GET /monitor/online/list
```

### 定时任务

```http
# 获取定时任务列表
GET /monitor/job/list

# 新增定时任务
POST /monitor/job

# 修改定时任务
PUT /monitor/job

# 删除定时任务
DELETE /monitor/job/delete/{ids}
```

## 贡献指南

欢迎参与项目贡献！请遵循以下流程：

1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

## 许可证

本项目采用 [GNU Affero General Public License v3.0](LICENSE) 许可证。

## 联系方式

- 项目地址：https://gitee.com/wuhobin/aurora-admin
- 问题反馈：请提交 Issue

## 更新日志

### v1.0.0

- 初始版本发布
- 完成基础系统管理功能
- 集成 Sa-Token 权限认证
- 添加流程引擎支持

## 使用说明

### 用户管理

```java
// 获取用户列表
GET /sys/user

// 新增用户
POST /sys/user

// 修改用户
PUT /sys/user

// 删除用户
DELETE /sys/user/delete/{ids}
```

### 角色权限

```java
// 获取角色列表
GET /sys/role

// 新增角色
POST /sys/role/add

// 修改角色
PUT /sys/role/update

// 删除角色
DELETE /sys/role/delete/{ids}
```

### 菜单管理

```java
// 获取菜单树
GET /sys/menu/tree

// 新增菜单
POST /sys/menu

// 修改菜单
PUT /sys/menu

// 删除菜单
DELETE /sys/menu/{id}
```

### 系统监控

```java
// 获取服务器信息
GET /monitor/server

// 获取缓存信息
GET /monitor/cache/info

// 获取在线用户列表
GET /monitor/online/list
```

### 定时任务

```java
// 获取定时任务列表
GET /monitor/job/list

// 新增定时任务
POST /monitor/job

// 修改定时任务
PUT /monitor/job

// 删除定时任务
DELETE /monitor/job/delete/{ids}
```

## 贡献指南

1. Fork 项目
2. 创建新分支
3. 提交代码
4. 创建 Pull Request

## 许可证

本项目采用 MIT 许可证。详情请查看项目根目录下的 LICENSE 文件。