# KhetSaathi Frontend

Three-file HTML/CSS/JS frontend for the KhetSaathi Digital Agriculture
Network prototype — `index.html` (markup), `style.css` (styles), `app.js`
(logic). No build step, no frameworks. Deployed as a Render Static Site.

## Status

Fully built, deployed, and verified end-to-end against the live backend:
- ✅ Farmer registration, diagnose, advisory, regenerative advice, voice
  Q&A, and regional alerts all tested working in production
- ✅ CORS-compatible with the backend (backend allows all origins)
- ✅ Voice tested working in both English and Hindi

## Before you use it

Open `app.js` and confirm this line points to your deployed backend:
```js
const BACKEND_BASE_URL = "https://<your-backend>.onrender.com";
```
Use `http://localhost:8080` only for local development against a
locally-running backend.

## What's covered

- **Farmer registration** — first-visit form, stored in `localStorage` so
  returning farmers skip straight to the app. Calls `POST /api/farmers`.
- **Diagnose** — camera capture or file upload (`capture="environment"`
  opens the rear camera on mobile), base64-encodes client-side, calls
  `POST /api/diagnose`.
- **Advisory** — browser Geolocation API for lat/long, manual
  country/state/district (set at registration), calls `POST /api/advisory`.
- **Regenerative advice** — same location handling, with an optional
  "I have lab soil data" toggle; when left off, the backend/AI service
  falls back to a SoilGrids estimate. Calls `POST /api/regenerative-advice`.
- **Ask (voice)** — Web Speech API for both STT (`SpeechRecognition`) and
  TTS (`speechSynthesis`), entirely client-side — no billed speech API
  involved. Falls back to a text box if the browser doesn't support speech
  recognition. Calls `POST /api/voice-query`.
- **Regional alerts** — pulls anonymized disease-count data for the
  farmer's district, the visible proof of the cross-district/cross-state
  "digital public good" requirement. Calls
  `GET /api/district/{districtId}/regional-alerts`.

## Design

Inline SVG icons throughout (no emoji, no icon font/network request),
a warm-stone/deep-forest-green palette distinct from generic default
themes, subtle shadows and motion (button lift on hover, a pulse ring on
the mic while listening), and result cards that tint by category/risk
level rather than relying on a plain border. `Fraunces` for headlines,
`Space Grotesk` for UI/body text.

## Known bug fix worth knowing about

The "Play answer" (TTS) button's click handler is attached via
`addEventListener` in JS, not an inline `onclick="..."` attribute — an
earlier version embedded dynamic text directly into an `onclick`
attribute string via `JSON.stringify`, which silently broke because the
attribute's own double quotes conflicted with the stringified text's
double quotes. If extending this pattern elsewhere, avoid inlining
dynamic strings into `onclick` attributes for the same reason.

## Browser support notes

- **Web Speech API (voice)** works best in Chrome (desktop and Android)
  and Edge. Safari has partial support; Firefox does not support
  `SpeechRecognition` at all — the app degrades to the always-visible
  text input in that case.
- **Geolocation** requires HTTPS in production (works over
  `http://localhost` for local dev without issue).

## Run locally

Open `index.html` directly, or serve via a local static server if the
microphone permission prompt doesn't appear when opened as a plain file:
```bash
python -m http.server 5500
```

## Deployment (Render Static Site)

- **Root Directory:** `frontend`
- **Build Command:** none needed
- **Publish Directory:** `.`

Render Static Sites don't spin down after inactivity (unlike free-tier
Web Services), so the frontend itself loads instantly — only the
backend and AI service have a cold-start delay after idle periods.
