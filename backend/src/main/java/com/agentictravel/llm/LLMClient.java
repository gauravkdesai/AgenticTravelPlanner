package com.agentictravel.llm;

import java.util.concurrent.CompletableFuture;

public interface LLMClient {
    // get the name of the client
    String getName();
    // Sends a prompt and returns a text response (async)
    CompletableFuture<String> prompt(String prompt, String modelName);
}
