package com.agentictravel.config;

import com.agentictravel.llm.OpenAILLMClient;
import com.agentictravel.llm.LLMClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LLMConfig {

    @Value("${OPENAI_API_KEY}")
    private String apiKey;

    @Value("${OPENAI_MODEL:gpt-3.5-turbo}")
    private String model;

    @Bean
    public LLMClient llmClient(){
        return new OpenAILLMClient(apiKey, model);
    }
}
