import { apiRequest } from '../api/client'

export class ChannelService {
  disconnect(connectionId) {
    return apiRequest(`/api/v1/channels/${connectionId}`, {
      method: 'DELETE',
    })
  }
}

export const channelService = new ChannelService()
