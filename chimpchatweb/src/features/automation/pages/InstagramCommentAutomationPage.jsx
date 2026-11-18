import { useState } from "react";
import { Navigate, useNavigate, useSearchParams } from "react-router-dom";
import { DashboardHeader } from "../../dashboard/components/DashboardHeader";
import { DashboardSidebar } from "../../dashboard/components/DashboardSidebar";
import { InstagramCommentAutomationBuilder } from "../components/InstagramCommentAutomationBuilder";
import { authService } from "../../../shared/services/auth-service";
import { useToast } from "../../../shared/components/toast-context";

const navigationItems = [
  { label: "Home", to: "/dashboard", icon: "home", end: true },
  { label: "Automations", to: "/dashboard/automations/instagram-comments" },
];

export function InstagramCommentAutomationPage({ bootstrap }) {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const { notify } = useToast();
  const [error, setError] = useState("");
  const instagram = bootstrap.channels.find(
    (channel) => channel.provider === "INSTAGRAM",
  );
  const account =
    instagram?.accounts.find((item) => item.id === params.get("account")) ||
    instagram?.accounts[0];

  if (instagram?.status !== "CONNECTED" || !account) {
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

  return (
    <div className="app-shell dashboard-shell">
      <DashboardHeader user={bootstrap.user} />
      <DashboardSidebar items={navigationItems} onSignOut={signOut} />
      <main className="dashboard-content">
        <div className="dashboard-pagebar instagram-automation-pagebar">
          <div>
            <p className="eyebrow">{bootstrap.workspace.name}</p>
            <h1>Auto-DM links from comments</h1>
          </div>
        </div>
        <div className="dashboard-page-content instagram-automation-page-content">
          {error && (
            <p className="inline-notice notice-error page-notice" role="alert">
              {error}
            </p>
          )}
          <InstagramCommentAutomationBuilder account={account} key={account.id} />
        </div>
      </main>
    </div>
  );
}
