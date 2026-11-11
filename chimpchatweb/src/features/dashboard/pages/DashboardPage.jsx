import { useEffect, useState } from "react";
import {
  Link,
  Navigate,
  useNavigate,
  useSearchParams,
} from "react-router-dom";
import { DashboardHeader } from "../components/DashboardHeader";
import { DashboardSidebar } from "../components/DashboardSidebar";
import { ConnectedChannelsList } from "../../channels/components/ConnectedChannelsList";
import { authService } from "../../../shared/services/auth-service";
import { channelService } from "../../../shared/services/channel-service";
import { useToast } from "../../../shared/components/toast-context";

const navigationItems = [{ label: "Home", to: "/dashboard", icon: "home" }];

export function DashboardPage({ bootstrap }) {
  const [params] = useSearchParams();
  const [disconnecting, setDisconnecting] = useState(false);
  const [error, setError] = useState("");
  const { notify } = useToast();
  const navigate = useNavigate();
  const selectedProvider = params.get("channel");
  const selectedAccountId = params.get("account");
  const connectedChannels = bootstrap.channels.filter(
    (channel) => channel.status === "CONNECTED",
  );
  const selectedChannel = connectedChannels.find(
    (channel) => channel.provider === selectedProvider,
  );
  const selectedAccount = selectedChannel?.accounts.find(
    (account) => account.id === selectedAccountId,
  );
  const hasSelection = selectedProvider !== null || selectedAccountId !== null;
  const dashboardChannels =
    selectedChannel && selectedAccount
      ? [{ ...selectedChannel, accounts: [selectedAccount] }]
      : connectedChannels;
  const connectedAccountCount = dashboardChannels.reduce(
    (count, channel) => count + channel.accounts.length,
    0,
  );
  const instagram = bootstrap.channels.find(
    (channel) => channel.provider === "INSTAGRAM",
  );
  const justConnected = params.get("channel_connected") === "instagram";

  useEffect(() => {
    if (justConnected) {
      notify("Instagram is connected to your workspace.");
    }
  }, [justConnected, notify]);

  if (hasSelection && (!selectedChannel || !selectedAccount)) {
    return <Navigate to="/connect-channels" replace />;
  }

  async function signOut() {
    try {
      await authService.logout();
      navigate("/signin", { replace: true });
    } catch (requestError) {
      setError(requestError.message);
      notify(requestError.message, "error");
    }
  }

  async function disconnectInstagram() {
    if (
      !instagram?.connectionId ||
      !window.confirm("Disconnect Instagram from this workspace?")
    ) {
      return;
    }

    setDisconnecting(true);
    setError("");
    try {
      await channelService.disconnect(instagram.connectionId);
      notify("Instagram has been disconnected from your workspace.");
      navigate("/connect-channels", { replace: true });
    } catch (requestError) {
      setError(requestError.message);
      notify(requestError.message, "error");
    } finally {
      setDisconnecting(false);
    }
  }

  return (
    <div className="app-shell dashboard-shell">
      <DashboardHeader user={bootstrap.user} />
      <DashboardSidebar items={navigationItems} onSignOut={signOut} />

      <main className="dashboard-content">
        <div className="dashboard-pagebar">
          <div>
            <p className="eyebrow">{bootstrap.workspace.name}</p>
            <h1>Home</h1>
          </div>
          <Link className="button button-secondary" to="/connect-channels">
            Manage channels
          </Link>
        </div>

        <div className="dashboard-page-content">
          {error && (
            <p className="inline-notice notice-error page-notice" role="alert">
              {error}
            </p>
          )}

          <section
            className="dashboard-section"
            aria-labelledby="channels-heading"
          >
            <div className="section-heading">
              <div>
                <p className="eyebrow">CONNECTED CHANNELS</p>
                <h2 id="channels-heading">
                  {selectedAccount
                    ? selectedAccount.username
                    : "Your accounts"}
                </h2>
              </div>
              <span className="count-label">
                {connectedAccountCount}{" "}
                {connectedAccountCount === 1 ? "account" : "accounts"}
              </span>
            </div>
            {connectedAccountCount > 0 ? (
              <ConnectedChannelsList channels={dashboardChannels} />
            ) : (
              <p className="dashboard-empty">
                No connected accounts are available.
              </p>
            )}
            {instagram?.connectionId && (
              <div className="dashboard-actions">
                <button
                  className="text-button text-button-danger"
                  type="button"
                  onClick={disconnectInstagram}
                  disabled={disconnecting}
                >
                  {disconnecting ? "Disconnecting…" : "Disconnect Instagram"}
                </button>
              </div>
            )}
          </section>
        </div>
      </main>
    </div>
  );
}
