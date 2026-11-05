import { apiGet, apiPost } from '../api/client'

export class AuthService {
  getOptions() {
    return apiGet('/api/v1/auth/options')
  }

  requestMagicLink(email) {
    return apiPost('/api/v1/auth/email-links', { email })
  }

  verifyMagicLink(token) {
    return apiPost('/api/v1/auth/email-links/consume', { token })
  }

  logout() {
    return apiPost('/api/v1/auth/logout', {})
  }
}

export const authService = new AuthService()
