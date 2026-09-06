# Agri AI Service (FastAPI)

AI/ML service for the Digital Agriculture Network prototype — handles
Gemini multimodal crop disease diagnosis, Earth Engine-based advisories,
and voice-query responses. Called by the Spring Boot backend over plain
REST.

## Run locally
```bash
pip install -r requirements.txt
uvicorn main:app --reload --port 8000
```
Visit `http://localhost:8000/docs` — FastAPI auto-generates interactive
Swagger docs for every endpoint, useful for testing without needing curl
or Postman.

## Current status: mocked
Every endpoint currently returns hardcoded mock data so the Spring Boot
backend can integrate against this service immediately, without waiting
on real AI wiring. Each function in `main.py` has a `TODO` docstring
explaining exactly what to replace and how — that's your starting point.

## API contract (must match the Spring Boot backend exactly)

| Endpoint | Method | Purpose |
|---|---|---|
| `/diagnose` | POST | Crop disease diagnosis from a photo |
| `/advisory` | POST | Crop/planting advisory from soil+weather |
| `/voice-query` | POST | Voice/text Q&A |
| `/health` | GET | Health check |

If you need to change a field name or add a field, update it here AND
tell your teammate to update the matching Java DTO in the Spring Boot
`backend/` — keeping both sides in sync is the only thing that matters
for integration to "just work."

### `/diagnose`
Request:
```json
{ "imageBase64": "...", "districtId": "dehradun", "farmerId": "abc123" }
```
Response:
```json
{ "disease": "Early Blight", "confidence": 0.87, "treatmentAdvice": "...", "language": "en" }
```

### `/advisory`
Request:
```json
{ "districtId": "dehradun", "cropType": "wheat", "farmerId": "abc123" }
```
Response:
```json
{ "recommendation": "...", "ndviSummary": "NDVI: 0.62", "weatherRisk": "Low", "language": "en" }
```

### `/voice-query`
Request:
```json
{ "transcript": "What should I plant this season?", "farmerId": "abc123", "languageHint": "hi-IN" }
```
Response:
```json
{ "transcript": "...", "responseText": "...", "responseAudioBase64": null, "language": "hi-IN" }
```

## Integration notes (billing-free stack)
- **No separate Translation API** — prompt Gemini to respond directly in
  the target language (`languageHint`). Simpler and avoids a second billed
  Google service.
- **No server-side Speech-to-Text/Text-to-Speech** — the frontend uses the
  browser's Web Speech API (free, client-side) for both. `transcript` will
  usually already be filled in when this service receives a `/voice-query`
  request; `audioBase64` is there as a fallback only if we later decide to
  do STT server-side.
- **Earth Engine is free** for non-commercial/research use — no billing
  account needed for that specific API. Sign up at
  https://earthengine.google.com/ and follow the Python API auth guide.
- **Gemini API** — get a free API key from Google AI Studio
  (https://aistudio.google.com/). Store it as an environment variable
  (`GEMINI_API_KEY`), never hardcode it in `main.py`.

## Suggested build order
1. Get Gemini API key working — test with one static image first (see
   the `/diagnose` TODO) before wiring it into the endpoint.
2. Get Earth Engine authenticated — pull one real NDVI value for your
   pilot district (agree on the district with your teammate — whatever
   they hardcoded on the backend side for testing) to confirm the
   pipeline works end-to-end before building the full `/advisory` logic.
3. Wire `/diagnose` for real, test against a few sample images
   (PlantVillage dataset is a good source of test images).
4. Wire `/advisory` for real using the Earth Engine + weather data.
5. Wire `/voice-query` last — reuses the same Gemini call pattern as the
   others, just with conversational framing instead of diagnosis/advisory.

## Deployment (Render/Railway — no billing account required)
Same pattern as the backend: this repo includes a `Dockerfile`. Push to
GitHub, create a new Web Service on Render/Railway pointed at this folder
(set Root Directory to `ai-service` if in a monorepo), and it'll build and
deploy automatically. Once live, share the URL with your teammate so they
can set `AI_SERVICE_BASE_URL` on the Spring Boot side and flip
`USE_MOCK = false`.

Remember to set `GEMINI_API_KEY` (and any Earth Engine service account
credentials) as environment variables on whatever platform you deploy to
— never commit them to the repo.
