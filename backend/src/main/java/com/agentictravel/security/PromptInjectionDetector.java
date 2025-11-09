package com.agentictravel.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Detects potential prompt injection attacks in user input.
 * This is a simplified, rule-based detector and should be supplemented with more advanced techniques.
 */
@Component
public class PromptInjectionDetector {

    private static final Logger LOG = LoggerFactory.getLogger(PromptInjectionDetector.class);

    // Maximum length before it's considered suspicious
    private static final int MAX_INPUT_LENGTH = 10000;

    // Keywords and patterns often used in prompt injection
    private static final List<String> INSTRUCTION_KEYWORDS = Arrays.asList(
        "ignore previous instructions", "forget everything", "you are now a different assistant",
        "pretend to be", "act as if", "roleplay as"
    );

    private static final List<String> SYSTEM_KEYWORDS = Arrays.asList(
        "system:", "assistant:", "user:", "### system:", "### assistant:", "### user:"
    );

    private static final List<String> CODE_KEYWORDS = Arrays.asList(
        "eval(", "function(", "execute(", "run this code", "```javascript", "```python", "```bash"
    );

    private static final List<String> JAILBREAK_KEYWORDS = Arrays.asList(
        "jailbreak", "dan mode", "developer mode", "admin mode", "bypass safety", "bypass restrictions"
    );

    // Patterns for detecting unusual character sequences
    private static final Pattern HEX_ENCODING = Pattern.compile("([0-9A-Fa-f]{2}){10,}");
    private static final Pattern BASE64_ENCODING = Pattern.compile("[A-Za-z0-9+/=]{20,}");
    private static final Pattern REPETITIVE_CHARS = Pattern.compile("(.)\\1{10,}");
    private static final Pattern REPETITIVE_WORDS = Pattern.compile("\\b(\\w+)\\b(?:\\W+\\1\\b){3,}", Pattern.CASE_INSENSITIVE);

    /**
     * Assesses the risk of prompt injection in the given input.
     *
     * @param input The user input to analyze.
     * @return A {@link PromptInjectionRisk} enum indicating the assessed risk level.
     */
    public PromptInjectionRisk assessRisk(String input) {
        if (input == null || input.isEmpty()) {
            return PromptInjectionRisk.NONE;
        }

        // Combine all keyword lists for a comprehensive check
        List<String> allKeywords = Arrays.asList(
            INSTRUCTION_KEYWORDS, SYSTEM_KEYWORDS, CODE_KEYWORDS, JAILBREAK_KEYWORDS
        ).stream().flatMap(List::stream).collect(Collectors.toList());

        // 1. Check for direct matches of high-risk keywords
        for (String keyword : allKeywords) {
            if (input.toLowerCase().contains(keyword)) {
                LOG.warn("Prompt injection detected: pattern match in input: {}", 
                    truncateForLog(input));
                return PromptInjectionRisk.HIGH;
            }
        }

        // 2. Check for high density of suspicious keywords
        long suspiciousCount = allKeywords.stream()
            .filter(keyword -> input.toLowerCase().contains(keyword))
            .count();
        if (suspiciousCount > 3) { // Arbitrary threshold
            LOG.warn("High density of suspicious keywords detected: {}", suspiciousCount);
            return PromptInjectionRisk.HIGH;
        }

        // 3. Extremely long input should be classified LOW (benign large payload)
        if (input.length() > MAX_INPUT_LENGTH) {
            LOG.warn("Extremely long input detected: {} characters", input.length());
            return PromptInjectionRisk.LOW;
        }

        // 4. Check for repeated word patterns (e.g., "ignore ignore ignore") -> HIGH
        if (REPETITIVE_WORDS.matcher(input).find()) {
            LOG.warn("Repetitive word patterns detected in input");
            return PromptInjectionRisk.HIGH;
        }

        // 5. Check for repeated character patterns (very suspicious)
        if (REPETITIVE_CHARS.matcher(input).find()) {
            LOG.warn("Repetitive character patterns detected in input");
            return PromptInjectionRisk.HIGH;
        }

        // 6. Check for encoding patterns (base64/hex) as MEDIUM
        if (HEX_ENCODING.matcher(input).find() || BASE64_ENCODING.matcher(input).find()) {
            LOG.warn("Unusual encoding patterns detected in input");
            return PromptInjectionRisk.MEDIUM;
        }

        return PromptInjectionRisk.NONE;
    }

    /**
     * A simple enum to represent the assessed risk level.
     */
    public enum PromptInjectionRisk {
        NONE, LOW, MEDIUM, HIGH;

        public boolean isHighRisk() {
            return this == HIGH;
        }
    }
    
    /**
     * Truncates input for logging purposes to avoid log injection.
     */
    private String truncateForLog(String input) {
        if (input == null) {
            return "";
        }
        return input.substring(0, Math.min(input.length(), 100)) + (input.length() > 100 ? "..." : "");
    }
}