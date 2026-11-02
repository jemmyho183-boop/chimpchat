# ChimpChat

ChimpChat is a React and Spring Boot application with Google sign-in, passwordless email sign-in, personal workspaces, and Instagram channel onboarding.

## Run through the ngrok tunnel

Install Node.js, Java 17+, and the ngrok CLI, and authenticate ngrok. Create a root `.env` containing `NGROK_AUTHTOKEN`; keep this ignored file private. The launcher starts the API, frontend, and tunnel, and waits for the API and frontend to become healthy before starting ngrok.

Always open the app at the public URL:

```text
https://gauntlet-sliver-headfirst.ngrok-free.dev
```

Do not use `http://localhost:3000` for browser sign-in. Local ports 3000 (Vite) and 8080 (Spring API) are internal upstreams behind the HTTPS tunnel; the Vite proxy forwards API and OAuth requests to the local API.

Create `chimpchatapi/.env` for provider credentials and settings; this ignored file is private and is loaded by Spring Boot. Google sign-in needs `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET`. Set `EMAIL_AUTH_ENABLED=true` and configure SMTP (`MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, and `MAIL_FROM`) to enable passwordless email links.

Register this exact Google authorized redirect URI in Google Cloud:

```text
https://gauntlet-sliver-headfirst.ngrok-free.dev/login/oauth2/code/google
```

Instagram connections require a Meta app, Unified Login configuration, and a 32-byte encryption key encoded as Base64. Generate a key with Node.js:

```powershell
node -e "console.log(require('crypto').randomBytes(32).toString('base64'))"
```

Set `TOKEN_ENCRYPTION_KEY` and the `META_APP_ID`, `META_APP_SECRET`, and `META_CONFIG_ID` values in `chimpchatapi/.env`. Add this exact redirect URI to the Meta Unified Login configuration:

```text
https://gauntlet-sliver-headfirst.ngrok-free.dev/api/v1/channels/instagram/oauth/callback
```

The launcher sets `FRONTEND_URL`, both OAuth callback URIs, and secure session cookies to the HTTPS ngrok URL. Keep these redirect URIs registered in the provider consoles.

From the project root, run:

```powershell
.\run.ps1
```

The script starts the API on port 8080 and frontend on port 3000, then starts the stable ngrok tunnel. Use the public URL above for all browser activity.

## Docker deployment

Create a root `.env` (ignored by Git) and configure `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `NGROK_AUTHTOKEN`, Google OAuth, optional SMTP settings, and `TOKEN_ENCRYPTION_KEY`. The Compose stack runs PostgreSQL, Redis-backed sessions and rate limits, the API, frontend, and ngrok. Browser access and both OAuth callbacks use the ngrok HTTPS URL above; register the exact redirect URIs listed earlier with Google and Meta.

Start the services with:

```powershell
docker compose up --build
```

Keep `.env` files and all provider credentials out of version control. Production requires HTTPS and valid SMTP, Google OAuth, Meta OAuth, database, Redis, and encryption-key configuration.

## Project structure

- `chimpchatweb`: React UI and same-origin API proxy.
- `chimpchatapi`: Spring Boot API organized by feature and by controller, business, DTO, entity, and repository responsibilities.
- `compose.yaml`: local containerized production-like stack.
