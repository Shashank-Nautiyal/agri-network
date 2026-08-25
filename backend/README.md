# Agri Backend (Spring Boot)

Backend service for the Digital Agriculture Network prototype
(Hack2skill "Build with AI: Code for Communities" — Challenge #4).

## Run locally

**Option A — Maven Wrapper (no local Maven install needed):**
```bash
./mvnw spring-boot:run        # macOS/Linux
mvnw.cmd spring-boot:run      # Windows
```
The wrapper scripts included here (`mvnw`, `mvnw.cmd`, `.mvn/wrapper/`) will
download Maven automatically on first run — you only need a JDK 17 installed.

⚠️ **If `./mvnw` fails for you** (permission or script issues), the most
reliable fix is to regenerate the wrapper from your own machine instead of
trusting the hand-written scripts in this zip:
- In IntelliJ: right-click `pom.xml` → Maven → "Add Maven Wrapper".
- Or from command line, if you have Maven installed once, anywhere:
  `mvn -N wrapper:wrapper -Dmaven=3.9.9` — this regenerates correct wrapper
  files in this project directory.

**Option B — Install Maven directly (most reliable):**
- macOS: `brew install maven`
- Ubuntu/Debian: `sudo apt install maven`
- Windows: `choco install maven` (via Chocolatey) or download from
  https://maven.apache.org/download.cgi and add `bin/` to PATH.

Then run:
```bash
mvn spring-boot:run
```
Server starts on `http://localhost:8080`. Check `GET /health` to confirm it's up.

## Current status: Phase 1 (mocked end-to-end flow)
All AI-backed endpoints currently return **mocked data** so the full flow
works without waiting on the AI/ML service. See `AiServiceClient.java` —
flip `USE_MOCK = false` once the teammate's AI service is deployed and
`AI_SERVICE_BASE_URL` is set.

Farmer and history storage is currently **in-memory** (`FarmerService`,
`AdvisoryHistoryService`) — swap for Firestore in Phase 2 without changing
controller code.

## API contract (matches the AI/ML service contract)

| Endpoint | Method | Purpose |
|---|---|---|
| `/api/farmers` | POST | Register a farmer profile |
| `/api/farmers/{id}` | GET | Fetch a farmer profile |
| `/api/diagnose` | POST | Crop disease diagnosis from photo |
| `/api/advisory` | POST | Crop/planting advisory from soil+weather |
| `/api/voice-query` | POST | Voice/text Q&A (STT can happen client-side via Web Speech API) |
| `/api/district/{districtId}/regional-alerts` | GET | Anonymized cross-district disease trend — proves the interoperability/"digital public good" story |
| `/health` | GET | Health check for deployment verification |

### Example: `/api/diagnose`
Request:
```json
{ "imageBase64": "...", "districtId": "dehradun", "farmerId": "abc123" }
```
Response:
```json
{ "disease": "Early Blight", "confidence": 0.87, "treatmentAdvice": "...", "language": "en" }
```

### Example: `/api/advisory`
Request:
```json
{ "districtId": "dehradun", "cropType": "wheat", "farmerId": "abc123" }
```
Response:
```json
{ "recommendation": "...", "ndviSummary": "NDVI: 0.62", "weatherRisk": "Low", "language": "en" }
```

### Example: `/api/voice-query`
Request (transcript already produced client-side via Web Speech API):
```json
{ "transcript": "What should I plant this season?", "farmerId": "abc123", "languageHint": "hi-IN" }
```
Response:
```json
{ "transcript": "...", "responseText": "...", "responseAudioBase64": null, "language": "hi-IN" }
```

## For the AI/ML teammate
Your service should expose matching `POST /diagnose`, `POST /advisory`,
`POST /voice-query` endpoints returning the same JSON shapes shown above.
Once deployed, set the `AI_SERVICE_BASE_URL` env var here and flip
`USE_MOCK = false` in `AiServiceClient.java` — no other changes needed on
this side.

## Deployment (Render/Railway — no billing account required)
This repo includes a `Dockerfile`. Both Render and Railway can build and
deploy directly from a Dockerfile-based repo on their free tiers:
1. Push this repo to GitHub.
2. Create a new Web Service on Render (or Railway), point it at the repo.
3. It auto-detects the `Dockerfile` and builds/deploys.
4. Set env var `AI_SERVICE_BASE_URL` once the AI service is live.
5. The platform injects `PORT` automatically — already wired up in `application.yml`.

## Tech decisions (see project notes)
- No GCP billing account required: hosting on Render/Railway instead of Cloud
  Run, Gemini handles translation instead of a separate Translation API,
  Web Speech API (browser-native) handles STT/TTS instead of Cloud Speech APIs,
  Firestore (free tier) instead of Cloud SQL once persistence is wired in.
