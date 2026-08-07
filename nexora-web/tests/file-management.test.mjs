import assert from 'node:assert/strict'
import {readFile} from 'node:fs/promises'
import test from 'node:test'
import {transform} from 'esbuild'

async function readSource(relativePath) {
    return readFile(new URL(`../${relativePath}`, import.meta.url), 'utf8')
}

const apiSource = await readSource('src/api/file/index.ts')
const pageSource = await readSource('src/views/file/index.vue')

async function importTypeScriptModule(relativePath) {
    const source = await readSource(relativePath)
    const result = await transform(source, {loader: 'ts', format: 'esm', target: 'es2022'})
    const encoded = Buffer.from(result.code).toString('base64')
    return import(`data:text/javascript;base64,${encoded}`)
}

test('file API exposes typed upload, list, download, and id-based delete operations', () => {
    assert.match(apiSource, /export interface OssFileRecord/)
    assert.match(apiSource, /id:\s*number/)
    assert.match(apiSource, /fileId:\s*string/)
    assert.match(apiSource, /export interface OssFileQuery/)
    assert.match(apiSource, /getFileListApi/)
    assert.match(apiSource, /url:\s*['"]\/file\/list['"]/)
    assert.match(apiSource, /deleteFileApi\(id:\s*number\)/)
    assert.match(apiSource, /method:\s*['"]delete['"]/)
    assert.match(apiSource, /downloadFileApi\(id:\s*number\)/)
    assert.match(apiSource, /url:\s*`\/file\/\$\{id\}\/download`/)
    assert.match(apiSource, /responseType:\s*['"]blob['"]/)
    assert.match(apiSource, /timeout:\s*0/)
    assert.match(apiSource, /export function uploadApi\(\s*data:\s*FormData,/)
    assert.match(apiSource, /onUploadProgress\?:\s*\(progressEvent:\s*AxiosProgressEvent\)/)
    assert.match(apiSource, /return request<string>\(/)
    assert.match(apiSource, /headers:\s*\{\s*['"]Content-Type['"]:\s*undefined\s*\}/)
    assert.match(apiSource, /data,\s*timeout:\s*0,\s*onUploadProgress/)
    assert.doesNotMatch(apiSource, /multipart\/articles-data/)
    assert.doesNotMatch(apiSource, /uploaderName/)
})

test('file page supports search, preview, authenticated download, URL commands, and permission-controlled actions', () => {
    assert.match(pageSource, /queryParams\.fileName/)
    assert.match(pageSource, /queryParams\.contentType/)
    assert.match(pageSource, /startsWith\(['"]image\/['"]\)/)
    assert.match(pageSource, /window\.open/)
    assert.match(pageSource, /navigator\.clipboard\.writeText/)
    assert.match(pageSource, /sys:file:download/)
    assert.match(pageSource, /downloadFileApi/)
    assert.match(pageSource, /URL\.createObjectURL/)
    assert.match(pageSource, /URL\.revokeObjectURL/)
    assert.match(pageSource, /file\.originalFilename\s*\|\|\s*file\.fileName/)
    assert.match(pageSource, /sys:file:delete/)
    assert.match(pageSource, /v-if=["']canDeleteFile\(row\)["']/)
    assert.match(pageSource, /userStore\.user\.roles\.includes\(['"]admin['"]\)/)
    assert.match(pageSource, /row\.uploaderId\s*===\s*userStore\.user\.id/)
    assert.match(pageSource, /deleteFileApi/)
    assert.match(pageSource, /v-permission=["']\['sys:file:upload'\]["']/)
    assert.match(pageSource, />\s*上传文件\s*</)
})

test('file page previews supported media through the authenticated preview API', () => {
    assert.match(pageSource, /previewFileApi/)
    assert.match(pageSource, /previewTextFileApi/)
    assert.match(pageSource, /previewDialogVisible/)
    assert.match(pageSource, /<video[^>]*controls/)
    assert.match(pageSource, /<audio[^>]*controls/)
    assert.match(pageSource, /<iframe/)
    assert.match(pageSource, /URL\.createObjectURL/)
    assert.match(pageSource, /URL\.revokeObjectURL/)
})

test('file page implements a manual, single-file upload flow with locked in-flight controls', () => {
    assert.match(pageSource, /<el-upload[\s\S]*?\sdrag[\s\S]*?:auto-upload=["']false["']/)
    assert.match(pageSource, /:limit=["']1["']/)
    assert.match(pageSource, /:multiple=["']false["']/)
    assert.match(pageSource, /:accept=["']UPLOAD_ACCEPT["']/)
    assert.match(pageSource, /:close-on-click-modal=["']!uploading["']/)
    assert.match(pageSource, /:close-on-press-escape=["']!uploading["']/)
    assert.match(pageSource, /:show-close=["']!uploading["']/)
    assert.match(pageSource, /<el-button :disabled=["']uploading["'] @click=["']closeUploadDialog["']>取消<\/el-button>/)
    assert.match(pageSource, /:disabled=["']!selectedUploadFile \|\| uploading["']/)
    assert.match(pageSource, /await uploadApi\(formData, updateUploadProgress\)/)
    assert.match(pageSource, /uploadProgress\.value\s*=\s*Math\.max/)
    assert.match(pageSource, /uploadError\.value\s*=\s*getUploadErrorMessage\(error\)/)
    assert.match(pageSource, /uploadActionText[\s\S]*?重新上传/)
    assert.match(pageSource, /queryParams\.pageNum\s*=\s*1;\s*uploadDialogVisible\.value\s*=\s*false;\s*await getList\(\)/)
    assert.doesNotMatch(pageSource, /暂停|续传/)
})

test('upload validation accepts the fixed whitelist and enforces name, MIME, and 50MB limits', async () => {
    const {
        MAX_UPLOAD_FILE_SIZE,
        getUploadErrorMessage,
        validateUploadFile
    } = await importTypeScriptModule('src/views/file/upload.ts')

    const supportedFiles = [
        ['photo.jpg', 'image/jpeg'],
        ['photo.jpeg', 'image/jpeg'],
        ['photo.png', 'image/png'],
        ['animation.gif', 'image/gif'],
        ['preview.webp', 'image/webp'],
        ['video.mp4', 'video/mp4'],
        ['document.pdf', 'application/pdf'],
        ['archive.zip', 'application/zip'],
        ['archive.zip', 'application/x-zip-compressed'],
        ['notes.txt', 'text/plain']
    ]
    for (const [name, type] of supportedFiles) {
        assert.equal(validateUploadFile({name, type, size: MAX_UPLOAD_FILE_SIZE}), null)
    }

    assert.equal(validateUploadFile({name: 'unknown.pdf', type: '', size: 1}), null)
    assert.equal(
        validateUploadFile({name: 'renamed.pdf', type: 'image/png', size: 1}),
        '文件扩展名与实际类型不一致，请选择正确的文件'
    )
    assert.equal(
        validateUploadFile({name: 'script.exe', type: 'application/octet-stream', size: 1}),
        '仅支持 JPG、JPEG、PNG、GIF、WEBP、MP4、PDF、ZIP 或 TXT 格式'
    )
    assert.equal(
        validateUploadFile({name: 'large.txt', type: 'text/plain', size: MAX_UPLOAD_FILE_SIZE + 1}),
        '文件大小不能超过 50MB'
    )
    assert.equal(validateUploadFile({name: '   ', type: '', size: 1}), '文件名不能为空')
    assert.equal(validateUploadFile({name: 'empty.txt', type: 'text/plain', size: 0}), '上传文件不能为空')
    assert.equal(
        validateUploadFile({name: `${'😀'.repeat(251)}.txt`, type: 'text/plain', size: 1}),
        null
    )
    assert.equal(
        validateUploadFile({name: `${'😀'.repeat(252)}.txt`, type: 'text/plain', size: 1}),
        '文件名不能超过 255 个字符'
    )
    assert.equal(getUploadErrorMessage(new Error('服务端拒绝该文件')), '服务端拒绝该文件')
})

test('file page limits uploader filtering to administrators and omits the uploader column', () => {
    assert.match(pageSource, /const isAdmin = computed\(\(\) => userStore\.user\.roles\.includes\(['"]admin['"]\)\)/)
    assert.match(pageSource, /<el-form-item v-if=["']isAdmin["'] label=["']上传人["'] prop=["']uploaderId["']>/)
    assert.match(pageSource, /v-model=["']queryParams\.uploaderId["']/)
    assert.match(pageSource, /if \(isAdmin\.value\) \{\s*loadUserOptions\(\)\s*\}/)
    assert.doesNotMatch(pageSource, /<el-table-column[^>]*prop=["']uploaderName["']/)
})

test('file page keeps per-user view preferences and protects group scope in admin mode', () => {
    assert.match(pageSource, /viewMode.*localStorage\.getItem\(['"]nexora:file:view:/)
    assert.match(pageSource, /mobile-group-toggle/)
    assert.match(pageSource, /group-menu-content.*collapsed/)
    assert.match(pageSource, /const canManageGroups = computed\(\(\) => !isAdmin\.value \|\| Boolean\(selectedOwnerId\.value\)\)/)
    assert.match(pageSource, /isAdmin\.value && selectedOwnerId\.value !== currentUserId\.value/)
    assert.match(pageSource, /const selectedOwnerId = ref<number \| undefined>\(currentUserId\.value \|\| undefined\)/)
    assert.match(pageSource, /uploaderId: selectedOwnerId\.value/)
    assert.match(pageSource, /CopyDocument/)
    assert.match(pageSource, /command=["']move["']/)
})

test('binary response helper reads JSON business errors from blobs', async () => {
    const {readBlobApiError} = await importTypeScriptModule('src/utils/binary-response.ts')
    const blob = new Blob([
        JSON.stringify({code: 500, message: '下载文件失败', data: null})
    ], {type: 'application/json;charset=UTF-8'})

    assert.deepEqual(await readBlobApiError(blob), {
        code: 500,
        message: '下载文件失败',
        data: null
    })
})

test('binary response helper ignores ordinary file blobs', async () => {
    const {readBlobApiError} = await importTypeScriptModule('src/utils/binary-response.ts')
    const blob = new Blob(['file content'], {type: 'image/png'})

    assert.equal(await readBlobApiError(blob), null)
})
