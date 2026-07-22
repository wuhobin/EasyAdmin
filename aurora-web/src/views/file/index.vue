<template>
  <div class="app-container">
    <div class="search-wrapper">
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
        <el-form-item label="上传人" prop="uploaderId">
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

    <el-card shadow="never">
      <el-table v-loading="loading" :data="fileList" row-key="id">
        <el-table-column prop="fileName" label="文件名称" min-width="280" show-overflow-tooltip />
        <el-table-column prop="contentType" label="文件类型" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag :type="getMimeTagType(row.contentType)" effect="plain">{{ row.contentType || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="大小" min-width="110" align="right">
          <template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column prop="uploaderName" label="上传人" min-width="120" align="center">
          <template #default="{ row }">{{ row.uploaderName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="上传时间" min-width="180" align="center" />
        <el-table-column label="预览" width="88" align="center">
          <template #default="{ row }">
            <el-image
              v-if="row.contentType?.startsWith('image/')"
              class="file-preview"
              :src="row.thumbnailUrl || row.fileUrl"
              :preview-src-list="[row.fileUrl]"
              preview-teleported
              fit="cover"
            />
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

      <div class="pagination-container">
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
  </div>
</template>

<script setup lang="ts">
import { CopyDocument, Delete, Document, Download, Link, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import {
  deleteFileApi,
  downloadFileApi,
  getFileListApi,
  type OssFileQuery,
  type OssFileRecord
} from '@/api/file'
import { getUserListApi } from '@/api/system/user'
import { getDictListApi, getDictDataListApi } from '@/api/system/dict'
import { useUserStore } from '@/store/modules/user'

const userStore = useUserStore()
const queryFormRef = ref<FormInstance>()
const loading = ref(false)
const total = ref(0)
const downloadingId = ref<number>()
const fileList = ref<OssFileRecord[]>([])
const userOptions = ref<{ id: number; nickname: string }[]>([])
const contentTypeOptions = ref<{ label: string; value: string }[]>([])

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

loadUserOptions()
loadContentTypeOptions()
getList()
</script>

<style scoped>
.file-preview {
  width: 48px;
  height: 48px;
  border-radius: 4px;
}

.file-icon {
  width: 48px;
  height: 48px;
  font-size: 30px;
  color: var(--el-text-color-secondary);
}
</style>
