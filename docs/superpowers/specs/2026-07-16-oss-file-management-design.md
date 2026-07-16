# OSS 文件管理功能设计

## 目标

为 Aurora Admin 增加 OSS 文件流水与后台文件列表。所有通过现有 `FileService` 上传成功的文件都记录到数据库，管理页面支持分页查询、查看文件信息和单条删除。本期不在文件管理页面提供上传按钮。

## 范围

本功能包含以下修改：

- 扩展 `platform-boot-starter` 的 `oss-spring-boot-starter` 上传结果模型。
- 在 EasyAdmin 后端新增 OSS 文件流水表、实体、Mapper、独立记录服务、延迟重试任务和管理接口。
- 在 EasyAdmin 前端新增文件列表页面和接口封装。
- 更新初始化 SQL 中的表结构、菜单和权限数据。

本期不包含以下能力：

- 文件管理页面上传文件。
- 批量删除文件。
- Redis 入队失败补偿。
- 重试耗尽后的死信表、消息通知或告警平台接入。
- 对历史 OSS 文件进行自动补录。

## Starter 修改

修改工程：`C:/IdeaProjects/personal/platform-boot-starter/platform-starter/oss-spring-boot-starter`。

`OssUploadResult` 增加以下字段，并在 `OssUploadResult.from(FileInfo)` 中完成映射：

- `id`：`FileInfo.id`
- `originalFilename`：`FileInfo.originalFilename`
- `contentType`：`FileInfo.contentType`

已有的 `url`、`filename`、`size`、`platform` 和 `thUrl` 字段保持兼容。EasyAdmin 现有上传接口仍返回 URL 字符串，避免破坏头像上传和通用图片上传组件。

当前项目没有自定义 x-file-storage `FileRecorder`，因此 `FileInfo.id` 可能为空。EasyAdmin 在上传成功后优先使用 starter 返回的文件 ID；为空时生成全局唯一字符串 ID，并回填到 `OssUploadResult`。该标识保存到 `file_id`，专门用于文件业务标识和重试幂等；数据库自增 `id` 只用于后台 CRUD。

## 数据模型

新增表 `sys_oss_file`：

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `id` | `bigint` | 主键、自增 | 数据库记录 ID |
| `file_id` | `varchar(64)` | 非空、唯一索引 | OSS 文件全局唯一 ID |
| `file_url` | `varchar(1000)` | 非空、普通索引 | OSS 访问地址 |
| `file_name` | `varchar(255)` | 非空 | OSS 保存文件名 |
| `original_filename` | `varchar(255)` | 可空、普通索引 | 上传时的原始文件名 |
| `content_type` | `varchar(128)` | 可空、普通索引 | 原始 MIME，例如 `image/png`、`video/mp4` |
| `file_size` | `bigint` | 非空、默认 0 | 文件大小，单位字节 |
| `platform` | `varchar(64)` | 可空 | x-file-storage 平台标识 |
| `thumbnail_url` | `varchar(1000)` | 可空 | 缩略图 URL |
| `uploader_id` | `bigint` | 可空、普通索引 | 上传人 ID |
| `uploader_name` | `varchar(64)` | 可空、普通索引 | 上传人用户名快照 |
| `create_time` | `datetime` | 可空 | 上传时间 |
| `update_time` | `datetime` | 可空 | 更新时间 |

实体命名为 `SysOssFile`，继承项目的 `BaseEntity`。`id` 使用 `Long` 自增主键，`fileId` 保存 OSS 文件唯一标识并建立唯一索引。文件 URL 不承担唯一标识职责。

## 后端架构

新增独立的 `SysOssFileService`，负责以下职责：

- 将上传结果和上传人信息转换为 `SysOssFile` 并保存。
- 按 `file_id` 幂等保存重试数据。
- 按文件名、MIME、上传人分页查询。
- 按数据库自增 `id` 查询记录并执行 OSS 删除。
- 在 OSS 删除成功后物理删除数据库记录。

`FileServiceImpl` 仍负责上传参数校验、日期路径生成和 OSS 调用。上传成功后，它组装文件流水数据并调用 `SysOssFileService.recordUpload()`。上传人从当前 Sa-Token 会话的 `Constants.CURRENT_USER` 中读取，保存用户 ID 和用户名快照。

如果 `OssUploadResult.id` 为空，`FileServiceImpl` 在首次保存前生成全局唯一字符串文件 ID。重试数据通过 `fileId` 携带该值，保证同步写入和异步重试使用同一个唯一键；数据库自增主键不进入重试数据。

## 上传与重试流程

上传流程如下：

1. 校验 `MultipartFile` 非空。
2. 调用 `OssTemplate` 上传文件。
3. 校验上传结果和 URL 非空。
4. 确定 `fileId`：优先使用上传结果 ID，为空时生成唯一文件 ID。
5. 读取当前上传人信息并构造可序列化的 `OssFileRecordRetryData`。
6. 同步调用 `SysOssFileService.saveIfAbsent()` 写入流水。
7. 同步写入成功时直接返回原有上传结果。
8. 同步写入失败时记录警告日志，并调用延迟任务的 `producer()`，初始延迟 15 秒。
9. 延迟任务成功入队后，上传接口仍按 OSS 上传成功处理并返回 URL。

新增 `OssFileRecordRetryTask extends DelayedRetryTask<OssFileRecordRetryData>`。任务的 `execute()` 调用 `saveIfAbsent()`：

- `file_id` 已存在时直接返回成功。
- `file_id` 不存在时插入流水，由数据库生成记录 `id`。
- 插入失败时抛出异常，由 `DelayedRetryTask` 触发下一次延迟消费。

`DelayRetry` 参数固定为：

- `maxCount = 9`
- `interval = 15`
- `useSameInterval = false`
- 首次 `producer()` 延迟 15 秒

当前 starter 的 `DelayedRetryTask.retry()` 每次失败会先调用 `addCount()`，随后 `checkAndIncrement()` 内再次调用 `addCount()`，因此一次失败会增加两次计数。实测 `maxCount=9` 对应 5 次异步消费；`maxCount=5` 仅有 3 次异步消费，`maxCount=11` 会有 6 次异步消费。

最终一次消费仍失败时，任务在 `handleException()` 中记录包含 `fileId`、URL 和异常堆栈的 ERROR 日志，并抛出 `IllegalStateException` 终止任务。该异常发生在异步线程中，不能返回给已经完成的上传请求。

本期按需求不处理 Redis `producer()` 本身失败的情况。

## 删除流程

管理页面使用数据库记录 `id` 删除：

1. 根据自增主键 `id` 查询 `sys_oss_file`。
2. 记录不存在时返回业务异常。
3. 使用记录中的 URL 调用现有 OSS 删除能力。
4. OSS 删除失败时返回失败，数据库记录保持不变。
5. OSS 删除成功后物理删除 `sys_oss_file` 记录。

新增 `DELETE /file/{id}` 作为管理页面删除接口，路径参数类型为 `Long`，使用现有 `sys:file:delete` 权限。

现有 `GET /file/delete?url=...` 暂时保留兼容。该接口优先按 URL 查询流水并调用统一删除服务；未找到流水时保留原有按 URL 删除 OSS 的行为。新页面不使用该兼容接口。

## 查询接口

新增 `GET /file/list`，返回 MyBatis Plus 分页结果。查询参数包括：

- `originalFilename`：模糊匹配原始文件名。
- `contentType`：匹配 MIME 类型。
- `uploaderName`：模糊匹配上传人用户名。
- `pageNum`、`pageSize`：沿用 `PageParam`。

列表默认按 `create_time` 倒序排列。接口使用 `sys:file:list` 权限。

## 前端页面

新增文件列表页面和对应 API：

- 页面路由位于文件管理目录下。
- 页面顶部提供原始文件名、MIME、上传人三个搜索条件及搜索、重置操作。
- 本期不显示上传按钮。
- 表格展示预览、原始文件名、MIME、文件大小、上传人、上传时间、URL 和操作。
- `contentType` 以 `image/` 开头时使用图片缩略图预览；其他文件显示文件图标。
- 文件大小按 B、KB、MB、GB 格式化展示。
- URL 提供打开和复制图标按钮，并提供工具提示。
- 每行提供删除按钮，删除前二次确认，成功后刷新当前分页。
- 本期只支持单条删除，不提供选择列和批量删除。

页面遵循现有 Element Plus、`app-container`、搜索区域、表格和分页样式，不引入新 UI 依赖。

## 菜单与权限

更新 `aurora-admin.sql`：

- 将现有“文件管理”目录调整为可见，并配置默认跳转到文件列表。
- 新增“文件列表”菜单，组件指向新页面。
- 新增 `sys:file:list` 列表权限。
- 复用已有 `sys:file:delete` 删除权限。
- 保留已有 `sys:file:upload` 权限，供头像和通用上传组件使用；本期文件列表页面不展示上传按钮。

初始化 SQL 同时创建 `sys_oss_file` 表。对于已部署数据库，实施说明需要提供等价的建表和菜单更新 SQL。

## 错误处理

- 上传文件为空或 OSS 返回空结果时，沿用现有业务异常。
- 首次流水写入失败不改变已经成功的上传响应，转入 Redis 延迟重试。
- 重复重试通过 `file_id` 幂等处理，不插入重复数据。
- 重试耗尽后记录 ERROR 日志并在异步线程抛出异常。
- OSS 删除失败时不删除数据库记录。
- 数据库物理删除失败时接口返回异常；OSS 文件已经删除，日志需要包含记录 `id` 和 `fileId` 便于人工处理。

## 测试与验收

后端测试覆盖：

- `OssUploadResult.from()` 映射 `id`、`originalFilename` 和 `contentType`。
- 上传结果存在 ID 时保存为 `fileId`。
- 上传结果 ID 为空时生成非空唯一 `fileId`。
- 首次流水写入成功时不调用重试任务。
- 首次流水写入失败时以 15 秒延迟生产任务。
- 相同 `fileId` 重复消费不会产生重复记录。
- 当前 starter 计数逻辑下，`maxCount=9` 对应 5 次异步消费。
- 文件列表查询条件和创建时间倒序。
- OSS 删除失败时保留数据库记录。
- OSS 删除成功后物理删除数据库记录。

前端验证覆盖：

- TypeScript 类型检查通过。
- 现有 Node 测试通过。
- 生产构建通过。
- 页面能正确渲染图片和非图片文件。
- 搜索、重置、分页、打开 URL、复制 URL 和删除交互正常。
- 页面在常用桌面宽度和移动宽度下没有文字或操作区重叠。

验收命令：

```bash
cd aurora-app
mvn -pl aurora-admin -am test -DskipTests=false

cd aurora-web
npm run check
```

starter 修改需要在 `platform-boot-starter` 工程执行对应模块测试并安装到本地 Maven 仓库后，再构建 EasyAdmin。
