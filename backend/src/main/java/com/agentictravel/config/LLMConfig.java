package com.agentictravel.config;

import com.agentictravel.llm.GeminiLLMClient;
import com.agentictravel.llm.LLMClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LLMConfig {

    @Value("${GEMINI_PROJECT_ID}")
    private String projectId;

    @Value("${GEMINI_LOCATION}")
    private String location;

    @Bean
    public LLMClient llmClient(){
        return new GeminiLLMClient(projectId, location);
    }
}
