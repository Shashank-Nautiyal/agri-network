# KhetSaathi AI Service (FastAPI)

AI/ML service for the KhetSaathi prototype — Gemini multimodal diagnosis,
Earth Engine satellite analysis, live weather, and soil estimates. Called
by the Spring Boot backend over plain REST. Deployed on Render.

## Status

Fully built, deployed, and verified against real data:
- ✅ Gemini (`gemini-3.6-flash`) — crop disease diagnosis, advisory
  reasoning, regenerative-practice recommendations, voice responses
- ✅ Google Earth Engine (Sentinel-2) — real NDVI, authenticated via a
  service account (required for headless deployment — interactive
  `earthengine authenticate` only works on a local machine)
- ✅ OpenWeatherMap (classic free endpoints) — live weather, swapped in
  after Open-Meteo hit persistent 429s on Render's shared IPs
- ✅ SoilGrids (ISRIC) — soil estimate fallback when a farmer hasn't
  submitted lab soil data

## Run locally

```bash
pip install -r requirements.txt
uvicorn main:app --reload --port 8000
```
Visit `http://localhost:8000/docs` for interactive Swagger docs.

**Environment variables needed (`.env` file, same directory as `main.py`):**

| Variable | Purpose |
|---|---|
| `GOOGLE_API_KEY` | Gemini API key from Google AI Studio |
| `GEE_PROJECT_ID` | Earth Engine Cloud project ID |
| `GEE_SERVICE_ACCOUNT_EMAIL` | Service account email (`...@....iam.gserviceaccount.com`) |
| `GEE_KEY_PATH` | Path to the service account's JSON key file |
| `OWM_API_KEY` | OpenWeatherMap API key (classic free tier — no card required) |

**Earth Engine note:** deployed environments use
`ee.ServiceAccountCredentials(...)`, not interactive
`earthengine authenticate` — the latter relies on a local OAuth cache that
doesn't exist on a fresh server.

## API endpoints

See [`../docs/api-contract.md`](../docs/api-contract.md) for the full
request/response contract. Summary:

| Endpoint | Method | Purpose |
|---|---|---|
| `/diagnose` | POST | Crop disease diagnosis from a photo (Gemini multimodal) |
| `/advisory` | POST | Planting advisory from NDVI + weather + soil estimate |
| `/regenerative-advice` | POST | Sustainable farming practices (soil data optional, SoilGrids fallback) |
| `/voice-query` | POST | Voice/text Q&A, transcript in/out (billing-free — no server-side speech APIs) |
| `/schema` | GET | Standalone endpoint documenting the country-agnostic data model — demonstrates the cross-border/BRICS interoperability requirement |
| `/health` | GET | Health check |

## Design decisions worth knowing

- **`thinking_level: LOW`** is set on all Gemini calls — the model
  defaults to `HIGH` reasoning, which added 10–20s of latency per call;
  `LOW` brought this down significantly with no meaningful quality loss
  for these tasks.
- **Voice stays transcript-based, not audio-based** — STT/TTS happen
  client-side via the browser's Web Speech API, so this service only
  ever handles plain text. Keeps the whole stack billing-free (no Google
  Speech-to-Text/Text-to-Speech API).
- **No separate Translation API** — Gemini is prompted to respond
  directly in the requested language.
- **Every endpoint returns 200 with a graceful fallback** rather than an
  error, even when an external API (Gemini, Earth Engine, weather, soil)
  fails — the Spring Boot backend doesn't need special error-handling
  for these calls beyond normal network timeouts.
- **`/diagnose` sets `isValidImage: false`** on bad/blurry photos or
  processing failures, so the backend knows not to count them toward
  district disease trend data.

## Deployment (Render, no Dockerfile)

Deployed as a native Python Web Service (no Docker needed for a plain
FastAPI app):
- **Root Directory:** `ai-service`
- **Build Command:** `pip install -r requirements.txt`
- **Start Command:** `uvicorn main:app --host 0.0.0.0 --port $PORT`
- **Health Check Path:** `/health`

**Environment variables + Secret File on Render:**
- `GOOGLE_API_KEY`, `GEE_PROJECT_ID`, `GEE_SERVICE_ACCOUNT_EMAIL`,
  `OWM_API_KEY` — set as environment variables
- `GEE_KEY_PATH` — set to `/etc/secrets/gee-key.json`, matching a Secret
  File upload of the service account's JSON key

## Tech stack

Python, FastAPI, Pydantic (with camelCase field aliases to match the
Java backend's JSON convention), `google-genai`, `earthengine-api`,
`requests`, `python-dotenv`, `pillow`.
