let csrfToken

export class ApiError extends Error {
  constructor(message, status, code = 'request_failed') {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
  }
}

function friendlyNetworkError(error) {
  if (error instanceof ApiError) return error
  if (error instanceof TypeError) {
    return new ApiError('We could not reach ChimpChat. Check your connection and try again.', 0, 'network_error')
  }
  return new ApiError('Something went wrong. Please try again.', 0, 'request_failed')
}

export async function apiRequest(path, options = {}) {
  const headers = new Headers(options.headers)
  let body = options.body

  if (body !== undefined) {
    headers.set('Content-Type', 'application/json')
  }

  if (options.method && options.method !== 'GET' && options.method !== 'HEAD') {
    let csrfResponse
    try {
      csrfResponse = await fetch('/api/auth/csrf', { credentials: 'same-origin' })
    } catch (error) {
      throw friendlyNetworkError(error)
    }
    if (!csrfResponse.ok) {
      throw new ApiError('We could not start a secure request. Reload the page and try again.', csrfResponse.status, 'csrf_unavailable')
    }
    const csrfBody = await csrfResponse.json().catch(() => null)
    if (!csrfBody?.token) {
      throw new ApiError('We could not start a secure request. Reload the page and try again.', csrfResponse.status, 'csrf_unavailable')
    }
    csrfToken = csrfBody.token
    headers.set('X-XSRF-TOKEN', csrfToken)
  }

  let response
  try {
    response = await fetch(path, {
      ...options,
      body: typeof body === 'string' || body === undefined ? body : JSON.stringify(body),
      credentials: 'same-origin',
      headers,
    })
  } catch (error) {
    throw friendlyNetworkError(error)
  }

  const contentType = response.headers.get('content-type') || ''
  const result = contentType.includes('json') ? await response.json().catch(() => null) : null
  if (!response.ok) {
    throw new ApiError(
      result?.detail || 'The request could not be completed. Please try again.',
      response.status,
      result?.code,
    )
  }
  return result
}
