# Aurora Admin Management System

Aurora Admin is a backend management system based on Spring Boot and Sa-Token, providing features such as user management, role permissions, menu management, system monitoring, scheduled tasks, and more.

## Features

- **User Management**: User list, add user, edit user, delete user, change password, personal information
- **Role Permissions**: Role list, add role, edit role, delete role, assign permissions
- **Menu Management**: Menu list, add menu, edit menu, delete menu
- **System Monitoring**: Server monitoring, cache monitoring, online users
- **Operation Logs**: Records all operation logs, supports deletion
- **Scheduled Tasks**: Manage scheduled tasks, supports immediate execution and status changes
- **Code Generation**: Supports automatic code generation for database tables
- **File Management**: File upload and deletion
- **Modular Architecture**: Designed with a modular structure for easy expansion and maintenance

## Technology Stack

- **Backend**: Spring Boot, MyBatis Plus, Sa-Token, Redis, Quartz
- **Frontend**: Vue3, Element Plus, TypeScript
- **Database**: MySQL
- **Others**: Swagger, Redis

## Installation Guide

1. Clone the project:
   ```bash
   git clone https://gitee.com/wuhobin/aurora-admin.git
   ```

2. Import the database:
   Import the database using the `aurora-admin.sql` file

3. Modify configuration:
   Update the database connection information in `admin-app/aurora-server/src/main/resources/application.properties`

4. Start the project:
   ```bash
   cd aurora-admin
   mvn spring-boot:run
   ```

## Usage Instructions

### User Management
```java
// Get user list
GET /sys/user

// Add user
POST /sys/user

// Edit user
PUT /sys/user

// Delete user
DELETE /sys/user/delete/{ids}
```

### Role Permissions
```java
// Get role list
GET /sys/role

// Add role
POST /sys/role/add

// Edit role
PUT /sys/role/update

// Delete role
DELETE /sys/role/delete/{ids}
```

### Menu Management
```java
// Get menu tree
GET /sys/menu/tree

// Add menu
POST /sys/menu

// Edit menu
PUT /sys/menu

// Delete menu
DELETE /sys/menu/{id}
```

### System Monitoring
```java
// Get server information
GET /monitor/server

// Get cache information
GET /monitor/cache/info

// Get online user list
GET /monitor/online/list
```

### Scheduled Tasks
```java
// Get scheduled task list
GET /monitor/job/list

// Add scheduled task
POST /monitor/job

// Edit scheduled task
PUT /monitor/job

// Delete scheduled task
DELETE /monitor/job/delete/{ids}
```

## Contribution Guide

1. Fork the project
2. Create a new branch
3. Submit your code
4. Create a Pull Request

## License

This project is licensed under the MIT License. For details, please refer to the LICENSE file in the project root directory.