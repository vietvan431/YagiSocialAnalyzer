# Python Analysis API Contract (Optional)

**Feature**: Disaster Social Media Analysis  
**Date**: 2025-10-31  
**Phase**: 1 - External API Contract

## Overview

This document defines the REST API contract for the optional Python sentiment
analysis service. This API is **only required if** the user chooses to use
ML-based sentiment analysis instead of the default Java lexicon approach.

**Technology Stack**:

- Python 3.11+
- FastAPI framework
- Uvicorn ASGI server
- Transformers library (PhoBERT for Vietnamese, RoBERTa for English)

---

## Base Configuration

**Base URL**: `http://localhost:8000` (configurable in analysis configuration)

**Authentication**: None (local-only deployment)

**Content Type**: `application/json`

**Encoding**: UTF-8

---

## API Endpoints

### 1. Health Check

**Endpoint**: `GET /health`

**Description**: Check if API is running and models are loaded.

**Request**: None

**Response** (200 OK):

```json
{
  "status": "healthy",
  "models_loaded": {
    "vietnamese": true,
    "english": true
  },
  "version": "1.0.0"
}
```

**Response** (503 Service Unavailable):

```json
{
  "status": "unhealthy",
  "error": "Models not loaded",
  "models_loaded": {
    "vietnamese": false,
    "english": false
  }
}
```

---

### 2. Analyze Single Post

**Endpoint**: `POST /analyze`

**Description**: Analyze sentiment of a single post.

**Request Body**:

```json
{
  "content": "Bão Yagi gây thiệt hại nặng nề cho miền Bắc Việt Nam. Rất đau lòng khi thấy nhiều gia đình mất nhà cửa.",
  "language": "vi"
}
```

**Request Schema**:

| Field      | Type   | Required | Constraints      | Description       |
| ---------- | ------ | -------- | ---------------- | ----------------- |
| `content`  | string | Yes      | 1-50,000 chars   | Post text content |
| `language` | string | Yes      | Enum: "vi", "en" | Language code     |

**Response** (200 OK):

```json
{
  "sentiment_label": "NEGATIVE",
  "confidence": 0.89,
  "scores": {
    "positive": 0.05,
    "negative": 0.89,
    "neutral": 0.06
  },
  "explanation": "Keywords: 'thiệt hại nặng nề', 'đau lòng', 'mất nhà cửa'",
  "model_used": "vinai/phobert-base-v2"
}
```

**Response Schema**:

| Field             | Type   | Description                             |
| ----------------- | ------ | --------------------------------------- |
| `sentiment_label` | string | Enum: "POSITIVE", "NEGATIVE", "NEUTRAL" |
| `confidence`      | float  | 0.0 to 1.0, highest score               |
| `scores.positive` | float  | Positive sentiment score                |
| `scores.negative` | float  | Negative sentiment score                |
| `scores.neutral`  | float  | Neutral sentiment score                 |
| `explanation`     | string | Key phrases influencing classification  |
| `model_used`      | string | Transformer model identifier            |

**Validation**:

- `scores.positive + scores.negative + scores.neutral == 1.0` (±0.01)
- `confidence` equals `max(scores.positive, scores.negative, scores.neutral)`

**Response** (400 Bad Request):

```json
{
  "detail": "Validation error: content cannot be empty"
}
```

**Response** (422 Unprocessable Entity):

```json
{
  "detail": [
    {
      "loc": ["body", "language"],
      "msg": "value is not a valid enumeration member; permitted: 'vi', 'en'",
      "type": "type_error.enum"
    }
  ]
}
```

**Response** (500 Internal Server Error):

```json
{
  "detail": "Model inference failed: CUDA out of memory"
}
```

---

### 3. Batch Analyze Posts

**Endpoint**: `POST /analyze/batch`

**Description**: Analyze sentiment of multiple posts in a single request.

**Request Body**:

```json
{
  "posts": [
    {
      "id": "twitter_1234567890",
      "content": "Emergency relief teams are working tirelessly to help victims.",
      "language": "en"
    },
    {
      "id": "facebook_9876543210",
      "content": "Ngập lụt nghiêm trọng tại Hà Nội, nhiều tuyến đường bị cô lập.",
      "language": "vi"
    }
  ]
}
```

**Request Schema**:

| Field              | Type   | Required | Constraints                 |
| ------------------ | ------ | -------- | --------------------------- |
| `posts`            | array  | Yes      | 1-100 items, max batch size |
| `posts[].id`       | string | Yes      | Unique post identifier      |
| `posts[].content`  | string | Yes      | Post content                |
| `posts[].language` | string | Yes      | "vi" or "en"                |

**Response** (200 OK):

```json
{
  "results": [
    {
      "id": "twitter_1234567890",
      "sentiment_label": "POSITIVE",
      "confidence": 0.76,
      "scores": {
        "positive": 0.76,
        "negative": 0.12,
        "neutral": 0.12
      },
      "explanation": "Keywords: 'working tirelessly', 'help victims'",
      "model_used": "roberta-base"
    },
    {
      "id": "facebook_9876543210",
      "sentiment_label": "NEGATIVE",
      "confidence": 0.83,
      "scores": {
        "positive": 0.08,
        "negative": 0.83,
        "neutral": 0.09
      },
      "explanation": "Keywords: 'ngập lụt nghiêm trọng', 'bị cô lập'",
      "model_used": "vinai/phobert-base-v2"
    }
  ],
  "processing_time_seconds": 2.45
}
```

**Response Schema**:

| Field                     | Type   | Description                            |
| ------------------------- | ------ | -------------------------------------- |
| `results`                 | array  | Analysis results (same order as input) |
| `results[].id`            | string | Post ID from request                   |
| `results[].*`             | object | Same fields as single analyze response |
| `processing_time_seconds` | float  | Total processing time                  |

**Response** (400 Bad Request):

```json
{
  "detail": "Batch size exceeds maximum of 100 posts"
}
```

**Response** (207 Multi-Status):

Partial success when some posts fail:

```json
{
  "results": [
    {
      "id": "twitter_1234567890",
      "sentiment_label": "POSITIVE",
      "confidence": 0.76,
      "scores": { "positive": 0.76, "negative": 0.12, "neutral": 0.12 }
    },
    {
      "id": "facebook_9876543210",
      "error": "Content too short for analysis (min 10 chars)"
    }
  ],
  "processing_time_seconds": 1.23
}
```

---

### 4. Model Information

**Endpoint**: `GET /models`

**Description**: Get information about loaded models.

**Request**: None

**Response** (200 OK):

```json
{
  "models": [
    {
      "language": "vi",
      "model_id": "vinai/phobert-base-v2",
      "type": "transformer",
      "parameters": "135M",
      "accuracy": "0.87"
    },
    {
      "language": "en",
      "model_id": "roberta-base",
      "type": "transformer",
      "parameters": "125M",
      "accuracy": "0.85"
    }
  ]
}
```

---

## Error Handling

### HTTP Status Codes

| Code | Meaning               | When Used                                 |
| ---- | --------------------- | ----------------------------------------- |
| 200  | OK                    | Successful analysis                       |
| 207  | Multi-Status          | Batch request with partial failures       |
| 400  | Bad Request           | Invalid request format, validation error  |
| 422  | Unprocessable Entity  | Pydantic validation error                 |
| 500  | Internal Server Error | Model inference failure, unexpected error |
| 503  | Service Unavailable   | Models not loaded, API starting up        |

### Error Response Format

All errors follow this structure:

```json
{
  "detail": "Error message",
  "error_code": "MODEL_INFERENCE_FAILED",
  "timestamp": "2024-09-15T10:30:00Z"
}
```

---

## Performance Expectations

| Metric                    | Target     | Notes                      |
| ------------------------- | ---------- | -------------------------- |
| Single post latency       | < 200ms    | CPU inference, no GPU      |
| Batch (100 posts) latency | < 5s       | Average 50ms per post      |
| Throughput                | 10 req/sec | Limited by model inference |
| Concurrent requests       | 4          | ThreadPoolExecutor workers |
| Startup time              | < 30s      | Model loading time         |

---

## Java Client Implementation

### Configuration

```java
public class AnalysisConfiguration {
    private String pythonApiUrl = "http://localhost:8000";
    private int connectionTimeoutSeconds = 30;
    private int readTimeoutSeconds = 60;
}
```

### Client Interface Usage

```java
public class PythonApiSentimentAnalyzer implements ISentimentAnalyzer {
    private final HttpClient httpClient;
    private final String apiUrl;

    @Override
    public SentimentResult analyze(SocialMediaPost post) {
        String endpoint = apiUrl + "/analyze";

        // Build request JSON
        JSONObject request = new JSONObject();
        request.put("content", post.getContent());
        request.put("language", post.getLanguage());

        // Send POST request
        HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(request.toString()))
            .build();

        HttpResponse<String> response = httpClient.send(httpRequest,
            HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new AnalysisException("Python API error: " + response.body());
        }

        // Parse response
        JSONObject json = new JSONObject(response.body());

        return new SentimentResult(
            UUID.randomUUID(),
            post.getId(),
            post.getProjectId(),
            SentimentLabel.valueOf(json.getString("sentiment_label")),
            json.getDouble("confidence"),
            json.getJSONObject("scores").getDouble("positive"),
            json.getJSONObject("scores").getDouble("negative"),
            json.getJSONObject("scores").getDouble("neutral"),
            Instant.now(),
            "python_api",
            json.optString("explanation", null)
        );
    }

    @Override
    public List<SentimentResult> analyzeBatch(List<SocialMediaPost> posts) {
        String endpoint = apiUrl + "/analyze/batch";

        // Build batch request
        JSONObject request = new JSONObject();
        JSONArray postsArray = new JSONArray();

        for (SocialMediaPost post : posts) {
            JSONObject postObj = new JSONObject();
            postObj.put("id", post.getId());
            postObj.put("content", post.getContent());
            postObj.put("language", post.getLanguage());
            postsArray.put(postObj);
        }

        request.put("posts", postsArray);

        // Send request and parse response (similar to single analyze)
        // ...

        return results;
    }

    @Override
    public String getAnalyzerType() {
        return "python_api";
    }

    @Override
    public boolean supportsLanguage(String languageCode) {
        return "vi".equals(languageCode) || "en".equals(languageCode);
    }
}
```

### Health Check

```java
public boolean isPythonApiAvailable() {
    try {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(apiUrl + "/health"))
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request,
            HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JSONObject json = new JSONObject(response.body());
            return "healthy".equals(json.getString("status"));
        }
        return false;
    } catch (Exception e) {
        return false;
    }
}
```

---

## Python API Implementation Notes

### Minimal FastAPI Implementation

```python
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from transformers import pipeline
from typing import List, Literal

app = FastAPI(title="Disaster Sentiment Analysis API", version="1.0.0")

# Load models on startup
vi_classifier = pipeline("sentiment-analysis",
                        model="vinai/phobert-base-v2",
                        device=-1)  # CPU

en_classifier = pipeline("sentiment-analysis",
                        model="roberta-base",
                        device=-1)

class AnalyzeRequest(BaseModel):
    content: str = Field(..., min_length=1, max_length=50000)
    language: Literal["vi", "en"]

class AnalyzeResponse(BaseModel):
    sentiment_label: Literal["POSITIVE", "NEGATIVE", "NEUTRAL"]
    confidence: float = Field(..., ge=0.0, le=1.0)
    scores: dict[str, float]
    explanation: str
    model_used: str

@app.get("/health")
async def health_check():
    return {
        "status": "healthy",
        "models_loaded": {"vietnamese": True, "english": True},
        "version": "1.0.0"
    }

@app.post("/analyze", response_model=AnalyzeResponse)
async def analyze_sentiment(request: AnalyzeRequest):
    classifier = vi_classifier if request.language == "vi" else en_classifier

    result = classifier(request.content)[0]

    # Map model output to standard format
    label_map = {"LABEL_0": "NEGATIVE", "LABEL_1": "NEUTRAL", "LABEL_2": "POSITIVE"}

    return {
        "sentiment_label": label_map.get(result["label"], "NEUTRAL"),
        "confidence": result["score"],
        "scores": {
            "positive": result["score"] if result["label"] == "LABEL_2" else 0.0,
            "negative": result["score"] if result["label"] == "LABEL_0" else 0.0,
            "neutral": 1.0 - result["score"]
        },
        "explanation": "Transformer-based classification",
        "model_used": "vinai/phobert-base-v2" if request.language == "vi" else "roberta-base"
    }
```

### Running the API

```bash
# Install dependencies
pip install fastapi uvicorn transformers torch

# Start server
uvicorn main:app --host 0.0.0.0 --port 8000 --workers 4
```

---

## Summary

This API contract defines:

- **Simple REST interface**: JSON request/response, standard HTTP status codes
- **Batch support**: Up to 100 posts per request for efficiency
- **Error handling**: Clear error messages and partial failure support
- **Language support**: Vietnamese (PhoBERT) and English (RoBERTa)
- **Java client**: Reference implementation using HttpClient

The Python API is **optional**. If not deployed, the Java application uses the
default `JavaLexiconSentimentAnalyzer` implementation. The
`AnalysisConfiguration` entity allows users to switch between backends without
code changes.
