import { describe, it, expect, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from './auth.js'

// Mock localStorage
beforeEach(() => {
  localStorage.clear()
  setActivePinia(createPinia())
})

describe('Auth Store', () => {
  it('should initialize with empty state when no token exists', () => {
    const authStore = useAuthStore()
    
    expect(authStore.user).toBeNull()
    expect(authStore.token).toBeNull()
    expect(authStore.tokenExpiry).toBeNull()
    expect(authStore.isAuthenticated).toBe(false)
  })

  it('should set token and user information', () => {
    const authStore = useAuthStore()
    const mockToken = 'test-token-123'
    const mockExpiry = Date.now() + 3600000 // 1 hour
    const mockUser = { id: 1, username: 'testuser', role: 'STUDENT' }

    authStore.setToken(mockToken, mockExpiry)
    authStore.setUser(mockUser)

    expect(authStore.token).toBe(mockToken)
    expect(authStore.tokenExpiry).toBe(mockExpiry)
    expect(authStore.user).toEqual(mockUser)
    expect(authStore.isAuthenticated).toBe(true)
    expect(localStorage.getItem('token')).toBe(mockToken)
    expect(localStorage.getItem('tokenExpiry')).toBe(mockExpiry.toString())
  })

  it('should clear state on logout', () => {
    const authStore = useAuthStore()
    const mockToken = 'test-token-123'
    const mockExpiry = Date.now() + 3600000
    const mockUser = { id: 1, username: 'testuser', role: 'STUDENT' }

    // Set up state
    authStore.setToken(mockToken, mockExpiry)
    authStore.setUser(mockUser)

    // Logout
    authStore.logout()

    expect(authStore.user).toBeNull()
    expect(authStore.token).toBeNull()
    expect(authStore.tokenExpiry).toBeNull()
    expect(authStore.isAuthenticated).toBe(false)
    expect(localStorage.getItem('token')).toBeNull()
    expect(localStorage.getItem('tokenExpiry')).toBeNull()
  })

  it('should return false for checkAuth when no token exists', () => {
    const authStore = useAuthStore()
    const result = authStore.checkAuth()
    expect(result).toBe(false)
  })

  it('should return true for checkAuth when valid token exists', () => {
    const authStore = useAuthStore()
    const mockToken = 'test-token-123'
    const mockExpiry = Date.now() + 3600000

    authStore.setToken(mockToken, mockExpiry)
    const result = authStore.checkAuth()
    expect(result).toBe(true)
  })

  it('should return false for checkAuth when token is expired', () => {
    const authStore = useAuthStore()
    const mockToken = 'test-token-123'
    const mockExpiry = Date.now() - 3600000 // 1 hour ago

    authStore.setToken(mockToken, mockExpiry)
    const result = authStore.checkAuth()
    expect(result).toBe(false)
    expect(authStore.isAuthenticated).toBe(false)
  })
})
