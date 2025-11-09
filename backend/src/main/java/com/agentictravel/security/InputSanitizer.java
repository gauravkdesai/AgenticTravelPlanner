package com.agentictravel.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Sanitizes user input to prevent security vulnerabilities and ensure clean data.
 * Handles special characters, encoding issues, and prepares text for LLM processing.
 */
@Component
public class InputSanitizer {
    
    private static final Logger LOG = LoggerFactory.getLogger(InputSanitizer.class);
    
    // Patterns for detecting potentially malicious content
    private static final Pattern SCRIPT_PATTERN = Pattern.compile("(?i)<script[^>]*>.*?</script>");
    private static final Pattern HTML_PATTERN = Pattern.compile("(?i)<[^>]+>");
    private static final Pattern EXCESSIVE_SPECIAL_CHARS = Pattern.compile("[^\\p{L}\\p{N}\\p{Z}\\p{P}]{10,}");
    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\p{Cntrl}&&[^\r\n\t]]");
    
    // Maximum lengths for different fields
    private static final int MAX_TRIP_TITLE_LENGTH = 200;
    private static final int MAX_REGION_LENGTH = 100;
    private static final int MAX_NOTES_LENGTH = 2000;
    private static final int MAX_AMENDMENTS_LENGTH = 1000;
    
    /**
     * Sanitizes a text input by removing dangerous content and normalizing encoding.
     * 
     * @param input The input text to sanitize
     * @param maxLength Maximum allowed length
     * @return Sanitized text, or empty string if input is null/invalid
     */
    public String sanitizeText(String input, int maxLength) {
        if (input == null) {
            return "";
        }
        
        // Remove control characters except common whitespace
        String sanitized = CONTROL_CHARS.matcher(input).replaceAll("");
        
        // Remove HTML/script tags
        sanitized = SCRIPT_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = HTML_PATTERN.matcher(sanitized).replaceAll("");
        
        // Check for excessive special characters (potential encoding attack)
        if (EXCESSIVE_SPECIAL_CHARS.matcher(sanitized).find()) {
            LOG.warn("Input contains excessive special characters, truncating: {}", 
                sanitized.substring(0, Math.min(50, sanitized.length())));
            sanitized = EXCESSIVE_SPECIAL_CHARS.matcher(sanitized).replaceAll("");
        }
        
        // Normalize whitespace
        sanitized = sanitized.replaceAll("\\s+", " ").trim();
        
        // Truncate if too long
        if (sanitized.length() > maxLength) {
            LOG.warn("Input truncated from {} to {} characters", sanitized.length(), maxLength);
            sanitized = sanitized.substring(0, maxLength).trim();
        }
        
        return sanitized;
    }
    
    /**
     * Sanitizes trip title with appropriate length limit.
     */
    public String sanitizeTripTitle(String title) {
        return sanitizeText(title, MAX_TRIP_TITLE_LENGTH);
    }
    
    /**
     * Sanitizes region/destination with appropriate length limit.
     */
    public String sanitizeRegion(String region) {
        return sanitizeText(region, MAX_REGION_LENGTH);
    }
    
    /**
     * Sanitizes notes with appropriate length limit.
     */
    public String sanitizeNotes(String notes) {
        return sanitizeText(notes, MAX_NOTES_LENGTH);
    }
    
    /**
     * Sanitizes amendments with appropriate length limit.
     */
    public String sanitizeAmendments(String amendments) {
        return sanitizeText(amendments, MAX_AMENDMENTS_LENGTH);
    }
    
    /**
     * Sanitizes a list of strings (e.g., interests, food preferences).
     */
    public List<String> sanitizeStringList(List<String> list, int maxItems, int maxItemLength) {
        if (list == null) {
            return List.of();
        }
        
        return list.stream()
            .limit(maxItems)
            .map(item -> sanitizeText(item, maxItemLength))
            .filter(item -> !item.isEmpty())
            .distinct()
            .collect(Collectors.toList());
    }
    
    /**
     * Validates and sanitizes numeric input.
     */
    public int sanitizePositiveInt(String input, int defaultValue, int maxValue) {
        if (input == null || input.trim().isEmpty()) {
            return defaultValue;
        }
        
        try {
            int value = Integer.parseInt(input.trim());
            if (value <= 0) {
                LOG.warn("Invalid positive integer input: {}, using default: {}", value, defaultValue);
                return defaultValue;
            }
            if (value > maxValue) {
                LOG.warn("Integer input {} exceeds maximum {}, using maximum", value, maxValue);
                return maxValue;
            }
            return value;
        } catch (NumberFormatException e) {
            LOG.warn("Invalid integer input: {}, using default: {}", input, defaultValue);
            return defaultValue;
        }
    }
    
    /**
     * Checks if input contains only safe characters for LLM processing.
     */
    public boolean isSafeForLLM(String input) {
        if (input == null || input.isEmpty()) {
            return true;
        }
        
        // Check for common injection patterns
        String lowerInput = input.toLowerCase();
        return !lowerInput.contains("ignore previous") &&
               !lowerInput.contains("you are now") &&
               !lowerInput.contains("system:") &&
               !lowerInput.contains("assistant:") &&
               !lowerInput.contains("user:") &&
               !lowerInput.contains("```") &&
               !lowerInput.contains("execute") &&
               !lowerInput.contains("eval(") &&
               !lowerInput.contains("function(");
    }
}