<template>
  <div class="notice-page data-list-page">
    <el-card shadow="never" class="data-list-card">
      <el-form :inline="true" :model="query" class="data-list-filters">
        <el-form-item label="标题">
          <el-input v-model="query.title" clearable placeholder="请输入标题" @keyup.enter="loadList" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="query.noticeType" clearable placeholder="全部类型" style="width: 130px">
            <el-option label="通知" :value="1" />
            <el-option label="公告" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部状态" style="width: 130px">
            <el-option label="草稿" :value="0" />
            <el-option label="已发布" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadList">搜索</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="data-list-toolbar">
        <el-button v-permission="['sys:notice:add']" type="primary" @click="openCreate">新建草稿</el-button>
      </div>

      <el-table v-loading="loading" :data="records" class="data-list-table">
        <el-table-column prop="title" label="标题" min-width="230" show-overflow-tooltip />
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }"><el-tag :type="row.noticeType === 2 ? 'warning' : 'primary'">{{ row.noticeType === 2 ? '公告' : '通知' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '已发布' : '草稿' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="接收数" width="90" align="center" prop="recipientCount" />
        <el-table-column label="已读/未读" width="110" align="center">
          <template #default="{ row }">{{ row.readCount || 0 }}/{{ row.unreadCount || 0 }}</template>
        </el-table-column>
        <el-table-column prop="publishTime" label="发布时间" width="180" />
        <el-table-column prop="createName" label="创建人" width="110" />
        <el-table-column label="操作" width="250" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">查看</el-button>
            <el-button v-if="row.status === 0" v-permission="['sys:notice:update']" link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="row.status === 0" v-permission="['sys:notice:delete']" link type="danger" @click="removeDraft(row)">删除</el-button>
            <el-button v-if="row.status === 0" v-permission="['sys:notice:publish']" link type="success" @click="publish(row)">发布</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="data-list-pagination">
        <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" background
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" :total="total"
          @size-change="loadList" @current-change="loadList" />
      </div>
    </el-card>

    <el-dialog v-model="editorVisible" :title="editorMode === 'create' ? '新建系统通知' : '编辑系统通知草稿'" width="720px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="标题" prop="title"><el-input v-model="form.title" maxlength="62" show-word-limit placeholder="最多62个字符" /></el-form-item>
        <el-row :gutter="18">
          <el-col :span="12"><el-form-item label="类型" prop="noticeType"><el-select v-model="form.noticeType" style="width: 100%"><el-option label="通知" :value="1" /><el-option label="公告" :value="2" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="内容格式" prop="contentFormat"><el-select v-model="form.contentFormat" style="width: 100%"><el-option label="普通文案" value="text" /><el-option label="HTML/CSS" value="html" /></el-select></el-form-item></el-col>
        </el-row>
        <el-form-item v-if="form.noticeType === 1" label="接收对象" prop="targetType"><el-select v-model="form.targetType" style="width: 100%"><el-option label="全部正常用户" :value="3" /><el-option label="指定用户" :value="1" /></el-select></el-form-item>
        <el-form-item v-if="form.noticeType === 1 && form.targetType === 1" label="指定用户" prop="targetUserIds"><el-select v-model="form.targetUserIds" multiple filterable collapse-tags style="width: 100%" placeholder="请选择正常用户"><el-option v-for="user in users" :key="user.id" :label="`${user.nickname || user.email}（${user.email}）`" :value="user.id" /></el-select></el-form-item>
        <el-form-item label="正文" prop="content"><el-input v-model="form.content" type="textarea" :rows="12" :maxlength="form.contentFormat === 'html' ? 262144 : 20000" show-word-limit :placeholder="form.contentFormat === 'html' ? '直接粘贴 HTML/CSS 源码，脚本不会执行' : '请输入通知文案，支持换行'" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="editorVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveDraft">保存草稿</el-button></template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="通知详情" width="720px" destroy-on-close>
      <template v-if="detail">
        <div class="detail-meta"><el-tag>{{ detail.noticeType === 2 ? '公告' : '通知' }}</el-tag><span>{{ detail.contentFormat === 'html' ? 'HTML/CSS' : '普通文案' }}</span><span>{{ detail.targetType === 3 ? '全部正常用户' : `指定用户（${detail.targetUserIds?.length || 0} 人）` }}</span><span>{{ detail.publishTime || detail.createTime || '' }}</span></div>
        <h2 class="detail-title">{{ detail.title }}</h2>
        <iframe v-if="detail.contentFormat === 'html'" class="notice-html-frame" sandbox="allow-popups" :srcdoc="detail.content" title="通知 HTML 内容" />
        <pre v-else class="notice-text-content">{{ detail.content }}</pre>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { addNoticeApi, deleteNoticeApi, getNoticeDetailApi, getNoticeListApi, publishNoticeApi, updateNoticeApi, type NoticeForm, type NoticeItem } from '@/api/system/notice'
import { getUserListApi } from '@/api/system/user'

const loading = ref(false)
const saving = ref(false)
const total = ref(0)
const records = ref<NoticeItem[]>([])
const users = ref<any[]>([])
const editorVisible = ref(false)
const detailVisible = ref(false)
const editorMode = ref<'create' | 'edit'>('create')
const detail = ref<NoticeItem | null>(null)
const formRef = ref<FormInstance>()
const query = reactive<any>({ pageNum: 1, pageSize: 10, title: '', noticeType: undefined, status: undefined })
const form = reactive<NoticeForm>({ title: '', content: '', contentFormat: 'text', noticeType: 1, targetType: 3, targetUserIds: [] })
const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入正文', trigger: 'blur' }],
  noticeType: [{ required: true, message: '请选择类型', trigger: 'change' }],
  contentFormat: [{ required: true, message: '请选择内容格式', trigger: 'change' }],
  targetType: [{ required: true, message: '请选择接收对象', trigger: 'change' }],
  targetUserIds: [{ required: true, message: '请选择用户', trigger: 'change' }]
}

async function loadList() {
  loading.value = true
  try {
    const { data } = await getNoticeListApi(query)
    records.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function resetQuery() { query.title = ''; query.noticeType = undefined; query.status = undefined; query.pageNum = 1; void loadList() }
function resetForm() { Object.assign(form, { id: undefined, title: '', content: '', contentFormat: 'text', noticeType: 1, targetType: 3, targetUserIds: [] }) }
async function openCreate() { resetForm(); editorMode.value = 'create'; await loadUsers(); editorVisible.value = true }
async function openEdit(row: any) { Object.assign(form, { ...row, targetUserIds: row.targetUserIds || [] }); editorMode.value = 'edit'; await loadUsers(); editorVisible.value = true }
async function loadUsers() {
  if (users.value.length) return
  const pageSize = 100
  let pageNum = 1
  let totalUsers = Number.MAX_SAFE_INTEGER
  const loadedUsers: any[] = []
  while (loadedUsers.length < totalUsers) {
    const { data } = await getUserListApi({ pageNum, pageSize, status: 1 })
    const records = data.records || []
    loadedUsers.push(...records)
    totalUsers = Number(data.total || loadedUsers.length)
    if (!records.length) break
    pageNum += 1
  }
  users.value = loadedUsers
}
async function saveDraft() {
  if (!(await formRef.value?.validate().catch(() => false))) return
  saving.value = true
  try { if (editorMode.value === 'create') await addNoticeApi(form); else await updateNoticeApi(form); ElMessage.success('草稿已保存'); editorVisible.value = false; await loadList() } finally { saving.value = false }
}
async function publish(row: any) { await ElMessageBox.confirm('发布后不可修改或删除，确定发布吗？', '发布确认', { type: 'warning' }); await publishNoticeApi(row.id); ElMessage.success('发布成功'); await loadList() }
async function removeDraft(row: any) { await ElMessageBox.confirm('确定删除该草稿吗？', '删除确认', { type: 'warning' }); await deleteNoticeApi(row.id); ElMessage.success('已删除'); await loadList() }
async function openDetail(row: any) { const { data } = await getNoticeDetailApi(row.id); detail.value = data; detailVisible.value = true }

void loadList()
</script>

<style scoped>
.detail-meta { display: flex; gap: 12px; align-items: center; color: var(--el-text-color-secondary); font-size: 13px; }
.detail-title { margin: 18px 0 12px; }
.notice-text-content { white-space: pre-wrap; word-break: break-word; font: inherit; line-height: 1.7; max-height: 60vh; overflow: auto; }
.notice-html-frame { width: 100%; height: 55vh; border: 1px solid var(--el-border-color-lighter); border-radius: 6px; background: #fff; }
</style>
