import { apiDelete } from '../api/client'

export class ChannelService {
  disconnect(connectionId) {
    return apiDelete(`/api/v1/channels/${connectionId}`)
  }
}

export const channelService = new ChannelService()
