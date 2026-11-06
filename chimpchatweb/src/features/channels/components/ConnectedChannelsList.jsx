import { PlatformMark } from "../../../shared/components/PlatformMark";

const channelDescriptions = {
  INSTAGRAM: "Instagram professional account",
};

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
            <span className="channel-icon-wrap">
              <PlatformMark platform={channel.provider} />
            </span>
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
