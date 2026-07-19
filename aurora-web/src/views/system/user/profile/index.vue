<template>
  <div class="profile-page">
    <header class="page-heading">
      <div>
        <span class="heading-kicker">ACCOUNT CENTER</span>
        <h1>个人中心</h1>
        <p>管理你的公开资料、联系方式与账户安全。</p>
      </div>
      <div class="account-state">
        <span class="state-dot"></span>
        账号状态正常
      </div>
    </header>

    <div v-if="profileLoading" class="profile-loading" aria-live="polite" aria-label="正在加载个人资料">
      <section class="loading-panel loading-identity">
        <el-skeleton animated>
          <template #template>
            <el-skeleton-item variant="text" class="loading-kicker" />
            <el-skeleton-item variant="circle" class="loading-avatar" />
            <el-skeleton-item variant="h3" class="loading-name" />
            <el-skeleton-item v-for="item in 4" :key="item" variant="text" class="loading-line" />
          </template>
        </el-skeleton>
      </section>
      <section class="loading-panel loading-settings">
        <el-skeleton :rows="8" animated />
      </section>
    </div>

    <el-result
      v-else-if="profileLoadFailed"
      class="profile-error"
      icon="warning"
      title="个人资料加载失败"
      sub-title="请检查网络连接后重新尝试。"
    >
      <template #extra>
        <el-button type="primary" @click="getUser()">
          <el-icon><Refresh /></el-icon>
          重新加载
        </el-button>
      </template>
    </el-result>

    <div v-else class="profile-layout">
      <aside class="identity-card">
        <div class="identity-pattern"></div>
        <div class="identity-content">
          <span class="identity-label">MY PROFILE</span>
          <el-upload
            class="avatar-uploader"
            :class="{ 'is-uploading': avatarLoading }"
            :show-file-list="false"
            :disabled="avatarLoading"
            :before-upload="beforeAvatarUpload"
            :http-request="handleAvatarUpload"
            aria-label="更换头像"
          >
            <el-avatar :size="96" :src="userInfo.sysUser.avatar" class="profile-avatar">
              {{ avatarInitial }}
            </el-avatar>
            <span class="avatar-action" aria-label="更换头像">
              <el-icon :class="{ 'is-loading': avatarLoading }">
                <Loading v-if="avatarLoading" />
                <Camera v-else />
              </el-icon>
            </span>
          </el-upload>

          <div class="identity-name">
            <h2>{{ displayName }}</h2>
            <p>@{{ userInfo.sysUser.username || 'user' }}</p>
          </div>

          <div class="role-list">
            <span v-for="role in userInfo.roles" :key="role" class="role-chip">{{ role }}</span>
            <span v-if="!userInfo.roles.length" class="role-chip">普通用户</span>
          </div>

          <dl class="identity-details">
            <div>
              <dt><el-icon><Iphone /></el-icon>手机号码</dt>
              <dd>{{ userInfo.sysUser.mobile || '暂未设置' }}</dd>
            </div>
            <div>
              <dt><el-icon><Message /></el-icon>电子邮箱</dt>
              <dd>{{ userInfo.sysUser.email || '暂未设置' }}</dd>
            </div>
            <div>
              <dt><el-icon><Calendar /></el-icon>加入时间</dt>
              <dd>{{ userInfo.sysUser.createTime || '—' }}</dd>
            </div>
          </dl>

          <p class="avatar-tip">点击头像可上传 JPG 或 PNG 图片，文件不超过 2MB。</p>
        </div>
      </aside>

      <main class="settings-card">
        <div class="settings-heading">
          <div>
            <span>PROFILE SETTINGS</span>
            <h2>账户设置</h2>
          </div>
          <el-icon class="settings-mark"><Setting /></el-icon>
        </div>

        <el-tabs v-model="activeTab" class="profile-tabs">
          <el-tab-pane name="basic">
            <template #label>
              <span class="tab-label"><el-icon><User /></el-icon>基本资料</span>
            </template>

            <section class="form-section">
              <div class="section-intro">
                <h3>基本信息</h3>
                <p>这些信息用于系统内的身份展示与必要联系。</p>
              </div>
              <el-form
                ref="userFormRef"
                :model="userForm"
                :rules="userRules"
                label-position="top"
                class="profile-form"
              >
                <div class="form-grid">
                  <el-form-item label="用户昵称" prop="nickname" class="full-field">
                    <el-input
                      v-model="userForm.nickname"
                      maxlength="30"
                      autocomplete="name"
                      placeholder="请输入用户昵称"
                    />
                  </el-form-item>
                  <el-form-item label="手机号码" prop="mobile">
                    <el-input
                      v-model="userForm.mobile"
                      type="tel"
                      inputmode="numeric"
                      maxlength="11"
                      autocomplete="tel"
                      placeholder="请输入手机号码"
                    />
                  </el-form-item>
                  <el-form-item label="邮箱地址" prop="email">
                    <el-input
                      v-model="userForm.email"
                      type="email"
                      maxlength="50"
                      autocomplete="email"
                      placeholder="请输入邮箱地址"
                    />
                  </el-form-item>
                  <el-form-item label="性别" class="full-field gender-field">
                    <el-radio-group v-model="userForm.sex">
                      <el-radio-button :value="1">男</el-radio-button>
                      <el-radio-button :value="2">女</el-radio-button>
                    </el-radio-group>
                  </el-form-item>
                </div>
                <div class="form-actions">
                  <span>修改后请及时保存，确保账户资料保持最新。</span>
                  <el-button
                    type="primary"
                    size="large"
                    :loading="submitLoading"
                    @click="submitUserForm"
                  >
                    <el-icon><Check /></el-icon>
                    保存更改
                  </el-button>
                </div>
              </el-form>
            </section>
          </el-tab-pane>

          <el-tab-pane name="password">
            <template #label>
              <span class="tab-label"><el-icon><Lock /></el-icon>安全设置</span>
            </template>

            <section class="form-section security-section">
              <div class="section-intro">
                <h3>修改登录密码</h3>
                <p>建议使用至少 6 位且不与其他网站重复的密码。</p>
              </div>
              <div class="security-note">
                <el-icon><Key /></el-icon>
                <div>
                  <strong>密码安全提示</strong>
                  <span>修改成功后，请使用新密码完成下一次登录。</span>
                </div>
              </div>
              <el-form
                ref="pwdFormRef"
                :model="pwdForm"
                :rules="pwdRules"
                label-position="top"
                class="profile-form password-form"
              >
                <el-form-item label="当前密码" prop="oldPassword">
                  <el-input
                    v-model="pwdForm.oldPassword"
                    type="password"
                    autocomplete="current-password"
                    placeholder="请输入当前密码"
                    show-password
                  />
                </el-form-item>
                <el-form-item label="新密码" prop="newPassword">
                  <el-input
                    v-model="pwdForm.newPassword"
                    type="password"
                    autocomplete="new-password"
                    placeholder="请输入新密码"
                    show-password
                  />
                </el-form-item>
                <el-form-item label="确认新密码" prop="confirmPassword">
                  <el-input
                    v-model="pwdForm.confirmPassword"
                    type="password"
                    autocomplete="new-password"
                    placeholder="请再次输入新密码"
                    show-password
                  />
                </el-form-item>
                <div class="form-actions">
                  <span>为了账户安全，请勿向任何人透露你的密码。</span>
                  <el-button
                    type="primary"
                    size="large"
                    :loading="pwdLoading"
                    @click="submitPwdForm"
                  >
                    <el-icon><Key /></el-icon>
                    更新密码
                  </el-button>
                </div>
              </el-form>
            </section>
          </el-tab-pane>
        </el-tabs>
      </main>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ElMessage } from 'element-plus'
import { Camera } from '@element-plus/icons-vue'
import { getUserProfileApi, updateUserProfileApi,updateUserPwdApi } from '@/api/system/user'
import { uploadApi } from '@/api/file'

const activeTab = ref('basic')
const userFormRef = ref()
const pwdFormRef = ref()

// 用户信息
const userInfo = ref<any>({
  sysUser: {},
  roles: []
})

const displayName = computed(() => userInfo.value.sysUser.nickname || userInfo.value.sysUser.username || '用户')
const avatarInitial = computed(() => displayName.value.slice(0, 1).toUpperCase())

// 表单数据
const userForm = reactive({
  nickname: '',
  mobile: '',
  email: '',
  sex: 1
})

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 表单校验规则
const userRules = reactive<any>({
  nickname: [{ required: true, message: '请输入用户昵称', trigger: 'blur' }],
  email: [
    { required: false, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  mobile: [
    { required: true, message: '请输入手机号码', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ]
})

const pwdRules = reactive<any>({
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能小于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule: any, value: string, callback: Function) => {
        if (value !== pwdForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
})

// 添加loading状态
const submitLoading = ref(false)
const pwdLoading = ref(false)
const avatarLoading = ref(false)
const profileLoading = ref(true)
const profileLoadFailed = ref(false)

// 获取用户信息
const getUser = async (showLoading = true) => {
  if (showLoading) {
    profileLoading.value = true
    profileLoadFailed.value = false
  }
  try {
    const { data } = await getUserProfileApi()
    Object.assign(userInfo.value, data)
    Object.assign(userForm, {
      id: data.sysUser.id,
      nickname: data.sysUser.nickname,
      mobile: data.sysUser.mobile,
      email: data.sysUser.email,
      sex: data.sysUser.sex
    })
  } catch (error) {
    console.error('获取用户信息失败:', error)
    if (showLoading) {
      profileLoadFailed.value = true
    }
  } finally {
    if (showLoading) {
      profileLoading.value = false
    }
  }
}

// 提交用户表单
const submitUserForm = async () => {
  try {
    submitLoading.value = true
    await userFormRef.value.validate()
    await updateUserProfileApi(userForm)
    ElMessage.success('修改成功')
    await getUser(false)
  } catch (error) {
    console.error('提交失败:', error)
  } finally {
    submitLoading.value = false
  }
}

// 提交密码表单
const submitPwdForm = async () => {
  try {
    pwdLoading.value = true
    await pwdFormRef.value.validate()
    await updateUserPwdApi(pwdForm.oldPassword, pwdForm.newPassword)
    ElMessage.success('修改成功')
    Object.assign(pwdForm, {
      oldPassword: '',
      newPassword: '',
      confirmPassword: ''
    })
  } catch (error) {
    console.error('修改密码失败:', error)
  } finally {
    pwdLoading.value = false
  }
}

// 头像上传前的验证
const beforeAvatarUpload = (file: File) => {
  const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png'
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isJpgOrPng) {
    ElMessage.error('头像只能是 JPG 或 PNG 格式!')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('头像大小不能超过 2MB!')
    return false
  }
  return true
}

// 处理头像上传
const handleAvatarUpload = async (options: any) => {
  avatarLoading.value = true
  try {
    const formData = new FormData()
    formData.append('file', options.file)
    const { data } = await uploadApi(formData)
    await updateUserProfileApi({id: userInfo.value.sysUser.id, avatar: data })
    userInfo.value.sysUser.avatar = data
    ElMessage.success('头像更新成功')
  } catch (error) {
    console.error('头像上传失败:', error)
    ElMessage.error('头像上传失败')
  } finally {
    avatarLoading.value = false
  }
}

onMounted(() => {
  getUser()
})
</script>

<style lang="scss" scoped>
.profile-page {
  --profile-ink: #172033;
  --profile-muted: #758096;
  --profile-line: #e8ebf1;
  --profile-accent: #0f8f83;
  --profile-accent-hover: #0b796f;
  --profile-surface: #ffffff;
  --profile-surface-soft: #f8f9fb;
  --profile-border: #e5e8ee;
  --profile-shadow: 0 18px 60px rgba(30, 39, 57, 0.08);
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  height: 100%;
  min-height: 0;
  padding: 16px 20px;
  overflow: hidden;
  background:
    radial-gradient(circle at 86% 8%, rgba(15, 143, 131, 0.08), transparent 24%),
    #f5f6f8;
  color: var(--profile-ink);
}

.page-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  max-width: 1320px;
  flex: none;
  width: 100%;
  margin: 0 auto 12px;

  h1 {
    margin: 2px 0 4px;
    font-family: "STSong", "Songti SC", serif;
    font-size: clamp(26px, 2.4vw, 32px);
    font-weight: 700;
    letter-spacing: -1px;
  }

  p {
    margin: 0;
    color: var(--profile-muted);
    font-size: 14px;
  }
}

.heading-kicker,
.identity-label,
.settings-heading span {
  color: var(--profile-accent);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 2.2px;
}

.account-state {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  min-height: 36px;
  padding: 6px 13px;
  border: 1px solid var(--profile-border);
  border-radius: 999px;
  background: color-mix(in srgb, var(--profile-surface) 82%, transparent);
  color: var(--profile-ink);
  font-size: 13px;
  backdrop-filter: blur(12px);
}

.state-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #20a56f;
  box-shadow: 0 0 0 4px rgba(32, 165, 111, 0.12);
}

.profile-layout {
  display: grid;
  grid-template-columns: minmax(300px, 360px) minmax(0, 1fr);
  align-items: start;
  gap: 24px;
  flex: 1;
  min-height: 0;
  max-width: 1320px;
  width: 100%;
  margin: 0 auto;
}

.profile-loading {
  display: grid;
  grid-template-columns: minmax(300px, 360px) minmax(0, 1fr);
  gap: 24px;
  flex: 1;
  min-height: 0;
  max-width: 1320px;
  width: 100%;
  margin: 0 auto;
}

.loading-panel {
  min-height: 0;
  padding: 24px;
  border-radius: 22px;
  background: var(--profile-surface);
  box-shadow: var(--profile-shadow);
}

.loading-identity {
  background: #182333;

  :deep(.el-skeleton__item) {
    background: rgba(255, 255, 255, 0.1);
  }
}

.loading-kicker {
  width: 96px;
}

.loading-avatar {
  display: block;
  width: 96px;
  height: 96px;
  margin: 32px auto 18px;
}

.loading-name {
  display: block;
  width: 45%;
  margin: 0 auto 28px;
}

.loading-line {
  display: block;
  margin-bottom: 20px;
}

.loading-settings {
  padding: 28px;
}

.profile-error {
  max-width: 760px;
  margin: 40px auto 0;
  border: 1px solid var(--profile-line);
  border-radius: 22px;
  background: var(--profile-surface);
  box-shadow: var(--profile-shadow);

  :deep(.el-button) {
    min-height: 44px;
    border-color: var(--profile-accent);
    border-radius: 10px;
    background: var(--profile-accent);
  }
}

.identity-card,
.settings-card {
  min-height: 0;
  border-radius: 22px;
  box-shadow: var(--profile-shadow);
  animation: profile-rise 0.32s ease-out both;
}

.identity-card {
  position: relative;
  overflow: hidden;
  background: #182333;
  color: #fff;
}

.identity-pattern {
  position: absolute;
  inset: 0;
  opacity: 0.8;
  background:
    linear-gradient(rgba(255, 255, 255, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.035) 1px, transparent 1px),
    radial-gradient(circle at 80% 15%, rgba(43, 196, 176, 0.28), transparent 30%);
  background-size: 38px 38px, 38px 38px, auto;
}

.identity-content {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
  padding: 20px 26px;
}

.identity-label {
  align-self: flex-start;
  color: rgba(255, 255, 255, 0.52);
}

.avatar-uploader {
  position: relative;
  display: inline-flex;
  margin-top: 18px;
  cursor: pointer;
  touch-action: manipulation;

  &.is-uploading {
    cursor: wait;
  }

  &:focus-within {
    outline: 3px solid rgba(69, 197, 183, 0.5);
    outline-offset: 5px;
    border-radius: 50%;
  }

  &:hover {
    .profile-avatar {
      transform: scale(1.025);
    }

    .avatar-action {
      transform: translateY(-2px);
      background: #23b3a5;
    }
  }
}

.profile-avatar {
  border: 5px solid rgba(255, 255, 255, 0.16);
  background: #314155;
  color: #fff;
  font-size: 30px;
  font-weight: 600;
  box-shadow: 0 18px 35px rgba(0, 0, 0, 0.28);
  transition: transform 0.25s ease;
}

.avatar-action {
  position: absolute;
  right: 1px;
  bottom: 5px;
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border: 3px solid #182333;
  border-radius: 50%;
  background: var(--profile-accent);
  color: #fff;
  transition: background-color 0.2s ease, transform 0.2s ease;

  .is-loading {
    animation: avatar-spin 0.8s linear infinite;
  }
}

.identity-name {
  margin-top: 10px;
  text-align: center;

  h2 {
    margin: 0;
    font-family: "STSong", "Songti SC", serif;
    font-size: 24px;
    font-weight: 700;
  }

  p {
    margin: 6px 0 0;
    color: rgba(255, 255, 255, 0.5);
    font-size: 13px;
  }
}

.role-list {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 7px;
  margin-top: 10px;
}

.role-chip {
  padding: 4px 9px;
  border: 1px solid rgba(255, 255, 255, 0.13);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.07);
  color: rgba(255, 255, 255, 0.76);
  font-size: 12px;
}

.identity-details {
  width: 100%;
  margin: 16px 0 0;
  border-top: 1px solid rgba(255, 255, 255, 0.1);

  > div {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 18px;
    padding: 10px 0;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  }

  dt {
    display: flex;
    align-items: center;
    gap: 8px;
    color: rgba(255, 255, 255, 0.5);
    font-size: 12px;
    white-space: nowrap;
  }

  dd {
    overflow: hidden;
    margin: 0;
    color: rgba(255, 255, 255, 0.9);
    font-size: 13px;
    text-align: right;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.avatar-tip {
  margin: auto 0 0;
  padding-top: 12px;
  color: rgba(255, 255, 255, 0.34);
  font-size: 11px;
  line-height: 1.7;
  text-align: center;
}

.settings-card {
  min-width: 0;
  padding: 20px clamp(24px, 3vw, 36px) 22px;
  background: var(--profile-surface);
  animation-delay: 0.08s;
}

.settings-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;

  h2 {
    margin: 2px 0 0;
    font-family: "STSong", "Songti SC", serif;
    font-size: 26px;
  }
}

.settings-mark {
  color: var(--profile-border);
  font-size: 26px;
}

.profile-tabs {
  :deep(.el-tabs__header) {
    margin: 0 0 14px;
  }

  :deep(.el-tabs__nav-wrap::after) {
    height: 1px;
    background: var(--profile-line);
  }

  :deep(.el-tabs__active-bar) {
    height: 3px;
    border-radius: 3px 3px 0 0;
    background: var(--profile-accent);
  }

  :deep(.el-tabs__item) {
    height: 44px;
    padding: 0 22px;
    color: var(--profile-muted);
    font-size: 14px;

    &.is-active,
    &:hover {
      color: var(--profile-ink);
    }

    &.is-active {
      font-weight: 650;
    }
  }
}

.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 7px;
}

.section-intro {
  margin-bottom: 14px;

  h3 {
    margin: 0 0 3px;
    font-size: 18px;
    font-weight: 650;
  }

  p {
    margin: 0;
    color: var(--profile-muted);
    font-size: 13px;
  }
}

.profile-form {
  :deep(.el-form-item) {
    margin-bottom: 14px;
  }

  :deep(.el-form-item__label) {
    padding-bottom: 5px;
    color: var(--profile-ink);
    font-size: 13px;
    font-weight: 600;
  }

  :deep(.el-input__wrapper) {
    min-height: 44px;
    border-radius: 10px;
    background: var(--profile-surface-soft);
    box-shadow: 0 0 0 1px var(--profile-border) inset;
    transition: 0.2s ease;

    &:hover {
      box-shadow: 0 0 0 1px #cbd2dc inset;
    }

    &.is-focus {
      background: var(--profile-surface);
      box-shadow: 0 0 0 1px var(--profile-accent) inset, 0 0 0 4px rgba(15, 143, 131, 0.08);
    }
  }

  :deep(.el-input__inner) {
    color: var(--profile-ink);
  }
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.full-field {
  grid-column: 1 / -1;
}

.gender-field {
  :deep(.el-radio-button__inner) {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 82px;
    height: 44px;
    padding: 0 20px;
    border-color: var(--profile-border);
    background: var(--profile-surface-soft);
    color: var(--profile-ink);
    box-shadow: none;
  }

  :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
    border-color: var(--profile-accent);
    background: var(--profile-accent);
    box-shadow: -1px 0 0 0 var(--profile-accent);
  }
}

.form-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-top: 0;
  padding-top: 14px;
  border-top: 1px solid var(--profile-line);

  > span {
    color: var(--profile-muted);
    font-size: 13px;
  }

  :deep(.el-button) {
    min-width: 132px;
    border-color: var(--profile-accent);
    border-radius: 10px;
    background: var(--profile-accent);
    box-shadow: 0 9px 20px rgba(15, 143, 131, 0.2);

    &:hover {
      border-color: var(--profile-accent-hover);
      background: var(--profile-accent-hover);
      transform: translateY(-1px);
    }

    &:focus-visible {
      outline: 3px solid rgba(15, 143, 131, 0.25);
      outline-offset: 2px;
    }
  }
}

.security-note {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 14px;
  padding: 12px 16px;
  border: 1px solid #dbece9;
  border-radius: 12px;
  background: #f2f9f7;
  color: var(--profile-accent);

  > .el-icon {
    font-size: 24px;
  }

  div {
    display: flex;
    flex-direction: column;
    gap: 3px;
  }

  strong {
    color: #27433f;
    font-size: 13px;
  }

  span {
    color: #718681;
    font-size: 12px;
  }
}

.password-form {
  max-width: 560px;
}

@keyframes profile-rise {
  from {
    opacity: 0;
    transform: translateY(14px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes avatar-spin {
  to {
    transform: rotate(360deg);
  }
}

:global(html.dark) .profile-page {
  --profile-ink: #edf2f7;
  --profile-muted: #a9b3c2;
  --profile-line: #343c49;
  --profile-accent: #45c5b7;
  --profile-accent-hover: #5ed4c7;
  --profile-surface: #1d222b;
  --profile-surface-soft: #242a34;
  --profile-border: #39414f;
  --profile-shadow: 0 18px 60px rgba(0, 0, 0, 0.26);
  background:
    radial-gradient(circle at 86% 8%, rgba(69, 197, 183, 0.1), transparent 25%),
    #14171d;

  .account-state {
    background: rgba(29, 34, 43, 0.88);
  }

  .security-note {
    border-color: rgba(69, 197, 183, 0.24);
    background: rgba(69, 197, 183, 0.08);

    strong {
      color: #d8f3ef;
    }

    span {
      color: #a9c3bf;
    }
  }

  :deep(.el-input__inner::placeholder) {
    color: #778293;
  }
}

@media (min-width: 961px) and (max-height: 800px) {
  .profile-page {
    padding-top: 12px;
    padding-bottom: 12px;
  }

  .page-heading {
    margin-bottom: 8px;

    p {
      display: none;
    }
  }

  .identity-content {
    padding-top: 16px;
    padding-bottom: 16px;
  }

  .avatar-tip,
  .section-intro p,
  .form-actions > span,
  .settings-mark {
    display: none;
  }

  .settings-card {
    padding-top: 16px;
    padding-bottom: 16px;
  }

  .form-actions {
    justify-content: flex-end;
    padding-top: 10px;
  }
}

@media (max-width: 960px) {
  .profile-page {
    height: auto;
    min-height: 100%;
    overflow: visible;
  }

  .profile-layout,
  .profile-loading {
    grid-template-columns: 1fr;
    flex: none;
  }

  .identity-card,
  .settings-card {
    height: auto;
    min-height: auto;
  }

  .avatar-tip {
    margin-top: 24px;
  }
}

@media (max-width: 640px) {
  .profile-page {
    padding: 18px 14px;
  }

  .page-heading {
    align-items: flex-start;
    flex-direction: column;
    gap: 16px;
  }

  .identity-card,
  .settings-card {
    border-radius: 16px;
  }

  .settings-card {
    padding: 26px 18px 30px;
  }

  .loading-panel {
    min-height: 420px;
    padding: 26px 20px;
    border-radius: 16px;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .full-field {
    grid-column: auto;
  }

  .form-actions {
    align-items: stretch;
    flex-direction: column;

    :deep(.el-button) {
      width: 100%;
    }
  }

  .page-heading p,
  .section-intro p,
  .profile-form :deep(.el-input__inner) {
    font-size: 16px;
  }

  .section-intro p {
    line-height: 1.6;
  }
}

@media (prefers-reduced-motion: reduce) {
  .identity-card,
  .settings-card,
  .avatar-action .is-loading {
    animation: none;
  }
}
</style>
