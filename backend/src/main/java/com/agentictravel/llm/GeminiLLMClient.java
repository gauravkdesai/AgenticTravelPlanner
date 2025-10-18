package com.agentictravel.llm;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

// Minimal Gemini client wrapper — this is a small example using HTTP. In production use the official SDK.
public class GeminiLLMClient implements LLMClient {

    private final String projectId;
    private final String location;

    public GeminiLLMClient(String projectId, String location) {
        this.projectId = projectId;
        this.location = location;
    }

    @Override
    public String getName() {
        return "gemini";
    }

    @Override
    public CompletableFuture<String> prompt(String prompt, String modelName) {
        return CompletableFuture.supplyAsync(() -> {
            try (VertexAI vertexAI = new VertexAI(projectId, location)) {
                GenerativeModel model = new GenerativeModel(modelName, vertexAI);
                GenerateContentResponse response = model.generateContent(prompt);
                return ResponseHandler.getText(response);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
