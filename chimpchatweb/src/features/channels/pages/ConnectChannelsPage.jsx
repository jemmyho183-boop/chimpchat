import { useState } from "react";
import { useSearchParams } from "react-router-dom";
import { BrandHeader } from "../../../shared/components/BrandHeader";
import { Button } from "../../../shared/components/Button";
import { PlatformMark } from "../../../shared/components/PlatformMark";
import { useToast } from "../../../shared/components/toast-context";
import { ConnectedChannelsList } from "../components/ConnectedChannelsList";

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
  const [showCatalog, setShowCatalog] = useState(
    !bootstrap.channels.some((channel) => channel.status === "CONNECTED"),
  );
  const { notify } = useToast();
  const error = channelErrors[params.get("channel_error")];
  const connectedChannels = bootstrap.channels.filter(
    (channel) => channel.status === "CONNECTED",
  );
  const connectedAccountCount = connectedChannels.reduce(
    (count, channel) => count + channel.accounts.length,
    0,
  );
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
          <p className="eyebrow">WORKSPACE CHANNELS</p>
          <h1>
            {showCatalog ? "Connect your channels" : "Your connected channels"}
          </h1>
          <p className="intro-copy">
            {showCatalog
              ? "Bring your customer conversations together in one inbox."
              : "Manage the channels connected to your workspace."}
          </p>
        </div>

        {error && (
          <p className="inline-notice notice-error page-notice" role="alert">
            {error}
          </p>
        )}

        {showCatalog ? (
          <>
            {connectedChannels.length > 0 && (
              <div className="channel-page-actions">
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => setShowCatalog(false)}
                >
                  View connected channels ({connectedAccountCount})
                </Button>
              </div>
            )}
            <div className="channel-options">
              <section className="channel-card channel-card-active">
                <div className="channel-card-top">
                  <span className="channel-icon-wrap">
                    <PlatformMark platform="INSTAGRAM" />
                  </span>
                  <span className="availability availability-ready">
                    AVAILABLE
                  </span>
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
                <p>WhatsApp Business conversations, all in one inbox.</p>
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
          </>
        ) : (
          <section className="dashboard-section" aria-labelledby="channels-heading">
            <div className="section-heading">
              <div>
                <p className="eyebrow">CONNECTED CHANNELS</p>
                <h2 id="channels-heading">Your conversations</h2>
              </div>
              <span className="count-label">
                {connectedAccountCount}{" "}
                {connectedAccountCount === 1 ? "account" : "accounts"}
              </span>
            </div>
            <ConnectedChannelsList channels={connectedChannels} />
            <div className="dashboard-actions">
              <p>Connect another channel to bring more conversations together.</p>
              <Button type="button" onClick={() => setShowCatalog(true)}>
                Connect another channel
              </Button>
            </div>
          </section>
        )}
      </main>
      <footer className="site-footer">
        <span>© {new Date().getFullYear()} ChimpChat</span>
        <span>Made for better conversations.</span>
      </footer>
    </div>
  );
}
