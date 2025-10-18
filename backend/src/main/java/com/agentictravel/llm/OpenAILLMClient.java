package com.agentictravel.llm;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.Map;
import java.util.HashMap;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

public class OpenAILLMClient implements LLMClient {
    
    private final String apiKey;
    private final String model;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiUrl = "https://api.openai.com/v1/chat/completions";
    
    public OpenAILLMClient(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public String getName() {
        return "openai";
    }
    
    @Override
    public CompletableFuture<String> prompt(String prompt, String modelName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Use the modelName parameter if provided, otherwise use the configured model
                String modelToUse = (modelName != null && !modelName.isEmpty()) ? modelName : this.model;
                
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("model", modelToUse);
                requestBody.put("messages", new Object[]{
                    Map.of("role", "system", "content", "You are a helpful travel planning assistant. Always respond with valid JSON when requested."),
                    Map.of("role", "user", "content", prompt)
                });
                requestBody.put("temperature", 0.7);
                requestBody.put("max_tokens", 2000);
                
                // Enable JSON mode for structured responses
                Map<String, Object> responseFormat = new HashMap<>();
                responseFormat.put("type", "json_object");
                requestBody.put("response_format", responseFormat);
                
                String requestBodyJson = objectMapper.writeValueAsString(requestBody);
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(apiUrl))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + apiKey)
                        .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
                        .timeout(Duration.ofSeconds(60))
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() != 200) {
                    throw new RuntimeException("OpenAI API error: " + response.statusCode() + " - " + response.body());
                }
                
                JsonNode responseJson = objectMapper.readTree(response.body());
                JsonNode content = responseJson.path("choices").get(0).path("message").path("content");
                
                if (content.isMissingNode()) {
                    throw new RuntimeException("No content in OpenAI response");
                }
                
                return content.asText();
                
            } catch (Exception e) {
                throw new RuntimeException("Failed to call OpenAI API: " + e.getMessage(), e);
            }
        });
    }
}
