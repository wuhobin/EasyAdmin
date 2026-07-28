<template>
  <div class="profile-page">
    <div v-if="profileLoading" class="profile-skeleton" aria-live="polite" aria-label="正在加载个人资料">
      <el-skeleton animated>
        <template #template>
          <div class="skeleton-credential">
            <el-skeleton-item variant="circle" class="skeleton-avatar" />
            <div class="skeleton-copy">
              <el-skeleton-item variant="h3" style="width: 180px" />
              <el-skeleton-item variant="text" style="width: 260px" />
            </div>
          </div>
          <div class="skeleton-workspace">
            <el-skeleton-item variant="rect" class="skeleton-nav" />
            <el-skeleton-item variant="rect" class="skeleton-form" />
          </div>
        </template>
      </el-skeleton>
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

    <template v-else>
      <section class="identity-pass" aria-label="账户身份凭证">
        <div class="pass-rail" aria-hidden="true">
          <span>NEXORA ADMIN</span>
          <span>{{ accountSerial }}</span>
        </div>

        <div class="identity-main">
          <el-upload
            class="avatar-uploader"
            :class="{ 'is-uploading': avatarLoading }"
            :show-file-list="false"
            :disabled="avatarLoading"
            :before-upload="beforeAvatarUpload"
            :http-request="handleAvatarUpload"
            aria-label="更换头像"
          >
            <el-avatar :size="88" :src="userInfo.sysUser.avatar" class="profile-avatar">
              {{ avatarInitial }}
            </el-avatar>
            <span class="avatar-action">
              <el-icon :class="{ 'is-loading': avatarLoading }">
                <Loading v-if="avatarLoading" />
                <Camera v-else />
              </el-icon>
            </span>
          </el-upload>

          <div class="identity-copy">
            <div class="identity-title-row">
              <h2>{{ displayName }}</h2>
              <span class="verified-badge"><el-icon><CircleCheckFilled /></el-icon> 已启用</span>
            </div>
            <p class="identity-email">{{ userInfo.sysUser.email }}</p>
            <div class="role-list">
              <span v-for="role in userInfo.roles" :key="role" class="role-chip">{{ role }}</span>
              <span v-if="!userInfo.roles.length" class="role-chip">普通用户</span>
            </div>
          </div>
        </div>

        <dl class="identity-meta">
          <div>
            <dt>手机</dt>
            <dd>{{ userInfo.sysUser.mobile || '暂未设置' }}</dd>
          </div>
          <div>
            <dt>邮箱</dt>
            <dd>{{ userInfo.sysUser.email || '暂未设置' }}</dd>
          </div>
          <div>
            <dt>加入时间</dt>
            <dd>{{ userInfo.sysUser.createTime || '—' }}</dd>
          </div>
        </dl>

        <div class="profile-completion">
          <div class="completion-heading">
            <span>资料完整度</span>
            <strong>{{ profileCompletion }}%</strong>
          </div>
          <div
            class="completion-track"
            role="progressbar"
            aria-label="资料完整度"
            aria-valuemin="0"
            aria-valuemax="100"
            :aria-valuenow="profileCompletion"
          >
            <span :style="{ width: `${profileCompletion}%` }"></span>
          </div>
          <p>{{ completionHint }}</p>
        </div>
      </section>

      <div class="settings-workspace">
        <aside class="settings-nav" aria-label="账户设置导航">
          <div class="nav-heading">
            <span>设置</span>
            <small>ACCOUNT SETTINGS</small>
          </div>
          <button
            type="button"
            class="nav-item"
            :class="{ 'is-active': activeTab === 'basic' }"
            :aria-selected="activeTab === 'basic'"
            @click="activeTab = 'basic'"
          >
            <span class="nav-icon"><el-icon><User /></el-icon></span>
            <span><strong>基本资料</strong><small>昵称与联系方式</small></span>
            <el-icon class="nav-arrow"><ArrowRight /></el-icon>
          </button>
          <button
            type="button"
            class="nav-item"
            :class="{ 'is-active': activeTab === 'password' }"
            :aria-selected="activeTab === 'password'"
            @click="activeTab = 'password'"
          >
            <span class="nav-icon"><el-icon><Lock /></el-icon></span>
            <span><strong>安全设置</strong><small>更新登录密码</small></span>
            <el-icon class="nav-arrow"><ArrowRight /></el-icon>
          </button>
          <div class="avatar-guidance">
            <el-icon><Picture /></el-icon>
            <p>点击上方头像可更换图片，支持 JPG、PNG，最大 2MB。</p>
          </div>
        </aside>

        <main class="settings-panel">
          <section v-show="activeTab === 'basic'" class="settings-section">
            <div class="section-heading">
              <div>
                <span class="section-kicker">PROFILE DETAILS</span>
                <h2>基本资料</h2>
                <p>这些信息用于系统内的身份展示与必要联系。</p>
              </div>
              <span class="section-icon"><el-icon><User /></el-icon></span>
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
                  <el-input v-model="userForm.nickname" maxlength="30" autocomplete="name" placeholder="请输入用户昵称" />
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
                <el-form-item label="登录邮箱">
                  <div class="email-setting">
                    <el-input :model-value="userInfo.sysUser.email" disabled />
                    <el-button @click="openEmailDialog">更换邮箱</el-button>
                  </div>
                </el-form-item>
                <el-form-item label="性别" class="full-field gender-field">
                  <el-radio-group v-model="userForm.sex">
                    <el-radio-button :value="1">男</el-radio-button>
                    <el-radio-button :value="2">女</el-radio-button>
                  </el-radio-group>
                </el-form-item>
              </div>
              <div class="form-actions">
                <p><el-icon><InfoFilled /></el-icon> 保存后，身份凭证会同步更新。</p>
                <el-button type="primary" size="large" :loading="submitLoading" @click="submitUserForm">
                  保存更改
                </el-button>
              </div>
            </el-form>
          </section>

          <section v-show="activeTab === 'password'" class="settings-section">
            <div class="section-heading">
              <div>
                <span class="section-kicker">SIGN-IN SECURITY</span>
                <h2>更新登录密码</h2>
                <p>设置独立且不易猜测的密码，降低账户被盗风险。</p>
              </div>
              <span class="section-icon"><el-icon><Key /></el-icon></span>
            </div>

            <div class="security-callout">
              <span class="callout-mark"><el-icon><Lock /></el-icon></span>
              <div>
                <strong>密码建议</strong>
                <p>至少 6 位，建议同时包含字母、数字和符号，并避免与其他网站重复。</p>
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
                <p><el-icon><InfoFilled /></el-icon> 修改后请使用新密码完成下一次登录。</p>
                <el-button type="primary" size="large" :loading="pwdLoading" @click="submitPwdForm">
                  更新密码
                </el-button>
              </div>
            </el-form>
          </section>
        </main>
      </div>

      <el-dialog v-model="emailDialogVisible" title="更换登录邮箱" width="460px" destroy-on-close>
        <p class="dialog-form-intro">验证新邮箱后更新后续登录使用的邮箱地址。</p>
        <el-form ref="emailFormRef" :model="emailForm" :rules="emailRules" label-position="top">
          <el-form-item label="新邮箱" prop="email">
            <el-input
              v-model="emailForm.email"
              type="email"
              autocomplete="email"
              placeholder="请输入新邮箱"
            />
          </el-form-item>
          <el-form-item label="邮箱验证码" prop="code">
            <div class="email-setting">
              <el-input
                v-model="emailForm.code"
                inputmode="numeric"
                maxlength="8"
                placeholder="请输入验证码"
              />
              <el-button :loading="codeSending" :disabled="codeCountdown > 0" @click="sendEmailCode">
                {{ codeCountdown > 0 ? `${codeCountdown} 秒后重试` : '发送验证码' }}
              </el-button>
            </div>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="emailDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="changeEmailLoading" @click="submitEmailChange">确认更换</el-button>
        </template>
      </el-dialog>
    </template>
  </div>
</template>

<script lang="ts" setup>
import { ElMessage } from 'element-plus'
import { Camera } from '@element-plus/icons-vue'
import {
  changeEmailApi,
  getUserProfileApi,
  sendChangeEmailCodeApi,
  updateUserProfileApi,
  updateUserPwdApi
} from '@/api/system/user'
import { uploadApi } from '@/api/file'
import { useUserStore } from '@/store/modules/user'

const userStore = useUserStore()
const activeTab = ref<'basic' | 'password'>('basic')
const userFormRef = ref()
const pwdFormRef = ref()
const emailFormRef = ref()

const userInfo = ref<any>({
  sysUser: {},
  roles: []
})

const displayName = computed(() => userInfo.value.sysUser.nickname || userInfo.value.sysUser.email || '用户')
const avatarInitial = computed(() => displayName.value.slice(0, 1).toUpperCase())
const accountSerial = computed(() => {
  const id = String(userInfo.value.sysUser.id || '0').padStart(6, '0')
  return `EA-${id}`
})
const profileCompletion = computed(() => {
  const profile = userInfo.value.sysUser
  const completed = [profile.avatar, profile.nickname, profile.mobile, profile.email].filter(Boolean).length
  return completed * 25
})
const completionHint = computed(() => {
  if (profileCompletion.value === 100) return '身份资料已完整'
  if (!userInfo.value.sysUser.avatar) return '上传头像，让账户更易识别'
  if (!userInfo.value.sysUser.mobile) return '补充手机号，方便必要联系'
  if (!userInfo.value.sysUser.email) return '补充邮箱，完善联系方式'
  return '继续完善你的身份资料'
})

const userForm = reactive({
  nickname: '',
  mobile: '',
  sex: 1
})

const emailDialogVisible = ref(false)
const emailForm = reactive({ email: '', code: '' })
const emailRules = reactive<any>({
  email: [
    { required: true, message: '请输入新邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入邮箱验证码', trigger: 'blur' },
    { pattern: /^\d{4,8}$/, message: '请输入正确的邮箱验证码', trigger: 'blur' }
  ]
})

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const userRules = reactive<any>({
  nickname: [{ required: true, message: '请输入用户昵称', trigger: 'blur' }],
  mobile: [
    { required: true, message: '请输入手机号码', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ]
})

const pwdRules = reactive<any>({
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能小于 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_rule: any, value: string, callback: Function) => {
        if (value !== pwdForm.newPassword) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
})

const submitLoading = ref(false)
const pwdLoading = ref(false)
const avatarLoading = ref(false)
const codeSending = ref(false)
const changeEmailLoading = ref(false)
const codeCountdown = ref(0)
const profileLoading = ref(true)
const profileLoadFailed = ref(false)
let codeCountdownTimer: ReturnType<typeof setInterval> | undefined

const getUser = async (showLoading = true) => {
  if (showLoading) {
    profileLoading.value = true
    profileLoadFailed.value = false
  }
  try {
    const { data } = await getUserProfileApi()
    Object.assign(userInfo.value, data)
    Object.assign(userForm, {
      nickname: data.sysUser.nickname,
      mobile: data.sysUser.mobile,
      sex: data.sysUser.sex
    })
  } catch (error) {
    console.error('获取用户信息失败:', error)
    if (showLoading) profileLoadFailed.value = true
  } finally {
    if (showLoading) profileLoading.value = false
  }
}

const submitUserForm = async () => {
  if (!userFormRef.value) return
  try {
    await userFormRef.value.validate()
    submitLoading.value = true
    await updateUserProfileApi(userForm)
    ElMessage.success('个人资料已更新')
    await getUser(false)
    await userStore.getUserInfo()
  } catch (error) {
    console.error('提交失败:', error)
  } finally {
    submitLoading.value = false
  }
}

const openEmailDialog = () => {
  Object.assign(emailForm, { email: '', code: '' })
  emailDialogVisible.value = true
}

const startCodeCountdown = () => {
  if (codeCountdownTimer) clearInterval(codeCountdownTimer)
  codeCountdown.value = 60
  codeCountdownTimer = setInterval(() => {
    codeCountdown.value -= 1
    if (codeCountdown.value <= 0 && codeCountdownTimer) {
      clearInterval(codeCountdownTimer)
      codeCountdownTimer = undefined
    }
  }, 1000)
}

const sendEmailCode = async () => {
  if (!emailFormRef.value) return
  try {
    await emailFormRef.value.validateField('email')
    codeSending.value = true
    await sendChangeEmailCodeApi(emailForm.email)
    startCodeCountdown()
    ElMessage.success('验证码已发送')
  } catch (error) {
    console.error('发送验证码失败:', error)
  } finally {
    codeSending.value = false
  }
}

const submitEmailChange = async () => {
  if (!emailFormRef.value) return
  try {
    await emailFormRef.value.validate()
    changeEmailLoading.value = true
    await changeEmailApi(emailForm.email, emailForm.code)
    ElMessage.success('登录邮箱已更新')
    emailDialogVisible.value = false
    await getUser(false)
    await userStore.getUserInfo()
  } catch (error) {
    console.error('更换邮箱失败:', error)
  } finally {
    changeEmailLoading.value = false
  }
}

const submitPwdForm = async () => {
  if (!pwdFormRef.value) return
  try {
    await pwdFormRef.value.validate()
    pwdLoading.value = true
    await updateUserPwdApi(pwdForm.oldPassword, pwdForm.newPassword)
    ElMessage.success('登录密码已更新')
    Object.assign(pwdForm, { oldPassword: '', newPassword: '', confirmPassword: '' })
    pwdFormRef.value.resetFields()
  } catch (error) {
    console.error('修改密码失败:', error)
  } finally {
    pwdLoading.value = false
  }
}

const beforeAvatarUpload = (file: File) => {
  const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png'
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isJpgOrPng) {
    ElMessage.error('头像仅支持 JPG 或 PNG 格式')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('头像大小不能超过 2MB')
    return false
  }
  return true
}

const handleAvatarUpload = async (options: any) => {
  avatarLoading.value = true
  try {
    const formData = new FormData()
    formData.append('file', options.file)
    const { data } = await uploadApi(formData)
    await updateUserProfileApi({
      nickname: userInfo.value.sysUser.nickname,
      mobile: userInfo.value.sysUser.mobile,
      sex: userInfo.value.sysUser.sex,
      avatar: data
    })
    userInfo.value.sysUser.avatar = data
    ElMessage.success('头像已更新')
  } catch (error) {
    console.error('头像上传失败:', error)
    ElMessage.error('头像上传失败')
  } finally {
    avatarLoading.value = false
  }
}

onBeforeUnmount(() => {
  if (codeCountdownTimer) clearInterval(codeCountdownTimer)
})

onMounted(() => getUser())
</script>

<style lang="scss" scoped>
.profile-page {
  --profile-primary: var(--el-color-primary, #2563eb);
  --profile-primary-dark: #1e40af;
  --profile-ink: #182033;
  --profile-muted: #69758a;
  --profile-line: #e4e9f2;
  --profile-surface: #ffffff;
  --profile-soft: #f6f8fc;
  box-sizing: border-box;
  min-height: 100%;
  padding: clamp(20px, 2.7vw, 36px);
  background:
    linear-gradient(135deg, rgba(37, 99, 235, 0.035) 0, transparent 32%),
    var(--nexora-content-bg, #f8fafc);
  color: var(--profile-ink);
}

.section-kicker,
.nav-heading small {
  margin: 0;
  color: var(--profile-primary);
  font-family: "Arial Narrow", "Roboto Condensed", sans-serif;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.16em;
}

/* The credential and settings workspace share the same 1280px content rail. */
.identity-pass,
.settings-workspace,
.profile-skeleton,
.profile-error {
  box-sizing: border-box;
  width: min(100%, 1280px);
}
.verified-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.identity-pass {
  position: relative;
  display: grid;
  grid-template-columns: minmax(300px, 1.2fr) minmax(360px, 1.6fr) minmax(180px, 0.65fr);
  align-items: center;
  gap: 28px;
  max-width: none;
  min-height: 174px;
  margin: 0 auto 22px;
  padding: 24px 30px 24px 70px;
  overflow: hidden;
  border: 1px solid var(--profile-line);
  border-radius: 18px;
  background: var(--profile-surface);
  box-shadow: 0 16px 46px rgba(35, 50, 78, 0.07);
  animation: profile-enter 0.35s ease-out both;
}

.identity-pass::after {
  position: absolute;
  top: -85px;
  right: 24%;
  width: 210px;
  height: 210px;
  border: 34px solid rgba(37, 99, 235, 0.035);
  border-radius: 50%;
  content: "";
  pointer-events: none;
}

.pass-rail {
  position: absolute;
  inset: 0 auto 0 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 38px;
  padding: 16px 0;
  background: linear-gradient(180deg, var(--profile-primary), var(--profile-primary-dark));
  color: rgba(255, 255, 255, 0.78);
  font-family: "Arial Narrow", sans-serif;
  font-size: 9px;
  letter-spacing: 0.14em;
  writing-mode: vertical-rl;
}

.identity-main {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 20px;
  min-width: 0;
}

.avatar-uploader {
  position: relative;
  display: inline-flex;
  flex: none;
  cursor: pointer;
}

.avatar-uploader.is-uploading { cursor: wait; }
.avatar-uploader:focus-within { outline: 3px solid rgba(37, 99, 235, 0.24); outline-offset: 5px; border-radius: 18px; }
.avatar-uploader:hover .profile-avatar { transform: translateY(-2px); }
.avatar-uploader:hover .avatar-action { transform: scale(1.06); }

.profile-avatar {
  border: 4px solid #fff;
  border-radius: 18px;
  background: linear-gradient(145deg, #dbe7ff, #b9cefb);
  color: var(--profile-primary-dark);
  font-size: 28px;
  font-weight: 750;
  box-shadow: 0 10px 28px rgba(36, 75, 150, 0.2);
  transition: transform 0.2s ease;
}

.avatar-action {
  position: absolute;
  right: -7px;
  bottom: -7px;
  display: grid;
  width: 31px;
  height: 31px;
  place-items: center;
  border: 3px solid #fff;
  border-radius: 10px;
  background: var(--profile-primary);
  color: #fff;
  transition: transform 0.2s ease;
}

.avatar-action .is-loading { animation: avatar-spin 0.8s linear infinite; }

.identity-copy { min-width: 0; }
.identity-title-row { display: flex; align-items: center; flex-wrap: wrap; gap: 10px; }
.identity-title-row h2 { margin: 0; font-size: 24px; letter-spacing: -0.035em; }
.identity-email { margin: 4px 0 12px; color: var(--profile-muted); font-size: 13px; }

.verified-badge {
  padding: 4px 8px;
  border-radius: 6px;
  background: #eaf8f1;
  color: #147a50;
  font-size: 11px;
  font-weight: 650;
}

.role-list { display: flex; flex-wrap: wrap; gap: 6px; }
.role-chip { padding: 4px 9px; border: 1px solid #dfe6f2; border-radius: 6px; background: var(--profile-soft); color: #516078; font-size: 11px; }

.identity-meta {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin: 0;
  padding: 0 28px;
  border-right: 1px solid var(--profile-line);
  border-left: 1px solid var(--profile-line);
}

.identity-meta > div { min-width: 0; padding: 3px 16px; }
.identity-meta dt { margin-bottom: 7px; color: var(--profile-muted); font-size: 11px; }
.identity-meta dd { overflow: hidden; margin: 0; color: #303b50; font-size: 13px; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }

.profile-completion { position: relative; z-index: 1; }
.completion-heading { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 11px; color: var(--profile-muted); font-size: 12px; }
.completion-heading strong { color: var(--profile-ink); font-size: 20px; }
.completion-track { height: 7px; overflow: hidden; border-radius: 10px; background: #e8edf5; }
.completion-track span { display: block; height: 100%; border-radius: inherit; background: linear-gradient(90deg, var(--profile-primary), #6c8cff); transition: width 0.35s ease; }
.profile-completion p { margin: 9px 0 0; color: var(--profile-muted); font-size: 11px; line-height: 1.5; }

.settings-workspace {
  display: grid;
  grid-template-columns: 250px minmax(0, 1fr);
  gap: 22px;
  max-width: none;
  margin: 0 auto;
  animation: profile-enter 0.35s 0.06s ease-out both;
}

.settings-nav,
.settings-panel,
.profile-error,
.profile-skeleton {
  border: 1px solid var(--profile-line);
  border-radius: 18px;
  background: var(--profile-surface);
  box-shadow: 0 12px 38px rgba(35, 50, 78, 0.055);
}

.settings-nav { display: flex; flex-direction: column; min-height: 430px; padding: 18px 12px 14px; }
.nav-heading { display: flex; align-items: baseline; justify-content: space-between; padding: 4px 10px 14px; }
.nav-heading > span { font-size: 16px; font-weight: 700; }
.nav-heading small { font-size: 8px; }

.nav-item {
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr) 16px;
  align-items: center;
  gap: 11px;
  width: 100%;
  margin-bottom: 5px;
  padding: 11px 10px;
  border: 0;
  border-radius: 11px;
  background: transparent;
  color: var(--profile-muted);
  font: inherit;
  text-align: left;
  cursor: pointer;
  transition: background-color 0.18s ease, color 0.18s ease;
}

.nav-item:hover { background: var(--profile-soft); color: var(--profile-ink); }
.nav-item:focus-visible { outline: 3px solid rgba(37, 99, 235, 0.2); outline-offset: 1px; }
.nav-item.is-active { background: #edf3ff; color: var(--profile-primary-dark); }
.nav-icon { display: grid; width: 38px; height: 38px; place-items: center; border: 1px solid var(--profile-line); border-radius: 10px; background: #fff; font-size: 17px; }
.nav-item.is-active .nav-icon { border-color: transparent; background: var(--profile-primary); color: #fff; box-shadow: 0 7px 17px rgba(37, 99, 235, 0.22); }
.nav-item strong, .nav-item small { display: block; }
.nav-item strong { margin-bottom: 3px; color: inherit; font-size: 13px; }
.nav-item small { color: var(--profile-muted); font-size: 10px; }
.nav-arrow { opacity: 0; transform: translateX(-4px); transition: 0.18s ease; }
.nav-item.is-active .nav-arrow { opacity: 1; transform: translateX(0); }

.avatar-guidance { margin: auto 4px 0; padding: 13px; border: 1px dashed #ccd6e6; border-radius: 11px; color: var(--profile-muted); }
.avatar-guidance .el-icon { color: var(--profile-primary); font-size: 18px; }
.avatar-guidance p { margin: 7px 0 0; font-size: 10px; line-height: 1.65; }

.settings-panel { min-width: 0; min-height: 430px; padding: clamp(24px, 3vw, 38px); }
.settings-section { max-width: 850px; }
.section-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 24px; margin-bottom: 26px; padding-bottom: 22px; border-bottom: 1px solid var(--profile-line); }
.section-heading h2 { margin: 4px 0 5px; font-size: 25px; letter-spacing: -0.035em; }
.section-heading p { margin: 0; color: var(--profile-muted); font-size: 13px; }
.section-icon { display: grid; flex: none; width: 46px; height: 46px; place-items: center; border-radius: 13px; background: #edf3ff; color: var(--profile-primary); font-size: 21px; }

.profile-form :deep(.el-form-item) { margin-bottom: 19px; }
.profile-form :deep(.el-form-item__label) { padding-bottom: 7px; color: #344054; font-size: 12px; font-weight: 650; }
.profile-form :deep(.el-input__wrapper) { min-height: 44px; border-radius: 9px; background: var(--profile-soft); box-shadow: 0 0 0 1px var(--profile-line) inset; transition: 0.18s ease; }
.profile-form :deep(.el-input__wrapper:hover) { box-shadow: 0 0 0 1px #b9c5d8 inset; }
.profile-form :deep(.el-input__wrapper.is-focus) { background: #fff; box-shadow: 0 0 0 1px var(--profile-primary) inset, 0 0 0 4px rgba(37, 99, 235, 0.08); }

.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0 18px; }
.email-setting { display: flex; width: 100%; gap: 10px; }
.email-setting .el-input { min-width: 0; }
.email-setting .el-button { flex: 0 0 auto; min-height: 44px; }
.full-field { grid-column: 1 / -1; }
.gender-field :deep(.el-radio-button__inner) { min-width: 78px; min-height: 40px; border-color: var(--profile-line); background: var(--profile-soft); color: var(--profile-ink); box-shadow: none; }
.gender-field :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) { border-color: var(--profile-primary); background: var(--profile-primary); box-shadow: -1px 0 0 var(--profile-primary); }

.form-actions { display: flex; align-items: center; justify-content: space-between; gap: 20px; margin-top: 7px; padding-top: 20px; border-top: 1px solid var(--profile-line); }
.form-actions p { display: flex; align-items: center; gap: 6px; margin: 0; color: var(--profile-muted); font-size: 11px; }
.form-actions :deep(.el-button) { min-width: 124px; border-radius: 9px; box-shadow: 0 8px 18px rgba(37, 99, 235, 0.18); }
.form-actions :deep(.el-button:hover) { transform: translateY(-1px); }
.form-actions :deep(.el-button:focus-visible) { outline: 3px solid rgba(37, 99, 235, 0.24); outline-offset: 2px; }

.security-callout { display: flex; align-items: center; gap: 14px; margin-bottom: 22px; padding: 14px 16px; border: 1px solid #dce5f5; border-radius: 12px; background: #f4f7fd; }
.callout-mark { display: grid; flex: none; width: 38px; height: 38px; place-items: center; border-radius: 10px; background: #dfeaff; color: var(--profile-primary); font-size: 18px; }
.security-callout strong { color: #344054; font-size: 12px; }
.security-callout p { margin: 3px 0 0; color: var(--profile-muted); font-size: 11px; line-height: 1.5; }
.password-form { max-width: 600px; }

.profile-skeleton, .profile-error { max-width: none; margin: 0 auto; padding: 28px; }
.skeleton-credential { display: flex; align-items: center; gap: 20px; height: 130px; }
.skeleton-avatar { width: 88px; height: 88px; }
.skeleton-copy { display: flex; flex-direction: column; gap: 13px; }
.skeleton-workspace { display: grid; grid-template-columns: 250px 1fr; gap: 22px; margin-top: 22px; }
.skeleton-nav, .skeleton-form { height: 420px; }

@keyframes profile-enter { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
@keyframes avatar-spin { to { transform: rotate(360deg); } }

:global(html.dark) .profile-page {
  --profile-ink: #edf2f9;
  --profile-muted: #99a6ba;
  --profile-line: #2b374a;
  --profile-surface: #171f2d;
  --profile-soft: #1d2737;
  background: var(--nexora-content-bg, #0f1520);
}

:global(html.dark) .account-status,
:global(html.dark) .profile-avatar,
:global(html.dark) .avatar-action,
:global(html.dark) .nav-icon { border-color: var(--profile-line); background-color: var(--profile-surface); color: var(--profile-ink); }
:global(html.dark) .avatar-action,
:global(html.dark) .nav-item.is-active .nav-icon { background: var(--profile-primary); color: #fff; }
:global(html.dark) .verified-badge { background: rgba(34, 160, 107, 0.14); color: #6ed2a8; }
:global(html.dark) .role-chip,
:global(html.dark) .nav-item.is-active,
:global(html.dark) .section-icon { border-color: var(--profile-line); background: rgba(37, 99, 235, 0.14); color: #8aabff; }
:global(html.dark) .identity-meta dd,
:global(html.dark) .profile-form :deep(.el-form-item__label) { color: #dce4ef; }
:global(html.dark) .profile-form :deep(.el-input__wrapper.is-focus) { background: #151d2a; }
:global(html.dark) .security-callout { border-color: var(--profile-line); background: var(--profile-soft); }
:global(html.dark) .security-callout strong { color: #dce4ef; }
:global(html.dark) .callout-mark { background: rgba(37, 99, 235, 0.16); color: #8aabff; }

@media (max-width: 1080px) {
  .identity-pass { grid-template-columns: minmax(290px, 1fr) minmax(300px, 1fr); }
  .profile-completion { grid-column: 1 / -1; display: grid; grid-template-columns: 130px 1fr; align-items: center; gap: 12px; }
  .completion-heading { margin: 0; }
  .profile-completion p { grid-column: 2; margin: -6px 0 0; }
}

@media (max-width: 800px) {
  .profile-page { padding: 20px 16px 28px; }
  .identity-pass { grid-template-columns: 1fr; gap: 22px; padding: 24px 22px 24px 62px; }
  .identity-meta { grid-template-columns: 1fr; gap: 0; padding: 16px 0; border: 0; border-top: 1px solid var(--profile-line); border-bottom: 1px solid var(--profile-line); }
  .identity-meta > div { display: flex; justify-content: space-between; gap: 20px; padding: 6px 0; }
  .identity-meta dt { margin: 0; }
  .profile-completion { grid-column: auto; }
  .settings-workspace { grid-template-columns: 1fr; }
  .settings-nav { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); min-height: auto; padding: 10px; }
  .nav-heading, .avatar-guidance { display: none; }
  .nav-item { margin: 0; }
  .nav-arrow { display: none; }
}

@media (max-width: 560px) {
  .identity-main { align-items: flex-start; }
  .profile-avatar { width: 72px; height: 72px; }
  .identity-title-row { align-items: flex-start; flex-direction: column; gap: 6px; }
  .identity-title-row h2 { font-size: 21px; }
  .profile-completion { display: block; }
  .completion-heading { margin-bottom: 10px; }
  .profile-completion p { margin-top: 8px; }
  .settings-nav { gap: 5px; }
  .nav-item { grid-template-columns: 34px 1fr; gap: 8px; padding: 9px 8px; }
  .nav-icon { width: 34px; height: 34px; }
  .nav-item small { display: none; }
  .settings-panel { padding: 22px 18px; }
  .section-heading { margin-bottom: 22px; }
  .section-icon { display: none; }
  .form-grid { grid-template-columns: 1fr; }
  .full-field { grid-column: auto; }
  .form-actions { align-items: stretch; flex-direction: column; }
  .form-actions :deep(.el-button) { width: 100%; }
  .profile-form :deep(.el-input__inner) { font-size: 16px; }
}

@media (prefers-reduced-motion: reduce) {
  .identity-pass,
  .settings-workspace,
  .avatar-action .is-loading { animation: none; }
  .completion-track span { transition: none; }
}
</style>
