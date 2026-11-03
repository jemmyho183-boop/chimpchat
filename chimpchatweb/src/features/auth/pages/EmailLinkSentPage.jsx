import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { apiPost } from '../../../shared/api/client'
import { BrandHeader } from '../../../shared/components/BrandHeader'
import { Button } from '../../../shared/components/Button'

export function EmailLinkSentPage() {
  const [params] = useSearchParams()
  const [sending, setSending] = useState(false)
  const [message, setMessage] = useState('')
  const email = params.get('email') || ''

  async function resend() {
    setSending(true)
    setMessage('')
    try {
      await apiPost('/api/v1/auth/email-links', { email })
      setMessage('If email sign-in is available, a fresh link will arrive shortly.')
    } catch (error) {
      setMessage(error.message)
    } finally {
      setSending(false)
    }
  }

  return <div className="app-shell">
    <BrandHeader caption="CHECK YOUR INBOX" />
    <main className="center-layout">
      <section className="message-panel">
        <div className="card-icon" aria-hidden="true">✉</div>
        <p className="eyebrow">ONE LINK, THEN YOU’RE IN</p>
        <h1>Check your email.</h1>
        <p className="message-copy">We sent a secure sign-in link to <strong>{email || 'your email address'}</strong>. It expires in 15 minutes and works once.</p>
        <Button type="button" onClick={resend} disabled={!email || sending}>
          {sending ? 'Sending…' : 'Send a new link'}
        </Button>
        {message && <p className="inline-notice" role="status">{message}</p>}
        <Link className="text-link" to="/signin">Use a different email</Link>
      </section>
    </main>
    <footer className="site-footer"><span>© {new Date().getFullYear()} ChimpChat</span><span>Made for better conversations.</span></footer>
  </div>
}
