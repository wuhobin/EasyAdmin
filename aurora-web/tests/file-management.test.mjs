import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

async function readSource(relativePath) {
  return readFile(new URL(`../${relativePath}`, import.meta.url), 'utf8')
}

const apiSource = await readSource('src/api/file/index.ts')
const pageSource = await readSource('src/views/file/index.vue')

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
})

test('file page supports search, preview, authenticated download, URL commands, and permission-controlled delete', () => {
  assert.match(pageSource, /queryParams\.fileName/)
  assert.match(pageSource, /queryParams\.contentType/)
  assert.match(pageSource, /queryParams\.uploaderName/)
  assert.match(pageSource, /startsWith\(['"]image\/['"]\)/)
  assert.match(pageSource, /window\.open/)
  assert.match(pageSource, /navigator\.clipboard\.writeText/)
  assert.match(pageSource, /sys:file:download/)
  assert.match(pageSource, /downloadFileApi/)
  assert.match(pageSource, /URL\.createObjectURL/)
  assert.match(pageSource, /URL\.revokeObjectURL/)
  assert.match(pageSource, /file\.originalFilename\s*\|\|\s*file\.fileName/)
  assert.match(pageSource, /sys:file:delete/)
  assert.match(pageSource, /deleteFileApi/)
  assert.doesNotMatch(pageSource, /上传文件|uploadApi/)
})
