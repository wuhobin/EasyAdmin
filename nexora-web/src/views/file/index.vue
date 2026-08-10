<template>
  <div class="app-container file-page">
    <el-card class="file-card" shadow="never">
      <div class="file-toolbar">
        <div class="file-heading"><h2>文件列表</h2><span class="file-total">共 {{ total }} 个文件</span></div>
        <div class="toolbar-actions">
          <el-button-group class="view-switcher" aria-label="切换文件视图">
            <el-button :type="viewMode === 'table' ? 'primary' : ''" :icon="List" :aria-pressed="viewMode === 'table'"
                       @click="setViewMode('table')"/>
            <el-button :type="viewMode === 'grid' ? 'primary' : ''" :icon="Grid" :aria-pressed="viewMode === 'grid'"
                       @click="setViewMode('grid')"/>
          </el-button-group>
          <el-button v-permission="['sys:file:upload']" type="primary" :icon="UploadFilled" @click="openUploadDialog">
            上传文件
          </el-button>
        </div>
      </div>
      <div class="file-body">
        <aside class="group-panel">
          <div class="group-panel-title"><span>文件分组</span>
            <el-button v-if="canManageGroups" link :icon="Plus" @click="openGroupDialog()">新建</el-button>
          </div>
          <button type="button" class="mobile-group-toggle" :aria-expanded="groupPanelExpanded"
                  @click="groupPanelExpanded = !groupPanelExpanded">
            <span>分组导航</span><span>{{ groupPanelExpanded ? '收起' : '展开' }}</span></button>
          <div class="group-menu-content" :class="{ collapsed: !groupPanelExpanded }">
            <el-alert v-if="isAdmin && !selectedOwnerId" type="info" :closable="false" show-icon
                      title="请选择上传人后查看分组"/>
            <el-menu v-else :default-active="activeGroupKey" @select="selectGroup">
              <el-menu-item index="all">
                <el-icon>
                  <Files/>
                </el-icon>
                <span>全部文件</span></el-menu-item>
              <el-menu-item index="ungrouped">
                <el-icon>
                  <FolderOpened/>
                </el-icon>
                <span>未分组</span><em>{{ groupData.ungroupedCount }}</em></el-menu-item>
              <el-menu-item v-for="group in groupData.groups" :key="group.id" :index="String(group.id)">
                <el-icon>
                  <Folder/>
                </el-icon>
                <span class="group-name" :title="group.name">{{ group.name }}</span><em>{{ group.fileCount }}</em>
                <el-dropdown v-if="canManageGroups" trigger="click"
                             @command="command => handleGroupCommand(command, group)">
                  <el-button link class="group-more" :icon="MoreFilled" aria-label="更多分组操作" @click.stop/>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="rename">重命名</el-dropdown-item>
                      <el-dropdown-item command="delete">删除分组</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </el-menu-item>
            </el-menu>
          </div>
        </aside>

        <main class="file-content">
          <div class="search-wrapper file-filters">
            <el-form ref="queryFormRef" :model="queryParams" :inline="true">
              <el-form-item label="文件名称" prop="fileName">
                <el-input v-model="queryParams.fileName" clearable placeholder="请输入文件名称"
                          @keyup.enter="handleQuery"/>
              </el-form-item>
              <el-form-item label="文件类型" prop="contentType">
                <el-select v-model="queryParams.contentType" clearable filterable placeholder="请选择文件类型"
                           @change="handleQuery">
                  <el-option v-for="item in contentTypeOptions" :key="item.value" :label="item.label"
                             :value="item.value"/>
                </el-select>
              </el-form-item>
              <el-form-item v-if="isAdmin" label="上传人" prop="uploaderId">
                <el-select v-model="queryParams.uploaderId" clearable filterable placeholder="请选择用户"
                           @change="handleOwnerChange">
                  <el-option v-for="user in userOptions" :key="user.id" :label="user.nickname" :value="user.id"/>
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
                <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
              </el-form-item>
            </el-form>
          </div>

          <div v-if="selectedRows.length" class="selection-bar" role="status" aria-live="polite">
            <span>已选 {{ selectedRows.length }} 个文件</span>
            <el-button v-permission="['sys:file:upload']" size="small" @click="openMoveDialog">移动到分组</el-button>
            <el-button v-permission="['sys:file:delete']" size="small" type="danger" @click="handleBatchDelete">
              批量删除
            </el-button>
          </div>

          <el-table v-if="viewMode === 'table'" ref="tableRef" v-loading="loading" :data="fileList" row-key="id"
                    class="file-table" @selection-change="handleSelectionChange">
            <el-table-column type="selection" width="48"/>
            <el-table-column label="文件" min-width="310">
              <template #default="{ row }">
                <div class="file-cell">
                  <button class="thumb-button" type="button" :aria-label="`预览 ${displayName(row)}`"
                          @click="openPreview(row)"><img
                      v-if="isImageFile(row) && (row.thumbnailUrl || row.fileUrl)"
                      :src="row.thumbnailUrl || row.fileUrl" alt=""/>
                    <el-icon v-else>
                      <component :is="fileIcon(row)"/>
                    </el-icon>
                  </button>
                  <div class="file-copy"><strong :title="displayName(row)">{{
                      displayName(row)
                    }}</strong><span>{{ row.groupName || '未分组' }} · {{ formatFileSize(row.fileSize) }}</span></div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="类型" min-width="170" show-overflow-tooltip>
              <template #default="{ row }">
                <el-tag :type="mimeTagType(row)" effect="plain">{{ row.contentType || '-' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="上传时间" prop="createTime" min-width="180" align="center"/>
            <el-table-column label="URL" width="100" align="center">
              <template #default="{ row }">
                <el-button link :icon="Link" aria-label="打开 OSS 地址" @click="openFile(row.fileUrl)"/>
                <el-button link :icon="CopyDocument" aria-label="复制 OSS 地址" @click="copyUrl(row.fileUrl)"/>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right" align="center">
              <template #default="{ row }">
                <el-button v-if="canPreview(row)" link :icon="View" @click="openPreview(row)">预览</el-button>
                <el-button v-permission="['sys:file:download']" link :icon="Download" @click="handleDownload(row)">
                  下载
                </el-button>
                <el-dropdown v-permission="['sys:file:upload']" trigger="click"
                             @command="command => handleFileCommand(command, row)">
                  <el-button link :icon="MoreFilled" aria-label="更多操作"/>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="rename">重命名</el-dropdown-item>
                      <el-dropdown-item command="move">移动分组</el-dropdown-item>
                      <el-dropdown-item v-if="canDeleteFile(row)" command="delete">删除</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </template>
            </el-table-column>
          </el-table>

          <div v-else v-loading="loading" class="file-grid">
            <div v-if="!fileList.length && !loading" class="grid-empty">
              <el-empty description="暂无文件"/>
            </div>
            <article v-for="row in fileList" :key="row.id" class="file-grid-item">
              <button class="grid-thumb" type="button" :aria-label="`预览 ${displayName(row)}`"
                      @click="openPreview(row)"><img
                  v-if="isImageFile(row) && (row.thumbnailUrl || row.fileUrl)" :src="row.thumbnailUrl || row.fileUrl"
                  alt=""/>
                <el-icon v-else>
                  <component :is="fileIcon(row)"/>
                </el-icon>
                <span v-if="canPreview(row)" class="thumb-view"><el-icon><View/></el-icon></span></button>
              <div class="grid-copy"><strong :title="displayName(row)">{{
                  displayName(row)
                }}</strong><span>{{ row.groupName || '未分组' }}</span><small>{{ formatFileSize(row.fileSize) }} ·
                {{ row.createTime || '-' }}</small></div>
              <div class="grid-actions">
                <el-checkbox :model-value="selectedRows.some(item => item.id === row.id)"
                             @change="checked => toggleGridSelection(row, checked)"/>
                <el-button v-permission="['sys:file:download']" link :icon="Download" aria-label="下载文件"
                           @click="handleDownload(row)"/>
                <el-button link :icon="Link" aria-label="打开 OSS 地址" @click="openFile(row.fileUrl)"/>
                <el-button link :icon="CopyDocument" aria-label="复制 OSS 地址" @click="copyUrl(row.fileUrl)"/>
                <el-dropdown v-permission="['sys:file:upload']" trigger="click"
                             @command="command => handleFileCommand(command, row)">
                  <el-button link :icon="MoreFilled" aria-label="更多操作"/>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="rename">重命名</el-dropdown-item>
                      <el-dropdown-item command="move">移动分组</el-dropdown-item>
                      <el-dropdown-item v-if="canDeleteFile(row)" command="delete">删除</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </article>
          </div>

          <div class="pagination-container">
            <el-pagination v-model:current-page="queryParams.pageNum" v-model:page-size="queryParams.pageSize"
                           background :page-sizes="[10, 20, 30, 50]" :total="total"
                           layout="total, sizes, prev, pager, next, jumper" @size-change="handlePageChange"
                           @current-change="handlePageChange"/>
          </div>
        </main>
      </div>
    </el-card>

    <el-dialog v-model="uploadDialogVisible" title="上传文件" width="min(560px, calc(100vw - 32px))" append-to-body
               destroy-on-close :close-on-click-modal="!uploading" :close-on-press-escape="!uploading"
               :show-close="!uploading" @closed="resetUploadState">
      <p class="upload-intro">选择一个文件并确认上传。支持 JPG、JPEG、PNG、GIF、WEBP、MP4、PDF、ZIP、TXT，最大 50MB。</p>
      <el-upload ref="uploadRef" class="upload-dropzone" drag :accept="UPLOAD_ACCEPT" :auto-upload="false"
                 :disabled="uploading" :limit="1" :multiple="false" :show-file-list="false"
                 :on-change="handleUploadFileChange" :on-exceed="handleUploadExceed">
        <div class="upload-dropzone-content">
          <el-icon>
            <UploadFilled/>
          </el-icon>
          <span>{{ selectedUploadFile ? '选择其他文件' : '拖放文件到这里' }}</span></div>
      </el-upload>
      <div v-if="selectedUploadFile" class="selected-file">
        <el-icon>
          <Document/>
        </el-icon>
        <strong>{{ selectedUploadFile.name }}</strong><span>{{ formatFileSize(selectedUploadFile.size) }}</span></div>
      <el-progress v-if="uploadProgressVisible" :percentage="uploadProgress"
                   :status="uploadError ? 'exception' : undefined"/>
      <el-alert v-if="uploadError" type="error" :title="uploadError" :closable="false" show-icon/>
      <template #footer>
        <el-button :disabled="uploading" @click="closeUploadDialog">取消</el-button>
        <el-button type="primary" :loading="uploading" :disabled="!selectedUploadFile || uploading"
                   @click="submitUpload">{{ uploadActionText }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="groupDialogVisible" :title="editingGroup ? '重命名分组' : '新建分组'" width="360px"
               append-to-body>
      <el-input v-model="groupNameInput" maxlength="50" show-word-limit placeholder="请输入分组名称"
                @keyup.enter="submitGroup"/>
      <template #footer>
        <el-button @click="groupDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitGroup">保存</el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="moveDialogVisible" title="移动到分组" width="360px" append-to-body>
      <el-select v-model="moveGroupId" clearable placeholder="请选择分组（空值为未分组）" style="width: 100%">
        <el-option v-for="group in groupData.groups" :key="group.id" :label="group.name" :value="group.id"/>
      </el-select>
      <template #footer>
        <el-button @click="moveDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitMove">移动</el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="renameDialogVisible" title="重命名文件" width="420px" append-to-body>
      <el-input v-model="renameInput" maxlength="255" show-word-limit @keyup.enter="submitRename"/>
      <template #footer>
        <el-button @click="renameDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRename">保存</el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="previewDialogVisible" :title="previewTitle" width="min(900px, calc(100vw - 32px))"
               append-to-body destroy-on-close @closed="handlePreviewClose">
      <div v-if="previewLoading" class="preview-loading">
        <el-icon class="is-loading">
          <Loading/>
        </el-icon>
        正在加载预览
      </div>
      <template v-else-if="previewKind === 'text'">
        <pre class="text-preview">{{ previewText }}</pre>
      </template>
      <img v-else-if="previewKind === 'image'" class="preview-media" :src="previewUrl" alt=""/>
      <video v-else-if="previewKind === 'video'" class="preview-media" :src="previewUrl" controls autoplay/>
      <audio v-else-if="previewKind === 'audio'" class="preview-audio" :src="previewUrl" controls autoplay/>
      <iframe v-else-if="previewKind === 'pdf'" class="preview-pdf" :src="previewUrl" title="PDF 预览"/>
      <el-empty v-else description="该文件仅支持下载或打开 OSS 地址"/>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import type {AxiosProgressEvent} from 'axios'
import {
  CopyDocument,
  Document,
  Download,
  Files,
  Folder,
  FolderOpened,
  Grid,
  Headset,
  Link,
  List,
  Loading,
  MoreFilled,
  Picture,
  Plus,
  Refresh,
  Search,
  UploadFilled,
  VideoPlay,
  View
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
  batchDeleteFilesApi,
  createFileGroupApi,
  deleteFileApi,
  deleteFileGroupApi,
  downloadFileApi,
  getFileGroupsApi,
  getFileListApi,
  moveFilesApi,
  previewFileApi,
  previewTextFileApi,
  renameFileApi,
  renameFileGroupApi,
  uploadApi,
  type FileGroup,
  type FileGroupList,
  type OssFileQuery,
  type OssFileRecord
} from '@/api/file'
import {getUserListApi} from '@/api/system/user'
import {getDictDataListApi, getDictListApi} from '@/api/system/dict'
import {useUserStore} from '@/store/modules/user'
import {getUploadErrorMessage, UPLOAD_ACCEPT, validateUploadFile} from './upload'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.user.roles.includes('admin'))
const currentUserId = computed(() => userStore.user.id)
const canManageGroups = computed(() => !isAdmin.value || Boolean(selectedOwnerId.value))
const queryFormRef = ref<FormInstance>()
const tableRef = ref()
const loading = ref(false)
const total = ref(0)
const fileList = ref<OssFileRecord[]>([])
const selectedRows = ref<OssFileRecord[]>([])
const userOptions = ref<{ id: number; nickname: string }[]>([])
const contentTypeOptions = ref<{ label: string; value: string }[]>([])
const groupData = ref<FileGroupList>({groups: [], ungroupedCount: 0, scopeRequired: false})
const selectedOwnerId = ref<number | undefined>(currentUserId.value || undefined)
const activeGroupKey = ref('all')
const groupPanelExpanded = ref(true)
const viewMode = ref<'table' | 'grid'>((localStorage.getItem('nexora:file:view:' + currentUserId.value) as 'table' | 'grid') || 'table')
const uploadDialogVisible = ref(false)
const uploadRef = ref<UploadInstance>()
const selectedUploadFile = ref<UploadRawFile>()
const uploading = ref(false)
const uploadProgress = ref(0)
const uploadProgressVisible = ref(false)
const uploadError = ref('')
const groupDialogVisible = ref(false)
const editingGroup = ref<FileGroup | null>(null)
const groupNameInput = ref('')
const moveDialogVisible = ref(false)
const moveGroupId = ref<number>()
const renameDialogVisible = ref(false)
const renameInput = ref('')
const renameTarget = ref<OssFileRecord>()
const previewDialogVisible = ref(false)
const previewLoading = ref(false)
const previewKind = ref<'image' | 'video' | 'audio' | 'pdf' | 'text' | 'none'>('none')
const previewUrl = ref('')
const previewText = ref('')
const previewTitle = ref('文件预览')
let previewObjectUrl = ''

const queryParams = reactive<OssFileQuery>({
  pageNum: 1,
  pageSize: 20,
  fileName: '',
  contentType: '',
  uploaderId: selectedOwnerId.value,
  groupId: undefined,
  ungrouped: false
})
const loadUserOptions = async () => {
  if (!isAdmin.value) return;
  try {
    userOptions.value = (await getUserListApi({pageNum: 1, pageSize: 1000})).data.records
  } catch {
  }
}
const loadContentTypeOptions = async () => {
  try {
    const list = await getDictListApi({pageNum: 1, pageSize: 100});
    const dict = list.data.records?.find((item: any) => item.type === 'file_content_type');
    if (dict) {
      const data = await getDictDataListApi({dictId: dict.id, pageNum: 1, pageSize: 100});
      contentTypeOptions.value = data.data.records.map((item: any) => ({label: item.label, value: item.value}))
    }
  } catch {
  }
}
const loadGroups = async () => {
  try {
    groupData.value = (await getFileGroupsApi(isAdmin.value ? selectedOwnerId.value || undefined : undefined)).data
  } catch {
    groupData.value = {groups: [], ungroupedCount: 0, scopeRequired: isAdmin.value && !selectedOwnerId.value}
  }
}
const getList = async () => {
  loading.value = true;
  selectedRows.value = [];
  try {
    const params = {
      ...queryParams,
      uploaderId: isAdmin.value ? selectedOwnerId.value : undefined,
      groupId: activeGroupKey.value === 'all' || activeGroupKey.value === 'ungrouped' ? undefined : Number(activeGroupKey.value),
      ungrouped: activeGroupKey.value === 'ungrouped'
    };
    const {data} = await getFileListApi(params);
    fileList.value = data.records;
    total.value = data.total
  } finally {
    loading.value = false
  }
}
const resetSelection = () => {
  selectedRows.value = [];
  tableRef.value?.clearSelection()
}
const handleQuery = () => {
  queryParams.pageNum = 1;
  resetSelection();
  getList()
}
const resetQuery = async () => {
  queryFormRef.value?.resetFields();
  selectedOwnerId.value = currentUserId.value || undefined;
  queryParams.uploaderId = selectedOwnerId.value;
  queryParams.pageNum = 1;
  activeGroupKey.value = 'all';
  resetSelection();
  await loadGroups();
  await getList()
}
const handleOwnerChange = async () => {
  selectedOwnerId.value = queryParams.uploaderId || currentUserId.value || undefined;
  queryParams.uploaderId = selectedOwnerId.value;
  activeGroupKey.value = 'all';
  queryParams.pageNum = 1;
  resetSelection();
  await loadGroups();
  await getList()
}
const selectGroup = (key: string) => {
  activeGroupKey.value = key;
  queryParams.pageNum = 1;
  resetSelection();
  getList()
}
const handlePageChange = () => {
  resetSelection();
  getList()
}
const setViewMode = (mode: 'table' | 'grid') => {
  viewMode.value = mode;
  localStorage.setItem('nexora:file:view:' + currentUserId.value, mode)
}
const displayName = (row: any) => {
  const file = row;
  return file.originalFilename || file.fileName
}
const normalizedType = (row: any) => (row.contentType || '').split(';')[0].trim().toLowerCase()
const isImageFile = (row: any) => normalizedType(row).startsWith('image/')
const isTextFile = (row: any) => normalizedType(row).startsWith('text/') || /\.(txt|md|csv|log|java|kt|js|ts|vue|html|css|json|xml|yaml|yml|sql)$/i.test(displayName(row))
const canPreview = (row: any) => isImageFile(row) || normalizedType(row).startsWith('video/') || normalizedType(row).startsWith('audio/') || normalizedType(row) === 'application/pdf' || isTextFile(row)
const fileIcon = (row: any) => isImageFile(row) ? Picture : normalizedType(row).startsWith('video/') ? VideoPlay : normalizedType(row).startsWith('audio/') ? Headset : Document
const mimeTagType = (row: any) => {
  const type = normalizedType(row)
  if (type.startsWith('image/')) return 'success'
  if (type.startsWith('video/') || type.startsWith('audio/')) return 'warning'
  if (type === 'application/pdf') return 'danger'
  return 'info'
}
const formatFileSize = (size?: number) => {
  if (!size) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB'];
  const index = Math.min(Math.floor(Math.log(size) / Math.log(1024)), units.length - 1);
  const value = size / Math.pow(1024, index);
  return (value >= 10 || index === 0 ? value.toFixed(0) : value.toFixed(1)) + ' ' + units[index]
}
const openFile = (url: string) => window.open(url, '_blank', 'noopener,noreferrer')
const copyUrl = async (url: string) => {
  await navigator.clipboard.writeText(url);
  ElMessage.success('OSS 地址已复制')
}
const revokePreviewUrl = () => {
  if (previewObjectUrl) URL.revokeObjectURL(previewObjectUrl);
  previewObjectUrl = '';
  previewUrl.value = ''
}
const handlePreviewClose = () => revokePreviewUrl()
const openPreview = async (row: any) => {
  previewTitle.value = displayName(row);
  previewDialogVisible.value = true;
  previewLoading.value = true;
  previewKind.value = 'none';
  previewText.value = '';
  revokePreviewUrl();
  const type = normalizedType(row);
  try {
    if (isTextFile(row)) {
      previewKind.value = 'text';
      previewText.value = (await previewTextFileApi(row.id)).data;
      return
    }
    if (isImageFile(row)) {
      previewKind.value = 'image';
      previewUrl.value = row.fileUrl;
      return
    }
    if (type.startsWith('video/')) previewKind.value = 'video'; else if (type.startsWith('audio/')) previewKind.value = 'audio'; else if (type === 'application/pdf') previewKind.value = 'pdf'; else return;
    const result = await previewFileApi(row.id);
    const blob = result.data;
    previewObjectUrl = URL.createObjectURL(blob instanceof Blob ? blob : new Blob([blob], {type}));
    previewUrl.value = previewObjectUrl
  } catch {
    ElMessage.error('预览加载失败')
  } finally {
    previewLoading.value = false
  }
}
const handleDownload = async (row: any) => {
  const data = await downloadFileApi(row.id);
  const blob = data instanceof Blob ? data : new Blob([data], {type: row.contentType});
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = displayName(row);
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url)
}
const canDeleteFile = (row: any) => {
  return isAdmin.value || row.uploaderId === userStore.user.id
}
const toggleGridSelection = (row: OssFileRecord, checked: boolean | string | number) => {
  const exists = selectedRows.value.some(item => item.id === row.id);
  if (Boolean(checked) && !exists) selectedRows.value.push(row);
  if (!Boolean(checked) && exists) selectedRows.value = selectedRows.value.filter(item => item.id !== row.id)
}
const handleSelectionChange = (rows: OssFileRecord[]) => {
  selectedRows.value = rows
}
const handleBatchDelete = async () => {
  await ElMessageBox.confirm('确定永久删除选中的 ' + selectedRows.value.length + ' 个文件吗？', '删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  });
  await batchDeleteFilesApi({
    fileIds: selectedRows.value.map(row => row.id),
    uploaderId: isAdmin.value ? selectedOwnerId.value : undefined
  });
  ElMessage.success('删除成功');
  await getList();
  await loadGroups()
}
const openMoveDialog = () => {
  moveGroupId.value = undefined;
  moveDialogVisible.value = true
}
const submitMove = async () => {
  await moveFilesApi({
    fileIds: selectedRows.value.map(row => row.id),
    groupId: moveGroupId.value,
    uploaderId: isAdmin.value ? selectedOwnerId.value : undefined
  });
  ElMessage.success('移动成功');
  moveDialogVisible.value = false;
  await getList();
  await loadGroups()
}
const openGroupDialog = (group?: FileGroup) => {
  editingGroup.value = group || null;
  groupNameInput.value = group?.name || '';
  groupDialogVisible.value = true
}
const handleGroupCommand = (command: string, group: FileGroup) => command === 'rename' ? openGroupDialog(group) : handleDeleteGroup(group)
const submitGroup = async () => {
  const name = groupNameInput.value.trim();
  if (!name) {
    ElMessage.warning('分组名称不能为空');
    return
  }
  const ownerId = isAdmin.value ? selectedOwnerId.value : undefined;
  if (editingGroup.value) await renameFileGroupApi(editingGroup.value.id, {
    name,
    ownerId
  }); else await createFileGroupApi({name, ownerId});
  ElMessage.success('保存成功');
  groupDialogVisible.value = false;
  await loadGroups()
}
const handleDeleteGroup = async (group: FileGroup) => {
  await ElMessageBox.confirm('删除“' + group.name + '”后，文件会转为未分组，是否继续？', '删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  });
  await deleteFileGroupApi(group.id, isAdmin.value ? selectedOwnerId.value : undefined);
  if (activeGroupKey.value === String(group.id)) activeGroupKey.value = 'all';
  ElMessage.success('分组已删除');
  await loadGroups();
  await getList()
}
const handleFileCommand = (command: string, row: any) => {
  if (command === 'rename') {
    renameTarget.value = row;
    renameInput.value = displayName(row);
    renameDialogVisible.value = true
  } else if (command === 'move') {
    selectedRows.value = [row];
    openMoveDialog()
  } else if (command === 'delete') handleDelete(row)
}
const submitRename = async () => {
  const row = renameTarget.value;
  const name = renameInput.value.trim();
  if (!row || !name) return;
  const oldExt = displayName(row).match(/\.[^.]+$/)?.[0]?.toLowerCase() || '';
  const newExt = name.match(/\.[^.]+$/)?.[0]?.toLowerCase() || '';
  if (oldExt !== newExt) {
    ElMessage.warning('不能修改文件扩展名');
    return
  }
  await renameFileApi(row.id, name);
  ElMessage.success('重命名成功');
  renameDialogVisible.value = false;
  await getList()
}
const handleDelete = async (row: any) => {
  await ElMessageBox.confirm('确定永久删除“' + displayName(row) + '”吗？', '删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  });
  await deleteFileApi(row.id);
  ElMessage.success('删除成功');
  await getList();
  await loadGroups()
}
const resetUploadState = () => {
  uploadRef.value?.clearFiles();
  selectedUploadFile.value = undefined;
  uploading.value = false;
  uploadProgress.value = 0;
  uploadProgressVisible.value = false;
  uploadError.value = ''
}
const closeUploadDialog = () => {
  if (!uploading.value) uploadDialogVisible.value = false
}
const openUploadDialog = () => {
  resetUploadState();
  uploadDialogVisible.value = true
}
const handleUploadFileChange: UploadProps['onChange'] = file => {
  if (!uploading.value && file.raw) {
    const error = validateUploadFile(file.raw);
    if (error) {
      resetUploadState();
      uploadError.value = error
    } else {
      selectedUploadFile.value = file.raw;
      uploadError.value = ''
    }
  }
}
const handleUploadExceed: UploadProps['onExceed'] = files => {
  const file = files[0] as UploadRawFile;
  file.uid = genFileId();
  uploadRef.value?.clearFiles();
  uploadRef.value?.handleStart(file)
}
const updateUploadProgress = (event: AxiosProgressEvent) => {
  const totalSize = event.total || selectedUploadFile.value?.size;
  const progress = event.progress ?? (totalSize ? event.loaded / totalSize : 0);
  uploadProgress.value = Math.max(uploadProgress.value, Math.round(progress * 100))
}
const uploadGroupId = computed(() => {
  if (activeGroupKey.value === 'all' || activeGroupKey.value === 'ungrouped') return undefined;
  if (isAdmin.value && selectedOwnerId.value !== currentUserId.value) return undefined;
  return Number(activeGroupKey.value)
})
const uploadActionText = computed(() => uploadError.value ? '重新上传' : '开始上传')
const submitUpload = async () => {
  const file = selectedUploadFile.value;
  if (!file) return;
  uploading.value = true;
  uploadProgressVisible.value = true;
  uploadError.value = '';
  try {
    const formData = new FormData();
    formData.append('file', file, file.name);
    if (uploadGroupId.value) formData.append('groupId', String(uploadGroupId.value));
    await uploadApi(formData, updateUploadProgress);
    uploadProgress.value = 100;
    ElMessage.success('上传成功');
    queryParams.pageNum = 1;
    uploadDialogVisible.value = false;
    await getList();
    await loadGroups()
  } catch (error) {
    uploadError.value = getUploadErrorMessage(error)
  } finally {
    uploading.value = false
  }
}

if (isAdmin.value) {
  loadUserOptions()
}
loadContentTypeOptions();
loadGroups();
getList()
</script>

<style scoped>
.file-card {
  min-height: calc(100vh - 120px);
}

.file-toolbar, .group-panel-title, .selection-bar, .file-cell, .grid-actions {
  display: flex;
  align-items: center;
}

.file-toolbar {
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.file-toolbar h2 {
  display: inline;
  margin: 0 10px 0 0;
  font-size: 18px;
}

.file-total, .file-copy span, .grid-copy span, .grid-copy small {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.toolbar-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.file-body {
  display: flex;
  gap: 18px;
  min-width: 0;
}

.group-panel {
  width: 220px;
  flex: none;
  border-right: 1px solid var(--el-border-color-lighter);
  padding-right: 14px;
}

.group-panel-title {
  justify-content: space-between;
  margin: 0 4px 12px;
  font-weight: 600;
}

.mobile-group-toggle {
  display: none;
}

.group-menu-content {
  display: block;
}

.group-menu-content.collapsed {
  display: block;
}

.group-panel :deep(.el-menu) {
  border-right: 0;
}

.group-panel :deep(.el-menu-item) {
  position: relative;
  gap: 5px;
  padding: 0 8px;
}

.group-panel em {
  margin-left: auto;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  font-style: normal;
}

.group-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.group-more {
  margin-left: 4px;
}

.file-content {
  min-width: 0;
  flex: 1;
}

.search-wrapper {
  margin-bottom: 10px;
}

.selection-bar {
  gap: 12px;
  margin-bottom: 10px;
  padding: 8px 12px;
  border-radius: 6px;
  background: var(--el-color-primary-light-9);
}

.selection-bar span {
  margin-right: auto;
  color: var(--el-color-primary);
  font-size: 13px;
}

.file-table :deep(.cell) {
  overflow: visible;
}

.file-cell {
  gap: 10px;
  min-width: 0;
}

.thumb-button, .grid-thumb {
  position: relative;
  display: grid;
  place-items: center;
  overflow: hidden;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-secondary);
  cursor: pointer;
}

.thumb-button {
  width: 42px;
  height: 42px;
  flex: none;
  font-size: 22px;
}

.thumb-button img, .grid-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.file-copy, .grid-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.file-copy strong, .grid-copy strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}

.file-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(210px, 1fr));
  gap: 14px;
  min-height: 240px;
}

.file-grid-item {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  transition: box-shadow .2s ease, transform .2s ease;
}

.file-grid-item:hover {
  transform: translateY(-2px);
  box-shadow: var(--el-box-shadow-light);
}

.grid-thumb {
  width: 100%;
  height: 150px;
  font-size: 46px;
}

.thumb-view {
  position: absolute;
  right: 8px;
  bottom: 8px;
  display: grid;
  width: 24px;
  height: 24px;
  place-items: center;
  border-radius: 50%;
  background: rgb(0 0 0 / 60%);
  color: white;
}

.grid-actions {
  gap: 5px;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

.upload-intro {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.upload-dropzone, .upload-dropzone :deep(.el-upload), .upload-dropzone :deep(.el-upload-dragger) {
  width: 100%;
}

.upload-dropzone :deep(.el-upload-dragger) {
  padding: 28px 20px;
}

.upload-dropzone-content {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
  color: var(--el-color-primary);
}

.selected-file {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 12px;
  padding: 10px 12px;
  background: var(--el-fill-color-light);
  border-radius: 8px;
}

.selected-file strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.selected-file span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.preview-loading {
  display: flex;
  justify-content: center;
  gap: 8px;
  padding: 100px 0;
  color: var(--el-text-color-secondary);
}

.preview-media {
  display: block;
  max-width: 100%;
  max-height: 65vh;
  margin: auto;
  object-fit: contain;
}

.preview-audio {
  display: block;
  width: min(100%, 520px);
  margin: 70px auto;
}

.preview-pdf {
  width: 100%;
  height: 65vh;
  border: 0;
}

.text-preview {
  max-height: 65vh;
  overflow: auto;
  margin: 0;
  padding: 16px;
  border-radius: 8px;
  background: #1e1e1e;
  color: #d4d4d4;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 800px) {
  .file-body {
    display: block;
  }

  .group-panel {
    width: auto;
    margin-bottom: 16px;
    padding: 0 0 12px;
    border-right: 0;
    border-bottom: 1px solid var(--el-border-color-lighter);
  }

  .mobile-group-toggle {
    display: flex;
    width: 100%;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;
    padding: 8px 10px;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 6px;
    background: var(--el-fill-color-light);
    color: var(--el-text-color-regular);
    cursor: pointer;
  }

  .group-menu-content.collapsed {
    display: none;
  }

  .toolbar-actions {
    flex-wrap: wrap;
    justify-content: flex-end;
  }
}

@media (max-width: 560px) {
  .file-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .toolbar-actions {
    justify-content: space-between;
  }

  .pagination-container :deep(.el-pagination) {
    flex-wrap: wrap;
    justify-content: center;
  }
}

/* 文件工作台：内容优先、轻边界、低干扰的视觉层级 */
.file-page {
  width: 100%;
  min-width: 0;
}

.file-card.el-card {
  overflow: hidden;
  min-height: calc(100vh - 120px);
  margin: 0;
  background: var(--nexora-list-surface, var(--el-bg-color));
  border: 1px solid var(--nexora-list-border, var(--el-border-color-lighter));
  border-radius: 14px;
  box-shadow: var(--nexora-list-shadow, 0 8px 24px rgb(15 23 42 / 5%));
}

.file-card :deep(.el-card__body) {
  min-width: 0;
  padding: 0;
}

.file-toolbar {
  min-width: 0;
  padding: 22px 24px 20px;
  margin: 0;
  background: linear-gradient(112deg, color-mix(in srgb, var(--el-color-primary) 5%, var(--el-bg-color)) 0%, var(--el-bg-color) 58%);
  border-bottom: 1px solid var(--nexora-list-divider, var(--el-border-color-lighter));
}

.file-heading {
  display: flex;
  min-width: 0;
  align-items: baseline;
  gap: 10px;
}

.file-toolbar h2 {
  flex: none;
  margin: 0;
  color: var(--el-text-color-primary);
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.file-total {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}

.toolbar-actions {
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.view-switcher :deep(.el-button) {
  width: 36px;
  height: 36px;
  padding: 0;
  border-color: var(--nexora-list-border, var(--el-border-color));
  font-weight: 600;
}

.view-switcher :deep(.el-button:not(.el-button--primary):hover) {
  color: var(--el-color-primary);
  background: color-mix(in srgb, var(--el-color-primary) 6%, var(--el-bg-color));
  border-color: color-mix(in srgb, var(--el-color-primary) 45%, var(--el-border-color));
}

.toolbar-actions > :deep(.el-button--primary) {
  min-height: 36px;
  padding-inline: 16px;
  border-radius: 7px;
  box-shadow: 0 8px 16px -10px color-mix(in srgb, var(--el-color-primary) 80%, transparent);
  font-weight: 650;
}

.file-body {
  gap: 24px;
  padding: 20px 24px 24px;
}

.group-panel {
  width: 216px;
  padding-right: 18px;
  border-right-color: var(--nexora-list-divider, var(--el-border-color-lighter));
}

.group-panel-title {
  min-height: 32px;
  margin: 0 4px 10px;
  color: var(--el-text-color-primary);
  font-size: 13px;
  letter-spacing: 0.01em;
}

.group-panel-title > span {
  font-weight: 700;
}

.group-panel-title :deep(.el-button) {
  min-height: 32px;
  padding: 4px 7px;
  border-radius: 6px;
  font-size: 12px;
}

.group-panel :deep(.el-menu) {
  padding: 4px 0;
  background: transparent;
}

.group-panel :deep(.el-menu-item) {
  min-height: 42px;
  height: 42px;
  padding: 0 10px;
  margin: 3px 0;
  color: var(--el-text-color-secondary);
  border-radius: 8px;
  line-height: 42px;
  transition: color 0.18s ease, background-color 0.18s ease;
}

.group-panel :deep(.el-menu-item:hover) {
  color: var(--el-text-color-primary);
  background: color-mix(in srgb, var(--el-color-primary) 6%, transparent);
}

.group-panel :deep(.el-menu-item.is-active) {
  color: var(--el-color-primary);
  background: color-mix(in srgb, var(--el-color-primary) 10%, transparent);
  font-weight: 650;
}

.group-panel :deep(.el-menu-item .el-icon) {
  margin-right: 7px;
  color: currentColor;
  font-size: 17px;
}

.group-panel em {
  padding: 0 6px;
  border-radius: 999px;
  background: var(--el-fill-color-light);
  font-variant-numeric: tabular-nums;
}

.group-panel :deep(.el-menu-item.is-active) em {
  color: var(--el-color-primary);
  background: color-mix(in srgb, var(--el-color-primary) 12%, transparent);
}

.group-more {
  min-width: 30px;
  min-height: 30px;
  padding: 5px;
  border-radius: 6px;
}

.group-more:hover {
  color: var(--el-color-primary);
  background: color-mix(in srgb, var(--el-color-primary) 9%, transparent);
}

.file-filters {
  padding: 16px 16px 4px;
  margin-bottom: 14px;
  background: var(--nexora-list-header, var(--el-fill-color-light));
  border: 1px solid var(--nexora-list-divider, var(--el-border-color-lighter));
  border-radius: 10px;
}

.file-filters :deep(.el-form) {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 10px 14px;
}

.file-filters :deep(.el-form-item) {
  margin: 0 0 12px;
}

.file-filters :deep(.el-form-item__label) {
  color: var(--el-text-color-regular);
  font-size: 12px;
  font-weight: 600;
}

.file-filters :deep(.el-input),
.file-filters :deep(.el-select) {
  width: 188px;
}

.file-filters :deep(.el-input__wrapper),
.file-filters :deep(.el-select__wrapper) {
  min-height: 36px;
  padding-inline: 11px;
  background: var(--el-bg-color);
  border-radius: 7px;
  box-shadow: inset 0 0 0 1px var(--el-border-color-light);
  transition: box-shadow 0.18s ease, background-color 0.18s ease;
}

.file-filters :deep(.el-input__wrapper:hover),
.file-filters :deep(.el-select__wrapper:hover) {
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--el-color-primary) 48%, var(--el-border-color));
}

.file-filters :deep(.el-input__wrapper.is-focus),
.file-filters :deep(.el-select__wrapper.is-focused) {
  box-shadow: inset 0 0 0 1px var(--el-color-primary), 0 0 0 3px color-mix(in srgb, var(--el-color-primary) 13%, transparent);
}

.file-filters :deep(.el-form-item:last-child) {
  margin-left: auto;
}

.file-filters :deep(.el-button) {
  min-height: 36px;
  border-radius: 7px;
  font-weight: 600;
}

.selection-bar {
  min-height: 44px;
  gap: 10px;
  padding: 6px 10px 6px 14px;
  margin-bottom: 12px;
  background: color-mix(in srgb, var(--el-color-primary) 8%, var(--el-bg-color));
  border: 1px solid color-mix(in srgb, var(--el-color-primary) 22%, var(--el-border-color-lighter));
  border-radius: 9px;
}

.selection-bar span {
  color: var(--el-text-color-primary);
  font-size: 13px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.selection-bar :deep(.el-button) {
  min-height: 32px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
}

.file-table {
  overflow: hidden;
  --el-table-header-bg-color: var(--nexora-list-header, var(--el-fill-color-light));
  --el-table-row-hover-bg-color: var(--nexora-list-hover, color-mix(in srgb, var(--el-color-primary) 5%, var(--el-bg-color)));
  --el-table-border-color: var(--nexora-list-divider, var(--el-border-color-lighter));
  border: 1px solid var(--nexora-list-divider, var(--el-border-color-lighter));
  border-radius: 10px;
}

.file-table :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.file-table :deep(th.el-table__cell) {
  height: 44px;
  padding: 0;
  color: var(--el-text-color-secondary);
  background: var(--nexora-list-header, var(--el-fill-color-light));
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.file-table :deep(td.el-table__cell) {
  height: 66px;
  padding: 0;
  border-bottom-color: var(--nexora-list-divider, var(--el-border-color-lighter));
}

.file-table :deep(.el-table__row:last-child td.el-table__cell) {
  border-bottom: 0;
}

.file-table :deep(.el-table__body tr:hover > td.el-table__cell) {
  background: var(--nexora-list-hover, color-mix(in srgb, var(--el-color-primary) 5%, var(--el-bg-color)));
}

.file-table :deep(.el-table__empty-block) {
  min-height: 190px;
}

.file-table :deep(.el-tag) {
  max-width: 145px;
  height: 24px;
  padding: 0 9px;
  overflow: hidden;
  border-radius: 999px;
  font-size: 11px !important;
  line-height: 22px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-table :deep(.el-button.is-link),
.grid-actions :deep(.el-button.is-link) {
  min-width: 32px;
  min-height: 32px;
  padding: 5px 6px;
  border-radius: 6px;
  font-weight: 600;
  transition: color 0.18s ease, background-color 0.18s ease;
}

.file-table :deep(.el-button.is-link:hover),
.grid-actions :deep(.el-button.is-link:hover) {
  background: color-mix(in srgb, var(--el-color-primary) 9%, transparent);
}

.file-cell {
  gap: 12px;
}

.thumb-button,
.grid-thumb {
  border-color: color-mix(in srgb, var(--el-color-primary) 18%, var(--el-border-color-lighter));
  background: color-mix(in srgb, var(--el-color-primary) 5%, var(--el-fill-color-light));
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.thumb-button:hover,
.grid-thumb:hover {
  border-color: color-mix(in srgb, var(--el-color-primary) 45%, var(--el-border-color));
  box-shadow: 0 5px 12px -8px color-mix(in srgb, var(--el-color-primary) 65%, transparent);
}

.thumb-button:focus-visible,
.grid-thumb:focus-visible,
.group-more:focus-visible,
.mobile-group-toggle:focus-visible {
  outline: 2px solid var(--el-color-primary);
  outline-offset: 2px;
}

.file-copy strong,
.grid-copy strong {
  color: var(--el-text-color-primary);
  font-weight: 650;
  line-height: 20px;
}

.file-copy span,
.grid-copy span,
.grid-copy small {
  line-height: 18px;
}

.file-grid {
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
  min-height: 250px;
}

.grid-empty {
  display: grid;
  min-height: 250px;
  place-items: center;
  grid-column: 1 / -1;
  border: 1px dashed var(--nexora-list-divider, var(--el-border-color-lighter));
  border-radius: 10px;
  background: var(--nexora-list-header, var(--el-fill-color-light));
}

.file-grid-item {
  min-width: 0;
  padding: 14px;
  background: var(--nexora-list-surface, var(--el-bg-color));
  border-color: var(--nexora-list-divider, var(--el-border-color-lighter));
  border-radius: 12px;
  box-shadow: 0 1px 2px rgb(15 23 42 / 3%);
}

.file-grid-item:hover {
  transform: translateY(-2px);
  border-color: color-mix(in srgb, var(--el-color-primary) 30%, var(--el-border-color-lighter));
  box-shadow: 0 12px 24px -18px color-mix(in srgb, var(--el-color-primary) 42%, transparent);
}

.grid-thumb {
  height: 148px;
  border-radius: 9px;
  font-size: 42px;
}

.grid-thumb:active,
.thumb-button:active {
  transform: scale(0.98);
}

.thumb-view {
  width: 28px;
  height: 28px;
  background: rgb(15 23 42 / 68%);
  box-shadow: 0 2px 8px rgb(15 23 42 / 20%);
}

.grid-copy {
  gap: 5px;
  padding: 0 2px;
}

.grid-copy span {
  color: var(--el-color-primary);
  font-weight: 550;
}

.grid-actions {
  min-height: 34px;
  gap: 3px;
  padding-top: 8px;
  border-top: 1px solid var(--nexora-list-divider, var(--el-border-color-lighter));
}

.grid-actions :deep(.el-checkbox) {
  min-height: 32px;
  margin-right: auto;
}

.pagination-container {
  justify-content: flex-end;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--nexora-list-divider, var(--el-border-color-lighter));
}

.pagination-container :deep(.el-pagination) {
  gap: 4px;
  font-variant-numeric: tabular-nums;
}

.pagination-container :deep(.el-pagination button),
.pagination-container :deep(.el-pager li) {
  min-width: 32px;
  height: 32px;
  border-radius: 6px;
  line-height: 32px;
}

.upload-intro {
  margin: -2px 0 16px;
  line-height: 1.65;
}

.upload-dropzone :deep(.el-upload-dragger) {
  padding: 30px 20px;
  background: var(--el-fill-color-light);
  border: 1px dashed color-mix(in srgb, var(--el-color-primary) 35%, var(--el-border-color));
  border-radius: 10px;
  transition: background-color 0.18s ease, border-color 0.18s ease;
}

.upload-dropzone :deep(.el-upload-dragger:hover) {
  background: color-mix(in srgb, var(--el-color-primary) 6%, var(--el-bg-color));
  border-color: var(--el-color-primary);
}

.upload-dropzone-content {
  flex-direction: column;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
}

.upload-dropzone-content .el-icon {
  font-size: 28px;
}

.selected-file {
  margin-top: 14px;
  border: 1px solid var(--nexora-list-divider, var(--el-border-color-lighter));
  background: var(--el-fill-color-light);
}

.selected-file strong {
  min-width: 0;
  font-weight: 600;
}

.preview-loading {
  min-height: 220px;
  align-items: center;
}

.preview-media {
  border-radius: 8px;
}

@media (max-width: 900px) {
  .file-body {
    gap: 18px;
    padding: 18px;
  }

  .file-filters :deep(.el-form-item) {
    min-width: min(240px, 100%);
    flex: 1 1 calc(50% - 7px);
  }

  .file-filters :deep(.el-input),
  .file-filters :deep(.el-select) {
    width: 100%;
  }

  .file-filters :deep(.el-form-item:last-child) {
    margin-left: 0;
    flex-basis: 100%;
  }
}

@media (max-width: 800px) {
  .file-toolbar {
    align-items: stretch;
    padding: 18px;
  }

  .toolbar-actions {
    justify-content: space-between;
  }

  .group-panel {
    margin-bottom: 0;
  }

  .file-filters {
    padding: 14px 14px 2px;
  }
}

@media (max-width: 560px) {
  .file-card.el-card {
    border-radius: 10px;
  }

  .file-toolbar,
  .file-body {
    padding-inline: 14px;
  }

  .file-heading {
    align-items: flex-start;
    flex-direction: column;
    gap: 3px;
  }

  .toolbar-actions {
    align-items: stretch;
  }

  .view-switcher,
  .toolbar-actions > :deep(.el-button--primary) {
    flex: 1;
  }

  .view-switcher :deep(.el-button) {
    min-width: 0;
    flex: 1;
  }

  .toolbar-actions > :deep(.el-button--primary) {
    justify-content: center;
  }

  .file-filters :deep(.el-form-item) {
    min-width: 100%;
    flex-basis: 100%;
  }

  .selection-bar {
    align-items: stretch;
    flex-wrap: wrap;
  }

  .selection-bar span {
    width: 100%;
    margin-right: 0;
  }

  .selection-bar :deep(.el-button) {
    flex: 1;
  }

  .file-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
  }

  .file-grid-item {
    padding: 10px;
    border-radius: 10px;
  }

  .grid-thumb {
    height: 112px;
  }

  .grid-actions :deep(.el-button.is-link) {
    min-width: 28px;
    padding-inline: 3px;
  }

  .pagination-container {
    justify-content: flex-start;
    overflow-x: auto;
  }

  .pagination-container :deep(.el-pagination) {
    flex-wrap: nowrap;
  }
}

@media (max-width: 360px) {
  .file-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .grid-thumb {
    height: 150px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .file-grid-item,
  .thumb-button,
  .grid-thumb,
  .file-filters :deep(.el-input__wrapper),
  .file-filters :deep(.el-select__wrapper),
  .upload-dropzone :deep(.el-upload-dragger) {
    transition: none;
  }
}
</style>
