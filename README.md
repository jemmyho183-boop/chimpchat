# ChimpChat local development

## Start the frontend, API, and ngrok tunnel

Install Node.js, Java 17, and the ngrok CLI, and make sure ngrok is authenticated. Add your `NGROK_AUTHTOKEN` to the root `.env` file.

## Configure Instagram Unified Login

1. In the [Meta for Developers app dashboard](https://developers.facebook.com/apps/), select a business-owned app with the **Manage messaging & content on Instagram** use case and Facebook Login for Business.
2. Open **Use cases > Manage messaging & content on Instagram > Customize > API setup with Unified login**.
3. Create a configuration with the Instagram permissions your app needs. For the current login, select `instagram_basic`, `instagram_manage_comments`, and `instagram_manage_messages`.
4. Set this redirect URL in the configuration and save it:

   ```text
   https://gauntlet-sliver-headfirst.ngrok-free.dev/api/auth/instagram/callback
   ```

5. Copy the configuration ID shown by Meta. In your app settings, copy the **Meta App ID** and **Meta App Secret**. Unified Login uses the Meta app credentials, not the separate Instagram App ID/secret.
6. Copy `chimpchatapi/.env.example` to `chimpchatapi/.env`, then set `META_APP_ID`, `META_APP_SECRET`, `META_CONFIG_ID`, and `META_REDIRECT_URI`. `META_CONFIG_ID` must be the ID of the configuration created under **API setup with Unified login**, not an Instagram Login configuration. Keep the secret private and never commit `.env`.

The single Instagram button opens Meta's Unified Login dialog, where the client can log in with Instagram or Facebook and select their business portfolio and Instagram account. The backend exchanges the code for a Business Integration System User Access Token and fetches the shared Instagram accounts. After authorization, the page displays the connected Instagram username(s). The token stays in the backend's in-memory session and is not exposed to the browser. Add persistent, encrypted token storage before production use; in-memory sessions are lost when the API restarts.

Meta requires Advanced Access/App Review and Live mode for clients without a role on your app. Standard Access is for testing with people who have an app role. Unified Login requires a business-owned app and does not support Instagram tester accounts; use a professional account for testing. See Meta's [Unified Login setup guide](https://developers.facebook.com/documentation/instagram-platform/instagram-api-with-unified-login/get-started) and [authentication guide](https://developers.facebook.com/documentation/instagram-platform/instagram-api-with-unified-login/authentication).

From the project root, run:

```powershell
.\run.ps1
```

The script opens separate windows for the backend, frontend, and ngrok. The frontend is available at `http://localhost:3000`; requests to `/api` are proxied to the backend on port 8080. The ngrok tunnel exposes the same frontend and API at `https://gauntlet-sliver-headfirst.ngrok-free.dev`. Close each service window to stop it.

Configure the Meta app's OAuth redirect URIs to use the ngrok domain. The root `.env` `FRONTEND_URL` controls where OAuth completion redirects; set it to `http://localhost:3000` to return to the local frontend.

If the launcher reports that port 3000 or 8080 is already in use, close the old frontend or API terminal window and run `.\run.ps1` again. The launcher will not start duplicate services on occupied ports.
