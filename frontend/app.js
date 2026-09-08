const BACKEND_BASE_URL = "https://agri-network-backend.onrender.com"; // e.g. https://agri-network-backend.onrender.com

const ICONS = {
  leaf: '<svg class="icon" viewBox="0 0 24 24"><circle cx="11" cy="11" r="7"/><path d="M21 21l-4.3-4.3"/></svg>',
  compass: '<svg class="icon" viewBox="0 0 24 24"><circle cx="12" cy="12" r="9"/><path d="M15 9l-2 5-5 2 2-5 5-2z"/></svg>',
  soil: '<svg class="icon" viewBox="0 0 24 24"><path d="M3 18c3-3 6-3 9-3s6 0 9 3"/><path d="M12 15V8"/><path d="M12 8c-1.5-1.5-1.5-3.5 0-5 1.5 1.5 1.5 3.5 0 5z"/></svg>',
  chat: '<svg class="icon" viewBox="0 0 24 24"><path d="M21 11.5a8.4 8.4 0 01-9 8.4 8.5 8.5 0 01-4-1L3 20l1.1-4.9A8.4 8.4 0 1121 11.5z"/></svg>',
  speaker: '<svg class="icon" viewBox="0 0 24 24" style="width:15px;height:15px;"><path d="M5 9v6h4l5 4V5L9 9H5z"/><path d="M17.5 8.5a5 5 0 010 7"/></svg>'
};

// ====================================================================
// State
// ====================================================================
let farmer = null; // { id, name, districtId (using district field), location }
let selectedImageBase64 = null;
let sharedLocation = null; // { country, state, district, latitude, longitude }
let recognizer = null;
let listening = false;

function loadFarmerFromStorage() {
  try {
    const raw = localStorage.getItem("khetsaathi_farmer");
    return raw ? JSON.parse(raw) : null;
  } catch (e) { return null; }
}
function saveFarmerToStorage(f) {
  try { localStorage.setItem("khetsaathi_farmer", JSON.stringify(f)); } catch (e) {}
}

function init() {
  farmer = loadFarmerFromStorage();
  if (farmer) {
    showApp();
  } else {
    showOverlay();
  }
  setupTabs();
}

function showOverlay() {
  document.getElementById("registerOverlay").classList.remove("hidden");
  document.getElementById("app").classList.add("hidden");
}
function showApp() {
  document.getElementById("registerOverlay").classList.add("hidden");
  document.getElementById("app").classList.remove("hidden");
  document.getElementById("farmerChip").innerHTML = `
    <svg class="icon" viewBox="0 0 24 24" style="width:14px;height:14px;"><circle cx="12" cy="8" r="4"/><path d="M4 21c0-4.4 3.6-8 8-8s8 3.6 8 8"/></svg>
    ${farmer.name || "Farmer"}
  `;
  sharedLocation = {
    country: farmer.country || "India",
    state: farmer.state || "",
    district: farmer.district || "",
    latitude: null,
    longitude: null
  };
}

async function registerFarmer() {
  const name = document.getElementById("regName").value.trim();
  const phone = document.getElementById("regPhone").value.trim();
  const lang = document.getElementById("regLang").value;
  const country = document.getElementById("regCountry").value;
  const state = document.getElementById("regState").value.trim();
  const district = document.getElementById("regDistrict").value.trim();
  const errBox = document.getElementById("regError");
  errBox.classList.add("hidden");

  if (!name || !district) {
    errBox.textContent = "Please enter at least your name and district.";
    errBox.classList.remove("hidden");
    return;
  }

  try {
    const resp = await fetch(`${BACKEND_BASE_URL}/api/farmers`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ name, phone, districtId: district, preferredLanguage: lang })
    });
    if (!resp.ok) throw new Error("Registration failed (" + resp.status + ")");
    const data = await resp.json();
    farmer = { id: data.id, name, phone, country, state, district, preferredLanguage: lang };
    saveFarmerToStorage(farmer);
    showApp();
  } catch (e) {
    errBox.textContent = "Couldn't reach the server. Check your connection and try again.";
    errBox.classList.remove("hidden");
  }
}

// ====================================================================
// Tabs
// ====================================================================
function setupTabs() {
  document.querySelectorAll(".tab-btn").forEach(btn => {
    btn.addEventListener("click", () => {
      document.querySelectorAll(".tab-btn").forEach(b => b.classList.remove("active"));
      document.querySelectorAll(".panel").forEach(p => p.classList.remove("active"));
      btn.classList.add("active");
      document.getElementById("panel-" + btn.dataset.tab).classList.add("active");
      if (btn.dataset.tab === "alerts") loadAlerts();
    });
  });
}

// ====================================================================
// Geolocation (shared across Advisory + Regenerative)
// ====================================================================
function useMyLocation(context) {
  const chipId = context === "advisory" ? "advisoryLocChip" : "regenLocChip";
  const chip = document.getElementById(chipId);
  if (!navigator.geolocation) {
    chip.textContent = "Geolocation isn't supported on this device.";
    return;
  }
  chip.textContent = "Getting your location…";
  navigator.geolocation.getCurrentPosition(
    (pos) => {
      sharedLocation.latitude = pos.coords.latitude;
      sharedLocation.longitude = pos.coords.longitude;
      chip.textContent = `${sharedLocation.district || "Location"} — (${pos.coords.latitude.toFixed(4)}, ${pos.coords.longitude.toFixed(4)})`;
    },
    (err) => {
      chip.textContent = "Couldn't get your location — using your registered district instead.";
    },
    { timeout: 8000 }
  );
}

function currentLocationPayload() {
  return {
    country: sharedLocation.country || "India",
    state: sharedLocation.state || "",
    district: sharedLocation.district || "",
    latitude: sharedLocation.latitude || 0,
    longitude: sharedLocation.longitude || 0
  };
}

// ====================================================================
// Diagnose
// ====================================================================
function onImageSelected(event) {
  const file = event.target.files[0];
  if (!file) return;
  const reader = new FileReader();
  reader.onload = () => {
    const result = reader.result;
    selectedImageBase64 = result.split(",")[1];
    document.getElementById("previewImg").src = result;
    document.getElementById("previewImg").classList.remove("hidden");
    document.getElementById("dropContent").classList.add("hidden");
    document.getElementById("diagnoseBtn").disabled = false;
  };
  reader.readAsDataURL(file);
}

async function submitDiagnose() {
  const loading = document.getElementById("diagnoseLoading");
  const errBox = document.getElementById("diagnoseError");
  const resultBox = document.getElementById("diagnoseResult");
  errBox.classList.add("hidden");
  resultBox.classList.add("hidden");
  loading.classList.remove("hidden");
  document.getElementById("diagnoseBtn").disabled = true;

  try {
    const resp = await fetch(`${BACKEND_BASE_URL}/api/diagnose`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        imageBase64: selectedImageBase64,
        districtId: farmer.district,
        farmerId: farmer.id
      })
    });
    if (!resp.ok) throw new Error("Request failed (" + resp.status + ")");
    const data = await resp.json();

    resultBox.className = "result";
    resultBox.innerHTML = `
      <h3>${ICONS.leaf}${data.disease}</h3>
      <div class="label">Confidence</div>
      <div class="confidence-bar"><div style="width:${Math.round((data.confidence||0)*100)}%"></div></div>
      <div class="label">Treatment advice</div>
      <p>${data.treatmentAdvice}</p>
    `;
    resultBox.classList.remove("hidden");
  } catch (e) {
    errBox.textContent = "Couldn't analyze the photo. Please try again.";
    errBox.classList.remove("hidden");
  } finally {
    loading.classList.add("hidden");
    document.getElementById("diagnoseBtn").disabled = false;
  }
}

// ====================================================================
// Advisory
// ====================================================================
function riskClass(level) {
  if (!level) return "";
  const l = level.toLowerCase();
  if (l === "high") return "risk-high";
  if (l === "moderate") return "risk-moderate";
  return "risk-low";
}
function badgeClass(level) {
  if (!level) return "low";
  const l = level.toLowerCase();
  if (l === "high") return "high";
  if (l === "moderate") return "moderate";
  return "low";
}

async function submitAdvisory() {
  const loading = document.getElementById("advisoryLoading");
  const errBox = document.getElementById("advisoryError");
  const resultBox = document.getElementById("advisoryResult");
  errBox.classList.add("hidden");
  resultBox.classList.add("hidden");
  loading.classList.remove("hidden");

  const cropType = document.getElementById("advCrop").value.trim() || undefined;

  try {
    const resp = await fetch(`${BACKEND_BASE_URL}/api/advisory`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        farmerId: farmer.id,
        location: currentLocationPayload(),
        cropType
      })
    });
    if (!resp.ok) throw new Error("Request failed (" + resp.status + ")");
    const data = await resp.json();

    resultBox.className = "result " + riskClass(data.diseaseRiskLevel);
    resultBox.innerHTML = `
      <h3>${ICONS.compass}Advisory <span class="badge ${badgeClass(data.diseaseRiskLevel)}">${data.diseaseRiskLevel || "n/a"} risk</span></h3>
      <p>${data.recommendation}</p>
      <div class="label">Satellite (NDVI)</div>
      <p>${data.ndviSummary}</p>
      <div class="label">Soil</div>
      <p>${data.soilSummary}</p>
      <div class="label">Weather risk</div>
      <p>${data.weatherRisk}</p>
    `;
    resultBox.classList.remove("hidden");
  } catch (e) {
    errBox.textContent = "Couldn't fetch advisory right now. Please try again.";
    errBox.classList.remove("hidden");
  } finally {
    loading.classList.add("hidden");
  }
}

// ====================================================================
// Regenerative
// ====================================================================
function toggleSoilFields() {
  document.getElementById("soilFields").classList.toggle("hidden");
}

async function submitRegenerative() {
  const loading = document.getElementById("regenLoading");
  const errBox = document.getElementById("regenError");
  const resultBox = document.getElementById("regenResult");
  errBox.classList.add("hidden");
  resultBox.classList.add("hidden");
  loading.classList.remove("hidden");

  const cropType = document.getElementById("regenCrop").value.trim();
  if (!cropType) {
    errBox.textContent = "Please enter a crop type.";
    errBox.classList.remove("hidden");
    loading.classList.add("hidden");
    return;
  }

  const soilVisible = !document.getElementById("soilFields").classList.contains("hidden");
  let soilData = undefined;
  if (soilVisible) {
    const ph = document.getElementById("soilPh").value;
    if (ph) {
      soilData = {
        ph: parseFloat(ph) || 0,
        nitrogen: parseFloat(document.getElementById("soilN").value) || 0,
        phosphorus: parseFloat(document.getElementById("soilP").value) || 0,
        potassium: parseFloat(document.getElementById("soilK").value) || 0,
        organicCarbon: parseFloat(document.getElementById("soilOC").value) || 0,
        moisture: parseFloat(document.getElementById("soilMoisture").value) || 0
      };
    }
  }

  try {
    const resp = await fetch(`${BACKEND_BASE_URL}/api/regenerative-advice`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        farmerId: farmer.id,
        location: currentLocationPayload(),
        soilData,
        cropType
      })
    });
    if (!resp.ok) throw new Error("Request failed (" + resp.status + ")");
    const data = await resp.json();

    resultBox.className = "result";
    resultBox.innerHTML = `
      <h3>${ICONS.soil}Recommended practices</h3>
      <p>${(data.practices || []).map(p => "• " + p).join("<br/>")}</p>
      <div class="label">Why</div>
      <p>${data.reasoning}</p>
      <div class="label">Expected benefit</div>
      <p>${data.expectedBenefit}</p>
    `;
    resultBox.classList.remove("hidden");
  } catch (e) {
    errBox.textContent = "Couldn't fetch recommendations right now. Please try again.";
    errBox.classList.remove("hidden");
  } finally {
    loading.classList.add("hidden");
  }
}

// ====================================================================
// Voice / Ask (Web Speech API — client-side STT + TTS
// ====================================================================
function toggleListening() {
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
  if (!SpeechRecognition) {
    document.getElementById("micStatus").textContent = "Voice input isn't supported in this browser — please type your question instead.";
    return;
  }
  if (listening) {
    recognizer.stop();
    return;
  }
  recognizer = new SpeechRecognition();
  recognizer.lang = document.getElementById("voiceLang").value;
  recognizer.interimResults = false;
  recognizer.maxAlternatives = 1;

  recognizer.onstart = () => {
    listening = true;
    document.getElementById("micBtn").classList.add("listening");
    document.getElementById("micStatus").textContent = "Listening…";
  };
  recognizer.onresult = (event) => {
    const transcript = event.results[0][0].transcript;
    document.getElementById("voiceTranscript").value = transcript;
  };
  recognizer.onerror = () => {
    document.getElementById("micStatus").textContent = "Didn't catch that — please try again or type instead.";
  };
  recognizer.onend = () => {
    listening = false;
    document.getElementById("micBtn").classList.remove("listening");
    document.getElementById("micStatus").textContent = "Tap to speak";
  };
  recognizer.start();
}

function speakText(text, lang) {
  if (!window.speechSynthesis) return;
  const utterance = new SpeechSynthesisUtterance(text);
  utterance.lang = lang;
  window.speechSynthesis.speak(utterance);
}

async function submitVoiceQuery() {
  const loading = document.getElementById("voiceLoading");
  const errBox = document.getElementById("voiceError");
  const resultBox = document.getElementById("voiceResult");
  errBox.classList.add("hidden");
  resultBox.classList.add("hidden");

  const transcript = document.getElementById("voiceTranscript").value.trim();
  const languageHint = document.getElementById("voiceLang").value;
  if (!transcript) {
    errBox.textContent = "Please speak or type a question first.";
    errBox.classList.remove("hidden");
    return;
  }
  loading.classList.remove("hidden");

  try {
    const resp = await fetch(`${BACKEND_BASE_URL}/api/voice-query`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ transcript, farmerId: farmer.id, languageHint })
    });
    if (!resp.ok) throw new Error("Request failed (" + resp.status + ")");
    const data = await resp.json();

    resultBox.className = "result";
    resultBox.innerHTML = `
      <h3>${ICONS.chat}Answer</h3>
      <p>${data.responseText}</p>
      <button class="secondary" id="playAnswerBtn" style="margin-top:10px;">${ICONS.speaker} Play answer</button>
    `;
    resultBox.classList.remove("hidden");

    document.getElementById("playAnswerBtn").addEventListener("click", () => {
      speakText(data.responseText, languageHint);
    });
  } catch (e) {
    errBox.textContent = "Couldn't get an answer right now. Please try again.";
    errBox.classList.remove("hidden");
  } finally {
    loading.classList.add("hidden");
  }
}

// ====================================================================
// Regional alerts
// ====================================================================
async function loadAlerts() {
  const loading = document.getElementById("alertsLoading");
  const errBox = document.getElementById("alertsError");
  const list = document.getElementById("alertsList");
  errBox.classList.add("hidden");
  list.innerHTML = "";
  loading.classList.remove("hidden");

  try {
    const district = farmer.district;
    const resp = await fetch(`${BACKEND_BASE_URL}/api/district/${encodeURIComponent(district)}/regional-alerts`);
    if (!resp.ok) throw new Error("Request failed (" + resp.status + ")");
    const data = await resp.json();
    const entries = Object.entries(data || {});
    if (entries.length === 0) {
      list.innerHTML = `<p style="color:var(--ink-soft);">No disease reports in ${district} yet.</p>`;
    } else {
      list.innerHTML = entries.map(([name, count]) => `
        <div class="alert-row">
          <span>${name}</span>
          <span class="count">${count}</span>
        </div>
      `).join("");
    }
  } catch (e) {
    errBox.textContent = "Couldn't load regional alerts right now.";
    errBox.classList.remove("hidden");
  } finally {
    loading.classList.add("hidden");
  }
}

init();
