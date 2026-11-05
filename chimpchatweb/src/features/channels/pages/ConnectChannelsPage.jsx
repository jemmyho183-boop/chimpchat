import { useState } from "react";
import { useSearchParams } from "react-router-dom";
import { BrandHeader } from "../../../shared/components/BrandHeader";
import { Button } from "../../../shared/components/Button";
import { PlatformMark } from "../../../shared/components/PlatformMark";
import { useToast } from "../../../shared/components/toast-context";

const channelErrors = {
  instagram_not_configured:
    "Instagram connection is not configured yet. Contact your workspace administrator.",
  instagram_login_expired:
    "Your Instagram sign-in expired. Start the connection again.",
  instagram_login_cancelled:
    "Instagram connection was cancelled. You can try again whenever you’re ready.",
  instagram_code_missing:
    "Instagram did not complete the connection. Please try again.",
  instagram_connection_failed:
    "We could not save the Instagram connection. Please try again.",
  token_encryption_unavailable: "Secure channel storage is not configured yet.",
  channel_credentials_unavailable:
    "The saved Instagram connection needs attention. Please connect Instagram again.",
  connection_not_found:
    "That channel connection is no longer available. Refresh and try again.",
};

export function ConnectChannelsPage({ bootstrap }) {
  const [params] = useSearchParams();
  const [connecting, setConnecting] = useState(false);
  const { notify } = useToast();
  const error = channelErrors[params.get("channel_error")];
  const instagram = bootstrap.channels.find(
    (channel) => channel.provider === "INSTAGRAM",
  );
  const instagramReady =
    instagram?.status === "NOT_CONNECTED" || instagram?.status === "CONNECTED";

  function connectInstagram() {
    if (!instagramReady) {
      notify("Instagram is not ready to connect yet.", "error");
      return;
    }
    setConnecting(true);
    window.location.assign("/api/v1/channels/instagram/oauth/start");
  }

  return (
    <div className="app-shell">
      <BrandHeader caption={bootstrap.workspace.name} />
      <main className="onboarding-layout channels-layout">
        <div className="onboarding-heading">
          <p className="eyebrow">STEP 1 OF 2 · WORKSPACE SETUP</p>
          <h1>Connect your channels</h1>
          <p className="intro-copy">
            Bring your customer conversations together in one inbox.
          </p>
        </div>

        {error && (
          <p className="inline-notice notice-error page-notice" role="alert">
            {error}
          </p>
        )}

        <div className="channel-options">
          <section className="channel-card channel-card-active">
            <div className="channel-card-top">
              <span className="channel-icon-wrap">
                <PlatformMark platform="INSTAGRAM" />
              </span>
              <span className="availability availability-ready">AVAILABLE</span>
            </div>
            <h2>Instagram</h2>
            <p>Comments and DMs, together.</p>
            <Button
              type="button"
              onClick={connectInstagram}
              disabled={connecting}
            >
              {connecting
                ? "Opening Instagram…"
                : instagramReady
                  ? "Connect Instagram"
                  : "Setup required"}
              {!connecting && (
                <span className="button-arrow" aria-hidden="true">
                  ↗
                </span>
              )}
            </Button>
          </section>

          <section className="channel-card channel-card-future">
            <div className="channel-card-top">
              <span className="channel-icon-wrap">
                <PlatformMark platform="WHATSAPP" />
              </span>
              <span className="availability">COMING SOON</span>
            </div>
            <h2>WhatsApp</h2>
            <p>WhatsApp Business conversations, all in one place.</p>
            <Button type="button" variant="disabled" disabled>
              Coming soon
            </Button>
          </section>

          <section className="channel-card channel-card-future">
            <div className="channel-card-top">
              <span className="channel-icon-wrap">
                <PlatformMark platform="FACEBOOK" />
              </span>
              <span className="availability">COMING SOON</span>
            </div>
            <h2>Facebook</h2>
            <p>Facebook Page conversations in your shared inbox.</p>
            <Button type="button" variant="disabled" disabled>
              Coming soon
            </Button>
          </section>
        </div>
      </main>
      <footer className="site-footer">
        <span>© {new Date().getFullYear()} ChimpChat</span>
        <span>Made for better conversations.</span>
      </footer>
    </div>
  );
}
