import { apiDelete, apiGet, apiPost, apiPut } from "../../../shared/api/client";

class InstagramCommentAutomationService {
  list({ accountId, signal } = {}) {
    const params = new URLSearchParams();
    if (accountId) params.set("accountId", accountId);
    const query = params.size ? `?${params}` : "";
    return apiGet(`/api/v1/channels/instagram/automations${query}`, { signal });
  }

  listPosts(accountId, { signal } = {}) {
    const params = new URLSearchParams({ accountId });
    return apiGet(
      `/api/v1/channels/instagram/automations/posts?${params}`,
      { signal },
    );
  }

  create(automation) {
    return apiPost("/api/v1/channels/instagram/automations", automation);
  }

  update(automationId, automation) {
    return apiPut(
      `/api/v1/channels/instagram/automations/${encodeURIComponent(automationId)}`,
      automation,
    );
  }

  delete(automationId) {
    return apiDelete(
      `/api/v1/channels/instagram/automations/${encodeURIComponent(automationId)}`,
    );
  }
}

export const instagramCommentAutomationService =
  new InstagramCommentAutomationService();
