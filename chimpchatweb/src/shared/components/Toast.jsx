import { useCallback, useEffect, useMemo, useState } from 'react'
import { ToastContext } from './toast-context'

const toastTypes = ['success', 'error', 'warning', 'info']
const toastDurations = {
  success: 3500,
  error: 6000,
  warning: 5000,
  info: 4500,
}

export function ToastProvider({ children }) {
  const [toast, setToast] = useState(null)

  const notify = useCallback((message, type = 'success') => {
    const toastType = toastTypes.includes(type) ? type : 'info'
    setToast({ id: Date.now(), message, type: toastType })
  }, [])

  const dismiss = useCallback(() => setToast(null), [])

  useEffect(() => {
    if (!toast) return undefined
    const timeout = window.setTimeout(dismiss, toastDurations[toast.type])
    return () => window.clearTimeout(timeout)
  }, [toast, dismiss])

  const value = useMemo(() => ({ notify, dismiss }), [notify, dismiss])

  return <ToastContext.Provider value={value}>
    {children}
    {toast && (
      <div
        className={`toast toast-${toast.type}`}
        role={toast.type === 'error' ? 'alert' : 'status'}
        aria-live={toast.type === 'error' ? 'assertive' : 'polite'}
      >
        <span className="toast-indicator" aria-hidden="true" />
        <span>{toast.message}</span>
        <button className="toast-dismiss" type="button" onClick={dismiss} aria-label="Dismiss notification">×</button>
      </div>
    )}
  </ToastContext.Provider>
}
