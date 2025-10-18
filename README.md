# AgenticTravelPlanner

A complete agentic travel planner that uses multiple AI agents to create personalized itineraries with interactive refinement capabilities.

## Features

- **Interactive Question Flow**: Optional clarifying questions to better understand your preferences
- **Multi-Agent System**: Specialized agents for flights, hotels, transport, events, and weather
- **Multiple Options**: Each agent provides 3-5 options with pros/cons for informed decision-making
- **Day-by-Day Itinerary**: Complete daily schedules with activities, timing, and logistics
- **Iterative Refinement**: Refine your itinerary based on feedback and amendments
- **Real-time Weather**: Weather forecasts and packing suggestions
- **Event Discovery**: Find relevant events and activities for your travel dates

## Web UI

A modern, responsive web interface that guides you through the trip planning process:

1. **Trip Requirements Form**: Enter your basic trip details
2. **Clarifying Questions** (optional): Answer questions to refine your preferences
3. **Generated Itinerary**: View your complete travel plan with multiple booking options
4. **Refinement**: Make amendments and regenerate your itinerary

### Files

- `index.html` — Interactive form UI with questions flow and itinerary display
- `styles.css` — Modern responsive styles with booking options and day plans
- `app.js` — Client-side logic for questions, itinerary display, and refinement

### Usage

Open `index.html` in your browser or run a local server:

```bash
# Python 3 built-in server
python3 -m http.server 8080

# Then open http://localhost:8080 in your browser
```

## Java Backend (Spring Boot)

A sophisticated multi-agent system built with Spring Boot that orchestrates specialized AI agents to create comprehensive travel itineraries.

### Architecture

- **AgentCoordinator**: Orchestrates all agents and manages the planning flow
- **QuestionAgent**: Generates clarifying questions based on trip requirements
- **FlightAgent**: Finds multiple flight options with detailed comparisons
- **HotelAgent**: Searches hotels with different price ranges and amenities
- **TransportAgent**: Provides car rental, train, and bus options
- **EventAgent**: Discovers relevant events and activities
- **WeatherAgent**: Provides detailed weather forecasts and recommendations
- **ItineraryPlannerAgent**: Creates day-by-day schedules from all agent results

### API Endpoints

- `POST /api/itineraries/questions` — Generate clarifying questions
- `POST /api/itineraries` — Generate complete itinerary or refine existing one

### Setup

1. **Prerequisites**: Java 17+ and Maven
2. **Environment Variables**: Set your OpenAI API key
3. **Run**: Start the Spring Boot application

```bash
# Set your OpenAI API key
export OPENAI_API_KEY="your_openai_api_key_here"

# Run the backend
cd backend
mvn spring-boot:run

# Backend runs on http://localhost:8080
```

### Configuration

The application uses `application.properties` for configuration:

```properties
# OpenAI Configuration
OPENAI_API_KEY=${OPENAI_API_KEY}
OPENAI_MODEL=gpt-3.5-turbo

# Server Configuration
server.port=8080
spring.application.name=agentic-travel-planner
```

## OpenAI Integration

The system uses OpenAI's GPT-3.5-turbo model with JSON mode for structured responses. Each agent is designed to:

- Generate multiple realistic options
- Provide detailed pros/cons comparisons
- Consider special needs (kids, elderly, accessibility)
- Handle amendments and refinement requests
- Return structured JSON responses

### Agent Prompts

Each agent uses carefully crafted prompts that:
- Include trip context and user preferences
- Request specific JSON schemas for consistent responses
- Handle edge cases and special requirements
- Provide fallback mock data if API calls fail

## Development

### Project Structure

```
backend/
├── src/main/java/com/agentictravel/
│   ├── api/                    # REST controllers
│   ├── config/                 # Spring configuration
│   ├── llm/                    # LLM client implementations
│   ├── model/                  # Data models
│   └── services/               # Agent implementations
└── src/main/resources/
    └── application.properties  # Configuration
```

### Key Components

- **LLMClient Interface**: Abstracted interface for different LLM providers
- **OpenAILLMClient**: HTTP-based client for OpenAI API with JSON mode
- **AgentCoordinator**: Manages parallel agent execution and result aggregation
- **LLMToModelMapper**: Maps LLM responses to typed model objects

### Error Handling

- Graceful degradation when LLM responses fail to parse
- Fallback to mock data for demonstration purposes
- Comprehensive error logging and user feedback
- Non-blocking agent failures (partial itineraries still generated)

## Future Enhancements

- **Real API Integration**: Connect to actual flight/hotel booking APIs
- **User Authentication**: Save and manage user profiles and trip history
- **Advanced Refinement**: Smart agent selection based on amendment analysis
- **Collaborative Planning**: Multi-user trip planning and sharing
- **Mobile App**: Native mobile application
- **Offline Mode**: Cached responses for offline itinerary viewing

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.
