import { useEffect, useState } from "react";
import { Button } from "../../../shared/components/Button";
import { useToast } from "../../../shared/components/toast-context";
import { instagramCommentAutomationService } from "../services/instagram-comment-automation-service";

const EMPTY_FORM = {
  matchType: "ANY_COMMENT",
  keyword: "",
  publicReply: "",
  directMessage: "",
};

function mediaLabel(media) {
  const caption = media.caption?.trim();
  if (caption) return caption.slice(0, 100);
  return media.mediaType ? `${media.mediaType} post` : "Instagram post";
}

function mediaThumbnail(media) {
  return (
    media.thumbnailUrl ||
    (media.mediaType === "VIDEO" ? "" : media.mediaUrl || "")
  );
}

function InstagramAutomationForm({
  posts,
  selectedMediaId,
  onMediaChange,
  form,
  onFormChange,
  saving,
  error,
  editing,
  onSubmit,
  onCancel,
}) {
  const editingMedia = editing
    ? { id: editing.mediaId, label: editing.mediaLabel }
    : null;
  const hasEditingMedia = posts.some((post) => post.id === editingMedia?.id);

  return (
    <form className="instagram-automation-form" onSubmit={onSubmit}>
      <div className="instagram-post-picker">
        <div className="instagram-field-heading">
          <span className="instagram-field-label">Choose a post or Reel</span>
          <span>Select the post that should start this flow.</span>
        </div>
        <div
          className="instagram-post-grid"
          role="group"
          aria-label="Instagram posts"
        >
          {editingMedia && !hasEditingMedia && (
            <button
              className="instagram-post-option is-selected"
              type="button"
              aria-pressed="true"
              aria-label={`Selected post: ${editingMedia.label}`}
              onClick={() => onMediaChange(editingMedia.id)}
              disabled={saving}
            >
              <span className="instagram-post-thumbnail instagram-post-thumbnail-empty">
                Post
              </span>
              <span className="instagram-post-caption">{editingMedia.label}</span>
            </button>
          )}
          {posts.map((post) => (
            <button
              className={`instagram-post-option${
                selectedMediaId === post.id ? " is-selected" : ""
              }`}
              key={post.id}
              type="button"
              aria-pressed={selectedMediaId === post.id}
              aria-label={`Choose post: ${mediaLabel(post)}`}
              onClick={() => onMediaChange(post.id)}
              disabled={saving}
            >
              <span className="instagram-post-thumbnail">
                {mediaThumbnail(post) ? (
                  <img
                    src={mediaThumbnail(post)}
                    alt=""
                    loading="lazy"
                    aria-hidden="true"
                  />
                ) : (
                  <span className="instagram-post-thumbnail-empty">
                    {post.mediaType || "Post"}
                  </span>
                )}
                {post.mediaType === "VIDEO" && (
                  <span className="instagram-post-type">VIDEO</span>
                )}
              </span>
              <span className="instagram-post-caption">{mediaLabel(post)}</span>
            </button>
          ))}
        </div>
        {posts.length === 0 && !error && (
          <p className="instagram-automation-help">
            No recent posts were returned for this Instagram account.
          </p>
        )}
      </div>

      <fieldset className="instagram-automation-trigger">
        <legend>When should this flow run?</legend>
        <label>
          <input
            type="radio"
            name="matchType"
            value="ANY_COMMENT"
            checked={form.matchType === "ANY_COMMENT"}
            onChange={() =>
              onFormChange({ ...form, matchType: "ANY_COMMENT", keyword: "" })
            }
            disabled={saving}
          />
          Any comment on this post
        </label>
        <label>
          <input
            type="radio"
            name="matchType"
            value="KEYWORD"
            checked={form.matchType === "KEYWORD"}
            onChange={() => onFormChange({ ...form, matchType: "KEYWORD" })}
            disabled={saving}
          />
          A comment contains specific words
        </label>
        {form.matchType === "KEYWORD" && (
          <label className="instagram-automation-keyword">
            Keyword or phrase
            <input
              type="text"
              maxLength={100}
              value={form.keyword}
              onChange={(event) =>
                onFormChange({ ...form, keyword: event.target.value })
              }
              disabled={saving}
              placeholder="For example: send me the link"
              required
            />
          </label>
        )}
      </fieldset>

      <label>
        Public comment reply
        <textarea
          value={form.publicReply}
          onChange={(event) =>
            onFormChange({ ...form, publicReply: event.target.value })
          }
          maxLength={2200}
          rows={3}
          required
          disabled={saving}
          placeholder="Thanks for your comment!"
        />
      </label>
      <label>
        Private DM message or link
        <textarea
          value={form.directMessage}
          onChange={(event) =>
            onFormChange({ ...form, directMessage: event.target.value })
          }
          maxLength={1000}
          rows={3}
          required
          disabled={saving}
          placeholder="Here is the link you asked for: https://…"
        />
      </label>

      {error && (
        <p className="inline-notice notice-error" role="alert">
          {error}
        </p>
      )}
      <div className="instagram-automation-actions">
        {editing && (
          <Button
            type="button"
            variant="secondary"
            onClick={onCancel}
            disabled={saving}
          >
            Cancel
          </Button>
        )}
        <Button
          type="submit"
          disabled={
            saving ||
            !selectedMediaId ||
            !form.publicReply.trim() ||
            !form.directMessage.trim() ||
            (form.matchType === "KEYWORD" && !form.keyword.trim())
          }
        >
          {saving ? "Saving…" : editing ? "Save changes" : "Go live"}
        </Button>
      </div>
    </form>
  );
}

function InstagramAutomationPreview({ account, media, form }) {
  const thumbnail = media && mediaThumbnail(media);
  const label = media
    ? media.caption
      ? mediaLabel(media)
      : media.label
    : "Select a post to preview it here.";

  return (
    <aside className="instagram-automation-preview" aria-label="Flow preview">
      <div className="instagram-preview-heading">
        <div>
          <p className="eyebrow">PREVIEW</p>
          <h2>Your Instagram post</h2>
        </div>
        <span className="instagram-preview-live">Live preview</span>
      </div>
      <div className="instagram-preview-device">
        <div className="instagram-preview-device-top" aria-hidden="true">
          <span />
        </div>
        <div className="instagram-preview-post">
          <div className="instagram-preview-account">
            <span className="instagram-preview-avatar" aria-hidden="true">
              {account.username.slice(0, 1).toUpperCase()}
            </span>
            <strong>@{account.username}</strong>
          </div>
          {media?.mediaType === "VIDEO" && media.mediaUrl ? (
            <video
              className="instagram-preview-media"
              controls
              muted
              playsInline
              poster={media.thumbnailUrl || undefined}
              src={media.mediaUrl}
            >
              Your browser does not support video.
            </video>
          ) : thumbnail ? (
            <img
              className="instagram-preview-media"
              src={thumbnail}
              alt={label}
            />
          ) : (
            <div className="instagram-preview-media instagram-preview-empty">
              {label}
            </div>
          )}
          <p className="instagram-preview-caption">{label}</p>
        </div>
      </div>
      <div className="instagram-preview-messages">
        <p>
          <strong>Public reply</strong>
          <span>{form.publicReply || "Your public reply will appear here."}</span>
        </p>
        <p>
          <strong>Private message</strong>
          <span>
            {form.directMessage || "Your DM or link will appear here."}
          </span>
        </p>
      </div>
    </aside>
  );
}

export function InstagramCommentAutomationBuilder({ account }) {
  const accountId = account.id;
  const [posts, setPosts] = useState([]);
  const [selectedMediaId, setSelectedMediaId] = useState("");
  const [automations, setAutomations] = useState([]);
  const [form, setForm] = useState(EMPTY_FORM);
  const [editing, setEditing] = useState(null);
  const [loadedAccountId, setLoadedAccountId] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [reloadKey, setReloadKey] = useState(0);
  const { notify } = useToast();

  useEffect(() => {
    const controller = new AbortController();
    Promise.allSettled([
      instagramCommentAutomationService.list({
        accountId,
        signal: controller.signal,
      }),
      instagramCommentAutomationService.listPosts(accountId, {
        signal: controller.signal,
      }),
    ]).then(([rulesResult, postsResult]) => {
      if (controller.signal.aborted) return;
      const requestErrors = [];
      if (rulesResult.status === "fulfilled") {
        setAutomations(rulesResult.value);
      } else {
        setAutomations([]);
        requestErrors.push(rulesResult.reason.message);
      }
      if (postsResult.status === "fulfilled") {
        setPosts(postsResult.value);
      } else {
        setPosts([]);
        requestErrors.push(postsResult.reason.message);
      }
      setError(requestErrors.join(" "));
      setLoadedAccountId(accountId);
    });

    return () => controller.abort();
  }, [accountId, reloadKey]);

  const loading = loadedAccountId !== accountId;
  const visiblePosts = loadedAccountId === accountId ? posts : [];
  const visibleAutomations = loadedAccountId === accountId ? automations : [];
  const selectedPost = visiblePosts.find((post) => post.id === selectedMediaId);
  const selectedRulePost =
    editing?.mediaId === selectedMediaId ? editing : null;

  function resetForm() {
    setEditing(null);
    setForm(EMPTY_FORM);
    setSelectedMediaId("");
    setError("");
  }

  async function submit(event) {
    event.preventDefault();
    if (saving) return;
    if (!selectedPost && !selectedRulePost) {
      setError("Choose an Instagram post for this flow.");
      return;
    }

    const payload = {
      accountId,
      mediaId: selectedMediaId,
      mediaLabel: selectedPost
        ? mediaLabel(selectedPost)
        : selectedRulePost.mediaLabel,
      matchType: form.matchType,
      keyword: form.matchType === "KEYWORD" ? form.keyword.trim() : null,
      publicReply: form.publicReply.trim(),
      directMessage: form.directMessage.trim(),
      enabled: editing?.enabled ?? true,
    };

    setSaving(true);
    setError("");
    try {
      if (editing) {
        await instagramCommentAutomationService.update(editing.id, payload);
      } else {
        await instagramCommentAutomationService.create(payload);
      }
      notify(editing ? "Flow updated." : "Your comment flow is live.");
      resetForm();
      setReloadKey((value) => value + 1);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setSaving(false);
    }
  }

  function editAutomation(automation) {
    setSelectedMediaId(automation.mediaId);
    setEditing(automation);
    setForm({
      matchType: automation.matchType,
      keyword: automation.keyword || "",
      publicReply: automation.publicReply,
      directMessage: automation.directMessage,
    });
    setError("");
  }

  async function toggleAutomation(automation) {
    setError("");
    try {
      await instagramCommentAutomationService.update(automation.id, {
        accountId: automation.accountId,
        mediaId: automation.mediaId,
        mediaLabel: automation.mediaLabel,
        matchType: automation.matchType,
        keyword: automation.keyword,
        publicReply: automation.publicReply,
        directMessage: automation.directMessage,
        enabled: !automation.enabled,
      });
      notify(automation.enabled ? "Flow paused." : "Flow activated.");
      setReloadKey((value) => value + 1);
    } catch (requestError) {
      setError(requestError.message);
    }
  }

  async function deleteAutomation(automation) {
    if (!window.confirm("Delete this Instagram comment flow?")) return;
    setError("");
    try {
      await instagramCommentAutomationService.delete(automation.id);
      notify("Flow deleted.");
      setAutomations((current) =>
        current.filter((item) => item.id !== automation.id),
      );
      if (editing?.id === automation.id) resetForm();
    } catch (requestError) {
      setError(requestError.message);
    }
  }

  return (
    <section className="instagram-automation-workspace">
      <div className="instagram-automation-layout">
        <div className="instagram-automation-editor">
          <div className="instagram-editor-heading">
            <p className="eyebrow">COMMENT AUTOMATION</p>
            <h2>When someone comments on</h2>
            <p>
              Choose a post, set a comment trigger, and decide what to send.
            </p>
          </div>
          {loading && (
            <p className="dashboard-empty" aria-live="polite">
              Loading your Instagram posts…
            </p>
          )}
          {!loading && (
            <InstagramAutomationForm
              posts={visiblePosts}
              selectedMediaId={selectedMediaId}
              onMediaChange={setSelectedMediaId}
              form={form}
              onFormChange={setForm}
              saving={saving}
              error={error}
              editing={editing}
              onSubmit={submit}
              onCancel={resetForm}
            />
          )}
        </div>
        <InstagramAutomationPreview
          account={account}
          media={selectedPost || selectedRulePost}
          form={form}
        />
      </div>

      {visibleAutomations.length > 0 && (
        <div className="instagram-automation-list">
          <div className="instagram-flow-list-heading">
            <div>
              <p className="eyebrow">YOUR FLOWS</p>
              <h2>Existing comment flows</h2>
            </div>
            <span className="count-label">
              {visibleAutomations.length}{" "}
              {visibleAutomations.length === 1 ? "flow" : "flows"}
            </span>
          </div>
          {visibleAutomations.map((automation) => (
            <article className="instagram-automation-card" key={automation.id}>
              <div className="instagram-automation-card-heading">
                <div>
                  <strong>{automation.mediaLabel}</strong>
                  <p>
                    {automation.matchType === "KEYWORD"
                      ? `When a comment contains “${automation.keyword}”`
                      : "When anyone comments"}
                  </p>
                </div>
                <span
                  className={`instagram-automation-status${
                    automation.enabled ? " is-active" : ""
                  }`}
                >
                  {automation.enabled ? "Active" : "Paused"}
                </span>
              </div>
              <div className="instagram-automation-messages">
                <p>
                  <strong>Public reply</strong>
                  <span>{automation.publicReply}</span>
                </p>
                <p>
                  <strong>Private DM</strong>
                  <span>{automation.directMessage}</span>
                </p>
              </div>
              <div className="instagram-automation-card-actions">
                <button
                  className="text-button"
                  type="button"
                  onClick={() => toggleAutomation(automation)}
                >
                  {automation.enabled ? "Pause" : "Activate"}
                </button>
                <button
                  className="text-button"
                  type="button"
                  onClick={() => editAutomation(automation)}
                >
                  Edit
                </button>
                <button
                  className="text-button text-button-danger"
                  type="button"
                  onClick={() => deleteAutomation(automation)}
                >
                  Delete
                </button>
              </div>
            </article>
          ))}
        </div>
      )}
      {!loading && !error && visibleAutomations.length === 0 && (
        <p className="instagram-automation-empty">
          No comment flows yet. Choose a post above to create your first one.
        </p>
      )}
    </section>
  );
}
