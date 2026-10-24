# TryStyle AI Online

Standalone Android app and Cloudflare Worker/D1 backend in branch trystyle-ai-online.

Users **self-register across devices** using one HTTPS server and D1 database. The server seeds reserved account ADM from private ADMIN_BOOTSTRAP_PASSWORD. Members cannot create ADM. ADM can list members, issue AI credits, set VIP and lock/unlock accounts. Backend enforces admin authorization. Android prompts for the real Worker HTTPS URL.

The AI virtual try-on uses FASHN v1.6, requiring a private FASHN_API_KEY on the server plus paid provider credits. User must opt in to sending their chosen pictures, and each successfully submitted image uses one app credit. VIP is a badge and is not unlimited AI. On failure, backend refunds the app credit. Results may expire at provider.

## Deployment necessary for online signup
The APK is an **online client**, but no server is deployed just by building on GitHub. A Cloudflare account and Worker URL are required; do not mistake app-local account forms for real deployment.

1. At server folder: npm install
2. npx wrangler d1 create trystyle-ai
3. Replace REPLACE_WITH_REAL_D1_DATABASE_ID in server/wrangler.toml with Cloudflare's database id.
4. npx wrangler d1 migrations apply trystyle-ai --remote
5. npx wrangler secret put ADMIN_BOOTSTRAP_PASSWORD (choose a strong private ADM password with 10+ characters; do not commit).
6. Optional for actual image generation: npx wrangler secret put FASHN_API_KEY
7. npx wrangler deploy
8. Open https://YOUR-WORKER.workers.dev/api/health and check ok:true.
9. Install Android APK, select CẤU HÌNH MÁY CHỦ, paste the deployed HTTPS URL, log in ADM. Other phones use the SAME URL to self-register.

Deploy instructions: https://developers.cloudflare.com/d1/get-started/
FASHN integration: https://docs.fashn.ai/api-reference/tryon-v1-6

**Not production ready:** add rate limiting, recovery, abuse prevention, monitoring, retention/deletion controls, audited billing, and verified payment before selling accounts or accepting money. TryStyle AI has no financial checkout. ADM does not have access to members' private source photos in the app.

## Android build
GitHub Actions on branch trystyle-ai-online: Build TryStyle AI Online APK, artifact TryStyleAI-Online-v1-debug, app-debug.apk.

## Optional GitHub Actions deployment
After setting real D1 database ID in server/wrangler.toml, set CLOUDFLARE_API_TOKEN and CLOUDFLARE_ACCOUNT_ID as GitHub Actions secrets. Set ADMIN_BOOTSTRAP_PASSWORD on Cloudflare using Wrangler secret put. On the trystyle-ai-online branch, manually trigger the workflow Deploy TryStyle Cloudflare API. This runs migrations and deploys, but only after you connect Cloudflare credentials.

## Setup UI in version 1.0.1
When a new user installs the APK, the Worker URL setup is shown only until the entered HTTPS URL passes the TryStyle AI health verification. The app then hides the setup warning and skips it on subsequent launches. A server change entry remains on the signed-in ADM screen. The ADM account is not created in Android: it is securely seeded on the **deployed** Cloudflare D1 backend after setting ADMIN_BOOTSTRAP_PASSWORD as a Worker secret; source code, GitHub, and local Android setup alone do not activate an online ADM.

## Bundled server URL — version 1.0.2
Set repository **Settings → Secrets and variables → Actions → Variables → New repository variable**:
- Name: `TRYSTYLE_API_URL`
- Value: your **actual deployed** TryStyle AI HTTPS Cloudflare Worker URL, e.g. `https://your-worker.workers.dev` (do not reuse unrelated TikXu API).

Re-run the **Build TryStyle AI Online APK** workflow on `trystyle-ai-online`. The Gradle build embeds the public Worker URL as an Android string resource; app preselects that URL and goes straight to sign-in. The setup warning is invisible when a URL is configured. Public API endpoint URLs are not secrets; only the server holds passwords/AI tokens.

When this GitHub variable is not configured, APK necessarily opens the configuration screen. This code does not make up a server URL or activate ADM by itself. To activate ADM, deploy Worker with D1 and set the private Cloudflare Worker secret `ADMIN_BOOTSTRAP_PASSWORD` to a unique strong password before the first protected request.

## One-run online activation + preconfigured Android APK
The workflow **Deploy TryStyle Server and Ready-to-Use APK** automatically creates/reuses a D1 database, runs migration, deploys a NEW named TryStyle Worker, saves encrypted Worker secrets, logs in as ADM once to prove account activation, then compiles an APK that already knows its Worker URL. It never changes the unrelated TikXu Worker or Coin Shop project.

To authorize this deployment, the owner must add three values under GitHub repository **Settings → Secrets and variables → Actions → Repository secrets**:
- `CLOUDFLARE_ACCOUNT_ID` — your 32-character Cloudflare account ID.
- `CLOUDFLARE_API_TOKEN` — a scoped Cloudflare token with Workers Scripts edit, D1 edit and subdomain read permissions. Do not expose it in chats or code.
- `ADMIN_BOOTSTRAP_PASSWORD` — a long **private** initial ADM login password, 10–128 characters. This is not stored in APK or repository source.

Optional fourth GitHub secret `FASHN_API_KEY` enables paid image generation. When missing, registration and ADM work after deployment, but the image generation UI states the AI is not configured.

Once the three required secrets are configured, run GitHub Actions → **Deploy TryStyle Server and Ready-to-Use APK** → Run workflow → branch `trystyle-ai-online`. The run produces `TryStyleAI-Online-Connected-APK`; it has the actual URL prefilled. Download/unzip to install `app-debug.apk`. If the account has not activated a workers.dev subdomain, enable it in Cloudflare first. You may need to confirm billing/free-plan limits and authorize access with Cloudflare. Never paste tokens or passwords to ChatGPT.

### GitHub Actions branch-only deployment note
Because Coin Shop lives on the default `main` branch, the deploy workflow is intentionally kept ONLY in `trystyle-ai-online`. GitHub's **Run workflow** button may not appear when workflow_dispatch is not on the default branch. Instead this workflow starts once when changed on the TryStyle branch, and will fail with a missing-secret message if Cloudflare has not been connected. After the owner securely adds the required secrets, use that failed run's **Re-run jobs** button (or ask ChatGPT to rerun failed jobs via the connected GitHub app) to actually deploy. This does not modify Coin Shop on main.

## Legacy TikXu Worker migration
The previous TikXu project is in a separate repository. Its original APK/source code remains unchanged because the GitHub connector is not installed on that repository. **This branch now targets the original Cloudflare Worker service name `dieu-tuan-tikxu-api` in account subdomain `tukuba41007`**, preserving the original public URL **after deployment**. The Cloudflare bootstrap checks the account subdomain and stops if it differs. The TryStyle backend uses its own new D1 database `trystyle-ai` and does NOT migrate TikXu user accounts, but the source code preserves old TikXu data by not deleting old databases.

**IMPORTANT:** A successful deploy will replace the API at the old TikXu URL and the old app may stop signing in. This is deliberate only because the project owner authorized retiring TikXu. The actual remote API is not changed until Cloudflare credentials are supplied and deployment succeeds. GitHub does not currently hold these credentials; no live ADM is yet active.

**To install safely:** supply Cloudflare GitHub secrets to the TryStyle project and rerun the workflow. If a different account is selected, migration fails before deploy rather than silently deploying to someone else's Worker.
