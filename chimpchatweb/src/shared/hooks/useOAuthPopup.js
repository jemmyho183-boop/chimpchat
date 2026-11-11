import { useCallback, useEffect, useRef, useState } from "react";
import { useToast } from "../components/toast-context";

export function useOAuthPopup() {
  const activeFlow = useRef(null);
  const [opening, setOpening] = useState(false);
  const [errorCode, setErrorCode] = useState(null);
  const { notify } = useToast();

  const finishFlow = useCallback((result) => {
    const flow = activeFlow.current;
    if (!flow) return;

    window.removeEventListener("message", flow.onMessage);
    window.clearInterval(flow.poll);
    activeFlow.current = null;
    setOpening(false);

    if (result.success) {
      flow.popup.close();
      flow.onSuccess();
      return;
    }

    if (result.errorCode) {
      setErrorCode(result.errorCode);
    } else {
      setErrorCode(null);
      notify(
        "The connection window was closed before authentication completed. Please try again.",
        "error",
      );
    }
  }, [notify]);

  const openPopup = useCallback((provider, url, onSuccess) => {
    setErrorCode(null);
    const popup = window.open(
      url,
      `chimpchat-${provider.toLowerCase()}-oauth`,
      "popup=yes,width=520,height=700,resizable=yes,scrollbars=yes",
    );

    if (!popup) {
      notify(
        "The authentication window was blocked. Allow pop-ups and try again.",
        "error",
      );
      return;
    }

    const onMessage = (event) => {
      if (
        event.origin !== window.location.origin ||
        event.source !== popup ||
        event.data?.type !== "chimpchat-oauth-result" ||
        event.data.provider !== provider
      ) {
        return;
      }

      if (event.data.status === "success") {
        finishFlow({ success: true });
      } else if (event.data.status === "error") {
        finishFlow({ errorCode: event.data.error });
      }
    };

    const poll = window.setInterval(() => {
      if (popup.closed) finishFlow({ success: false });
    }, 400);

    activeFlow.current = { popup, onMessage, onSuccess, poll };
    window.addEventListener("message", onMessage);
    setOpening(true);
  }, [finishFlow, notify]);

  useEffect(() => () => {
    const flow = activeFlow.current;
    if (!flow) return;
    window.removeEventListener("message", flow.onMessage);
    window.clearInterval(flow.poll);
    activeFlow.current = null;
  }, []);

  return { openPopup, opening, errorCode };
}
