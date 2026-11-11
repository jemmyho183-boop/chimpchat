import { useEffect } from "react";
import { useSearchParams } from "react-router-dom";

const providers = ["google", "instagram"];

export function OAuthPopupResultPage() {
  const [params] = useSearchParams();
  const provider = params.get("provider");
  const status = params.get("status");
  const error = params.get("error");
  const validResult =
    providers.includes(provider) &&
    (status === "success" || (status === "error" && error));

  useEffect(() => {
    if (!validResult || !window.opener) return;
    window.opener.postMessage(
      {
        type: "chimpchat-oauth-result",
        provider,
        status,
        error,
      },
      window.location.origin,
    );
    window.close();
  }, [error, provider, status, validResult]);

  return (
    <main className="loading-page" aria-live="polite">
      {validResult && window.opener ? (
        <>
          <span className="loading-mark" aria-hidden="true" />
          Returning to ChimpChat…
        </>
      ) : (
        <>
          <p className="eyebrow">AUTHENTICATION COMPLETE</p>
          <p>You can close this window and return to ChimpChat.</p>
        </>
      )}
    </main>
  );
}
