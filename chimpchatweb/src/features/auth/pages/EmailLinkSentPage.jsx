import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { authService } from "../../../shared/services/auth-service";
import { BrandHeader } from "../../../shared/components/BrandHeader";
import { Button } from "../../../shared/components/Button";

export function EmailLinkSentPage() {
  const [params] = useSearchParams();
  const [sending, setSending] = useState(false);
  const [cooldown, setCooldown] = useState(0);
  const [message, setMessage] = useState("");
  const email = params.get("email") || "";

  useEffect(() => {
    if (cooldown <= 0) return undefined;
    const timer = window.setInterval(
      () => setCooldown((value) => Math.max(0, value - 1)),
      1000,
    );
    return () => window.clearInterval(timer);
  }, [cooldown]);

  async function resend() {
    if (!email || sending || cooldown > 0) return;
    setSending(true);
    setMessage("");
    try {
      await authService.requestMagicLink(email);
      setCooldown(30);
      setMessage(
        "If email sign-in is available, a fresh link will arrive shortly.",
      );
    } catch (error) {
      setMessage(error.message);
    } finally {
      setSending(false);
    }
  }

  return (
    <div className="app-shell">
      <BrandHeader caption="CHECK YOUR INBOX" />
      <main className="center-layout">
        <section className="message-panel">
          <div className="card-icon" aria-hidden="true">
            ✉
          </div>
          <p className="eyebrow">ONE LINK, THEN YOU’RE IN</p>
          <h1>Check your email.</h1>
          <p className="message-copy">
            We sent a secure sign-in link to{" "}
            <strong>{email || "your email address"}</strong>. It expires in 15
            minutes and works once.
          </p>
          <Button
            type="button"
            onClick={resend}
            disabled={!email || sending || cooldown > 0}
          >
            {sending ? "Sending…" : "Send a new link"}
          </Button>
          {message && (
            <p className="inline-notice" role="status">
              {message}
            </p>
          )}
          <Button
            as={Link}
            variant="secondary"
            className="auth-secondary-button"
            to="/signin"
          >
            Use a different email
          </Button>
        </section>
      </main>
      <footer className="site-footer">
        <span>© {new Date().getFullYear()} ChimpChat</span>
        <span>Made for better conversations.</span>
      </footer>
    </div>
  );
}
