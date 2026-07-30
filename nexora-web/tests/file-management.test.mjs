import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import { transform } from 'esbuild'

async function readSource(relativePath) {
  return readFile(new URL(`../${relativePath}`, import.meta.url), 'utf8')
}

const apiSource = await readSource('src/api/file/index.ts')
const pageSource = await readSource('src/views/file/index.vue')

async function importTypeScriptModule(relativePath) {
  const source = await readSource(relativePath)
  const result = await transform(source, { loader: 'ts', format: 'esm', target: 'es2022' })
  const encoded = Buffer.from(result.code).toString('base64')
  return import(`data:text/javascript;base64,${encoded}`)
}

test('file API exposes typed list, download, and id-based delete operations', () => {
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
  assert.doesNotMatch(apiSource, /uploaderName/)
})

test('file page supports search, preview, authenticated download, URL commands, and permission-controlled delete', () => {
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
  assert.match(pageSource, /file\.uploaderId\s*===\s*userStore\.user\.id/)
  assert.match(pageSource, /deleteFileApi/)
  assert.doesNotMatch(pageSource, /上传文件|uploadApi/)
})

test('file page limits uploader filtering to administrators and omits the uploader column', () => {
  assert.match(pageSource, /const isAdmin = computed\(\(\) => userStore\.user\.roles\.includes\(['"]admin['"]\)\)/)
  assert.match(pageSource, /<el-form-item v-if=["']isAdmin["'] label=["']上传人["'] prop=["']uploaderId["']>/)
  assert.match(pageSource, /v-model=["']queryParams\.uploaderId["']/)
  assert.match(pageSource, /if \(isAdmin\.value\) \{\s*loadUserOptions\(\)\s*\}/)
  assert.doesNotMatch(pageSource, /<el-table-column[^>]*prop=["']uploaderName["']/)
})

test('binary response helper reads JSON business errors from blobs', async () => {
  const { readBlobApiError } = await importTypeScriptModule('src/utils/binary-response.ts')
  const blob = new Blob([
    JSON.stringify({ code: 500, message: '下载文件失败', data: null })
  ], { type: 'application/json;charset=UTF-8' })

  assert.deepEqual(await readBlobApiError(blob), {
    code: 500,
    message: '下载文件失败',
    data: null
  })
})

test('binary response helper ignores ordinary file blobs', async () => {
  const { readBlobApiError } = await importTypeScriptModule('src/utils/binary-response.ts')
  const blob = new Blob(['file content'], { type: 'image/png' })

  assert.equal(await readBlobApiError(blob), null)
})
