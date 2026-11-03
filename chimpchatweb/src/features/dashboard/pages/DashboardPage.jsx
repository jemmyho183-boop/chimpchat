import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { apiDelete, apiPost } from '../../../shared/api/client'
import { BrandHeader } from '../../../shared/components/BrandHeader'
import { useToast } from '../../../shared/components/toast-context'

export function DashboardPage({ bootstrap }) {
  const [params] = useSearchParams()
  const [disconnecting, setDisconnecting] = useState(false)
  const [error, setError] = useState('')
  const { notify } = useToast()
  const navigate = useNavigate()
  const instagram = bootstrap.channels.find((channel) => channel.provider === 'INSTAGRAM')
  const justConnected = params.get('channel_connected') === 'instagram'

  if (!instagram) {
    return <main className="loading-page error-page">
      <p className="eyebrow">WORKSPACE SETUP</p>
      <h1>We could not load your channel.</h1>
      <p className="intro-copy">Refresh the page to try again. If the problem continues, contact your workspace administrator.</p>
      <button className="button button-primary" type="button" onClick={() => window.location.reload()}>Refresh</button>
    </main>
  }

  async function disconnectInstagram() {
    if (!instagram?.connectionId || !window.confirm('Disconnect Instagram from this workspace?')) return
    setDisconnecting(true)
    setError('')
    try {
      await apiDelete(`/api/v1/channels/${instagram.connectionId}`)
      notify('Instagram has been disconnected from your workspace.')
      navigate('/connect-channels', { replace: true })
    } catch (requestError) {
      setError(requestError.message)
      notify(requestError.message, 'error')
    } finally {
      setDisconnecting(false)
    }
  }

  async function signOut() {
    try {
      await apiPost('/api/v1/auth/logout', {})
      navigate('/signin', { replace: true })
    } catch (requestError) {
      setError(requestError.message)
      notify(requestError.message, 'error')
    }
  }

  return <div className="app-shell">
    <BrandHeader caption={bootstrap.workspace.name}>
      <div className="header-account">
        <span className="account-email">{bootstrap.user.email}</span>
        <button className="text-button" type="button" onClick={signOut}>Sign out</button>
      </div>
    </BrandHeader>

    <main className="dashboard-layout">
      <div className="dashboard-heading">
        <p className="eyebrow">YOUR WORKSPACE</p>
        <h1>Good conversations start here.</h1>
        <p className="intro-copy">Your ChimpChat workspace is ready. Connected channels will appear here as you bring them on.</p>
      </div>

      {justConnected && <p className="inline-notice notice-success page-notice" role="status">Instagram is connected to your workspace.</p>}
      {error && <p className="inline-notice notice-error page-notice" role="alert">{error}</p>}

      <section className="dashboard-section" aria-labelledby="channels-heading">
        <div className="section-heading">
          <div>
            <p className="eyebrow">CONNECTED CHANNELS</p>
            <h2 id="channels-heading">Your conversations</h2>
          </div>
          <span className="count-label">{instagram.accounts.length} {instagram.accounts.length === 1 ? 'account' : 'accounts'}</span>
        </div>
        <div className="connected-list">
          {instagram.accounts.map((account) => (
            <div className="connected-account" key={account.id}>
              <span className="channel-icon-wrap"><span className="channel-icon-text">◎</span></span>
              <div className="connected-account-info">
                <strong>@{account.username}</strong>
                <span>Instagram professional account</span>
              </div>
              <span className="connected-status"><span aria-hidden="true" /> Connected</span>
            </div>
          ))}
        </div>
        <div className="dashboard-actions">
          <p>More ways to connect are on the way.</p>
          <button className="text-button text-button-danger" type="button" onClick={disconnectInstagram} disabled={disconnecting}>
            {disconnecting ? 'Disconnecting…' : 'Disconnect Instagram'}
          </button>
        </div>
      </section>
    </main>

    <footer className="site-footer"><span>© {new Date().getFullYear()} ChimpChat</span><span>Made for better conversations.</span></footer>
  </div>
}
