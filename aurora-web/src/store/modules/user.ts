import { ref } from "vue";
import { defineStore } from 'pinia';
import { loginApi,getUserInfoApi,logoutApi } from "@/api/system/auth";
import { resetRouter } from "@/router";
import { store } from "@/store";
import { setToken,removeToken } from "@/utils/auth";

interface UserState {
  roles: string[];
  perms: string[];
  intro: any;
  avatar: any;
  nickname: any;
  permissions: string[];
}

export const useUserStore = defineStore("user", () => {
  const user = ref({
    roles: [],
    intro: null,
    avatar: null,
    nickname: null,
    permissions: []
  });

  // 自定义 reset：setup-style store 没有 $reset()，且 pinia-plugin-persistedstate@3.2 不会 polyfill
  // plugin 会通过 $subscribe 监听 state 变化，user 重置后它会自动把空状态写回 localStorage
  function resetUser() {
    user.value = {
      roles: [],
      intro: null,
      avatar: null,
      nickname: null,
      permissions: []
    };
  }

  /**
   * 登录
   *
   * @param {LoginData}
   * @returns
   */
  function login(loginData: any) {
    return new Promise<void>((resolve, reject) => {
      loginApi(loginData)
        .then((response) => {
          const { data } = response;
          setToken(data.token)
          resolve();
        })
        .catch((error) => {
          reject(error);
        });
    });
  }

  // 获取信息(用户昵称、头像、角色集合、权限集合)
  function getUserInfo() {
    return new Promise<any>((resolve, reject) => {
      getUserInfoApi()
        .then(({ data }) => {
          if (!data) {
            reject("Verification failed, please Login again.");
            return;
          }
          Object.assign(user.value, { ...data });
          resolve(data);
        })
        .catch((error) => {
          reject(error);
        });
    });
  }

  // user logout
  function logout() {
    return new Promise<void>((resolve, reject) => {
      logoutApi()
        .then(() => {
          removeToken()
          resetUser()  // plugin auto-syncs empty state to localStorage
          location.reload()
          resolve();
        })
        .catch((error) => {
          reject(error);
        });
    });
  }

  // remove token
  function resetToken() {
    console.log("resetToken");
    return new Promise<void>((resolve) => {
      removeToken()
      resetUser()  // plugin auto-syncs empty state to localStorage
      resetRouter();
      resolve();
    });
  }

  return {
    user,
    login,
    getUserInfo,
    logout,
    resetToken,
    resetUser,
  };
}, {
  persist: {
    key: 'aurora_admin_user_info',
    storage: localStorage,
  }
});

// 非setup
export function useUserStoreHook() {
  return useUserStore(store);
}
