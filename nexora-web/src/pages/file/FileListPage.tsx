import {
  AudioOutlined,
  CopyOutlined,
  DeleteOutlined,
  DownloadOutlined,
  EditOutlined,
  EyeOutlined,
  FileImageOutlined,
  FileOutlined,
  FilePdfOutlined,
  FileTextOutlined,
  FileZipOutlined,
  FolderOpenOutlined,
  FolderOutlined,
  LinkOutlined,
  MoreOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
  UnorderedListOutlined,
  UndoOutlined,
  UploadOutlined,
  VideoCameraOutlined
} from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import AntApp from 'antd/es/app'
import Empty from 'antd/es/empty'
import Progress from 'antd/es/progress'
import Spin from 'antd/es/spin'
import Table from 'antd/es/table'
import Tag from 'antd/es/tag'
import type { ColumnsType } from 'antd/es/table'
import { useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { useForm } from 'react-hook-form'
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
  uploadFileApi,
  type FileGroup,
  type OssFileQuery,
  type OssFileRecord
} from '@/api/file'
import { getDictDataListApi, getDictListApi } from '@/api/dict'
import { getUserListApi } from '@/api/user'
import { ManagementPagination, ManagementRowAction } from '@/components/management/ManagementUi'
import { Attachment, AttachmentAction, AttachmentActions, AttachmentContent, AttachmentDescription, AttachmentMedia, AttachmentTitle, AttachmentTrigger } from '@/components/ui/attachment'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import { Dialog, DialogClose, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form'
import { Input } from '@/components/ui/input'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import {
  displayFileName,
  filePreviewKind,
  formatFileSize,
  getUploadErrorMessage,
  normalizedFileType,
  UPLOAD_ACCEPT,
  validateRenamedFile,
  validateUploadFile,
  type FilePreviewKind
} from '@/pages/file/fileUtils'
import { useAuthStore } from '@/store/authStore'

interface FileFilterValues {
  fileName: string
  contentType: string
  uploaderId: string
}

interface PreviewState {
  file: OssFileRecord
  kind: FilePreviewKind
  loading: boolean
  url: string
  text: string
}

const emptyGroups = { groups: [], ungroupedCount: 0, scopeRequired: false }

function fileTypeIcon(file: OssFileRecord): ReactNode {
  const type = normalizedFileType(file)
  if (type.startsWith('image/')) return <FileImageOutlined />
  if (type.startsWith('video/')) return <VideoCameraOutlined />
  if (type.startsWith('audio/')) return <AudioOutlined />
  if (type === 'application/pdf') return <FilePdfOutlined />
  if (type.includes('zip') || /\.zip$/i.test(displayFileName(file))) return <FileZipOutlined />
  if (filePreviewKind(file) === 'text') return <FileTextOutlined />
  return <FileOutlined />
}

function mimeColor(file: OssFileRecord) {
  const type = normalizedFileType(file)
  if (type.startsWith('image/')) return 'green'
  if (type.startsWith('video/') || type.startsWith('audio/')) return 'gold'
  if (type === 'application/pdf') return 'red'
  if (type.includes('zip')) return 'purple'
  return undefined
}

function FileThumbnail({ file, onClick }: { file: OssFileRecord; onClick: () => void }) {
  const imageUrl = file.thumbnailUrl || file.fileUrl
  return (
    <button type="button" className="file-list-thumb" aria-label={`预览 ${displayFileName(file)}`} onClick={onClick}>
      {filePreviewKind(file) === 'image' && imageUrl ? <img src={imageUrl} alt="" /> : fileTypeIcon(file)}
    </button>
  )
}

function FileActionMenu({ file, canDelete, onRename, onMove, onDelete, attachment = false }: { file: OssFileRecord; canDelete: boolean; onRename: () => void; onMove: () => void; onDelete: () => void; attachment?: boolean }) {
  const [open, setOpen] = useState(false)
  const run = (action: () => void) => { setOpen(false); action() }
  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>{attachment ? <AttachmentAction className="file-attachment-action settings" aria-label={`${displayFileName(file)}更多操作`}><MoreOutlined /></AttachmentAction> : <Button type="button" variant="ghost" size="icon" className="management-row-action management-row-action-settings" aria-label={`${displayFileName(file)}更多操作`}><MoreOutlined /></Button>}</PopoverTrigger>
      <PopoverContent align="end" className="file-action-menu">
        <button type="button" onClick={() => run(onRename)}><EditOutlined />重命名</button>
        <button type="button" onClick={() => run(onMove)}><FolderOpenOutlined />移动分组</button>
        {canDelete ? <button type="button" className="is-danger" onClick={() => run(onDelete)}><DeleteOutlined />删除文件</button> : null}
      </PopoverContent>
    </Popover>
  )
}

function FileAttachment({ file, selected, canDownload, canUpload, canDelete, onSelectedChange, onPreview, onDownload, onCopy, onRename, onMove, onDelete }: { file: OssFileRecord; selected: boolean; canDownload: boolean; canUpload: boolean; canDelete: boolean; onSelectedChange: (checked: boolean) => void; onPreview: () => void; onDownload: () => void; onCopy: () => void; onRename: () => void; onMove: () => void; onDelete: () => void }) {
  const previewKind = filePreviewKind(file)
  const imageUrl = file.thumbnailUrl || file.fileUrl
  const isImage = previewKind === 'image' && Boolean(imageUrl)
  return (
    <Attachment orientation="vertical" state="done" className={selected ? 'file-attachment selected' : 'file-attachment'}>
      <AttachmentMedia variant={isImage ? 'image' : 'icon'} className="file-attachment-media">
        {isImage ? <img src={imageUrl} alt="" /> : fileTypeIcon(file)}
        {previewKind !== 'none' ? <span className="file-attachment-preview-mark"><EyeOutlined /></span> : null}
      </AttachmentMedia>
      <AttachmentContent>
        <AttachmentTitle title={displayFileName(file)}>{displayFileName(file)}</AttachmentTitle>
        <AttachmentDescription className="file-attachment-group">{file.groupName || '未分组'}</AttachmentDescription>
        <AttachmentDescription>{formatFileSize(file.fileSize)} · {file.createTime || '-'}</AttachmentDescription>
      </AttachmentContent>
      <AttachmentActions>
        <Checkbox checked={selected} aria-label={`选择 ${displayFileName(file)}`} onCheckedChange={checked => onSelectedChange(Boolean(checked))} />
        {canDownload ? <AttachmentAction className="file-attachment-action download" aria-label={`下载${displayFileName(file)}`} onClick={onDownload}><DownloadOutlined /></AttachmentAction> : null}
        <AttachmentAction className="file-attachment-action link" aria-label={`打开${displayFileName(file)}的 OSS 地址`} disabled={!file.fileUrl} onClick={() => window.open(file.fileUrl, '_blank', 'noopener,noreferrer')}><LinkOutlined /></AttachmentAction>
        <AttachmentAction className="file-attachment-action copy" aria-label={`复制${displayFileName(file)}的 OSS 地址`} disabled={!file.fileUrl} onClick={onCopy}><CopyOutlined /></AttachmentAction>
        {canUpload ? <FileActionMenu file={file} canDelete={canDelete} attachment onRename={onRename} onMove={onMove} onDelete={onDelete} /> : null}
      </AttachmentActions>
      {previewKind !== 'none' ? <AttachmentTrigger aria-label={`预览 ${displayFileName(file)}`} onClick={onPreview} /> : null}
    </Attachment>
  )
}

function GroupActionMenu({ group, onRename, onDelete }: { group: FileGroup; onRename: () => void; onDelete: () => void }) {
  const [open, setOpen] = useState(false)
  const run = (action: () => void) => { setOpen(false); action() }
  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild><Button type="button" variant="ghost" size="icon" className="file-group-more" aria-label={`${group.name}分组操作`}><MoreOutlined /></Button></PopoverTrigger>
      <PopoverContent align="end" className="file-action-menu">
        <button type="button" onClick={() => run(onRename)}><EditOutlined />重命名</button>
        <button type="button" className="is-danger" onClick={() => run(onDelete)}><DeleteOutlined />删除分组</button>
      </PopoverContent>
    </Popover>
  )
}

function saveBlob(blob: Blob, fileName: string) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

async function copyText(value: string) {
  if (navigator.clipboard?.writeText) return navigator.clipboard.writeText(value)
  const input = document.createElement('textarea')
  input.value = value
  input.style.position = 'fixed'
  input.style.opacity = '0'
  document.body.appendChild(input)
  input.select()
  document.execCommand('copy')
  input.remove()
}

export function FileListPage() {
  const { message, modal } = AntApp.useApp()
  const queryClient = useQueryClient()
  const user = useAuthStore(state => state.user)
  const isAdmin = user.roles.includes('admin')
  const canUpload = user.permissions.includes('sys:file:upload')
  const canDelete = user.permissions.includes('sys:file:delete')
  const canDownload = user.permissions.includes('sys:file:download')
  const defaultOwnerId = user.id ? String(user.id) : ''
  const [queryParams, setQueryParams] = useState<OssFileQuery>({ pageNum: 1, pageSize: 20, uploaderId: isAdmin ? user.id ?? undefined : undefined })
  const [selectedOwnerId, setSelectedOwnerId] = useState<number | undefined>(user.id ?? undefined)
  const [activeGroup, setActiveGroup] = useState('all')
  const [selectedIds, setSelectedIds] = useState<number[]>([])
  const [viewMode, setViewMode] = useState<'table' | 'grid'>(() => localStorage.getItem(`nexora:file:view:${user.id}`) === 'grid' ? 'grid' : 'table')
  const [groupPanelExpanded, setGroupPanelExpanded] = useState(true)
  const [uploadOpen, setUploadOpen] = useState(false)
  const [uploadFile, setUploadFile] = useState<File>()
  const [uploading, setUploading] = useState(false)
  const [uploadProgress, setUploadProgress] = useState(0)
  const [uploadError, setUploadError] = useState('')
  const [dragging, setDragging] = useState(false)
  const [groupEditor, setGroupEditor] = useState<{ group?: FileGroup; name: string }>()
  const [moveFiles, setMoveFiles] = useState<OssFileRecord[]>()
  const [moveGroupId, setMoveGroupId] = useState('ungrouped')
  const [renameTarget, setRenameTarget] = useState<OssFileRecord>()
  const [renameValue, setRenameValue] = useState('')
  const [preview, setPreview] = useState<PreviewState>()
  const fileInputRef = useRef<HTMLInputElement>(null)
  const previewObjectUrl = useRef('')
  const previewRequest = useRef(0)
  const filterForm = useForm<FileFilterValues>({ defaultValues: { fileName: '', contentType: 'all', uploaderId: defaultOwnerId } })

  const filesQuery = useQuery({
    queryKey: ['files', queryParams],
    queryFn: async () => (await getFileListApi(queryParams)).data
  })
  const groupsQuery = useQuery({
    queryKey: ['file-groups', isAdmin ? selectedOwnerId : 'self'],
    queryFn: async () => (await getFileGroupsApi(isAdmin ? selectedOwnerId : undefined)).data,
    enabled: !isAdmin || Boolean(selectedOwnerId)
  })
  const usersQuery = useQuery({
    queryKey: ['file-user-options'],
    queryFn: async () => (await getUserListApi({ pageNum: 1, pageSize: 1000 })).data.records,
    enabled: isAdmin
  })
  const contentTypesQuery = useQuery({
    queryKey: ['file-content-types'],
    queryFn: async () => {
      const dictionaries = (await getDictListApi({ pageNum: 1, pageSize: 100 })).data.records
      const dictionary = dictionaries.find(item => item.type === 'file_content_type')
      if (!dictionary) return []
      return (await getDictDataListApi({ dictId: dictionary.id, pageNum: 1, pageSize: 100 })).data.records
        .filter(item => Boolean(item.value && item.label))
        .map(item => ({ label: item.label, value: item.value }))
    }
  })
  const groupData = groupsQuery.data ?? { ...emptyGroups, scopeRequired: isAdmin && !selectedOwnerId }
  const files = filesQuery.data?.records ?? []
  const selectedFiles = files.filter(file => selectedIds.includes(file.id))
  const canManageGroups = canUpload && (!isAdmin || Boolean(selectedOwnerId))

  useEffect(() => setSelectedIds([]), [queryParams])
  useEffect(() => () => { if (previewObjectUrl.current) URL.revokeObjectURL(previewObjectUrl.current) }, [])

  const refreshFiles = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['files'] }),
      queryClient.invalidateQueries({ queryKey: ['file-groups'] })
    ])
  }

  const deleteMutation = useMutation({
    mutationFn: deleteFileApi,
    onSuccess: async () => { await refreshFiles(); message.success('文件已删除') },
    onError: error => message.error(error instanceof Error ? error.message : '文件删除失败')
  })
  const batchDeleteMutation = useMutation({
    mutationFn: (records: OssFileRecord[]) => batchDeleteFilesApi({ fileIds: records.map(file => file.id), uploaderId: isAdmin ? selectedOwnerId : undefined }),
    onSuccess: async () => { setSelectedIds([]); await refreshFiles(); message.success('选中的文件已删除') },
    onError: error => message.error(error instanceof Error ? error.message : '批量删除失败')
  })
  const moveMutation = useMutation({
    mutationFn: (records: OssFileRecord[]) => moveFilesApi({ fileIds: records.map(file => file.id), groupId: moveGroupId === 'ungrouped' ? undefined : Number(moveGroupId), uploaderId: isAdmin ? selectedOwnerId : undefined }),
    onSuccess: async () => { setMoveFiles(undefined); setSelectedIds([]); await refreshFiles(); message.success('文件已移动') },
    onError: error => message.error(error instanceof Error ? error.message : '移动失败')
  })
  const renameMutation = useMutation({
    mutationFn: ({ id, name }: { id: number; name: string }) => renameFileApi(id, name),
    onSuccess: async () => { setRenameTarget(undefined); await refreshFiles(); message.success('文件已重命名') },
    onError: error => message.error(error instanceof Error ? error.message : '重命名失败')
  })
  const groupMutation = useMutation({
    mutationFn: async (editor: { group?: FileGroup; name: string }) => {
      const data = { name: editor.name.trim(), ownerId: isAdmin ? selectedOwnerId : undefined }
      return editor.group ? renameFileGroupApi(editor.group.id, data) : createFileGroupApi(data)
    },
    onSuccess: async () => { setGroupEditor(undefined); await queryClient.invalidateQueries({ queryKey: ['file-groups'] }); message.success('文件分组已保存') },
    onError: error => message.error(error instanceof Error ? error.message : '文件分组保存失败')
  })
  const groupDeleteMutation = useMutation({
    mutationFn: (group: FileGroup) => deleteFileGroupApi(group.id, isAdmin ? selectedOwnerId : undefined),
    onSuccess: async (_, group) => { if (activeGroup === String(group.id)) selectGroup('all'); await refreshFiles(); message.success('文件分组已删除') },
    onError: error => message.error(error instanceof Error ? error.message : '文件分组删除失败')
  })

  const applyFilters = (values: FileFilterValues) => {
    const ownerId = isAdmin && values.uploaderId ? Number(values.uploaderId) : undefined
    const ownerChanged = ownerId !== selectedOwnerId
    if (ownerChanged) { setSelectedOwnerId(ownerId); setActiveGroup('all') }
    setQueryParams(previous => ({
      pageNum: 1,
      pageSize: previous.pageSize,
      fileName: values.fileName.trim() || undefined,
      contentType: values.contentType === 'all' ? undefined : values.contentType,
      uploaderId: ownerId,
      groupId: ownerChanged || activeGroup === 'all' || activeGroup === 'ungrouped' ? undefined : Number(activeGroup),
      ungrouped: !ownerChanged && activeGroup === 'ungrouped' ? true : undefined
    }))
  }

  const resetFilters = () => {
    filterForm.reset({ fileName: '', contentType: 'all', uploaderId: defaultOwnerId })
    setSelectedOwnerId(user.id ?? undefined)
    setActiveGroup('all')
    setQueryParams(previous => ({ pageNum: 1, pageSize: previous.pageSize, uploaderId: isAdmin ? user.id ?? undefined : undefined }))
  }

  function selectGroup(key: string) {
    setActiveGroup(key)
    setQueryParams(previous => ({
      ...previous,
      pageNum: 1,
      groupId: key === 'all' || key === 'ungrouped' ? undefined : Number(key),
      ungrouped: key === 'ungrouped' ? true : undefined
    }))
  }

  const setFileView = (mode: 'table' | 'grid') => {
    setViewMode(mode)
    localStorage.setItem(`nexora:file:view:${user.id}`, mode)
  }

  const confirmDelete = (file: OssFileRecord) => modal.confirm({
    title: `永久删除“${displayFileName(file)}”？`,
    content: '删除后无法恢复。',
    okText: '删除',
    cancelText: '取消',
    okButtonProps: { danger: true },
    onOk: () => deleteMutation.mutateAsync(file.id)
  })

  const confirmBatchDelete = () => modal.confirm({
    title: `永久删除选中的 ${selectedFiles.length} 个文件？`,
    content: '此操作不可撤销。',
    okText: '批量删除',
    cancelText: '取消',
    okButtonProps: { danger: true },
    onOk: () => batchDeleteMutation.mutateAsync(selectedFiles)
  })

  const confirmDeleteGroup = (group: FileGroup) => modal.confirm({
    title: `删除分组“${group.name}”？`,
    content: '分组中的文件不会删除，将转为未分组。',
    okText: '删除分组',
    cancelText: '取消',
    okButtonProps: { danger: true },
    onOk: () => groupDeleteMutation.mutateAsync(group)
  })

  const openMove = (records: OssFileRecord[]) => { setMoveGroupId('ungrouped'); setMoveFiles(records) }
  const openRename = (file: OssFileRecord) => { setRenameTarget(file); setRenameValue(displayFileName(file)) }
  const submitRename = () => {
    if (!renameTarget) return
    const error = validateRenamedFile(displayFileName(renameTarget), renameValue)
    if (error) { message.warning(error); return }
    renameMutation.mutate({ id: renameTarget.id, name: renameValue.trim() })
  }

  const closePreview = () => {
    previewRequest.current += 1
    if (previewObjectUrl.current) URL.revokeObjectURL(previewObjectUrl.current)
    previewObjectUrl.current = ''
    setPreview(undefined)
  }

  const openPreview = async (file: OssFileRecord) => {
    const requestId = ++previewRequest.current
    if (previewObjectUrl.current) URL.revokeObjectURL(previewObjectUrl.current)
    previewObjectUrl.current = ''
    const kind = filePreviewKind(file)
    setPreview({ file, kind, loading: kind !== 'image' && kind !== 'none', url: kind === 'image' ? file.fileUrl : '', text: '' })
    if (kind === 'image' || kind === 'none') return
    try {
      if (kind === 'text') {
        const result = await previewTextFileApi(file.id)
        if (previewRequest.current === requestId) setPreview({ file, kind, loading: false, url: '', text: result.data })
        return
      }
      const blob = await previewFileApi(file.id)
      if (previewRequest.current !== requestId) return
      previewObjectUrl.current = URL.createObjectURL(blob instanceof Blob ? blob : new Blob([blob], { type: file.contentType }))
      setPreview({ file, kind, loading: false, url: previewObjectUrl.current, text: '' })
    } catch (error) {
      if (previewRequest.current === requestId) {
        setPreview(current => current ? { ...current, loading: false, kind: 'none' } : current)
        message.error(error instanceof Error ? error.message : '预览加载失败')
      }
    }
  }

  const downloadFile = async (file: OssFileRecord) => {
    try { saveBlob(await downloadFileApi(file.id), displayFileName(file)) }
    catch (error) { message.error(error instanceof Error ? error.message : '文件下载失败') }
  }

  const chooseUploadFile = (file?: File) => {
    if (!file) return
    const error = validateUploadFile(file)
    setUploadError(error || '')
    setUploadFile(error ? undefined : file)
    setUploadProgress(0)
  }

  const resetUpload = () => {
    setUploadFile(undefined)
    setUploadProgress(0)
    setUploadError('')
    setDragging(false)
    if (fileInputRef.current) fileInputRef.current.value = ''
  }

  const submitUpload = async () => {
    if (!uploadFile) return
    setUploading(true)
    setUploadError('')
    try {
      const data = new FormData()
      data.append('file', uploadFile, uploadFile.name)
      if (activeGroup !== 'all' && activeGroup !== 'ungrouped' && (!isAdmin || selectedOwnerId === user.id)) data.append('groupId', activeGroup)
      await uploadFileApi(data, event => {
        const total = event.total || uploadFile.size
        const progress = event.progress ?? (total ? event.loaded / total : 0)
        setUploadProgress(previous => Math.max(previous, Math.round(progress * 100)))
      })
      setUploadProgress(100)
      setUploadOpen(false)
      resetUpload()
      await refreshFiles()
      message.success('文件上传成功')
    } catch (error) {
      setUploadError(getUploadErrorMessage(error))
    } finally {
      setUploading(false)
    }
  }

  const columns = useMemo<ColumnsType<OssFileRecord>>(() => [
    {
      title: '文件', key: 'file', width: 240,
      render: (_, file) => <div className="file-name-cell"><FileThumbnail file={file} onClick={() => void openPreview(file)} /><span><span title={displayFileName(file)}>{displayFileName(file)}</span><small>{file.groupName || '未分组'} · {formatFileSize(file.fileSize)}</small></span></div>
    },
    { title: '类型', dataIndex: 'contentType', width: 112, ellipsis: true, render: (_, file) => <Tag color={mimeColor(file)}>{file.contentType || '未知类型'}</Tag> },
    { title: '上传时间', dataIndex: 'createTime', width: 146, align: 'center', render: value => value ? <span className="file-upload-time">{value}</span> : <span className="management-empty-value">-</span> },
    {
      title: 'URL', key: 'url', width: 82, align: 'center', responsive: ['xxl'],
      render: (_, file) => <div className="file-inline-actions"><Button type="button" variant="ghost" size="icon" aria-label={`打开 ${displayFileName(file)} 的 OSS 地址`} disabled={!file.fileUrl} onClick={() => window.open(file.fileUrl, '_blank', 'noopener,noreferrer')}><LinkOutlined /></Button><Button type="button" variant="ghost" size="icon" aria-label={`复制 ${displayFileName(file)} 的 OSS 地址`} disabled={!file.fileUrl} onClick={() => void copyText(file.fileUrl).then(() => message.success('OSS 地址已复制'))}><CopyOutlined /></Button></div>
    },
    {
      title: '操作', key: 'actions', width: 132, align: 'center',
      render: (_, file) => <div className="management-row-actions">{filePreviewKind(file) !== 'none' ? <ManagementRowAction tone="data" icon={<EyeOutlined />} aria-label={`预览${displayFileName(file)}`} onClick={() => void openPreview(file)} /> : null}{canDownload ? <ManagementRowAction tone="approve" icon={<DownloadOutlined />} aria-label={`下载${displayFileName(file)}`} onClick={() => void downloadFile(file)} /> : null}{canUpload ? <FileActionMenu file={file} canDelete={canDelete && (isAdmin || file.uploaderId === user.id)} onRename={() => openRename(file)} onMove={() => openMove([file])} onDelete={() => confirmDelete(file)} /> : null}</div>
    }
  ], [canDelete, canDownload, canUpload, isAdmin, message, user.id])

  return (
    <section className="management-page file-page">
      <div className="file-workspace-card">
        <header className="file-workspace-toolbar">
          <div><span>全部文件</span><small>共 {filesQuery.data?.total ?? 0} 个文件</small></div>
          <div className="management-actions">
            <div className="file-view-switch" role="group" aria-label="切换文件视图">
              <button type="button" className={viewMode === 'table' ? 'active' : ''} aria-label="列表视图" aria-pressed={viewMode === 'table'} onClick={() => setFileView('table')}><UnorderedListOutlined /></button>
              <button type="button" className={viewMode === 'grid' ? 'active' : ''} aria-label="网格视图" aria-pressed={viewMode === 'grid'} onClick={() => setFileView('grid')}><FolderOpenOutlined /></button>
            </div>
            <Button type="button" variant="outline" loading={filesQuery.isFetching} onClick={() => void refreshFiles()}><ReloadOutlined />刷新</Button>
            {canUpload ? <Button type="button" onClick={() => { resetUpload(); setUploadOpen(true) }}><UploadOutlined />上传文件</Button> : null}
          </div>
        </header>

        <div className="file-workspace-body">
          <aside className="file-group-panel">
            <div className="file-group-heading"><span>文件分组</span>{canManageGroups ? <Button type="button" variant="ghost" size="sm" onClick={() => setGroupEditor({ name: '' })}><PlusOutlined />新建</Button> : null}</div>
            <button type="button" className="file-group-mobile-toggle" aria-expanded={groupPanelExpanded} onClick={() => setGroupPanelExpanded(value => !value)}><span>分组导航</span><span>{groupPanelExpanded ? '收起' : '展开'}</span></button>
            <div className={groupPanelExpanded ? 'file-group-list' : 'file-group-list collapsed'}>
              {groupData.scopeRequired ? <p className="file-scope-notice">请选择上传人后查看分组</p> : <>
                <button type="button" className={activeGroup === 'all' ? 'active' : ''} onClick={() => selectGroup('all')}><FolderOpenOutlined /><span>全部文件</span></button>
                <button type="button" className={activeGroup === 'ungrouped' ? 'active' : ''} onClick={() => selectGroup('ungrouped')}><FolderOutlined /><span>未分组</span><em>{groupData.ungroupedCount}</em></button>
                {groupData.groups.map(group => <div key={group.id} className={activeGroup === String(group.id) ? 'file-group-row active' : 'file-group-row'}><button type="button" onClick={() => selectGroup(String(group.id))}><FolderOutlined /><span title={group.name}>{group.name}</span><em>{group.fileCount}</em></button>{canManageGroups ? <GroupActionMenu group={group} onRename={() => setGroupEditor({ group, name: group.name })} onDelete={() => confirmDeleteGroup(group)} /> : null}</div>)}
              </>}
            </div>
          </aside>

          <div className="file-list-panel">
            <Form {...filterForm}><form className="management-filter-form file-filter-form" onSubmit={filterForm.handleSubmit(applyFilters)}>
              <FormField control={filterForm.control} name="fileName" render={({ field }) => <FormItem className="management-filter-field"><FormLabel>文件名称</FormLabel><FormControl><Input placeholder="请输入文件名称" {...field} /></FormControl><FormMessage /></FormItem>} />
              <FormField control={filterForm.control} name="contentType" render={({ field }) => <FormItem className="management-filter-field management-filter-field-select"><FormLabel>文件类型</FormLabel><Select value={field.value} onValueChange={field.onChange}><FormControl><SelectTrigger><SelectValue /></SelectTrigger></FormControl><SelectContent><SelectItem value="all">全部类型</SelectItem>{(contentTypesQuery.data ?? []).map(item => <SelectItem key={item.value} value={item.value}>{item.label}</SelectItem>)}</SelectContent></Select><FormMessage /></FormItem>} />
              {isAdmin ? <FormField control={filterForm.control} name="uploaderId" render={({ field }) => <FormItem className="management-filter-field management-filter-field-select"><FormLabel>上传人</FormLabel><Select value={field.value} onValueChange={field.onChange}><FormControl><SelectTrigger><SelectValue placeholder="请选择用户" /></SelectTrigger></FormControl><SelectContent>{(usersQuery.data ?? []).map(option => <SelectItem key={option.id} value={String(option.id)}>{option.nickname}</SelectItem>)}</SelectContent></Select><FormMessage /></FormItem>} /> : null}
              <div className="management-filter-actions"><Button type="submit"><SearchOutlined />搜索</Button><Button type="button" variant="outline" onClick={resetFilters}><UndoOutlined />重置</Button></div>
            </form></Form>

            {selectedFiles.length ? <div className="file-selection-bar"><span>已选择 {selectedFiles.length} 个文件</span>{canUpload ? <Button type="button" variant="outline" size="sm" onClick={() => openMove(selectedFiles)}><FolderOpenOutlined />移动到分组</Button> : null}{canDelete ? <Button type="button" variant="destructive" size="sm" onClick={confirmBatchDelete}><DeleteOutlined />批量删除</Button> : null}</div> : null}

            {viewMode === 'table' ? <Table<OssFileRecord>
              rowKey="id"
              columns={columns}
              dataSource={files}
              loading={filesQuery.isLoading}
              pagination={false}
              scroll={{ x: 650 }}
              locale={{ emptyText: '暂无符合条件的文件' }}
              rowSelection={{ selectedRowKeys: selectedIds, onChange: keys => setSelectedIds(keys.map(Number)) }}
            /> : <Spin spinning={filesQuery.isLoading}><div className="file-grid">{files.map(file => <FileAttachment key={file.id} file={file} selected={selectedIds.includes(file.id)} canDownload={canDownload} canUpload={canUpload} canDelete={canDelete && (isAdmin || file.uploaderId === user.id)} onSelectedChange={checked => setSelectedIds(current => checked ? [...new Set([...current, file.id])] : current.filter(id => id !== file.id))} onPreview={() => void openPreview(file)} onDownload={() => void downloadFile(file)} onCopy={() => void copyText(file.fileUrl).then(() => message.success('OSS 地址已复制'))} onRename={() => openRename(file)} onMove={() => openMove([file])} onDelete={() => confirmDelete(file)} />)}{!filesQuery.isLoading && !files.length ? <div className="file-grid-empty"><Empty description="暂无符合条件的文件" /></div> : null}</div></Spin>}

            <div className="file-pagination"><ManagementPagination current={queryParams.pageNum} pageSize={queryParams.pageSize} total={filesQuery.data?.total ?? 0} onChange={(pageNum, pageSize) => setQueryParams(previous => ({ ...previous, pageNum: pageSize === previous.pageSize ? pageNum : 1, pageSize }))} /></div>
          </div>
        </div>
      </div>

      <Dialog open={uploadOpen} onOpenChange={open => { if (!uploading) { setUploadOpen(open); if (!open) resetUpload() } }}>
        <DialogContent className="max-w-[560px]" onEscapeKeyDown={event => { if (uploading) event.preventDefault() }} onPointerDownOutside={event => { if (uploading) event.preventDefault() }}>
          <DialogHeader><DialogTitle>上传文件</DialogTitle><DialogDescription>支持 JPG、JPEG、PNG、GIF、WEBP、MP4、PDF、ZIP、TXT，单个文件最大 50MB。</DialogDescription></DialogHeader>
          <div className="management-dialog-body file-upload-body">
            <input ref={fileInputRef} className="file-upload-input" type="file" accept={UPLOAD_ACCEPT} disabled={uploading} onChange={event => chooseUploadFile(event.target.files?.[0])} />
            <button type="button" className={dragging ? 'file-upload-dropzone dragging' : 'file-upload-dropzone'} disabled={uploading} onClick={() => fileInputRef.current?.click()} onDragEnter={event => { event.preventDefault(); setDragging(true) }} onDragOver={event => event.preventDefault()} onDragLeave={event => { event.preventDefault(); setDragging(false) }} onDrop={event => { event.preventDefault(); setDragging(false); chooseUploadFile(event.dataTransfer.files[0]) }}><UploadOutlined /><span>{uploadFile ? '选择其他文件' : '点击选择或拖放文件'}</span><small>文件上传过程中请勿关闭此弹窗</small></button>
            {uploadFile ? <div className="file-upload-selected"><FileOutlined /><span><span title={uploadFile.name}>{uploadFile.name}</span><small>{formatFileSize(uploadFile.size)}</small></span></div> : null}
            {uploading || uploadProgress > 0 ? <Progress percent={uploadProgress} status={uploadError ? 'exception' : uploadProgress === 100 ? 'success' : 'active'} /> : null}
            {uploadError ? <p className="file-upload-error" role="alert">{uploadError}</p> : null}
          </div>
          <DialogFooter><DialogClose asChild><Button type="button" variant="outline" disabled={uploading}>取消</Button></DialogClose><Button type="button" loading={uploading} disabled={!uploadFile} onClick={() => void submitUpload()}>{uploadError ? '重新上传' : '开始上传'}</Button></DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={Boolean(groupEditor)} onOpenChange={open => { if (!open) setGroupEditor(undefined) }}>
        <DialogContent className="max-w-[400px]"><DialogHeader><DialogTitle>{groupEditor?.group ? '重命名分组' : '新建分组'}</DialogTitle><DialogDescription>分组仅用于整理当前上传人的文件。</DialogDescription></DialogHeader><div className="management-dialog-body"><Input autoFocus maxLength={50} placeholder="请输入分组名称" value={groupEditor?.name ?? ''} onChange={event => setGroupEditor(current => current ? { ...current, name: event.target.value } : current)} onKeyDown={event => { if (event.key === 'Enter' && groupEditor?.name.trim()) groupMutation.mutate(groupEditor) }} /></div><DialogFooter><DialogClose asChild><Button type="button" variant="outline">取消</Button></DialogClose><Button type="button" loading={groupMutation.isPending} disabled={!groupEditor?.name.trim()} onClick={() => { if (groupEditor) groupMutation.mutate(groupEditor) }}>保存</Button></DialogFooter></DialogContent>
      </Dialog>

      <Dialog open={Boolean(moveFiles)} onOpenChange={open => { if (!open) setMoveFiles(undefined) }}>
        <DialogContent className="max-w-[400px]"><DialogHeader><DialogTitle>移动到分组</DialogTitle><DialogDescription>将 {moveFiles?.length ?? 0} 个文件移动到指定分组。</DialogDescription></DialogHeader><div className="management-dialog-body"><Select value={moveGroupId} onValueChange={setMoveGroupId}><SelectTrigger><SelectValue /></SelectTrigger><SelectContent><SelectItem value="ungrouped">未分组</SelectItem>{groupData.groups.map(group => <SelectItem key={group.id} value={String(group.id)}>{group.name}</SelectItem>)}</SelectContent></Select></div><DialogFooter><DialogClose asChild><Button type="button" variant="outline">取消</Button></DialogClose><Button type="button" loading={moveMutation.isPending} onClick={() => { if (moveFiles) moveMutation.mutate(moveFiles) }}>移动</Button></DialogFooter></DialogContent>
      </Dialog>

      <Dialog open={Boolean(renameTarget)} onOpenChange={open => { if (!open) setRenameTarget(undefined) }}>
        <DialogContent className="max-w-[460px]"><DialogHeader><DialogTitle>重命名文件</DialogTitle><DialogDescription>可以修改文件名称，但必须保留原扩展名。</DialogDescription></DialogHeader><div className="management-dialog-body"><Input autoFocus maxLength={255} value={renameValue} onChange={event => setRenameValue(event.target.value)} onKeyDown={event => { if (event.key === 'Enter') submitRename() }} /></div><DialogFooter><DialogClose asChild><Button type="button" variant="outline">取消</Button></DialogClose><Button type="button" loading={renameMutation.isPending} disabled={!renameValue.trim()} onClick={submitRename}>保存</Button></DialogFooter></DialogContent>
      </Dialog>

      <Dialog open={Boolean(preview)} onOpenChange={open => { if (!open) closePreview() }}>
        <DialogContent className="file-preview-dialog max-w-[900px]"><DialogHeader><DialogTitle>{preview ? displayFileName(preview.file) : '文件预览'}</DialogTitle><DialogDescription>{preview?.file.contentType || '未知文件类型'} · {formatFileSize(preview?.file.fileSize)}</DialogDescription></DialogHeader><div className="file-preview-body">{preview?.loading ? <div className="file-preview-loading"><Spin /><span>正在加载预览</span></div> : preview?.kind === 'text' ? <pre>{preview.text}</pre> : preview?.kind === 'image' ? <img src={preview.url || preview.file.fileUrl} alt={displayFileName(preview.file)} /> : preview?.kind === 'video' ? <video src={preview.url} controls autoPlay /> : preview?.kind === 'audio' ? <audio src={preview.url} controls autoPlay /> : preview?.kind === 'pdf' ? <iframe src={preview.url} title="PDF 预览" /> : <Empty description="该文件仅支持下载或打开 OSS 地址" />}</div></DialogContent>
      </Dialog>
    </section>
  )
}
