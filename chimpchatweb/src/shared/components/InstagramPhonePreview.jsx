import { useState } from "react";

const PREVIEW_SCREENS = [
  { id: "comment", label: "Comment" },
  { id: "dm", label: "DM" },
  { id: "story", label: "Story reply" },
];

function PreviewIcon({ name }) {
  const paths = {
    heart:
      "M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1.1-1.1a5.5 5.5 0 0 0-7.8 7.8l1.1 1.1L12 21l7.8-7.5 1.1-1.1a5.5 5.5 0 0 0-.1-7.8Z",
    comment:
      "M21 11.5a8.4 8.4 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.4 8.4 0 0 1-3.8-.9L3 21l1.9-5.7a8.4 8.4 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.4 8.4 0 0 1 3.8-.9h.5a8.5 8.5 0 0 1 8 8v.5Z",
    send: "m22 2-7 20-4-9-9-4 20-7ZM22 2 11 13",
    bookmark:
      "M6 4.5A2.5 2.5 0 0 1 8.5 2h7A2.5 2.5 0 0 1 18 4.5V22l-6-4-6 4V4.5Z",
    more: "M5 12h.01M12 12h.01M19 12h.01",
  };
  const path = paths[name];

  return (
    <svg
      aria-hidden="true"
      className="instagram-phone-icon"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d={path} />
    </svg>
  );
}

function PhoneAvatar({ username, className = "" }) {
  return (
    <span className={`instagram-phone-avatar ${className}`} aria-hidden="true">
      {username?.slice(0, 1).toUpperCase() || "I"}
    </span>
  );
}

function PhoneMedia({ media, className = "" }) {
  return media?.src ? (
    <img
      className={`instagram-phone-media ${className}`}
      src={media.src}
      alt={media.label || "Selected Instagram post"}
    />
  ) : (
    <div
      className={`instagram-phone-media instagram-phone-media-empty ${className}`}
    >
      {media?.label || "Choose a post to preview"}
    </div>
  );
}

function PhoneStatusBar() {
  return (
    <div className="instagram-phone-statusbar" aria-hidden="true">
      <span className="instagram-phone-island" />
    </div>
  );
}

function PhoneBottomBar() {
  return (
    <div className="instagram-phone-bottom-bar" aria-hidden="true">
      <span className="is-current">⌂</span>
      <span>⌕</span>
      <span>
        <PreviewIcon name="comment" />
      </span>
      <span>♡</span>
      <PhoneAvatar username="you" />
    </div>
  );
}

function CommentScreen({ username, media, publicReply }) {
  return (
    <>
      <div className="instagram-phone-post-header">
        <PhoneAvatar username={username} />
        <strong>{username ? `@${username}` : "your_account"}</strong>
        <span className="instagram-phone-more">
          <PreviewIcon name="more" />
        </span>
      </div>
      <div className="instagram-phone-feed">
        <PhoneMedia media={media} />
        <div className="instagram-phone-action-row" aria-hidden="true">
          <PreviewIcon name="heart" />
          <PreviewIcon name="comment" />
          <PreviewIcon name="send" />
          <span className="instagram-phone-action-spacer" />
          <PreviewIcon name="bookmark" />
        </div>
        <div className="instagram-phone-comments">
          <strong>Liked by your followers</strong>
          <p>
            <strong>{username || "your_account"}</strong>{" "}
            {media?.label || "Your post caption will appear here."}
          </p>
          <p>
            <strong>@someone</strong> This looks great!
          </p>
          <p className="instagram-phone-own-comment">
            <strong>{username ? `@${username}` : "@your_account"}</strong>{" "}
            {publicReply || "Your public reply will appear here."}
          </p>
          <span className="instagram-phone-muted">View all comments</span>
        </div>
      </div>
    </>
  );
}

function MessageBubble({ children, own = false }) {
  return (
    <p
      className={`instagram-phone-message${
        own ? " instagram-phone-message-own" : ""
      }`}
    >
      {children}
    </p>
  );
}

function DirectMessageScreen({ username, directMessage }) {
  return (
    <>
      <div className="instagram-phone-chat-header">
        <PhoneAvatar username={username} />
        <div>
          <strong>{username ? `@${username}` : "your_account"}</strong>
          <span>Business chat</span>
        </div>
      </div>
      <div className="instagram-phone-chat">
        <p className="instagram-phone-chat-date">TODAY</p>
        <MessageBubble>
          Hi! I commented on your post. Can you send me the details?
        </MessageBubble>
        <MessageBubble own>
          {directMessage || "Your private message or link will appear here."}
        </MessageBubble>
        <span className="instagram-phone-message-note">
          Preview of the automatic DM
        </span>
      </div>
      <div className="instagram-phone-composer">
        <span>Message...</span>
        <span aria-hidden="true">↑</span>
      </div>
    </>
  );
}

function StoryReplyScreen({ username, media, directMessage }) {
  return (
    <>
      <div className="instagram-phone-chat-header">
        <PhoneAvatar username={username} />
        <div>
          <strong>{username ? `@${username}` : "your_account"}</strong>
          <span>Story reply</span>
        </div>
      </div>
      <div className="instagram-phone-story-chat">
        <div className="instagram-phone-story">
          <PhoneMedia media={media} />
          <span>Your story</span>
        </div>
        <p className="instagram-phone-story-reaction">
          <strong>@someone</strong> replied to your story
        </p>
        <MessageBubble own>
          {directMessage || "Your story reply will appear here."}
        </MessageBubble>
      </div>
      <div className="instagram-phone-composer">
        <span>Reply to story...</span>
        <span aria-hidden="true">↑</span>
      </div>
    </>
  );
}

function InstagramScreen({ screen, ...props }) {
  if (screen === "dm") return <DirectMessageScreen {...props} />;
  if (screen === "story") return <StoryReplyScreen {...props} />;
  return <CommentScreen {...props} />;
}

export function InstagramPhonePreview({
  username,
  media,
  publicReply,
  directMessage,
}) {
  const [screen, setScreen] = useState("comment");

  return (
    <aside className="instagram-automation-preview" aria-label="Flow preview">
      <div
        className="instagram-phone-screen-picker"
        role="group"
        aria-label="Choose a preview screen"
      >
        {PREVIEW_SCREENS.map((item) => (
          <button
            aria-pressed={screen === item.id}
            className="instagram-phone-screen-option"
            key={item.id}
            onClick={() => setScreen(item.id)}
            type="button"
          >
            {item.label}
          </button>
        ))}
      </div>
      <div className="instagram-phone-frame">
        <div className="instagram-phone-display">
          <PhoneStatusBar />
          <div
            className={`instagram-phone-content instagram-phone-content-${screen}`}
            aria-label={`${PREVIEW_SCREENS.find((item) => item.id === screen).label} preview`}
          >
            <InstagramScreen
              screen={screen}
              username={username}
              media={media}
              publicReply={publicReply}
              directMessage={directMessage}
            />
          </div>
          <PhoneBottomBar />
        </div>
      </div>
    </aside>
  );
}
