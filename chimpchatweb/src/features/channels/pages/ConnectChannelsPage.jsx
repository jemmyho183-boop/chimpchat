import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { BrandHeader } from '../../../shared/components/BrandHeader'

const channelErrors = {
  instagram_not_configured: 'Instagram connection is not configured yet. Contact your workspace administrator.',
  instagram_login_expired: 'Your Instagram sign-in expired. Start the connection again.',
  instagram_login_cancelled: 'Instagram connection was cancelled. You can try again whenever you’re ready.',
  instagram_code_missing: 'Instagram did not complete the connection. Please try again.',
  instagram_connection_failed: 'We could not save the Instagram connection. Please try again.',
  token_encryption_unavailable: 'Secure channel storage is not configured yet.',
  channel_credentials_unavailable: 'The saved Instagram connection needs attention. Please connect Instagram again.',
  connection_not_found: 'That channel connection is no longer available. Refresh and try again.',
}

function InstagramMark() {
  return <svg className="channel-icon" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <rect x="3.5" y="3.5" width="17" height="17" rx="5.5" />
    <circle cx="12" cy="12" r="4" />
    <circle className="icon-dot" cx="17.5" cy="6.8" r="1" />
  </svg>
}

export function ConnectChannelsPage({ bootstrap }) {
  const [params] = useSearchParams()
  const [connecting, setConnecting] = useState(false)
  const error = channelErrors[params.get('channel_error')]
  const instagram = bootstrap.channels.find((channel) => channel.provider === 'INSTAGRAM')
  const instagramReady = instagram?.status === 'NOT_CONNECTED' || instagram?.status === 'CONNECTED'

  function connectInstagram() {
    setConnecting(true)
    window.location.assign('/api/v1/channels/instagram/oauth/start')
  }

  return <div className="app-shell">
    <BrandHeader caption="YOUR WORKSPACE" />
    <main className="onboarding-layout">
      <div className="onboarding-heading">
        <p className="eyebrow">STEP 1 OF 2 <span className="step-divider">/</span> CHANNEL SETUP</p>
        <h1>Bring your conversations together.</h1>
        <p className="intro-copy">Connect a channel to finish setting up your workspace. You can add more channels as ChimpChat grows.</p>
      </div>

      {error && <p className="inline-notice notice-error page-notice" role="alert">{error}</p>}

      <div className="channel-grid">
        <section className="channel-card channel-card-active">
          <div className="channel-card-top">
            <span className="channel-icon-wrap"><InstagramMark /></span>
            <span className="availability availability-ready">AVAILABLE NOW</span>
          </div>
          <h2>Instagram</h2>
          <p>Manage professional account comments and direct messages from one workspace.</p>
          <button className="button button-primary" type="button" onClick={connectInstagram} disabled={!instagramReady || connecting}>
            {connecting ? 'Opening Instagram…' : instagramReady ? 'Connect Instagram' : 'Setup required'}
            {!connecting && <span className="button-arrow" aria-hidden="true">↗</span>}
          </button>
          {!instagramReady && (
            <p className="channel-help">Instagram credentials and secure token storage must be configured before connecting.</p>
          )}
        </section>

        <section className="channel-card channel-card-future" aria-label="WhatsApp, coming soon">
          <div className="channel-card-top">
            <span className="channel-icon-wrap channel-icon-text" aria-hidden="true">W</span>
            <span className="availability">COMING SOON</span>
          </div>
          <h2>WhatsApp</h2>
          <p>Keep customer conversations moving across WhatsApp Business.</p>
          <button className="button button-disabled" type="button" disabled>Coming soon</button>
        </section>

        <section className="channel-card channel-card-future" aria-label="Facebook, coming soon">
          <div className="channel-card-top">
            <span className="channel-icon-wrap channel-icon-text" aria-hidden="true">f</span>
            <span className="availability">COMING SOON</span>
          </div>
          <h2>Facebook</h2>
          <p>Bring your Facebook Page messages into the same shared inbox.</p>
          <button className="button button-disabled" type="button" disabled>Coming soon</button>
        </section>
      </div>
      <p className="onboarding-footnote">Your channels belong to <strong>{bootstrap.workspace.name}</strong>. You can disconnect them at any time.</p>
    </main>
    <footer className="site-footer"><span>© {new Date().getFullYear()} ChimpChat</span><span>Made for better conversations.</span></footer>
  </div>
}
