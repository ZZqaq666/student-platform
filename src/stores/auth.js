import { defineStore } from 'pinia';

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null,
    token: localStorage.getItem('token') || null,
    tokenExpiry: localStorage.getItem('tokenExpiry') || null,
    isAuthenticated: !!localStorage.getItem('token')
  }),
  getters: {
    currentUser: (state) => state.user,
    isLoggedIn: (state) => state.isAuthenticated,
    userId: () => localStorage.getItem('userId'),
    role: () => localStorage.getItem('userRole')
  },
  actions: {
    setToken(token, expiry) {
      this.token = token;
      this.tokenExpiry = expiry;
      this.isAuthenticated = true;
      localStorage.setItem('token', token);
      localStorage.setItem('tokenExpiry', expiry);
    },
    setRefreshToken(refreshToken) {
      // 刷新令牌只落 localStorage 供 401 续期使用；后端轮换后会覆盖旧值
      if (refreshToken) {
        localStorage.setItem('refreshToken', refreshToken);
      } else {
        localStorage.removeItem('refreshToken');
      }
    },
    setUser(user) {
      this.user = user;
      // 确保用户ID存储在localStorage中，并转换为字符串避免精度丢失
      if (user && user.id) {
        localStorage.setItem('userId', String(user.id));
      }
      // role 与 userId 同级持久化：user 对象本身不持久化，刷新后为 null，
      // 而像「仅管理员可编辑目录」这类入口判断需要在刷新后依然可用
      if (user && user.role) {
        localStorage.setItem('userRole', String(user.role));
      }
    },
    logout() {
      this.user = null;
      this.token = null;
      this.tokenExpiry = null;
      this.isAuthenticated = false;
      localStorage.removeItem('token');
      localStorage.removeItem('tokenExpiry');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('userId');
      localStorage.removeItem('userName');
      localStorage.removeItem('userRole');
    },
    checkAuth() {
      const token = localStorage.getItem('token');
      const tokenExpiry = localStorage.getItem('tokenExpiry');
      if (token && tokenExpiry) {
        if (Date.now() > Number(tokenExpiry)) {
          this.logout();
          return false;
        }
        this.token = token;
        this.tokenExpiry = tokenExpiry;
        this.isAuthenticated = true;
        return true;
      }
      this.logout();
      return false;
    }
  }
});
