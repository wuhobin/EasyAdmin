# Aurora Admin Management System

Aurora Admin (EasyAdmin) is an enterprise backend management system based on Spring Boot 3 + Sa-Token, featuring a separated front-end and back-end architecture with RBAC permission control, scheduled tasks, file management, and aggregated email platform.

## Features

- **User Management**: User list, add/edit/delete users, reset password, personal profile
- **Role Permissions**: Role CRUD, menu permission assignment, role-user association
- **Menu Management**: Tree-structured menu CRUD with button-level permission control
- **Dictionary Management**: Dictionary type and data maintenance for standardized configuration
- **Operation Logs**: Automatic AOP-based operation logging with query and deletion
- **Scheduled Tasks**: Quartz-based job management with Cron visual editor and execution logs
- **File Management**: OSS file upload, download, preview, and deletion
- **Aggregated Mailbox**: Multi-account (QQ/163/126/Yeah) email aggregation, inbox reading, attachment download

## Technology Stack

- **Backend**: Spring Boot 3.1.x, MyBatis Plus, Sa-Token, Redis, Quartz
- **Frontend**: Vue 3, Element Plus, Pinia, Vue Router, Axios
- **Database**: MySQL 8.0+
- **API Docs**: Knife4j (Swagger)

## Installation Guide

1. Clone the project:
   ```bash
   git clone https://gitee.com/wuhobin/aurora-admin.git
   ```

2. Import the database:
   ```bash
   mysql -u root -p
   CREATE DATABASE easyadmin CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
   USE easyadmin;
   SOURCE aurora-admin.sql;
   ```

3. Start the backend:
   ```bash
   cd aurora-app
   mvn clean install -DskipTests
   cd aurora-server
   mvn spring-boot:run
   ```
   Backend runs at `http://localhost:8800`

4. Start the frontend:
   ```bash
   cd aurora-web
   npm install
   npm run dev
   ```

## API Documentation

Visit `http://localhost:8800/doc.html` after starting the backend.

## License

This project is licensed under the GNU Affero General Public License v3.0.
