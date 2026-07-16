import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

async function readSource(relativePath) {
  return readFile(new URL(`../${relativePath}`, import.meta.url), 'utf8')
}

const apiSource = await readSource('src/api/file/index.ts')
const pageSource = await readSource('src/views/file/index.vue')

test('file API exposes typed list and id-based delete operations', () => {
  assert.match(apiSource, /export interface OssFileRecord/)
  assert.match(apiSource, /id:\s*number/)
  assert.match(apiSource, /fileId:\s*string/)
  assert.match(apiSource, /export interface OssFileQuery/)
  assert.match(apiSource, /getFileListApi/)
  assert.match(apiSource, /url:\s*['"]\/file\/list['"]/)
  assert.match(apiSource, /deleteFileApi\(id:\s*number\)/)
  assert.match(apiSource, /method:\s*['"]delete['"]/)
})

test('file page supports search, preview, URL commands, and permission-controlled delete', () => {
  assert.match(pageSource, /queryParams\.originalFilename/)
  assert.match(pageSource, /queryParams\.contentType/)
  assert.match(pageSource, /queryParams\.uploaderName/)
  assert.match(pageSource, /startsWith\(['"]image\/['"]\)/)
  assert.match(pageSource, /window\.open/)
  assert.match(pageSource, /navigator\.clipboard\.writeText/)
  assert.match(pageSource, /sys:file:delete/)
  assert.match(pageSource, /deleteFileApi/)
  assert.doesNotMatch(pageSource, /上传文件|uploadApi/)
})
