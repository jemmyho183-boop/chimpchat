# ChimpChat local development

## Start the frontend, API, and ngrok tunnel

Install Node.js, Java 17, and the ngrok CLI, and make sure ngrok is authenticated. Add your `NGROK_AUTHTOKEN` to the root `.env` file. Add your Meta app credentials to `chimpchatapi/.env` if you want to test OAuth.

From the project root, run:

```powershell
.\run.ps1
```

The script opens separate windows for the backend, frontend, and ngrok. The frontend is available at `http://localhost:3000`; requests to `/api` are proxied to the backend on port 8080. The ngrok tunnel exposes the same frontend and API at `https://gauntlet-sliver-headfirst.ngrok-free.dev`. Close each service window to stop it.

Configure the Meta app's OAuth redirect URIs to use the ngrok domain. The root `.env` `FRONTEND_URL` controls where OAuth completion redirects; set it to `http://localhost:3000` to return to the local frontend.
