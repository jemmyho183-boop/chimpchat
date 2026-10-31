import { StrictMode, useEffect, useState } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'

function App() {
  const [message, setMessage] = useState('')
  const [connecting, setConnecting] = useState(false)
  useEffect(() => {
    const params = new URLSearchParams(window.location.search)
    const username = params.get('instagram_username')
    if (username) setMessage(`Connected as @${username}`)
    if (params.get('oauth_error')) setMessage(params.get('oauth_error'))
    if (params.has('instagram_username') || params.has('oauth_error')) window.history.replaceState({}, '', '/')
  }, [])
  const connectInstagram = () => {
    setConnecting(true)
    window.location.href = '/api/auth/instagram'
  }
  return <div className="app-shell">
    <section className="marketing-panel"><div className="logo">C</div><p className="eyebrow">CHIMPCHAT AUTOMATION</p><h1>Turn every conversation into a customer.</h1><p className="hero-copy">Manage Instagram comments, DMs, and customer conversations from one calm, intelligent workspace.</p><div className="features"><span><b>01</b> Auto-reply to comments</span><span><b>02</b> Send smart Instagram DMs</span><span><b>03</b> Grow conversations on autopilot</span></div></section>
    <main className="login-panel"><div className="login-card"><div className="logo mobile-logo">C</div><p className="eyebrow">WELCOME BACK</p><h2>Sign in to ChimpChat</h2><p className="subtitle">Connect your Instagram professional account.</p><div className="buttons"><button onClick={connectInstagram} disabled={connecting}><span className="instagram">◎</span>{connecting ? 'Connecting…' : 'Continue with Instagram'}</button></div>{message && <p className="status" role="status">{message}</p>}<p className="privacy">We only request the permissions needed to manage your Instagram conversations.</p><p className="terms">By continuing, you agree to our <a href="#terms">Terms of Service</a> and <a href="#privacy">Privacy Policy</a>.</p></div></main>
  </div>
}

createRoot(document.getElementById('root')).render(<StrictMode><App /></StrictMode>)
