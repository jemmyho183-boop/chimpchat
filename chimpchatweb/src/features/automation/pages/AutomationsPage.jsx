import { useEffect, useState } from "react";
import { Link, Navigate, useNavigate, useSearchParams } from "react-router-dom";
import { DashboardHeader } from "../../dashboard/components/DashboardHeader";
import { DashboardSidebar } from "../../dashboard/components/DashboardSidebar";
import { authService } from "../../../shared/services/auth-service";
import { useToast } from "../../../shared/components/toast-context";
import { instagramCommentAutomationService } from "../services/instagram-comment-automation-service";

const navigationItems = [
  { label: "Home", to: "/dashboard", icon: "home", end: true },
  { label: "Automations", to: "/dashboard/automations", icon: "automation", end: true },
];

export function AutomationsPage({ bootstrap }) {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const { notify } = useToast();
  const [automations, setAutomations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const instagram = bootstrap.channels.find((item) => item.provider === "INSTAGRAM");
  const account = instagram?.accounts.find((item) => item.id === params.get("account")) || instagram?.accounts[0];

  useEffect(() => {
    if (!account) return;
    const controller = new AbortController();
    setLoading(true);
    instagramCommentAutomationService.list({ accountId: account.id, signal: controller.signal })
      .then(setAutomations)
      .catch((requestError) => {
        if (!controller.signal.aborted) setError(requestError.message);
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [account?.id]);

  if (instagram?.status !== "CONNECTED" || !account) return <Navigate to="/connect-channels" replace />;

  async function signOut() {
    try { await authService.logout(); navigate("/signin", { replace: true }); }
    catch (requestError) { setError(requestError.message); notify(requestError.message, "error"); }
  }

  async function removeAutomation(automation) {
    if (!window.confirm(`Delete “${automation.mediaLabel}” automation?`)) return;
    try {
      await instagramCommentAutomationService.delete(automation.id);
      setAutomations((items) => items.filter((item) => item.id !== automation.id));
      notify("Automation deleted.");
    } catch (requestError) { setError(requestError.message); }
  }

  return (
    <div className="app-shell dashboard-shell">
      <DashboardHeader user={bootstrap.user} />
      <DashboardSidebar items={navigationItems} onSignOut={signOut} />
      <main className="dashboard-content">
        <div className="dashboard-page-content automations-page-content">
          <div className="automations-page-heading">
            <div>
              <p className="eyebrow">WORKSPACE AUTOMATIONS</p>
              <h1>My Automations</h1>
              <p className="automations-account">Instagram account: @{account.username}</p>
            </div>
            <Link className="button automations-new-button" to={`/dashboard/automations/instagram-comments?account=${encodeURIComponent(account.id)}`}>
              <span aria-hidden="true">+</span> New Automation
            </Link>
          </div>
          {error && <p className="inline-notice notice-error" role="alert">{error}</p>}
          <div className="automation-list-toolbar">
            <span>{automations.length} automation{automations.length === 1 ? "" : "s"}</span>
            <span>Active and paused flows</span>
          </div>
          {loading && <p className="dashboard-empty">Loading automations…</p>}
          {!loading && automations.length === 0 && (
            <div className="automation-empty-card">
              <h2>No automations yet</h2>
              <p>Create your first automation to start turning Instagram comments into private DMs.</p>
              <Link className="button" to={`/dashboard/automations/instagram-comments?account=${encodeURIComponent(account.id)}`}>Create automation</Link>
            </div>
          )}
          <div className="automation-directory-list">
            {automations.map((automation) => (
              <article className="automation-directory-card" key={automation.id}>
                <div className="automation-directory-main">
                  <span className={`automation-directory-status${automation.enabled ? " is-active" : ""}`}>
                    {automation.enabled ? "Active" : "Paused"}
                  </span>
                  <div>
                    <h2>Auto-DM links from comments</h2>
                    <p>{automation.mediaLabel}</p>
                    <span className="automation-directory-trigger">
                      {automation.matchType === "KEYWORD" ? `Comment contains “${automation.keyword}”` : "Any comment on this post"}
                    </span>
                  </div>
                </div>
                <div className="automation-directory-actions">
                  <Link className="text-button" to={`/dashboard/automations/instagram-comments?account=${encodeURIComponent(account.id)}&edit=${encodeURIComponent(automation.id)}`}>Edit</Link>
                  <button className="text-button text-button-danger" type="button" onClick={() => removeAutomation(automation)}>Delete</button>
                </div>
              </article>
            ))}
          </div>
        </div>
      </main>
    </div>
  );
}
