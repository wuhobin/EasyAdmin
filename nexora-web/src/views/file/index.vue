<template>
  <div class="app-container data-list-page">
    <el-card class="data-list-card" shadow="never">
    <div class="search-wrapper data-list-filters">
      <el-form ref="queryFormRef" :model="queryParams" :inline="true">
        <el-form-item label="文件名称" prop="fileName">
          <el-input
            v-model="queryParams.fileName"
            placeholder="请输入文件名称"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="文件类型" prop="contentType">
          <el-select
            v-model="queryParams.contentType"
            placeholder="请选择文件类型"
            clearable
            style="width: 200px"
            @change="handleQuery"
          >
            <el-option
              v-for="item in contentTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="isAdmin" label="上传人" prop="uploaderId">
          <el-select
            v-model="queryParams.uploaderId"
            placeholder="请选择用户"
            clearable
            filterable
            style="width: 200px"
          >
            <el-option
              v-for="user in userOptions"
              :key="user.id"
              :label="user.nickname"
              :value="user.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

        <div class="file-card-header data-list-toolbar">
          <div class="file-card-heading data-list-heading">
            <strong>文件列表</strong>
            <span>共 {{ total }} 个文件</span>
          </div>
          <el-button
            v-permission="['sys:file:upload']"
            type="primary"
            :icon="UploadFilled"
            @click="openUploadDialog"
          >
            上传文件
          </el-button>
        </div>

      <el-table v-loading="loading" :data="fileList" row-key="id" class="data-list-table">
        <el-table-column prop="id" label="ID" width="90" align="center" />
        <el-table-column prop="fileName" label="文件名称" min-width="280" show-overflow-tooltip />
        <el-table-column prop="contentType" label="文件类型" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag :type="getMimeTagType(row.contentType)" effect="plain">{{ row.contentType || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="大小" min-width="110" align="center">
          <template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="上传时间" min-width="180" align="center" />
        <el-table-column label="预览" width="96" align="center">
          <template #default="{ row }">
            <el-image
              v-if="row.contentType?.startsWith('image/')"
              class="file-preview"
              :src="row.thumbnailUrl || row.fileUrl"
              :preview-src-list="[row.fileUrl]"
              preview-teleported
              fit="cover"
            />
            <button
              v-else-if="isVideoFile(row)"
              type="button"
              class="file-preview-trigger file-video-preview"
              :title="`新窗口打开视频：${row.fileName}`"
              :aria-label="`新窗口打开视频：${row.fileName}`"
              @click="openFile(row.fileUrl)"
            >
              <img
                v-if="row.thumbnailUrl"
                class="file-video-thumbnail"
                :src="row.thumbnailUrl"
                alt=""
                aria-hidden="true"
              />
              <video
                v-else
                class="file-video-thumbnail"
                :src="row.fileUrl"
                muted
                playsinline
                preload="metadata"
                aria-hidden="true"
                tabindex="-1"
              ></video>
              <span class="file-video-play" aria-hidden="true">
                <el-icon><VideoPlay /></el-icon>
              </span>
            </button>
            <button
              v-else-if="isPdfFile(row)"
              type="button"
              class="file-preview-trigger file-pdf-preview"
              :title="`新窗口打开 PDF：${row.fileName}`"
              :aria-label="`新窗口打开 PDF：${row.fileName}`"
              @click="openFile(row.fileUrl)"
            >
              <el-icon class="file-pdf-icon" aria-hidden="true"><Document /></el-icon>
              <span class="file-pdf-label" aria-hidden="true">PDF</span>
            </button>
            <el-icon v-else class="file-icon"><Document /></el-icon>
          </template>
        </el-table-column>
        <el-table-column label="URL" min-width="100" align="center">
          <template #default="{ row }">
            <el-tooltip content="打开文件" placement="top">
              <el-button link :icon="Link" aria-label="打开文件" @click="openFile(row.fileUrl)" />
            </el-tooltip>
            <el-tooltip content="复制 URL" placement="top">
              <el-button link :icon="CopyDocument" aria-label="复制 URL" @click="copyUrl(row.fileUrl)" />
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="118" align="center" fixed="right">
          <template #default="{ row }">
            <el-tooltip content="下载文件" placement="top">
              <el-button
                v-permission="['sys:file:download']"
                link
                :icon="Download"
                :loading="downloadingId === row.id"
                aria-label="下载文件"
                @click="handleDownload(row)"
              />
            </el-tooltip>
            <el-tooltip v-if="canDeleteFile(row)" content="删除文件" placement="top">
              <el-button
                v-permission="['sys:file:delete']"
                link
                type="danger"
                :icon="Delete"
                aria-label="删除文件"
                @click="handleDelete(row)"
              />
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container data-list-pagination">
        <el-pagination
          v-model:current-page="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          background
          :page-sizes="[10, 20, 30, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="getList"
          @current-change="getList"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="uploadDialogVisible"
      title="上传文件"
      width="min(560px, calc(100vw - 32px))"
      append-to-body
      destroy-on-close
      :close-on-click-modal="!uploading"
      :close-on-press-escape="!uploading"
      :show-close="!uploading"
      :before-close="handleUploadDialogClose"
      @closed="resetUploadState"
    >
      <p class="upload-intro">
        选择一个文件并确认上传。支持 JPG、JPEG、PNG、GIF、WEBP、MP4、PDF、ZIP、TXT，最大 50MB。
      </p>

      <el-upload
        ref="uploadRef"
        class="upload-dropzone"
        drag
        :accept="UPLOAD_ACCEPT"
        :auto-upload="false"
        :disabled="uploading"
        :limit="1"
        :multiple="false"
        :show-file-list="false"
        :on-change="handleUploadFileChange"
        :on-exceed="handleUploadExceed"
      >
        <div class="upload-dropzone-content">
          <span class="upload-dropzone-icon" aria-hidden="true">
            <el-icon><UploadFilled /></el-icon>
          </span>
          <div>
            <strong>{{ selectedUploadFile ? '选择其他文件' : '拖放文件到这里' }}</strong>
            <p>也可以点击此区域选择文件</p>
          </div>
        </div>
      </el-upload>

      <div v-if="selectedUploadFile" class="selected-file" aria-label="已选择的文件">
        <span class="selected-file-icon" aria-hidden="true">
          <el-icon><Document /></el-icon>
        </span>
        <div class="selected-file-copy">
          <strong :title="selectedUploadFile.name">{{ selectedUploadFile.name }}</strong>
          <span>{{ formatFileSize(selectedUploadFile.size) }}</span>
        </div>
        <el-button
          link
          type="danger"
          :icon="Delete"
          :disabled="uploading"
          aria-label="移除已选择的文件"
          @click="clearUploadFile"
        />
      </div>

      <div v-if="uploadProgressVisible" class="upload-progress" aria-live="polite">
        <div class="upload-progress-heading">
          <span>{{ uploadProgressText }}</span>
          <strong>{{ uploadProgress }}%</strong>
        </div>
        <el-progress
          :percentage="uploadProgress"
          :show-text="false"
          :status="uploadProgressStatus"
        />
      </div>

      <el-alert
        v-if="uploadError"
        class="upload-error"
        type="error"
        :title="uploadError"
        :closable="false"
        show-icon
      />

      <template #footer>
        <div class="upload-dialog-footer">
          <el-button :disabled="uploading" @click="closeUploadDialog">取消</el-button>
          <el-button
            type="primary"
            :icon="UploadFilled"
            :loading="uploading"
            :disabled="!selectedUploadFile || uploading"
            @click="submitUpload"
          >
            {{ uploadActionText }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import type { AxiosProgressEvent } from 'axios'
import {
  CopyDocument,
  Delete,
  Document,
  Download,
  Link,
  Refresh,
  Search,
  UploadFilled,
  VideoPlay
} from '@element-plus/icons-vue'
import {
  ElMessage,
  ElMessageBox,
  genFileId,
  type FormInstance,
  type UploadInstance,
  type UploadProps,
  type UploadRawFile
} from 'element-plus'
import {
  deleteFileApi,
  downloadFileApi,
  getFileListApi,
  uploadApi,
  type OssFileQuery,
  type OssFileRecord
} from '@/api/file'
import { getUserListApi } from '@/api/system/user'
import { getDictListApi, getDictDataListApi } from '@/api/system/dict'
import { useUserStore } from '@/store/modules/user'
import { getUploadErrorMessage, UPLOAD_ACCEPT, validateUploadFile } from './upload'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.user.roles.includes('admin'))
const queryFormRef = ref<FormInstance>()
const loading = ref(false)
const total = ref(0)
const downloadingId = ref<number>()
const fileList = ref<OssFileRecord[]>([])
const userOptions = ref<{ id: number; nickname: string }[]>([])
const contentTypeOptions = ref<{ label: string; value: string }[]>([])
const uploadDialogVisible = ref(false)
const uploadRef = ref<UploadInstance>()
const selectedUploadFile = ref<UploadRawFile>()
const uploading = ref(false)
const uploadProgress = ref(0)
const uploadProgressVisible = ref(false)
const uploadError = ref('')

const uploadActionText = computed(() => uploadError.value ? '重新上传' : '开始上传')
const uploadProgressText = computed(() => {
  if (uploadError.value) return '上传失败'
  if (uploadProgress.value >= 100) return '文件已传输，正在处理'
  return '正在上传'
})
const uploadProgressStatus = computed(() => uploadError.value ? 'exception' as const : undefined)

const queryParams = reactive<OssFileQuery>({
  pageNum: 1,
  pageSize: 10,
  fileName: '',
  contentType: '',
  uploaderId: undefined
})

const loadUserOptions = async () => {
  try {
    const { data } = await getUserListApi({ pageNum: 1, pageSize: 1000 })
    userOptions.value = data.records
  } catch {
    // ignore
  }
}

const loadContentTypeOptions = async () => {
  try {
    const { data: dictList } = await getDictListApi({ pageNum: 1, pageSize: 100 })
    const dict = dictList.records?.find((d: any) => d.type === 'file_content_type')
    if (!dict) return
    const { data: dictData } = await getDictDataListApi({ dictId: dict.id, pageNum: 1, pageSize: 100 })
    contentTypeOptions.value = dictData.records.map((item: any) => ({
      label: item.label,
      value: item.value
    }))
  } catch {
    // ignore
  }
}

const getList = async () => {
  loading.value = true
  try {
    const { data } = await getFileListApi(queryParams)
    fileList.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNum = 1
  getList()
}

const resetQuery = () => {
  queryFormRef.value?.resetFields()
  queryParams.pageNum = 1
  getList()
}

const getMimeTagType = (contentType?: string) => {
  if (!contentType) return 'info'
  if (contentType.startsWith('image/')) return 'success'
  if (contentType.startsWith('video/')) return 'danger'
  if (contentType.startsWith('audio/')) return 'warning'
  if (contentType.startsWith('application/')) return 'primary'
  return 'info'
}

const formatFileSize = (size?: number) => {
  if (!size) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  const unitIndex = Math.min(Math.floor(Math.log(size) / Math.log(1024)), units.length - 1)
  const value = size / Math.pow(1024, unitIndex)
  return `${value >= 10 || unitIndex === 0 ? value.toFixed(0) : value.toFixed(1)} ${units[unitIndex]}`
}

const normalizedContentType = (row: unknown) => {
  const file = row as Pick<OssFileRecord, 'contentType'>
  return file.contentType?.split(';', 1)[0].trim().toLowerCase() || ''
}

const isVideoFile = (row: unknown) => normalizedContentType(row).startsWith('video/')

const isPdfFile = (row: unknown) => normalizedContentType(row) === 'application/pdf'

const resetUploadState = () => {
  uploadRef.value?.clearFiles()
  selectedUploadFile.value = undefined
  uploading.value = false
  uploadProgress.value = 0
  uploadProgressVisible.value = false
  uploadError.value = ''
}

const openUploadDialog = () => {
  resetUploadState()
  uploadDialogVisible.value = true
}

const handleUploadDialogClose = (done: () => void) => {
  if (!uploading.value) done()
}

const closeUploadDialog = () => {
  if (!uploading.value) uploadDialogVisible.value = false
}

const clearUploadFile = () => {
  if (uploading.value) return
  uploadRef.value?.clearFiles()
  selectedUploadFile.value = undefined
  uploadProgress.value = 0
  uploadProgressVisible.value = false
  uploadError.value = ''
}

const handleUploadFileChange: UploadProps['onChange'] = (uploadFile) => {
  if (uploading.value || !uploadFile.raw) return

  const validationError = validateUploadFile(uploadFile.raw)
  if (validationError) {
    clearUploadFile()
    uploadError.value = validationError
    return
  }

  selectedUploadFile.value = uploadFile.raw
  uploadProgress.value = 0
  uploadProgressVisible.value = false
  uploadError.value = ''
}

const handleUploadExceed: UploadProps['onExceed'] = (files) => {
  if (uploading.value || !files.length) return

  const file = files[0] as UploadRawFile
  file.uid = genFileId()
  uploadRef.value?.clearFiles()
  uploadRef.value?.handleStart(file)
}

const updateUploadProgress = (event: AxiosProgressEvent) => {
  const total = event.total || selectedUploadFile.value?.size
  const ratio = typeof event.progress === 'number'
    ? event.progress
    : total
      ? event.loaded / total
      : 0
  const percentage = Math.min(100, Math.max(0, Math.round(ratio * 100)))
  uploadProgress.value = Math.max(uploadProgress.value, percentage)
}

const submitUpload = async () => {
  const file = selectedUploadFile.value
  if (!file || uploading.value) return

  const validationError = validateUploadFile(file)
  if (validationError) {
    uploadError.value = validationError
    return
  }

  const formData = new FormData()
  formData.append('file', file, file.name)
  uploading.value = true
  uploadProgress.value = 0
  uploadProgressVisible.value = true
  uploadError.value = ''

  try {
    await uploadApi(formData, updateUploadProgress)
    uploadProgress.value = 100
  } catch (error) {
    uploadError.value = getUploadErrorMessage(error)
    return
  } finally {
    uploading.value = false
  }

  ElMessage.success('上传成功')
  queryParams.pageNum = 1
  uploadDialogVisible.value = false
  await getList().catch(() => undefined)
}

const openFile = (url: string) => {
  window.open(url, '_blank', 'noopener,noreferrer')
}

const copyUrl = async (url: string) => {
  await navigator.clipboard.writeText(url)
  ElMessage.success('URL 已复制')
}

const canDeleteFile = (row: unknown) => {
  const file = row as OssFileRecord
  return userStore.user.roles.includes('admin') || file.uploaderId === userStore.user.id
}

const handleDownload = async (row: unknown) => {
  const file = row as OssFileRecord
  downloadingId.value = file.id
  try {
    const data = await downloadFileApi(file.id)
    const blob = data instanceof Blob ? data : new Blob([data], { type: file.contentType })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    try {
      link.href = url
      link.download = file.originalFilename || file.fileName
      document.body.appendChild(link)
      link.click()
    } finally {
      document.body.removeChild(link)
      URL.revokeObjectURL(url)
    }
  } finally {
    downloadingId.value = undefined
  }
}

const handleDelete = async (row: unknown) => {
  const file = row as OssFileRecord
  await ElMessageBox.confirm(
    `确定删除文件”${file.fileName}”吗？`,
    '删除确认',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
  )
  await deleteFileApi(file.id)
  ElMessage.success('删除成功')
  if (fileList.value.length === 1 && queryParams.pageNum > 1) {
    queryParams.pageNum -= 1
  }
  getList()
}

if (isAdmin.value) {
  loadUserOptions()
}
loadContentTypeOptions()
getList()
</script>

<style scoped>
.file-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.file-card-heading {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.file-card-heading strong {
  color: var(--el-text-color-primary);
  font-size: 15px;
}

.file-card-heading span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.file-preview {
  width: 48px;
  height: 48px;
  border-radius: 8px;
}

.file-preview-trigger {
  position: relative;
  display: inline-grid;
  width: 48px;
  height: 48px;
  padding: 0;
  overflow: hidden;
  place-items: center;
  vertical-align: middle;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-fill-color-light);
  color: inherit;
  cursor: pointer;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.file-preview-trigger:hover {
  border-color: var(--el-color-primary-light-5);
  box-shadow: 0 6px 14px rgb(0 0 0 / 12%);
  transform: translateY(-1px);
}

.file-preview-trigger:focus-visible {
  outline: 2px solid var(--el-color-primary);
  outline-offset: 2px;
}

.file-video-preview {
  background: #0b0d12;
}

.file-video-thumbnail {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  pointer-events: none;
}

.file-video-play {
  position: absolute;
  display: grid;
  width: 24px;
  height: 24px;
  place-items: center;
  border: 1px solid rgb(255 255 255 / 72%);
  border-radius: 50%;
  background: rgb(0 0 0 / 58%);
  color: #fff;
  font-size: 15px;
  box-shadow: 0 2px 8px rgb(0 0 0 / 24%);
}

.file-pdf-preview {
  align-content: center;
  gap: 2px;
  background: var(--el-color-danger-light-9);
  color: var(--el-color-danger);
}

.file-pdf-preview::after {
  position: absolute;
  top: 0;
  right: 0;
  width: 0;
  height: 0;
  border-width: 0 0 11px 11px;
  border-style: solid;
  border-color: transparent transparent var(--el-color-danger-light-7) transparent;
  content: '';
}

.file-pdf-icon {
  font-size: 22px;
}

.file-pdf-label {
  font-size: 9px;
  font-weight: 800;
  letter-spacing: 0.08em;
  line-height: 1;
}

.file-icon {
  width: 48px;
  height: 48px;
  font-size: 30px;
  color: var(--el-text-color-secondary);
}

.upload-intro {
  margin: -4px 0 18px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 1.7;
}

.upload-dropzone {
  width: 100%;
}

.upload-dropzone :deep(.el-upload),
.upload-dropzone :deep(.el-upload-dragger) {
  width: 100%;
}

.upload-dropzone :deep(.el-upload-dragger) {
  padding: 30px 24px;
  border-color: var(--el-border-color);
  background:
    linear-gradient(135deg, var(--el-color-primary-light-9), transparent 46%),
    var(--el-fill-color-lighter);
  transition: border-color 0.2s ease, background-color 0.2s ease;
}

.upload-dropzone :deep(.el-upload-dragger:hover) {
  border-color: var(--el-color-primary);
}

.upload-dropzone-content {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  text-align: left;
}

.upload-dropzone-icon {
  display: grid;
  flex: none;
  width: 48px;
  height: 48px;
  place-items: center;
  border: 1px solid var(--el-color-primary-light-7);
  border-radius: 14px;
  background: var(--el-bg-color);
  color: var(--el-color-primary);
  font-size: 24px;
  box-shadow: 0 8px 20px rgb(64 158 255 / 10%);
}

.upload-dropzone-content strong {
  display: block;
  color: var(--el-text-color-primary);
  font-size: 15px;
  line-height: 1.4;
}

.upload-dropzone-content p {
  margin: 5px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.selected-file {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  margin-top: 14px;
  padding: 12px 14px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  background: var(--el-fill-color-light);
}

.selected-file-icon {
  display: grid;
  flex: none;
  width: 34px;
  height: 34px;
  place-items: center;
  border-radius: 8px;
  background: var(--el-bg-color);
  color: var(--el-color-primary);
  font-size: 18px;
}

.selected-file-copy {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  gap: 3px;
}

.selected-file-copy strong {
  overflow: hidden;
  color: var(--el-text-color-primary);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.selected-file-copy span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.upload-progress {
  margin-top: 18px;
}

.upload-progress-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  color: var(--el-text-color-regular);
  font-size: 12px;
}

.upload-progress-heading strong {
  color: var(--el-color-primary);
  font-variant-numeric: tabular-nums;
}

.upload-error {
  margin-top: 14px;
}

.upload-dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 520px) {
  .file-card-header {
    align-items: stretch;
    flex-direction: column;
  }

  .file-card-header :deep(.el-button) {
    width: 100%;
  }

  .upload-dropzone-content {
    flex-direction: column;
    text-align: center;
  }

}

@media (prefers-reduced-motion: reduce) {
  .file-preview-trigger,
  .upload-dropzone :deep(.el-upload-dragger) {
    transition: none;
  }

  .file-preview-trigger:hover {
    transform: none;
  }
}
</style>
