package com.agentictravel.llm;

import java.util.concurrent.CompletableFuture;

public class FakeLLMClient implements LLMClient {
    private final String cannedResponse;

    public FakeLLMClient(String cannedResponse){
        this.cannedResponse = cannedResponse;
    }

    @Override
    public CompletableFuture<String> prompt(String prompt) {
        return CompletableFuture.completedFuture(cannedResponse);
    }
}
