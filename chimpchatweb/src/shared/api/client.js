let csrfToken

export class ApiError extends Error {
  constructor(message, status, code) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
  }
}

async function request(path, options = {}) {
  const headers = new Headers(options.headers)
  let body = options.body

  if (body !== undefined) {
    headers.set('Content-Type', 'application/json')
  }

  if (options.method && options.method !== 'GET' && options.method !== 'HEAD') {
    const csrfResponse = await fetch('/api/v1/auth/csrf', { credentials: 'same-origin' })
    if (!csrfResponse.ok) {
      throw new ApiError('Could not start a secure request. Reload and try again.', csrfResponse.status)
    }
    csrfToken = (await csrfResponse.json()).token
    headers.set('X-XSRF-TOKEN', csrfToken)
  }

  const response = await fetch(path, {
    ...options,
    body: typeof body === 'string' || body === undefined ? body : JSON.stringify(body),
    credentials: 'same-origin',
    headers,
  })

  const contentType = response.headers.get('content-type') || ''
  const result = contentType.includes('json') ? await response.json() : null
  if (!response.ok) {
    throw new ApiError(
      result?.detail || 'The request could not be completed. Please try again.',
      response.status,
      result?.code,
    )
  }
  return result
}

export const apiGet = (path) => request(path)
export const apiPost = (path, body) => request(path, { method: 'POST', body })
export const apiDelete = (path) => request(path, { method: 'DELETE' })
