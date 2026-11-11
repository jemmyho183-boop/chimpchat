import { useState } from "react";
import { PlatformMark } from "../../../shared/components/PlatformMark";

const channelDescriptions = {
  INSTAGRAM: "Instagram professional account",
};

function ConnectedAccountAvatar({ account, platform }) {
  const [failedImageUrl, setFailedImageUrl] = useState(null);

  if (
    account.profilePictureUrl &&
    failedImageUrl !== account.profilePictureUrl
  ) {
    return (
      <img
        className="connected-account-avatar"
        src={account.profilePictureUrl}
        alt=""
        aria-hidden="true"
        loading="lazy"
        onError={() => setFailedImageUrl(account.profilePictureUrl)}
      />
    );
  }

  return (
    <span className="channel-icon-wrap connected-account-avatar-fallback">
      <PlatformMark platform={platform} />
    </span>
  );
}

export function ConnectedChannelsList({ channels, onSelectAccount }) {
  const connectedChannels = channels.filter(
    (channel) => channel.status === "CONNECTED",
  );

  return (
    <div className="connected-list">
      {connectedChannels.flatMap((channel) =>
        channel.accounts.map((account) => (
          <div
            className={`connected-account${
              onSelectAccount ? " connected-account-selectable" : ""
            }`}
            key={`${channel.provider}-${account.id}`}
          >
            <ConnectedAccountAvatar
              account={account}
              platform={channel.provider}
            />
            <div className="connected-account-info">
              <strong>
                {channel.provider === "INSTAGRAM"
                  ? `@${account.username}`
                  : account.username}
              </strong>
              <span>
                {channelDescriptions[channel.provider] ?? "Connected account"}
              </span>
            </div>
            <span className="connected-status">
              <span aria-hidden="true" />
              Connected
            </span>
            {onSelectAccount && (
              <button
                className="button button-secondary connected-account-select"
                type="button"
                aria-label={`Open ${channel.provider} dashboard for ${account.username}`}
                onClick={() => onSelectAccount(channel, account)}
              >
                Open dashboard
                <span className="button-arrow" aria-hidden="true">
                  ↗
                </span>
              </button>
            )}
          </div>
        )),
      )}
    </div>
  );
}
