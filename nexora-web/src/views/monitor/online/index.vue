<template>
  <div class="app-container data-list-page online-session-page">
    <el-card class="data-list-card" shadow="never">
      <div class="search-wrapper data-list-filters">
        <el-form ref="queryFormRef" :model="queryParams" :inline="true">
          <el-form-item label="用户" prop="keyword">
            <el-input
              v-model="queryParams.keyword"
              placeholder="请输入邮箱或昵称"
              clearable
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item label="IP" prop="ip">
            <el-input
              v-model="queryParams.ip"
              placeholder="请输入 IP"
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

      <div class="data-list-toolbar">
        <div class="data-list-heading">
          <strong>在线用户</strong>
          <span>共 {{ total }} 个有效会话</span>
        </div>
        <el-button
          :icon="Refresh"
          :loading="loading"
          aria-label="刷新在线用户列表"
          @click="handleRefresh"
        >
          刷新
        </el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="sessionList"
        row-key="sessionId"
        class="data-list-table"
        empty-text="暂无在线用户"
      >
        <el-table-column label="会话编号" min-width="150" align="center">
          <template #default="{ row }">
            <div class="session-cell">
              <el-tooltip :content="row.sessionId" placement="top">
                <span class="session-id">{{ abbreviateSessionId(row.sessionId) }}</span>
              </el-tooltip>
              <el-tag v-if="row.currentSession" type="info" effect="plain" size="small">
                当前会话
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="用户" min-width="210" align="left">
          <template #default="{ row }">
            <div class="user-cell">
              <strong v-if="normalize(row.nickname)">{{ normalize(row.nickname) }}</strong>
              <span>{{ displayValue(row.email) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="IP / 地点" min-width="190" align="left">
          <template #default="{ row }">
            <div class="detail-cell">
              <span>{{ displayValue(row.ip) }}</span>
              <small>{{ displayValue(row.location) }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="浏览器" min-width="140" align="center">
          <template #default="{ row }">{{ displayValue(row.browser) }}</template>
        </el-table-column>
        <el-table-column label="操作系统" min-width="150" align="center">
          <template #default="{ row }">{{ displayValue(row.os) }}</template>
        </el-table-column>
        <el-table-column label="登录时间" min-width="180" align="center">
          <template #default="{ row }">
            <time v-if="row.loginTime" :datetime="row.loginTime">{{ formatDateTime(row.loginTime) }}</time>
            <span v-else>--</span>
          </template>
        </el-table-column>
        <el-table-column label="最后访问时间" min-width="180" align="center">
          <template #default="{ row }">
            <time v-if="row.lastAccessTime" :datetime="row.lastAccessTime">
              {{ formatDateTime(row.lastAccessTime) }}
            </time>
            <span v-else>--</span>
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
          @size-change="handleSizeChange"
          @current-change="getList"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import dayjs from 'dayjs'
import { Refresh, Search } from '@element-plus/icons-vue'
import type { FormInstance } from 'element-plus'
import {
  getOnlineSessionListApi,
  type OnlineSessionQuery,
  type OnlineSessionRecord
} from '@/api/monitor/online'

const queryFormRef = ref<FormInstance>()
const loading = ref(false)
const total = ref(0)
const sessionList = ref<OnlineSessionRecord[]>([])
const queryParams = reactive<OnlineSessionQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  ip: ''
})

const normalize = (value?: string) => value?.trim() || ''

const displayValue = (value?: string) => normalize(value) || '--'

const abbreviateSessionId = (sessionId: string) => {
  const value = normalize(sessionId)
  if (!value) return '--'
  if (value.length <= 13) return value
  return `${value.slice(0, 8)}…${value.slice(-4)}`
}

const formatDateTime = (value?: string) => {
  if (!value) return '--'
  const parsed = dayjs(value)
  return parsed.isValid() ? parsed.format('YYYY-MM-DD HH:mm:ss') : '--'
}

const getList = async () => {
  loading.value = true
  try {
    const { data } = await getOnlineSessionListApi(queryParams)
    sessionList.value = data.records
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

const handleRefresh = () => {
  getList()
}

const handleSizeChange = () => {
  queryParams.pageNum = 1
  getList()
}

getList()
</script>

<style scoped>
.session-cell,
.user-cell,
.detail-cell {
  display: flex;
  min-width: 0;
  flex-direction: column;
  justify-content: center;
  gap: 3px;
}

.session-cell {
  align-items: center;
  gap: 6px;
}

.session-id,
.online-session-page time {
  font-variant-numeric: tabular-nums;
}

.session-id {
  color: var(--el-text-color-regular);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  letter-spacing: 0.02em;
}

.user-cell strong {
  overflow: hidden;
  color: var(--el-text-color-primary);
  font-size: 13px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-cell span,
.detail-cell small {
  overflow: hidden;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-cell > span {
  color: var(--el-text-color-primary);
  font-variant-numeric: tabular-nums;
}

@media (max-width: 640px) {
  .data-list-heading {
    width: 100%;
  }

  .data-list-toolbar :deep(.el-button) {
    width: 100%;
  }
}
</style>
