# AgenticTravelPlanner

Agentic travel planner that takes trip requirements and uses agentic middleware to produce a tailor-made itinerary.

Web UI
------

A small static web page is included to collect travel requirements from a user and produce a JSON summary. The agentic middleware will consume this JSON to generate itineraries.

Files added
 - `index.html` — the form UI for collecting trip requirements
 - `styles.css` — simple responsive styles
 - `app.js` — client-side logic to serialize the form to JSON, copy, and download

Usage
-----

Open `index.html` in your browser. On macOS you can double-click the file or run a simple local server from the repository root:

```bash
# Python 3 built-in server (works in the repo root)
python3 -m http.server 8080

# then open http://localhost:8080 in your browser
```

Next steps
----------

 - Implement the agentic middleware that accepts the JSON and returns a proposed itinerary.
 - Add backend storage or authentication if you want to save user profiles.

Java backend (Spring Boot)
--------------------------

A simple Spring Boot backend skeleton is included in `backend/`. It exposes a POST `/api/itineraries` endpoint which accepts the JSON produced by the UI and returns a stubbed itinerary assembled from asynchronous agent stubs.

To run the backend (requires Java 17+ and Maven):

```bash
cd backend
mvn spring-boot:run

# Backend listens on http://localhost:8080
```

LangChain4J integration
-----------------------

The agent implementations are currently stubs. When you're ready, replace the stub logic with calls to LangChain4J (Java) to orchestrate LLM-driven agents that call real APIs (flight/hotel searches, events, weather). Keep secrets (API keys) in environment variables and use secure connectors.

Gemini setup
------------

This project includes a simple Gemini HTTP wrapper `GeminiLLMClient` which expects two environment variables:

- `GEMINI_API_KEY` — your Gemini Pro API key
- `GEMINI_ENDPOINT` — the Gemini endpoint URL (example placeholder: https://api.example.com/v1/generate)

Set those before running the backend:

```bash
export GEMINI_API_KEY="your_key_here"
export GEMINI_ENDPOINT="https://api.example.com/v1/generate"
cd backend
mvn spring-boot:run
```
