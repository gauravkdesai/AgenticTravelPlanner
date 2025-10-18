package com.agentictravel.config;

import com.agentictravel.llm.OpenAILLMClient;
import com.agentictravel.llm.LLMClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OpenAIConfig.class)
public class LLMConfig {

    private final OpenAIConfig openAIConfig;

    public LLMConfig(OpenAIConfig openAIConfig) {
        this.openAIConfig = openAIConfig;
    }

    @Bean
    public LLMClient llmClient(){
        if (openAIConfig.getApiKey() == null || openAIConfig.getApiKey().trim().isEmpty()) {
            throw new IllegalStateException("OpenAI API key is required. Set OPENAI_API_KEY environment variable.");
        }
        return new OpenAILLMClient(openAIConfig.getApiKey(), openAIConfig.getModel());
    }
}
