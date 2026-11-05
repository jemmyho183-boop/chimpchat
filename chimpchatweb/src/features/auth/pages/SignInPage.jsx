<<<<<<< Updated upstream
import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { authService } from '../../../shared/services/auth-service'
import { BrandHeader } from '../../../shared/components/BrandHeader'
import { Button } from '../../../shared/components/Button'

const authErrors = {
  identity_link_required: 'This email already has an account. Sign in with your email to continue.',
  google_sign_in_failed: 'Google sign-in could not be completed. Please try again.',
  google_sign_in_unavailable: 'Google sign-in is temporarily unavailable. Please try again later.',
}

export function SignInPage() {
  const [options, setOptions] = useState(null)
  const [email, setEmail] = useState('')
  const [emailMode, setEmailMode] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()

  useEffect(() => {
    authService.getOptions()
=======
import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { apiGet, apiPost } from "../../../shared/api/client";
import { BrandHeader } from "../../../shared/components/BrandHeader";

const authErrors = {
  identity_link_required:
    "This email already has an account. Sign in with your email to continue.",
  google_sign_in_failed:
    "Google sign-in could not be completed. Please try again.",
};

export function SignInPage() {
  const [options, setOptions] = useState(null);
  const [email, setEmail] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  useEffect(() => {
    apiGet("/api/v1/auth/options")
>>>>>>> Stashed changes
      .then(setOptions)
      .catch(() =>
        setError(
          "Sign-in options are temporarily unavailable. Please reload the page.",
        ),
      );
  }, []);

  async function submitEmail(event) {
    event.preventDefault();
    setError("");
    setBusy(true);
    try {
<<<<<<< Updated upstream
      await authService.requestMagicLink(email)
      navigate(`/email-sent?email=${encodeURIComponent(email.trim())}`)
=======
      await apiPost("/api/v1/auth/email-links", { email });
      navigate(`/email-sent?email=${encodeURIComponent(email.trim())}`);
>>>>>>> Stashed changes
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="app-shell">
      <BrandHeader />
      <main className="entry-layout">
        <section className="intro-panel">
          <p className="eyebrow">YOUR SOCIAL INBOX, IN SYNC</p>
          <h1>
            Make every <span>conversation</span> count.
          </h1>
          <p className="intro-copy">
            Turn Instagram comments and DMs into lasting customer
            relationships—without missing a moment.
          </p>
          <div
            className="benefit-list"
            aria-label="What you can do with ChimpChat"
          >
            <div className="benefit">
              <span className="benefit-number">01</span>
              <span>Keep up with every comment</span>
            </div>
            <div className="benefit">
              <span className="benefit-number">02</span>
              <span>Start thoughtful conversations in DMs</span>
            </div>
            <div className="benefit">
              <span className="benefit-number">03</span>
              <span>Give every customer a timely reply</span>
            </div>
          </div>
        </section>

        <section className="login-panel" aria-labelledby="login-heading">
          <div className="login-card">
            <h2 id="login-heading">
              A better inbox
              <br />
              starts here.
            </h2>

<<<<<<< Updated upstream
          {options?.googleEnabled && (
            <Button as="a" variant="secondary" className="google-button" href="/api/auth/google/start">
              <span className="google-mark" aria-hidden="true">G</span>
              <span>Continue with Google</span>
            </Button>
          )}

          {options?.googleEnabled && options?.emailEnabled && <div className="separator"><span>or</span></div>}

          {options?.emailEnabled && !emailMode && (
            <Button type="button" onClick={() => setEmailMode(true)}>
              Continue with email
              <span className="button-arrow" aria-hidden="true">↗</span>
            </Button>
          )}

          {options?.emailEnabled && emailMode && <form className="email-form" onSubmit={submitEmail}>
            <label htmlFor="signin-email">Email address</label>
            <input
              id="signin-email"
              name="email"
              type="email"
              autoComplete="email"
              placeholder="you@company.com"
              maxLength={320}
              required
              value={email}
              onChange={(event) => setEmail(event.target.value)}
            />
            <Button type="submit" disabled={busy}>
              {busy ? 'Sending sign-in link…' : 'Send me a sign-in link'}
              {!busy && <span className="button-arrow" aria-hidden="true">↗</span>}
            </Button>
            <button type="button" className="text-link" onClick={() => setEmailMode(false)}>Back to sign-in options</button>
          </form>}
=======
            {options?.googleEnabled && (
              <a
                className="button button-secondary google-button"
                href="/oauth2/authorization/google"
              >
                <span className="google-mark" aria-hidden="true">
                  G
                </span>
                <span>Continue with Google</span>
              </a>
            )}

            {options?.googleEnabled && options?.emailEnabled && (
              <div className="separator">
                <span>or continue with email</span>
              </div>
            )}

            <form className="email-form" onSubmit={submitEmail}>
              <label htmlFor="signin-email">Email address</label>
              <input
                id="signin-email"
                name="email"
                type="email"
                autoComplete="email"
                placeholder="you@company.com"
                maxLength={320}
                required
                value={email}
                onChange={(event) => setEmail(event.target.value)}
              />
              <button
                className="button button-primary"
                type="submit"
                disabled={!options?.emailEnabled || busy}
              >
                {busy ? "Sending sign-in link…" : "Continue with email"}
                {!busy && (
                  <span className="button-arrow" aria-hidden="true">
                    ↗
                  </span>
                )}
              </button>
            </form>
>>>>>>> Stashed changes

            {options && !options.emailEnabled && !options.googleEnabled && (
              <p className="inline-notice" role="status">
                Sign-in is being set up. Add Google OAuth or email delivery
                credentials to enable access.
              </p>
            )}
            {error && (
              <p className="inline-notice notice-error" role="alert">
                {error}
              </p>
            )}
            {authErrors[searchParams.get("auth_error")] && (
              <p className="inline-notice notice-error" role="alert">
                {authErrors[searchParams.get("auth_error")]}
              </p>
            )}
            <p className="privacy-note">
              Secure sign-in. ChimpChat never sees or stores your Google
              password.
            </p>
          </div>
        </section>
      </main>
      <footer className="site-footer">
        <span>© {new Date().getFullYear()} ChimpChat</span>
        <span>Made for better conversations.</span>
      </footer>
    </div>
  );
}
