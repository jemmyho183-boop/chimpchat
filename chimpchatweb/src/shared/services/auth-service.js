import { apiRequest } from '../api/client'

export class AuthService {
  getOptions() {
    return apiRequest('/api/v1/auth/options')
  }

  requestMagicLink(email) {
    return apiRequest('/api/auth/magic-link/request', {
      method: 'POST',
      body: { email },
    })
  }

  verifyMagicLink(token) {
    return apiRequest('/api/auth/magic-link/verify', {
      method: 'POST',
      body: { token },
    })
  }

  logout() {
    return apiRequest('/api/v1/auth/logout', {
      method: 'POST',
      body: {},
    })
  }
}

export const authService = new AuthService()
