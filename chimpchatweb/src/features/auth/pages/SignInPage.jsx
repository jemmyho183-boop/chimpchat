import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { BrandHeader } from "../../../shared/components/BrandHeader";
import { authService } from "../../../shared/services/auth-service";
import { PlatformMark } from "../../../shared/components/PlatformMark";

const authErrors = {
  identity_link_required:
    "This email already has an account. Sign in with your email to continue.",
  google_sign_in_failed:
    "Google sign-in could not be completed. Please try again.",
  google_sign_in_unavailable:
    "Google sign-in is temporarily unavailable. Please try again later.",
};

export function SignInPage() {
  const [options, setOptions] = useState(null);
  const [email, setEmail] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  useEffect(() => {
    authService
      .getOptions()
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
      await authService.requestMagicLink(email);
      navigate(`/email-sent?email=${encodeURIComponent(email.trim())}`);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setBusy(false);
    }
  }

  const authError = authErrors[searchParams.get("auth_error")];

  return (
    <div className="app-shell">
      <BrandHeader caption="SECURE SIGN IN" />
      <main className="signin-layout">
        <section className="login-panel" aria-labelledby="login-heading">
          <div className="login-card">
            <p className="eyebrow">WELCOME BACK</p>
            <h1 id="login-heading">Sign in to ChimpChat</h1>
            <p className="signin-copy">
              Pick up where your conversations left off.
            </p>

            {options?.googleEnabled && (
              <a
                className="button button-secondary google-button"
                href="/oauth2/authorization/google"
              >
                <span className="google-mark">
                  <PlatformMark platform="GOOGLE" />
                </span>
                <span>Continue with Google</span>
              </a>
            )}

            {options?.googleEnabled && (
              <div className="separator" aria-hidden="true">
                <span>or</span>
              </div>
            )}

            {options && (
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
                  disabled={!options.emailEnabled || busy}
                  onChange={(event) => setEmail(event.target.value)}
                />
                <button
                  className="button button-primary"
                  type="submit"
                  disabled={!options.emailEnabled || busy}
                >
                  {busy ? "Sending sign-in link…" : "Continue with email"}
                  {!busy && (
                    <span className="button-arrow" aria-hidden="true">
                      ↗
                    </span>
                  )}
                </button>
              </form>
            )}

            {options && !options.emailEnabled && (
              <p className="signin-hint" role="status">
                Email sign-in is temporarily unavailable.
              </p>
            )}

            {!options && !error && (
              <p className="signin-loading" role="status">
                Loading sign-in options…
              </p>
            )}
            {options && !options.emailEnabled && !options.googleEnabled && (
              <p className="inline-notice" role="status">
                Sign-in is being set up. Please try again later.
              </p>
            )}
            {(error || authError) && (
              <p className="inline-notice notice-error" role="alert">
                {error || authError}
              </p>
            )}
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
