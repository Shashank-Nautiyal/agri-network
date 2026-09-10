# KhetSaathi Backend (Spring Boot)

Backend API for the KhetSaathi Digital Agriculture Network prototype —
orchestrates calls to the AI service, manages farmer profiles, and
aggregates anonymized regional disease data. Deployed on Render.

## Status

Fully integrated and deployed:
- ✅ Connected to the live AI service in production (`USE_MOCK = false`)
- ✅ Firestore-backed persistence for farmers and advisory history
- ✅ CORS configured for the deployed frontend
- ✅ All endpoints tested end-to-end through the live frontend

## Run locally

**Using the Maven Wrapper (no local Maven install needed):**
```bash
./mvnw spring-boot:run        # macOS/Linux
mvnw.cmd spring-boot:run      # Windows
```
Requires a JDK 17 installed.

**Environment variables needed for local runs:**

| Variable | Purpose |
|---|---|
| `AI_SERVICE_BASE_URL` | URL of the AI service (e.g. `http://localhost:8000` for local dev, or the deployed AI service URL) |
| `GOOGLE_APPLICATION_CREDENTIALS` | Path to a Google Cloud service account JSON key with Firestore access — needed to run the app at all, since the Firestore bean is created at startup |

Without `GOOGLE_APPLICATION_CREDENTIALS` set locally, the app will fail to
start (not just fail Firestore calls) — the same service account used for
Earth Engine works here too, as long as it has the "Cloud Datastore User"
role granted.

Server runs on `http://localhost:8080`. Check `GET /health` to confirm it's up.

## API endpoints

See [`../docs/api-contract.md`](../docs/api-contract.md) for the full
request/response contract shared with the AI service. Summary:

| Endpoint | Method | Purpose |
|---|---|---|
| `/api/farmers` | POST | Register a farmer profile (Firestore) |
| `/api/farmers/{id}` | GET | Fetch a farmer profile |
| `/api/diagnose` | POST | Crop disease diagnosis from a photo (proxies to AI service) |
| `/api/advisory` | POST | Crop/planting advisory from soil+satellite+weather (proxies to AI service) |
| `/api/regenerative-advice` | POST | Regenerative farming practices (proxies to AI service) |
| `/api/voice-query` | POST | Voice/text Q&A (proxies to AI service) |
| `/api/district/{districtId}/regional-alerts` | GET | Anonymized cross-district disease trend — owned entirely by this backend, not the AI service |
| `/health` | GET | Health check |

## Data model

- **Farmers** and **advisory records** (diagnosis/advisory history, used
  to build regional alerts) are stored in Firestore — collections
  `farmers` and `advisory_records`.
- Only diagnoses flagged `isValidImage: true` by the AI service get
  recorded into district history, so bad/blurry photos don't pollute the
  regional disease trend data.

## Deployment (Render)

Deployed as a Docker-based Web Service on Render:
- **Root Directory:** `backend`
- **Dockerfile Path:** `backend/Dockerfile`
- **Health Check Path:** `/health`

**Environment variables set on Render:**
- `AI_SERVICE_BASE_URL` — the deployed AI service's URL
- `GOOGLE_APPLICATION_CREDENTIALS` — `/etc/secrets/gee-key.json`, matching a Secret File upload of the same service account key used for Earth Engine

**Note:** free-tier Render Web Services sleep after ~15 minutes of
inactivity and take 30–60s to wake on the next request — worth pinging
the URL before a live demo.

## CORS

`config/CorsConfig.java` allows requests from any origin (`*`), so the
frontend can be hosted anywhere (Render Static Site, Netlify, local file,
etc.) without further backend changes.

## Tech stack

Java 17, Spring Boot 3.3, Maven (with Maven Wrapper), Lombok, Spring
WebFlux (`WebClient`, used only for its HTTP client — not reactive
controllers), Google Cloud Firestore.
