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

export function ConnectedChannelsList({ channels }) {
  const connectedChannels = channels.filter(
    (channel) => channel.status === "CONNECTED",
  );

  return (
    <div className="connected-list">
      {connectedChannels.flatMap((channel) =>
        channel.accounts.map((account) => (
          <div
            className="connected-account"
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
          </div>
        )),
      )}
    </div>
  );
}
