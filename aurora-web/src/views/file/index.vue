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
        <el-form-item label="MIME" prop="contentType">
          <el-input
            v-model="queryParams.contentType"
            placeholder="例如 image/png"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="上传人" prop="uploaderName">
          <el-input
            v-model="queryParams.uploaderName"
            placeholder="请输入用户名"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="fileList" row-key="id">
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
        <el-table-column prop="fileName" label="文件名称" min-width="280" show-overflow-tooltip />
        <el-table-column prop="contentType" label="MIME" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag type="info" effect="plain">{{ row.contentType || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="大小" min-width="110" align="right">
          <template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column prop="uploaderName" label="上传人" min-width="120" align="center">
          <template #default="{ row }">{{ row.uploaderName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="上传时间" min-width="180" align="center" />
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
        <el-table-column label="操作" width="78" align="center" fixed="right">
          <template #default="{ row }">
            <el-tooltip content="删除文件" placement="top">
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
import { CopyDocument, Delete, Document, Link, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import {
  deleteFileApi,
  getFileListApi,
  type OssFileQuery,
  type OssFileRecord
} from '@/api/file'

const queryFormRef = ref<FormInstance>()
const loading = ref(false)
const total = ref(0)
const fileList = ref<OssFileRecord[]>([])

const queryParams = reactive<OssFileQuery>({
  pageNum: 1,
  pageSize: 10,
  fileName: '',
  contentType: '',
  uploaderName: ''
})

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
