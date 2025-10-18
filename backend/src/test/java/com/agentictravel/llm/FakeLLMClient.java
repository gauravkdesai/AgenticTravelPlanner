package com.agentictravel.llm;

import java.util.concurrent.CompletableFuture;

public class FakeLLMClient implements LLMClient {
    private final String cannedResponse;

    public FakeLLMClient(String cannedResponse){
        this.cannedResponse = cannedResponse;
    }

    @Override
    public String getName() {
        return "fake";
    }

    @Override
    public CompletableFuture<String> prompt(String prompt, String modelName) {
        return CompletableFuture.completedFuture(cannedResponse);
    }
}
