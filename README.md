# ChimpChat

ChimpChat is a React and Spring Boot application with Google sign-in, passwordless email sign-in, personal workspaces, and Instagram channel onboarding.

## Local development

Install Node.js, Java 17+, and Maven (or use the Maven wrapper in `chimpchatapi`). Create `chimpchatapi/.env` for local provider settings; this file is ignored by Git. The local Spring profile uses file-based H2. Google sign-in requires `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET`; email sign-in requires `EMAIL_AUTH_ENABLED=true` and SMTP settings (`MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, and `MAIL_FROM`).

Configure Google OAuth with this authorized redirect URI for local development:

```text
http://localhost:3000/login/oauth2/code/google
```

For email sign-in, set `EMAIL_AUTH_ENABLED=true` and provide working SMTP settings, including `MAIL_FROM`. Email links are single-use and expire after 15 minutes.

Instagram connections require a Meta app, Unified Login configuration, and a 32-byte encryption key encoded as Base64. Generate a key with Node.js:

```powershell
node -e "console.log(require('crypto').randomBytes(32).toString('base64'))"
```

Set `TOKEN_ENCRYPTION_KEY` and the `META_APP_ID`, `META_APP_SECRET`, `META_CONFIG_ID`, and `META_REDIRECT_URI` values in `chimpchatapi/.env`. Add the exact redirect URI to the Meta Unified Login configuration:

```text
http://localhost:3000/api/v1/channels/instagram/oauth/callback
```

From the project root, run:

```powershell
.\run.ps1
```

The script starts the API on port 8080 and frontend on port 3000. Use the frontend at `http://localhost:3000`.

## Docker deployment

Create a root `.env` (ignored by Git) and configure `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `NGROK_AUTHTOKEN`, Google OAuth, optional SMTP settings, and `TOKEN_ENCRYPTION_KEY`. The Compose stack runs PostgreSQL, Redis-backed sessions and rate limits, the API, frontend, and ngrok. Set `FRONTEND_URL`, `GOOGLE_REDIRECT_URI`, and `META_REDIRECT_URI` to the public frontend/tunnel URLs, and register both callback URIs with their identity providers.

Start the services with:

```powershell
docker compose up --build
```

Keep `.env` files and all provider credentials out of version control. Production requires HTTPS and valid SMTP, Google OAuth, Meta OAuth, database, Redis, and encryption-key configuration.

## Project structure

- `chimpchatweb`: React UI and same-origin API proxy.
- `chimpchatapi`: Spring Boot API organized by feature and by controller, business, DTO, entity, and repository responsibilities.
- `compose.yaml`: local containerized production-like stack.
