<template>
  <div class="app-container">
    <!-- 搜索表单 -->
    <div class="search-wrapper">
      <el-form ref="queryFormRef" :model="queryParams" :inline="true">
        <el-form-item label="用户名" prop="nickname">
          <el-input
            v-model="queryParams.nickname"
            placeholder="请输入用户名"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
            <el-option label="启用" value="1" />
            <el-option label="禁用" value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 操作按钮区域 -->
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
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
              icon="Delete"
              :disabled="selectedIds.length === 0"
              @click="handleBatchDelete"
            >批量删除</el-button>
          </ButtonGroup>
        </div>
      </template>

      <!-- 数据表格 -->
      <el-table
        v-loading="loading"
        :data="userList"
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection"  width="55" align="center" />
        <el-table-column label="头像"  prop="avatar" align="center">
          <template #default="{ row }">
            <el-image
              :src="row.avatar"
              fit="contain"
              class="user-avatar"
              alt="用户头像"
            />
          </template>
        </el-table-column>
        <el-table-column label="昵称" align="center" prop="nickname" show-overflow-tooltip />
        <el-table-column label="登录IP" align="center" prop="ip" show-overflow-tooltip />
        <el-table-column label="登录地址" align="center" prop="ipLocation" show-overflow-tooltip />
        <el-table-column label="状态" align="center" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" align="center" prop="createTime" width="180" />
        <el-table-column label="操作" align="center" width="280" fixed="right">
          <template #default="scope">
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
              @click="handleDelete(scope.row)"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页组件 -->
      <div class="pagination-container">
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
      width="600px"
      append-to-body
      destroy-on-close
      class="custom-dialog"
    >
      <el-form
        ref="userFormRef"
        :model="userForm"
        :rules="rules"
        label-width="80px"
        class="custom-form"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input 
                v-model="userForm.username" 
                placeholder="请输入用户名" 
                :disabled="dialog.type === 'edit'"
                clearable
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="昵称" prop="nickname">
              <el-input 
                v-model="userForm.nickname" 
                placeholder="请输入昵称"
                clearable 
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="手机号" prop="mobile">
              <el-input 
                v-model="userForm.mobile" 
                placeholder="请输入手机号"
                clearable 
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input 
                v-model="userForm.email" 
                placeholder="请输入邮箱"
                clearable 
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="性别" prop="sex">
              <el-radio-group v-model="userForm.sex">
                <el-radio :value="1">男</el-radio>
                <el-radio :value="2">女</el-radio>
                <el-radio :value="0">保密</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="密码" prop="password" v-if="dialog.type === 'add'">
              <el-input 
                v-model="userForm.password" 
                type="password" 
                placeholder="请输入密码"
                show-password
                clearable
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="角色" prop="roleIds">
          <el-select
            v-model="userForm.roleIds"
            multiple
            placeholder="请选择角色"
            style="width: 100%"
            :disabled="userForm.username === 'admin'"
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

        <el-form-item label="状态">
          <el-radio-group v-model="userForm.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancel">取 消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="submitForm">确 定</el-button>
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
      <el-form
        ref="resetPwdFormRef"
        :model="resetPwdForm"
        :rules="resetPwdRules"
        label-width="100px"
      >
        <el-form-item label="新密码" prop="password">
          <el-input
            v-model="resetPwdForm.password"
            type="password"
            placeholder="请输入新密码"
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
          <el-button @click="resetPwdDialog.visible = false">取 消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="submitResetPwd">确 定</el-button>
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
  resetPasswordApi
} from '@/api/system/user'
import { getAllRoleList } from '@/api/system/role'
import ButtonGroup from '@/components/ButtonGroup/index.vue'

// 查询参数
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  nickname: '',
  status: ''
})

const loading = ref(false)
const total = ref(0)
const userList = ref([])
const queryFormRef = ref<FormInstance>()
const userFormRef = ref<FormInstance>()
const submitLoading = ref(false)

// 选中项数组
const selectedIds = ref<string[]>([])

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
  id: undefined,
  username: '',
  nickname: '',
  password: null,
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
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  mobile: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  email: [
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
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
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
  userForm.password = null
}

// 提交表单
const submitForm = async () => {
  if (!userFormRef.value) return
  
  await userFormRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        const data = {user: userForm, roleIds: userForm.roleIds}
        if (dialog.type === 'add') {
          await createUserApi(data)
          ElMessage.success('新增成功')
        } else {
          await updateUserApi(data)
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
  ElMessageBox.confirm(`是否确认删除用户"${row.username}"?`, '警告', {
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

// 初始化
onMounted(() => {
  getList()
  getRoleOptions()
})
</script>

<style lang="scss" scoped>
.app-container {
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

  .pagination-container {
    display: flex;
    justify-content: center;
    margin-top: 20px;
  }

  // 添加自定义表单样式
  :deep(.custom-dialog) {
    .el-dialog__body {
      padding: 20px 40px;
    }

    .custom-form {
      .el-form-item {
        margin-bottom: 22px;
        
        &:last-child {
          margin-bottom: 0;
        }
      }

      .el-form-item__label {
        font-weight: 500;
        color: var(--el-text-color-regular);
      }

      .el-input__wrapper {
        box-shadow: 0 0 0 1px #dcdfe6 inset;
        
        &:hover {
          box-shadow: 0 0 0 1px var(--el-border-color-hover) inset;
        }
        
        &.is-focus {
          box-shadow: 0 0 0 1px var(--el-color-primary) inset;
        }
      }

      .el-select {
        width: 100%;
      }

      .el-radio-group {
        .el-radio {
          margin-right: 30px;
          
          &:last-child {
            margin-right: 0;
          }
        }
      }
    }
  }

  .dialog-footer {
    text-align: right;
    padding-top: 20px;
    
    .el-button {
      padding-left: 25px;
      padding-right: 25px;
      
      & + .el-button {
        margin-left: 12px;
      }
    }
  }
}

// 暗黑模式适配
:root[data-theme='dark'] {
  :deep(.custom-dialog) {
    .custom-form {
      .el-input__wrapper {
        box-shadow: 0 0 0 1px var(--el-border-color) inset;
      }
    }
  }
}
</style> 
