<template>
  <main class="config-container">
    <div class="search-wrapper">
      <el-form :inline="true" :model="queryParams" class="search-form" @submit.prevent>
        <el-form-item label="配置键">
          <el-input
            v-model.trim="queryParams.configKey"
            placeholder="请输入配置键关键字"
            clearable
            maxlength="128"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <ButtonGroup>
            <el-button
              v-permission="['sys:config:add']"
              type="primary"
              icon="Plus"
              @click="handleAdd"
            >
              新增
            </el-button>
          </ButtonGroup>
        </div>
      </template>

      <el-table v-loading="loading" :data="configList" row-key="id">
        <el-table-column label="配置键" prop="configKey" min-width="220" show-overflow-tooltip />
        <el-table-column label="配置值" prop="configValue" min-width="260" show-overflow-tooltip />
        <el-table-column label="备注" prop="remark" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.remark || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="更新时间" prop="updateTime" width="180" align="center" />
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              v-permission="['sys:config:update']"
              type="primary"
              link
              icon="Edit"
              @click="handleEdit(row)"
            >
              修改
            </el-button>
            <el-button
              v-permission="['sys:config:delete']"
              type="danger"
              link
              icon="Delete"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无配置" :image-size="88" />
        </template>
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

    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'add' ? '新增配置' : '修改配置'"
      width="min(560px, calc(100vw - 32px))"
      append-to-body
      destroy-on-close
      @closed="resetForm"
    >
      <el-form
        ref="configFormRef"
        :model="configForm"
        :rules="rules"
        label-width="88px"
        @submit.prevent
      >
        <el-form-item label="配置键" prop="configKey">
          <el-input
            v-model.trim="configForm.configKey"
            :disabled="dialogType === 'edit'"
            maxlength="128"
            placeholder="例如 register.enabled"
          />
          <div class="form-tip">
            只能使用小写字母以及 . _ -，不允许数字；创建后不能修改。
          </div>
        </el-form-item>
        <el-form-item label="配置值" prop="configValue">
          <el-input
            v-model.trim="configForm.configValue"
            type="textarea"
            :rows="5"
            maxlength="512"
            show-word-limit
            resize="vertical"
            placeholder="请输入配置值"
          />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model.trim="configForm.remark"
            type="textarea"
            :rows="3"
            maxlength="255"
            show-word-limit
            resize="vertical"
            placeholder="可填写用途、单位或修改影响"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="submitForm">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </main>
</template>

<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import ButtonGroup from '@/components/ButtonGroup/index.vue'
import {
  addConfigApi,
  deleteConfigApi,
  getConfigListApi,
  updateConfigApi,
  type SysConfigForm,
  type SysConfigQuery,
  type SysConfigRecord
} from '@/api/system/config'

const CONFIG_KEY_PATTERN = /^[a-z]+(?:[._-][a-z]+)*$/

const loading = ref(false)
const submitLoading = ref(false)
const total = ref(0)
const configList = ref<SysConfigRecord[]>([])
const dialogVisible = ref(false)
const dialogType = ref<'add' | 'edit'>('add')
const configFormRef = ref<FormInstance>()

const queryParams = reactive<SysConfigQuery>({
  pageNum: 1,
  pageSize: 10,
  configKey: ''
})

const configForm = reactive<SysConfigForm>({
  configKey: '',
  configValue: '',
  remark: ''
})

const rules: FormRules<SysConfigForm> = {
  configKey: [
    { required: true, message: '请输入配置键', trigger: 'blur' },
    { min: 2, max: 128, message: '配置键长度必须在2到128个字符之间', trigger: 'blur' },
    {
      pattern: CONFIG_KEY_PATTERN,
      message: '只能使用小写字母及点、短横线、下划线，且不能使用数字',
      trigger: 'blur'
    }
  ],
  configValue: [
    { required: true, whitespace: true, message: '请输入配置值', trigger: 'blur' },
    { max: 512, message: '配置值不能超过512个字符', trigger: 'blur' }
  ],
  remark: [
    { max: 255, message: '备注不能超过255个字符', trigger: 'blur' }
  ]
}

async function getList() {
  loading.value = true
  try {
    const { data } = await getConfigListApi(queryParams)
    configList.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  queryParams.pageNum = 1
  void getList()
}

function resetQuery() {
  queryParams.configKey = ''
  handleQuery()
}

function resetForm() {
  configForm.id = undefined
  configForm.configKey = ''
  configForm.configValue = ''
  configForm.remark = ''
  configFormRef.value?.clearValidate()
}

function handleAdd() {
  resetForm()
  dialogType.value = 'add'
  dialogVisible.value = true
}

function handleEdit(row: unknown) {
  const config = row as SysConfigRecord
  resetForm()
  dialogType.value = 'edit'
  Object.assign(configForm, {
    id: config.id,
    configKey: config.configKey,
    configValue: config.configValue,
    remark: config.remark || ''
  })
  dialogVisible.value = true
}

async function submitForm() {
  if (!configFormRef.value) return
  const valid = await configFormRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    if (dialogType.value === 'add') {
      await addConfigApi(configForm)
      ElMessage.success('新增成功')
    } else {
      await updateConfigApi(configForm)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    await getList()
  } finally {
    submitLoading.value = false
  }
}

function handleDelete(row: unknown) {
  const config = row as SysConfigRecord
  ElMessageBox.confirm(
    `删除配置“${config.configKey}”后，依赖它的业务可能使用默认值或执行失败。确定删除吗？`,
    '删除配置',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    await deleteConfigApi(config.id)
    ElMessage.success('删除成功')
    if (configList.value.length === 1 && queryParams.pageNum > 1) {
      queryParams.pageNum -= 1
    }
    await getList()
  }).catch(() => undefined)
}

onMounted(getList)
</script>

<style scoped>
.config-container {
  min-width: 0;
}

.search-form {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  overflow-x: auto;
  padding-top: 20px;
}

.form-tip {
  margin-top: 6px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
