# ChimpChat

ChimpChat is a React and Spring Boot application with Google sign-in, passwordless email sign-in, personal workspaces, and Instagram channel onboarding.

## Run through the ngrok tunnel

Install Node.js, Java 17+, and the ngrok CLI, and authenticate ngrok. Copy `.env.example` to the root `.env` and configure `NGROK_AUTHTOKEN` and `NGROK_DOMAIN`. Keep the ignored `.env` file private. The launcher starts the API, frontend, and tunnel, and waits for the API and frontend to become healthy before starting ngrok.

The public app URL is `https://${NGROK_DOMAIN}`. Always use that HTTPS URL for browser sign-in.

The local frontend and API URLs and ports are configured by `LOCAL_FRONTEND_URL`, `LOCAL_API_URL`, `FRONTEND_PORT`, and `API_PORT`. When using ngrok, the Vite proxy forwards API and OAuth requests to the local API.

Set provider credentials in the root `.env`; Spring Boot and Vite both read this shared configuration during local development. Google sign-in needs `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET`. Set `EMAIL_AUTH_ENABLED=true` and configure SMTP (`MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, and `MAIL_FROM`) to enable passwordless email links.

Register this Google authorized redirect URI in Google Cloud, substituting your `NGROK_DOMAIN`:

```text
https://${NGROK_DOMAIN}/login/oauth2/code/google
```

Instagram connections require a Meta app, Unified Login configuration, and a 32-byte encryption key encoded as Base64. Generate a key with Node.js:

```powershell
node -e "console.log(require('crypto').randomBytes(32).toString('base64'))"
```

Set `TOKEN_ENCRYPTION_KEY` and the `META_APP_ID`, `META_APP_SECRET`, and `META_CONFIG_ID` values in the root `.env`. Add this redirect URI to the Meta Unified Login configuration, substituting your `NGROK_DOMAIN`:

```text
https://${NGROK_DOMAIN}/api/v1/channels/instagram/oauth/callback
```

For Instagram comments, set `META_WEBHOOK_VERIFY_TOKEN` in `.env` to a private random value. In the Meta app's **Manage messaging & content on Instagram** use case, request `instagram_manage_comments` and `instagram_manage_messages` (required for private replies), configure the webhook callback as `https://${NGROK_DOMAIN}/api/v1/channels/instagram/webhook`, and subscribe to the `comments` field. The callback verifies Meta's challenge and event signature, then stores comments against each matching connected workspace; webhook retries update the existing comment rather than creating duplicates. Reconnect the Instagram account after changing permissions or webhook setup so the app receives an updated token and subscribes the account to comment notifications. The signed-in workspace can view its comments in the dashboard and reply through Meta's API. Replies with an ambiguous result are marked for manual verification to avoid accidental duplicates.

The launcher derives the frontend URL, OAuth callback URLs, and secure session cookie setting from `NGROK_DOMAIN`.

From the project root, run:

```powershell
.\run.ps1
```

The script starts the API and frontend on the ports configured by `API_PORT` and `FRONTEND_PORT`, then starts the ngrok tunnel. Use the public URL above for all browser activity.

## Run frontend and backend separately (local Google sign-in)

Google sign-in is already implemented using Spring Security's OAuth authorization-code flow. The browser goes to Google, Google returns to the backend callback, and the backend creates the app session. For local development, the `local` profile derives callback URLs from `LOCAL_FRONTEND_URL` and allows the session cookie over local HTTP.

1. In Google Cloud, add this authorized redirect URI to your OAuth client:

   ```text
   http://localhost:3000/login/oauth2/code/google
   ```

   If `LOCAL_FRONTEND_URL` is different, register that URL followed by `/login/oauth2/code/google` instead.

2. Provide `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` in the root `.env` (or the IntelliJ run configuration's environment variables). Do not commit these credentials.
3. In IntelliJ, run/debug the Spring Boot application with working directory `$PROJECT_DIR$/chimpchatapi` and active profile `local`. The backend loads the shared root `.env`.
4. In a terminal, start the frontend:

   ```powershell
   Set-Location .\chimpchatweb
   npm run dev -- --host 0.0.0.0
   ```

5. Open the URL configured by `LOCAL_FRONTEND_URL` (default `http://localhost:3000`) and choose **Continue with Google**. Vite forwards OAuth and API requests to the URL configured by `LOCAL_API_URL`.

If you want Google sign-in to use ngrok instead, register `https://${NGROK_DOMAIN}/login/oauth2/code/google` with Google and set `FRONTEND_URL=https://${NGROK_DOMAIN}` and `SESSION_COOKIE_SECURE=true` in the IntelliJ run configuration. The redirect URI must exactly match the URI registered in Google Cloud.

## Docker deployment

Copy `.env.example` to the ignored root `.env` and configure `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `NGROK_AUTHTOKEN`, `NGROK_DOMAIN`, Google OAuth, optional SMTP settings, and `TOKEN_ENCRYPTION_KEY`. The Compose stack runs PostgreSQL, Redis-backed sessions and rate limits, the API, frontend, and ngrok. Browser access and both OAuth callbacks use the HTTPS URL derived from `NGROK_DOMAIN`; register the matching redirect URIs with Google and Meta.

Start the services with:

```powershell
docker compose up --build
```

Keep `.env` files and all provider credentials out of version control. Production requires HTTPS and valid SMTP, Google OAuth, Meta OAuth, database, Redis, and encryption-key configuration.

## Project structure

- `chimpchatweb`: React UI and same-origin API proxy.
- `chimpchatapi`: Spring Boot API organized by feature and by controller, business, DTO, entity, and repository responsibilities.
- `compose.yaml`: local containerized production-like stack.

## Brand and design reference

Use this section as the shared reference for public-site UI and social graphics. The live web design tokens are defined in `chimpchatweb/src/index.css`; update that file when changing the product palette or shared UI styles.

### Brand

- **Name:** ChimpChat. Use lowercase `chimpchat` for the wordmark, matching the web header.
- **Personality:** friendly, clear, and conversation-focused.
- **Current tagline:** “Made for better conversations.”
- **Logo files** (in `chimpchatweb/public/`):
  - `logo-transparent.png` — dark monkey mark on a transparent background; used in the web header.
  - `logo-yellow.png` — dark monkey mark on a yellow background.
  - `logo-bgremoved.png` — alternate mark export with a transparent background.
- Keep the logo proportional and use the provided artwork; do not redraw, stretch, or add effects. Check contrast against the chosen background.

### Color palette

| Token              | Hex       | Use                                         |
| ------------------ | --------- | ------------------------------------------- |
| Brand yellow       | `#ffe01b` | Primary accent, calls to action, highlights |
| Brand yellow hover | `#efd000` | Hover state where a darker yellow is needed |
| Ink                | `#171717` | Primary text, dark logo, strong outlines    |
| Soft ink           | `#55534b` | Secondary text                              |
| Muted ink          | `#77756e` | Supporting text, placeholders               |
| White              | `#ffffff` | Default page and surface background         |
| Border             | `#e7e5dc` | Dividers and control borders                |
| Neutral surface    | `#E2E9ED` | Subtle neutral fills                        |
| Success            | `#315d3b` | Success text and icons                      |
| Success surface    | `#edf5eb` | Success messages                            |
| Error              | `#8d2924` | Error text and icons                        |
| Error surface      | `#fbefed` | Error messages                              |
| Info               | `#55534b` | Informational text                          |
| Warning            | `#8a6400` | Warning text                                |

Use yellow as an accent with ink text, not as a substitute for readable text color. Keep page backgrounds white and use neutral/status colors for their intended roles. For social media, preserve contrast and verify text legibility at the final displayed size.

### Typography

- **Font family:** `Alga`, followed by `"Iowan Old Style"`, `"Palatino Linotype"`, `Georgia`, and a generic serif fallback. The web stylesheet specifies this stack; Alga is not bundled in the public assets.
- **Display and body:** both use the same shared serif stack.
- **Headings:** weight `500`, tight letter spacing (`-0.05em`) and line height `1.08`.
- **Brand wordmark:** weight `700`, tight letter spacing (`-0.06em`).
- **Eyebrows/labels:** small, uppercase, semibold, and spaced lettering.

| Token               | Size                            |
| ------------------- | ------------------------------- |
| `--text-xs`         | `0.75rem`                       |
| `--text-sm`         | `0.875rem`                      |
| `--text-base`       | `1rem`                          |
| `--text-lg`         | `1.125rem`                      |
| `--text-subheading` | `clamp(1.25rem, 2.4vw, 1.5rem)` |
| `--text-heading`    | `clamp(1.9rem, 5vw, 2.8rem)`    |

### Layout and components

Reuse the web UI spacing and corner-radius tokens when designing matching screens:

| Token           | Value     |
| --------------- | --------- |
| `--space-1`     | `0.5rem`  |
| `--space-2`     | `1rem`    |
| `--space-3`     | `1.5rem`  |
| `--space-4`     | `2rem`    |
| `--space-5`     | `3rem`    |
| `--space-6`     | `4rem`    |
| `--radius-sm`   | `0.75rem` |
| `--radius-md`   | `1.25rem` |
| `--radius-lg`   | `1.75rem` |
| `--radius-pill` | `999px`   |

Prefer clean white layouts, restrained borders, rounded controls, and yellow primary actions. Keep layouts responsive, use semantic hierarchy, provide visible keyboard focus, and avoid shadows to match the current UI.

### Existing copy

The following short copy appears in the current public-facing app and can be reused where appropriate:

- “Made for better conversations.”
- “WELCOME BACK”
- “SECURE SIGN IN”
- “INSTAGRAM CONVERSATION AUTOMATION”

Check the product UI before introducing new claims or campaign copy; these examples are existing interface text, not a complete marketing messaging guide.
