import { apiGet } from '../api/client'

export class AppService {
  getBootstrap() {
    return apiGet('/api/v1/app/bootstrap')
  }
}

export const appService = new AppService()
