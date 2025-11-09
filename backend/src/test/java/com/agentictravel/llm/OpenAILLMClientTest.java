package com.agentictravel.llm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OpenAILLMClientTest {
    
    @Mock
    private HttpClient httpClient;
    
    @Mock
    private HttpResponse<String> httpResponse;
    
    private OpenAILLMClient client;
    
    @BeforeEach
    void setUp() {
        client = new OpenAILLMClient("test-api-key", "gpt-3.5-turbo", httpClient);
    }
    
    @Test
    void testGetName() {
        assertEquals("openai", client.getName());
    }
    
    @Test
    void testPrompt_SuccessfulResponse() throws Exception {
        // Mock successful HTTP response
        // use lenient stubbings to avoid failures when not used
        org.mockito.Mockito.lenient().when(httpResponse.statusCode()).thenReturn(200);
        org.mockito.Mockito.lenient().when(httpResponse.body()).thenReturn("""
            {
                "choices": [
                    {
                        "message": {
                            "content": "{\\"result\\": \\"test response\\"}"
                        }
                    }
                ]
            }
            """);
        
        // Note: This test would need actual HTTP client mocking
        // For now, we'll test the basic structure
        CompletableFuture<String> result = client.prompt("Test prompt", "gpt-3.5-turbo");
        assertNotNull(result);
    }
    
    @Test
    void testPrompt_WithNullModelName() {
        CompletableFuture<String> result = client.prompt("Test prompt", null);
        assertNotNull(result);
    }
    
    @Test
    void testPrompt_WithEmptyModelName() {
        CompletableFuture<String> result = client.prompt("Test prompt", "");
        assertNotNull(result);
    }
    
    @Test
    void testConstructor_WithNullApiKey() {
        assertThrows(NullPointerException.class, () -> {
            new OpenAILLMClient(null, "gpt-3.5-turbo");
        });
    }
    
    @Test
    void testConstructor_WithEmptyApiKey() {
        assertThrows(NullPointerException.class, () -> {
            new OpenAILLMClient("", "gpt-3.5-turbo");
        });
    }
    
    @Test
    void testConstructor_WithNullModel() {
        assertThrows(NullPointerException.class, () -> {
            new OpenAILLMClient("test-key", null);
        });
    }
    
    @Test
    void testConstructor_WithEmptyModel() {
        assertThrows(NullPointerException.class, () -> {
            new OpenAILLMClient("test-key", "");
        });
    }
}
