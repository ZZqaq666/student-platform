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
    isLoggedIn: (state) => state.isAuthenticated
  },
  actions: {
    setToken(token, expiry) {
      this.token = token;
      this.tokenExpiry = expiry;
      this.isAuthenticated = true;
      localStorage.setItem('token', token);
      localStorage.setItem('tokenExpiry', expiry);
    },
    setUser(user) {
      this.user = user;
    },
    logout() {
      this.user = null;
      this.token = null;
      this.tokenExpiry = null;
      this.isAuthenticated = false;
      localStorage.removeItem('token');
      localStorage.removeItem('tokenExpiry');
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
