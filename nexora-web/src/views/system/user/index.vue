<template>
  <div class="app-container data-list-page user-management-page">
    <el-card class="box-card data-list-card" shadow="never">
    <!-- 搜索表单 -->
    <div class="search-wrapper data-list-filters">
      <el-form ref="queryFormRef" :model="queryParams" :inline="true">
        <el-form-item label="昵称" prop="nickname">
          <el-input
            v-model="queryParams.nickname"
            placeholder="请输入昵称"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input
            v-model="queryParams.email"
            placeholder="请输入邮箱"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
            <el-option label="启用" value="1" />
            <el-option label="禁用" value="0" />
            <el-option label="待审核" value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

      <!-- 操作按钮区域 -->
        <div class="card-header data-list-toolbar">
          <ButtonGroup>
            <el-button
              v-permission="['sys:user:add']"
              type="primary"
              icon="Plus"
              @click="handleAdd"
            >新增</el-button>
            <el-button
              v-permission="['sys:user:delete']"
              type="danger"
              plain
              icon="Delete"
              :disabled="selectedIds.length === 0"
              @click="handleBatchDelete"
            >批量删除</el-button>
          </ButtonGroup>
        </div>

      <!-- 数据表格 -->
      <el-table
        class="data-list-table"
        v-loading="loading"
        :data="userList"
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" :selectable="row => row.id !== 1" width="55" align="center" />
        <el-table-column label="ID" prop="id" width="90" align="center" />
        <el-table-column label="头像" prop="avatar" align="center" width="72">
          <template #default="{ row }">
            <el-image
              :src="row.avatar"
              fit="contain"
              class="user-avatar"
              alt="用户头像"
            />
          </template>
        </el-table-column>
        <el-table-column label="昵称" prop="nickname" min-width="120" show-overflow-tooltip />
        <el-table-column label="邮箱" prop="email" min-width="190" show-overflow-tooltip />
        <el-table-column label="手机号" prop="mobile" min-width="150" show-overflow-tooltip />
        <el-table-column label="角色" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ roleNames(row.roleIds) }}</template>
        </el-table-column>
        <el-table-column label="登录IP" prop="ip" min-width="130" show-overflow-tooltip />
        <el-table-column label="登录地址" prop="ipLocation" min-width="140" show-overflow-tooltip />
        <el-table-column label="状态" align="center" width="88">
          <template #default="{ row }">
            <el-tag :type="statusMeta(row.status).type">
              {{ statusMeta(row.status).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" align="center" prop="createTime" width="180" />
        <el-table-column label="操作" align="center" width="320" fixed="right">
          <template #default="scope">
            <el-button
              v-if="scope.row.status === 2"
              v-permission="['sys:user:update']"
              type="success"
              link
              icon="CircleCheck"
              @click="handleAudit(scope.row)"
            >审核通过</el-button>
            <el-button
              v-permission="['sys:user:update']"
              type="primary"
              link
              icon="Edit"
              @click="handleUpdate(scope.row)"
            >修改</el-button>
            <el-button
            v-permission="['sys:user:reset']"
              type="primary"
              link
              icon="Key"
              @click="handleResetPwd(scope.row)"
            >重置密码</el-button>
            <el-button
              v-permission="['sys:user:delete']"
              type="danger"
              link
              icon="Delete"
              :disabled="scope.row.id === 1"
              @click="handleDelete(scope.row)"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页组件 -->
      <div class="pagination-container data-list-pagination">
        <el-pagination
          v-model:current-page="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          :page-sizes="[10, 20, 30, 50]"
          :total="total"
          :background="true"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 添加或修改用户对话框 -->
    <el-dialog
      :title="dialog.title"
      v-model="dialog.visible"
      width="680px"
      append-to-body
      destroy-on-close
      class="user-form-dialog"
    >
      <p class="dialog-form-intro">
        {{ dialog.type === 'add' ? '填写登录资料并分配用户角色。' : '更新用户资料和角色权限。' }}
      </p>
      <el-form
        ref="userFormRef"
        :model="userForm"
        :rules="rules"
        label-position="top"
        class="user-form"
      >
        <div class="user-form-grid">
          <el-form-item label="昵称" prop="nickname">
            <el-input
              v-model="userForm.nickname"
              placeholder="请输入昵称"
              clearable
            />
          </el-form-item>

          <el-form-item label="邮箱" prop="email">
            <el-input
              v-model="userForm.email"
              type="email"
              placeholder="请输入邮箱"
              :disabled="dialog.type === 'edit'"
              clearable
            />
          </el-form-item>

          <el-form-item label="手机号" prop="mobile">
            <el-input
              v-model="userForm.mobile"
              placeholder="请输入手机号"
              clearable
            />
          </el-form-item>

          <el-form-item v-if="dialog.type === 'add'" label="密码" prop="password">
            <el-input
              v-model="userForm.password"
              type="password"
              :placeholder="passwordDescription"
              show-password
              clearable
            />
          </el-form-item>

          <el-form-item label="性别" prop="sex" class="choice-field">
            <el-radio-group v-model="userForm.sex">
              <el-radio :value="1">男</el-radio>
              <el-radio :value="2">女</el-radio>
              <el-radio :value="0">保密</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item
            label="状态"
            class="choice-field"
            :class="{ 'is-wide': dialog.type === 'edit' }"
          >
            <el-radio-group v-model="userForm.status" :disabled="userForm.id === 1">
              <el-radio :value="1">启用</el-radio>
              <el-radio :value="0">禁用</el-radio>
              <el-radio v-if="dialog.type === 'edit'" :value="2">待审核</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="角色" prop="roleIds" class="is-wide">
            <el-select
              v-model="userForm.roleIds"
              multiple
              placeholder="请选择角色"
              :disabled="userForm.id === 1"
              clearable
            >
              <el-option
                v-for="role in roleOptions"
                :key="role.id"
                :label="role.name"
                :value="role.id"
              />
            </el-select>
          </el-form-item>
        </div>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancel">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="submitForm">
            {{ dialog.type === 'add' ? '创建用户' : '保存修改' }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 添加重置密码弹窗 -->
    <el-dialog
      title="重置密码"
      v-model="resetPwdDialog.visible"
      width="500px"
      append-to-body
      destroy-on-close
      class="custom-dialog"
    >
      <p class="dialog-form-intro">为该用户设置新的登录密码。</p>
      <el-form
        ref="resetPwdFormRef"
        :model="resetPwdForm"
        :rules="resetPwdRules"
        label-position="top"
      >
        <el-form-item label="新密码" prop="password">
          <el-input
            v-model="resetPwdForm.password"
            type="password"
            :placeholder="passwordDescription"
            show-password
            clearable
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="resetPwdForm.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            show-password
            clearable
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="resetPwdDialog.visible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="submitResetPwd">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getUserListApi,
  createUserApi,
  updateUserApi,
  deleteUserApi,
  resetPasswordApi,
  auditUserApi
} from '@/api/system/user'
import { getAllRoleList } from '@/api/system/role'
import ButtonGroup from '@/components/ButtonGroup/index.vue'
import { usePublicConfigStore } from '@/store/modules/publicConfig'
import {
  passwordPolicyDescription,
  validatePasswordByPolicy
} from '@/utils/password-policy'

const publicConfigStore = usePublicConfigStore()
const passwordDescription = computed(() =>
  passwordPolicyDescription(publicConfigStore.password)
)
const passwordValidator = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  const message = validatePasswordByPolicy(value, publicConfigStore.password)
  message ? callback(new Error(message)) : callback()
}

// 查询参数
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  nickname: '',
  email: '',
  status: ''
})

const loading = ref(false)
const total = ref(0)
const userList = ref([])
const queryFormRef = ref<FormInstance>()
const userFormRef = ref<FormInstance>()
const submitLoading = ref(false)

// 选中项数组
const selectedIds = ref<number[]>([])

// 弹窗控制
const dialog = reactive({
  title: '',
  visible: false,
  type: 'add'
})

// 角色选项
const roleOptions = ref<any[]>([])

// 表单初始值（用于重置）
const initialUserForm = {
  id: undefined as number | undefined,
  nickname: '',
  password: '',
  mobile: '',
  email: '',
  sex: 0,
  status: 1,
  ip: undefined,
  ipLocation: undefined,
  lastLoginTime: undefined,
  createTime: undefined,
  roleIds: [] as number[]
}

// 表单数据
const userForm = reactive({ ...initialUserForm })

// 表单校验规则
const rules = reactive<FormRules>({
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' }
  ],
  password: [
    { validator: passwordValidator, trigger: 'blur' }
  ],
  mobile: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  roleIds: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ],
  sex: [
    { required: true, message: '请选择性别', trigger: 'change' }
  ]
})

// 重置密码弹窗控制
const resetPwdDialog = reactive({
  id: undefined,
  visible: false
})

// 重置密码表单
const resetPwdForm = reactive({
  password: '',
  confirmPassword: ''
})

// 重置密码表单校验规则
const resetPwdRules = reactive<FormRules>({
  password: [
    { validator: passwordValidator, trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== resetPwdForm.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
})

const resetPwdFormRef = ref<FormInstance>()

const statusMeta = (status: number) => {
  if (status === 1) return { label: '启用', type: 'success' as const }
  if (status === 2) return { label: '待审核', type: 'warning' as const }
  return { label: '禁用', type: 'info' as const }
}

const handleAudit = (row: any) => {
  ElMessageBox.confirm(
    `确认审核通过用户“${row.nickname || row.email}”吗？`,
    '用户审核',
    { type: 'warning', confirmButtonText: '审核通过', cancelButtonText: '取消' }
  ).then(async () => {
    await auditUserApi(row.id)
    ElMessage.success('用户已审核通过')
    await getList()
  }).catch(() => undefined)
}

// 获取用户列表
const getList = async () => {
  loading.value = true
  try {
    const { data } = await getUserListApi(queryParams)
    userList.value = data.records
    total.value = data.total
  } catch (error) {
    console.error('Failed to fetch user list:', error)
  }
  loading.value = false
}

// 表格选择项变化
const handleSelectionChange = (selection: any[]) => {
  selectedIds.value = selection.map(item => item.id)
}

// 批量删除
const handleBatchDelete = () => {
  if (selectedIds.value.length === 0) return
  
  ElMessageBox.confirm('是否确认批量删除选中的用户?', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteUserApi(selectedIds.value)
      ElMessage.success('批量删除成功')
      getList()
      selectedIds.value = []
    } catch (error) {
      console.error('Failed to batch delete users:', error)
    }
  })
}

// 搜索
const handleQuery = () => {
  queryParams.pageNum = 1
  getList()
}

// 重置查询
const resetQuery = () => {
  queryFormRef.value?.resetFields()
  handleQuery()
}

// 新增用户
const handleAdd = () => {
  dialog.type = 'add'
  dialog.title = '新增用户'
  dialog.visible = true
  Object.assign(userForm, initialUserForm)
}

// 修改用户
const handleUpdate = (row: any) => {
  dialog.type = 'edit'
  dialog.title = '修改用户'
  dialog.visible = true
  Object.assign(userForm, row)
  userForm.password = ''
}

// 提交表单
const submitForm = async () => {
  if (!userFormRef.value) return
  
  await userFormRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        if (dialog.type === 'add') {
          await createUserApi({
            nickname: userForm.nickname,
            email: userForm.email,
            password: userForm.password,
            mobile: userForm.mobile,
            sex: userForm.sex,
            status: userForm.status,
            roleIds: userForm.roleIds
          })
          ElMessage.success('新增成功')
        } else {
          await updateUserApi({
            id: userForm.id!,
            nickname: userForm.nickname,
            mobile: userForm.mobile,
            sex: userForm.sex,
            status: userForm.status,
            roleIds: userForm.roleIds
          })
          ElMessage.success('修改成功')
        }
        dialog.visible = false
        getList()
      } catch (error) {
        console.error('Failed to submit user form:', error)
      } finally {
        submitLoading.value = false
      }
    }
  })
}

// 删除用户
const handleDelete = (row: any) => {
  ElMessageBox.confirm(`是否确认删除用户“${row.nickname}”（ID: ${row.id}）?`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteUserApi(row.id)
      ElMessage.success('删除成功')
      getList()
    } catch (error) {
      console.error('Failed to delete user:', error)
    }
  })
}

// 修改重置密码方法
const handleResetPwd = (row: any) => {
  resetPwdDialog.id = row.id
  resetPwdDialog.visible = true
  resetPwdForm.password = ''
  resetPwdForm.confirmPassword = ''
}

// 提交重置密码
const submitResetPwd = async () => {
  if (!resetPwdFormRef.value) return
  
  await resetPwdFormRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        await resetPasswordApi({
          id: resetPwdDialog.id,
          password: resetPwdForm.password
        })
        ElMessage.success('重置密码成功')
        resetPwdDialog.visible = false
      } catch (error) {
        console.error('Failed to reset password:', error)
      } finally {
        submitLoading.value = false
      }
    }
  })
}

// 取消按钮
const cancel = () => {
  dialog.visible = false
  userFormRef.value?.resetFields()
}

// 分页大小改变
const handleSizeChange = (val: number) => {
  queryParams.pageSize = val
  getList()
}

// 页码改变
const handleCurrentChange = (val: number) => {
  queryParams.pageNum = val
  getList()
}

// 获取角色列表
const getRoleOptions = async () => {
  try {
    const { data } = await getAllRoleList()

    roleOptions.value = data
  } catch (error) {
    console.error('Failed to fetch role options:', error)
  }
}

const roleNames = (roleIds: number[] = []) => {
  const names = roleOptions.value
    .filter(role => roleIds.includes(role.id))
    .map(role => role.name)
  return names.join('、') || '-'
}

// 初始化
onMounted(() => {
  getList()
  getRoleOptions()
})
</script>

<style lang="scss" scoped>
.user-management-page {
  :deep(.data-list-table td.el-table__cell) {
    height: 72px;
  }

  .user-avatar {
    display: block;
    width: 42px;
    height: 42px;
    margin: 0 auto;
    overflow: hidden;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 8px;
    background: var(--el-fill-color-light);
  }

}
</style>

<!-- 弹窗使用 append-to-body，需要用独立的全局类命中传送后的 DOM。 -->
<style lang="scss">
.user-form-dialog {
  .el-dialog__body {
    padding: 24px;
  }

  .user-form-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    column-gap: 24px;
    row-gap: 20px;
  }

  .user-form-grid .el-form-item {
    min-width: 0;
    margin-bottom: 0;

    &.is-wide {
      grid-column: 1 / -1;
    }
  }

  .el-form-item__label {
    height: auto;
    padding: 0;
    margin-bottom: 7px;
    color: var(--el-text-color-regular);
    font-size: 13px;
    font-weight: 600;
    line-height: 20px;
  }

  .el-form-item__content {
    min-width: 0;
  }

  .el-input__wrapper,
  .el-select__wrapper {
    min-height: 42px;
    border-radius: 6px;
  }

  .el-select {
    width: 100%;
  }

  .choice-field .el-form-item__content {
    min-height: 42px;
    align-items: center;
  }

  .el-radio-group {
    min-height: 42px;
    display: flex;
    width: auto;
    flex-wrap: wrap;
    gap: 10px 24px;
  }

  .el-radio {
    justify-content: flex-start;
    padding-inline: 0 !important;
    margin-right: 0;
  }

}

@media (max-width: 640px) {
  .user-form-dialog {
    .el-dialog__body {
      padding: 20px;
    }

    .user-form-grid {
      grid-template-columns: minmax(0, 1fr);
      row-gap: 20px;
    }

    .user-form-grid .el-form-item.is-wide {
      grid-column: auto;
    }
  }
}
</style>
