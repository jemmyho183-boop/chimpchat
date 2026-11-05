import { apiRequest } from '../api/client'

export class AppService {
  getBootstrap() {
    return apiRequest('/api/v1/app/bootstrap')
  }
}

export const appService = new AppService()
