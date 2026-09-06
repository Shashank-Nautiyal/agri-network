from pydantic import BaseModel ,Field ,ConfigDict
from fastapi import FastAPI
from typing import Optional,Literal,List,Dict
from datetime import datetime , timedelta
import PIL.Image
from google import genai
import base64
import time
import io
import json 
from dotenv import load_dotenv
import os 
load_dotenv()
import ee
import requests
from google.genai import types

credentials = ee.ServiceAccountCredentials(
    os.getenv("GEE_SERVICE_ACCOUNT_EMAIL"),
    os.getenv("GEE_SERVICE_ACCOUNT_KEY_PATH")
)

try:
    ee.Initialize(credentials)
except Exception as e :
     print(f"Error: {e}")

try :
    client = genai.Client(api_key=os.getenv("GOOGLE_API_KEY"))
    FAST_CONFIG = types.GenerateContentConfig(
    thinking_config=types.ThinkingConfig(thinking_level=types.ThinkingLevel.LOW)
    )
except Exception as e :
    print(f"Error : {e}")

app=FastAPI()

# schema for location 

class Location (BaseModel):
    model_config=ConfigDict(populate_by_name=True)

    country: str = Field(...,description="The country of Farmer from The BRICS country")
    state: str = Field(...,description="The state name ")
    district: str = Field(...,description="The district name ")
    latitude: float = Field(...,description="The latitude value of Farmer location ")
    longitude: float = Field(...,description="The longitude value of framer location ")

#schema for soil data 
class SoilData(BaseModel):

    model_config=ConfigDict(populate_by_name=True)

    ph: float =Field(...,description="The ph value of The soil ")
    nitrogen: float =Field(...,description= "The nitrogen level of The soil ")
    phosphorus: float =Field(...,description= "The phosphorus value of The soil ")
    potassium: float =Field(...,description= "Potassium value of The soil ")
    organic_carbon: float =Field(...,description= "Organic carbon vlue of The soil" ,alias="organicCarbon")
    moisture: float =Field(...,description= "How much moisture present The soil ")


class WeatherData(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    temperature: float=Field(...,description="The temprature of The location ")
    rainfall_mm: float=Field(...,description= "The amount of rainfall in mm")
    humidity: float=Field(...,description="humidity level of The location ")
    forecast_summary: str=Field(...,description= "This is forecast summery ")

class SatelliteData (BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    ndvi: float=Field(...,description="The Normalized Difference Vegetation Index value ")
    vegetation_health: Literal["healthy", "stressed", "critical"] = Field(
    ...,
    description="The health of vegetation",
    examples=["healthy", "stressed"]
) 
    land_surface_temp: float  =Field(...,description="Temparture of land surface")


# diagnosis 

class DiagnoseRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    image_base64: str = Field(..., alias="imageBase64")
    district_id: str = Field(..., alias="districtId")
    farmer_id: str = Field(..., alias="farmerId")



class DiagnoseResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    disease: str
    confidence: float
    treatment_advice: str = Field(..., alias="treatmentAdvice")
    language: str = "en"

# advisory 

class AdvisoryRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    farmer_id: str = Field(..., alias="farmerId")
    location: Location
    crop_type: Optional[str] = Field(None, alias="cropType")

class AdvisoryResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    recommendation: str
    ndvi_summary: str = Field(..., alias="ndviSummary")
    soil_summary: str = Field(..., alias="soilSummary")
    weather_risk: str = Field(..., alias="weatherRisk")
    disease_risk_level: Literal["low", "moderate", "high"] = Field(..., alias="diseaseRiskLevel")
    confidence: float
    language: str = "en"


#  regenerative-advice 

class RegenerativeRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    farmer_id: str = Field(..., alias="farmerId")
    location: Location
    soil_data: Optional[SoilData] = Field(None, alias="soilData")
    crop_type: str = Field(..., alias="cropType")


class RegenerativeResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    practices: List[str]
    reasoning: str
    expected_benefit: str = Field(..., alias="expectedBenefit")

# voice-query 

class VoiceQueryRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    transcript: str
    farmer_id: str = Field(..., alias="farmerId")
    language_hint: Optional[str] = Field(None, alias="languageHint")


class VoiceQueryResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    transcript: str
    response_text: str = Field(..., alias="responseText")
    response_audio_base64: Optional[str] = Field(None, alias="responseAudioBase64")
    language: str = "en"

class SchemaResponse(BaseModel):
   
    model_config = ConfigDict(populate_by_name=True)

    schema_version: str = Field(..., alias="schemaVersion")
    fields: dict

# function for extracting of gemini response and converting that into json format " "
def extract_json(raw_text: str) -> dict:
    text = raw_text.strip()
    if text.startswith("```"):
        text = text.strip("`")
        if text.startswith("json"):
            text = text[4:]
    return json.loads(text.strip())

# This is for geting the ndvi ( Normalized Difference Vegetation Index) value

def get_ndvi(latitude: float, longitude: float) -> float | None:
    try:
        point = ee.Geometry.Point([longitude, latitude])
        end_date = datetime.now().strftime("%Y-%m-%d")
        start_date = (datetime.now() - timedelta(days=90)).strftime("%Y-%m-%d")

        collection = (
            ee.ImageCollection("COPERNICUS/S2_SR_HARMONIZED")
            .filterBounds(point)
            .filterDate(start_date, end_date)
            .filter(ee.Filter.lt("CLOUDY_PIXEL_PERCENTAGE", 20))
            .sort("CLOUDY_PIXEL_PERCENTAGE")
        )

        image = collection.first()
        if image is None:
            return None

        ndvi = image.normalizedDifference(["B8", "B4"]).rename("NDVI")
        value = ndvi.reduceRegion(
            reducer=ee.Reducer.mean(),
            geometry=point.buffer(100),
            scale=10
        ).get("NDVI")

        return value.getInfo()
    except Exception:
        return None

# this function for to getting the weather data using the open-meteo " "

def get_weather(latitude: float, longitude: float) -> dict | None:

    try:
        url = "https://api.open-meteo.com/v1/forecast"
        params = {
            "latitude": latitude,
            "longitude": longitude,
            "daily": "temperature_2m_max,temperature_2m_min,precipitation_sum",
            "current": "temperature_2m,relative_humidity_2m",
            "forecast_days": 7,
            "timezone": "auto",
        }
        resp = requests.get(url, params=params, timeout=8)
        resp.raise_for_status()
        data = resp.json()

        current = data.get("current", {})
        daily = data.get("daily", {})

        total_rain = sum(daily.get("precipitation_sum", []) or [0])
        max_temps = daily.get("temperature_2m_max", [])
        min_temps = daily.get("temperature_2m_min", [])

        return {
            "current_temperature": current.get("temperature_2m"),
            "current_humidity": current.get("relative_humidity_2m"),
            "forecast_max_temp": max(max_temps) if max_temps else None,
            "forecast_min_temp": min(min_temps) if min_temps else None,
            "total_rainfall_7day_mm": round(total_rain, 1),
        }
    except Exception as e:
        print(f"DEBUG get_weather error: {type(e).__name__}:{e}")
        return None

def get_soil_data(latitude: float, longitude: float) -> dict | None:
    """
    Returns estimated soil data from SoilGrids for the given coordinates,
    used as a fallback when the farmer hasn't submitted lab-tested soil data.

    Covers: ph, nitrogen, organic_carbon, moisture.
    NOT covered by SoilGrids: phosphorus, potassium — these stay as None/0.0
    until a suitable data source is added (Bhoomi Geoportal has no queryable
    API as of now — see note below).

    Returns None if the request fails or the point has no SoilGrids coverage
    (some coordinates return null for all properties — happens in data-sparse
    regions, water bodies, etc).
    """
    try:
        url = "https://rest.isric.org/soilgrids/v2.0/properties/query"
        params = {
            "lon": longitude,
            "lat": latitude,
            "property": ["phh2o", "nitrogen", "soc", "wv0033"],
            "depth": "0-5cm",
            "value": "mean",
        }
        resp = requests.get(url, params=params, timeout=8)
        resp.raise_for_status()
        data = resp.json()

        layers = {layer["name"]: layer for layer in data["properties"]["layers"]}

        def get_value(layer_name, divisor=1):
            try:
                depths = layers[layer_name]["depths"]
                raw = depths[0]["values"]["mean"]
                return raw / divisor if raw is not None else None
            except (KeyError, IndexError, TypeError):
                return None

        # SoilGrids conversion factors (per their documented d_factor):
        # phh2o: pH*10 -> divide by 10
        # nitrogen: cg/kg -> divide by 100 (approx g/kg)
        # soc: dg/kg -> divide by 10 (approx g/kg)
        # wv0033: (10-2 cm3/cm3)*10 -> divide by 10, gives volumetric moisture %
        ph = get_value("phh2o", divisor=10)
        nitrogen = get_value("nitrogen", divisor=100)
        organic_carbon = get_value("soc", divisor=10)
        moisture = get_value("wv0033", divisor=10)

        if ph is None:
            return None

        return {
            "ph": ph,
            "nitrogen": nitrogen if nitrogen is not None else 0.0,
            "phosphorus": None,   # not available 
            "potassium": None,    # not available we will do something these 
            "organic_carbon": organic_carbon if organic_carbon is not None else 0.0,
            "moisture": moisture if moisture is not None else 0.0,
        }
    except Exception:
        return None


@app.get("/")
def home():
    return {"message": "Agri AI Service", "docs": "/docs"}


@app.get("/health")
def health():
    return {"status": "ok", "service": "agri-ai-service"}


@app.post("/diagnose", response_model=DiagnoseResponse, response_model_by_alias=True)
def diagnose(req: DiagnoseRequest):
    try:
        image_bytes = base64.b64decode(req.image_base64)
        image = PIL.Image.open(io.BytesIO(image_bytes))
    except Exception:
        return DiagnoseResponse(
            disease="Unable to process image",
            confidence=0.0,
            treatment_advice="Please upload a clear photo of the affected leaf.",
            language="en",
        )
    
    prompt="""You are an agricultural expert. Look at this crop leaf image
    and identify any disease present. Respond ONLY with valid JSON in this
    exact format, no other text:
    {
      "disease": "<disease name, or 'Healthy' if no disease detected>",
      "confidence": <float between 0 and 1>,
      "treatment_advice": "<short, practical treatment advice, prefer organic/regenerative options>"
    }"""

    try:
        response = client.models.generate_content(model="gemini-3.6-flash", contents=[prompt, image], config=FAST_CONFIG)
        result = extract_json(response.text)
    except Exception:
        result = {"disease": "Unable to determine",
                "confidence": 0.0,
                "treatment_advice": "Please try again with a clearer image, or consult a local agricultural extension officer."
            }
        
    return DiagnoseResponse(
        disease=result["disease"],
        confidence=result["confidence"],
        treatment_advice=result["treatment_advice"],
        language="en",
    )


@app.post("/advisory", response_model=AdvisoryResponse, response_model_by_alias=True)
def advisory(req: AdvisoryRequest):

    ndvi_value = get_ndvi(req.location.latitude, req.location.longitude)
    ndvi_status = "unavailable" if ndvi_value is None else f"{ndvi_value:.2f}"
    
    weather = get_weather(req.location.latitude, req.location.longitude)
    if weather is not None:
        weather_block = f'''
        Current temperature: {weather['current_temperature']}°C
        Current humidity: {weather['current_humidity']}%
        7-day forecast: {weather['forecast_min_temp']}°C to {weather['forecast_max_temp']}°C
        Total expected rainfall (7 days): {weather['total_rainfall_7day_mm']}mm'''
    else:
        weather_block = "\n    Weather data unavailable — give general seasonal guidance."


    prompt =f'''You are an agricultural advisor. Based on the following data
    for a farm, generate a practical recommendation.

    Location: {req.location.district}, {req.location.state}, {req.location.country}
    Crop: {req.crop_type or "not specified"}
    NDVI (vegetation health index): {ndvi_status}
    (NDVI ranges -1 to 1; below 0.3 suggests stressed or sparse vegetation,
    0.3-0.6 moderate, above 0.6 healthy dense vegetation){weather_block}

    Respond ONLY with valid JSON in this exact format, no other text:
    {{
    "recommendation": "<practical advice for the farmer>",
    "ndvi_summary": "<one sentence interpreting the NDVI value>",
    "soil_summary": "<brief note, soil data not part of this endpoint>",
    "weather_risk": "<summary of the actual weather data above and any risk it implies>",
    "disease_risk_level": "<low, moderate, or high>",
    "confidence": <float 0-1>
    }}'''

   

    try:
        response = client.models.generate_content(model="gemini-3.6-flash", contents=[prompt], config=FAST_CONFIG)
        result = extract_json(response.text)
    except Exception:
        result = {
        "recommendation": "Unable to generate recommendation at this time.",
        "ndvi_summary": ndvi_status,
        "soil_summary": "Not available",
        "weather_risk": "Not available",
        "disease_risk_level": "low",
        "confidence": 0.0,
    }

    return AdvisoryResponse(
        recommendation=result["recommendation"],
        ndvi_summary=result["ndvi_summary"],
        soil_summary=result["soil_summary"],
        weather_risk=result["weather_risk"],
        disease_risk_level=result["disease_risk_level"],
        confidence=result["confidence"],
        language="en",
    )


@app.post("/regenerative-advice", response_model=RegenerativeResponse, response_model_by_alias=True)
def regenerative_advice(req: RegenerativeRequest):

    if req.soil_data is not None:
        soil_dict = req.soil_data.model_dump()
        soil_source_note = "Based on your submitted soil test results."
    else:
        fallback = get_soil_data(req.location.latitude, req.location.longitude)
        if fallback is not None:
            soil_dict = {k: (v if v is not None else "unavailable") for k, v in fallback.items()}
            soil_source_note = "No soil test provided — using regional soil estimates for your area."
        else:
            soil_dict = None
            soil_source_note = "No soil test provided and regional soil data unavailable."

    if soil_dict is not None:
        soil_block = f"\n    Soil data:\n    {json.dumps(soil_dict, indent=4)}"
    else:
        soil_block = "\n    Soil data unavailable; give general regenerative practice advice for this crop and region."

    prompt = f'''You are an agricultural expert specializing in regenerative farming.
    Note: {soil_source_note}

    Farm details:
    - Location: {req.location.district}, {req.location.state}, {req.location.country}
    - Crop: {req.crop_type}{soil_block}

    If any soil value above is "unavailable", do not assume deficiency —
    base your recommendations only on the fields that are actually known.

    Based on the available data, recommend 2-4 regenerative agriculture practices
    (e.g. cover cropping, crop rotation, reduced tillage, residue management)
    that would improve long-term soil health for this crop and location.

    Respond ONLY with valid JSON in this exact format, no other text:
    {{
    "practices": ["<practice 1>", "<practice 2>"],
    "reasoning": "<brief explanation>",
    "expected_benefit": "<what improvement to expect and over what timeframe>"
    }}'''

    try:
        response = client.models.generate_content(model="gemini-3.6-flash", contents=[prompt], config=FAST_CONFIG)
        result = extract_json(response.text)
    except Exception:
        result = {
        "practices": ["Cover cropping", "Crop rotation"],
        "reasoning": "Unable to generate specific analysis at this time.",
        "expected_benefit": "General soil health improvement expected."
    }

    return RegenerativeResponse(
        practices=result["practices"],
        reasoning=result["reasoning"],
        expected_benefit=result["expected_benefit"],
    )


@app.post("/voice-query", response_model=VoiceQueryResponse, response_model_by_alias=True)
def voice_query(req: VoiceQueryRequest):
    language = req.language_hint or "en"

    prompt = f"""You are a helpful agricultural advisor speaking with a farmer.
    Respond in {language}. Keep the answer short, practical, and conversational —
    this will be read aloud to the farmer.

    Farmer's question: "{req.transcript}"

    Respond ONLY with valid JSON in this exact format, no other text:
    {{
    "response_text": "<your spoken-style answer, in {language}>"
    }}"""

    

    try:
        t0 = time.time()
        response = client.models.generate_content(model="gemini-3.6-flash", contents=[prompt], config=FAST_CONFIG)
        print(f"DEBUG: generate_content took {time.time() - t0:.2f}s")
        result = extract_json(response.text)
        response_text = result["response_text"]
    except Exception as e:
        print(f"DEBUG voice-query error: {type(e).__name__}: {e}")
        response_text = "I'm sorry, I couldn't understand that. Could you please ask again?"
    return VoiceQueryResponse(
        transcript=req.transcript,
        response_text=response_text,
        response_audio_base64=None,
        language=language,
        )

@app.get("/schema", response_model=SchemaResponse, response_model_by_alias=True)
def schema():
    return SchemaResponse(
        schema_version="1.0",
        fields={
            "location": "country, state, district, latitude, longitude",
            "soilData": "ph, nitrogen, phosphorus, potassium, organicCarbon, moisture",
            "satelliteData": "ndvi, vegetationHealth, landSurfaceTemp",
            "weatherData": "temperature, rainfallMm, humidity, forecastSummary",
        },
    )


