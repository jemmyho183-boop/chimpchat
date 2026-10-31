import { StrictMode, useEffect, useState } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'

function App() {
  const [message, setMessage] = useState('')
  useEffect(() => {
    const params = new URLSearchParams(window.location.search)
    if (params.get('auth') === 'success') setMessage('Your account is connected. Welcome to ChimpChat!')
    if (params.get('oauth_error')) setMessage(params.get('oauth_error'))
    if (params.has('auth') || params.has('oauth_error')) window.history.replaceState({}, '', '/')
  }, [])
  const connect = (provider) => {
    setMessage(`Connecting to ${provider}…`)
    window.location.href = `/api/auth/${provider}`
  }
  return <div className="app-shell">
    <section className="marketing-panel"><div className="logo">C</div><p className="eyebrow">CHIMPCHAT AUTOMATION</p><h1>Turn every conversation into a customer.</h1><p className="hero-copy">Manage Instagram comments, DMs, and customer conversations from one calm, intelligent workspace.</p><div className="features"><span><b>01</b> Auto-reply to comments</span><span><b>02</b> Send smart Instagram DMs</span><span><b>03</b> Grow conversations on autopilot</span></div></section>
    <main className="login-panel"><div className="login-card"><div className="logo mobile-logo">C</div><p className="eyebrow">WELCOME BACK</p><h2>Sign in to ChimpChat</h2><p className="subtitle">Connect your social account to get started.</p><div className="buttons"><button onClick={() => connect('instagram')}><span className="instagram">◎</span> Continue with Instagram</button><button onClick={() => connect('facebook')}><span className="facebook">f</span> Continue with Facebook</button></div>{message && <p className="status">{message}</p>}<p className="privacy">We only request the permissions needed to manage your connected pages and conversations.</p><p className="terms">By continuing, you agree to our <a href="#terms">Terms of Service</a> and <a href="#privacy">Privacy Policy</a>.</p></div></main>
  </div>
}

createRoot(document.getElementById('root')).render(<StrictMode><App /></StrictMode>)
