import { useEffect, useState } from "react";
import {
  BrowserRouter,
  Navigate,
  Route,
  Routes,
  useLocation,
} from "react-router-dom";
import { ApiError } from "../shared/api/client";
import { appService } from "../shared/services/app-service";
import { ConnectChannelsPage } from "../features/channels/pages/ConnectChannelsPage";
import { DashboardPage } from "../features/dashboard/pages/DashboardPage";
import { InstagramCommentAutomationPage } from "../features/automation/pages/InstagramCommentAutomationPage";
import { EmailLinkSentPage } from "../features/auth/pages/EmailLinkSentPage";
import { SignInPage } from "../features/auth/pages/SignInPage";
import { VerifyEmailPage } from "../features/auth/pages/VerifyEmailPage";
import { OAuthPopupResultPage } from "../features/auth/pages/OAuthPopupResultPage";
import { Button } from "../shared/components/Button";
import { ToastProvider } from "../shared/components/Toast";

function LoadingPage({ message = "Getting your workspace ready…" }) {
  return (
    <main className="loading-page" aria-live="polite">
      <span className="loading-mark" aria-hidden="true" />
      {message}
    </main>
  );
}

function LoadErrorPage({ onRetry }) {
  return (
    <main className="loading-page error-page">
      <p className="eyebrow">TEMPORARILY UNAVAILABLE</p>
      <h1>We couldn’t load your workspace.</h1>
      <Button type="button" onClick={onRetry}>
        Try again
      </Button>
    </main>
  );
}

function useBootstrap() {
  const [bootstrap, setBootstrap] = useState(null);
  const [loading, setLoading] = useState(true);
  const [failed, setFailed] = useState(false);
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    let active = true;
    appService
      .getBootstrap()
      .then((response) => {
        if (active) setBootstrap(response);
      })
      .catch((error) => {
        if (!active) return;
        if (error instanceof ApiError && error.status === 401) {
          setBootstrap(false);
        } else {
          setFailed(true);
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [attempt]);

  return {
    bootstrap,
    loading,
    failed,
    retry: () => {
      setFailed(false);
      setLoading(true);
      setAttempt((value) => value + 1);
    },
  };
}

function HomeRoute() {
  const { bootstrap, loading, failed, retry } = useBootstrap();
  const location = useLocation();
  const params = new URLSearchParams(location.search);

  if (loading) return <LoadingPage />;
  if (failed) return <LoadErrorPage onRetry={retry} />;
  if (!bootstrap) return <Navigate to={`/signin${location.search}`} replace />;
  if (params.has("channel_error")) {
    return <Navigate to={`/connect-channels${location.search}`} replace />;
  }
  if (params.has("channel_connected")) {
    return <Navigate to={`/dashboard${location.search}`} replace />;
  }
  return <Navigate to="/connect-channels" replace />;
}

function AuthenticatedRoute({ children, requireConnected = false }) {
  const { bootstrap, loading, failed, retry } = useBootstrap();
  const location = useLocation();

  if (loading) return <LoadingPage />;
  if (failed) return <LoadErrorPage onRetry={retry} />;
  if (!bootstrap) return <Navigate to={`/signin${location.search}`} replace />;

  const connected = bootstrap.nextStep === "DASHBOARD";
  if (requireConnected && !connected)
    return <Navigate to="/connect-channels" replace />;
  return children(bootstrap);
}

function SignedOutRoute({ children }) {
  const { bootstrap, loading, failed, retry } = useBootstrap();
  if (loading) return <LoadingPage />;
  if (failed) return <LoadErrorPage onRetry={retry} />;
  if (bootstrap) return <Navigate to="/" replace />;
  return children;
}

export function App() {
  return (
    <ToastProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<HomeRoute />} />
          <Route
            path="/signin"
            element={
              <SignedOutRoute>
                <SignInPage />
              </SignedOutRoute>
            }
          />
          <Route path="/email-sent" element={<EmailLinkSentPage />} />
          <Route path="/auth/verify" element={<VerifyEmailPage />} />
          <Route path="/auth/popup-result" element={<OAuthPopupResultPage />} />
          <Route
            path="/connect-channels"
            element={
              <AuthenticatedRoute>
                {(bootstrap) => <ConnectChannelsPage bootstrap={bootstrap} />}
              </AuthenticatedRoute>
            }
          />
          <Route
            path="/dashboard"
            element={
              <AuthenticatedRoute requireConnected>
                {(bootstrap) => <DashboardPage bootstrap={bootstrap} />}
              </AuthenticatedRoute>
            }
          />
          <Route
            path="/dashboard/automations/instagram-comments"
            element={
              <AuthenticatedRoute requireConnected>
                {(bootstrap) => (
                  <InstagramCommentAutomationPage bootstrap={bootstrap} />
                )}
              </AuthenticatedRoute>
            }
          />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </ToastProvider>
  );
}
