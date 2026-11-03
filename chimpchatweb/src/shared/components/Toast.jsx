import { useCallback, useEffect, useMemo, useState } from 'react'
import { ToastContext } from './toast-context'

export function ToastProvider({ children }) {
  const [toast, setToast] = useState(null)

  const notify = useCallback((message, type = 'success') => {
    setToast({ id: Date.now(), message, type })
  }, [])

  const dismiss = useCallback(() => setToast(null), [])

  useEffect(() => {
    if (!toast) return undefined
    const timeout = window.setTimeout(dismiss, 4500)
    return () => window.clearTimeout(timeout)
  }, [toast, dismiss])

  const value = useMemo(() => ({ notify, dismiss }), [notify, dismiss])

  return <ToastContext.Provider value={value}>
    {children}
    {toast && (
      <div className={`toast toast-${toast.type}`} role={toast.type === 'error' ? 'alert' : 'status'}>
        <span className="toast-indicator" aria-hidden="true" />
        <span>{toast.message}</span>
        <button className="toast-dismiss" type="button" onClick={dismiss} aria-label="Dismiss notification">×</button>
      </div>
    )}
  </ToastContext.Provider>
}
