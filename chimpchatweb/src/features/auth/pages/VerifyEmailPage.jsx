import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { apiPost } from '../../../shared/api/client'
import { BrandHeader } from '../../../shared/components/BrandHeader'

export function VerifyEmailPage() {
  const [params] = useSearchParams()
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const navigate = useNavigate()
  const token = params.get('token')

  async function verify() {
    setBusy(true)
    setError('')
    try {
      await apiPost('/api/v1/auth/email-links/consume', { token })
      navigate('/', { replace: true })
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setBusy(false)
    }
  }

  return <div className="app-shell">
    <BrandHeader caption="SECURE SIGN-IN" />
    <main className="center-layout">
      <section className="message-panel">
        <div className="card-icon" aria-hidden="true">✓</div>
        <p className="eyebrow">YOUR EMAIL LINK IS READY</p>
        <h1>Confirm it’s you.</h1>
        <p className="message-copy">This one-time sign-in link will securely open your ChimpChat workspace.</p>
        <button className="button button-primary" type="button" onClick={verify} disabled={!token || busy}>
          {busy ? 'Verifying link…' : 'Continue to ChimpChat'}
        </button>
        {!token && <p className="inline-notice notice-error" role="alert">This sign-in link is missing. Request a new one.</p>}
        {error && <p className="inline-notice notice-error" role="alert">{error}</p>}
        <Link className="text-link" to="/signin">Back to sign in</Link>
      </section>
    </main>
    <footer className="site-footer"><span>© {new Date().getFullYear()} ChimpChat</span><span>Made for better conversations.</span></footer>
  </div>
}
