package com.agentictravel.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Detects and prevents prompt injection attacks against LLM systems.
 * Identifies attempts to manipulate AI behavior through malicious prompts.
 */
@Component
public class PromptInjectionDetector {
    
    private static final Logger log = LoggerFactory.getLogger(PromptInjectionDetector.class);
    
    // Common prompt injection patterns
    private static final List<Pattern> INJECTION_PATTERNS = List.of(
        // Instruction override patterns
        Pattern.compile("(?i)ignore\\s+(previous|all|above)\\s+(instructions?|prompts?)"),
        Pattern.compile("(?i)forget\\s+(everything|all|previous)"),
        Pattern.compile("(?i)disregard\\s+(previous|all)\\s+(instructions?)"),
        Pattern.compile("(?i)you\\s+are\\s+now\\s+(a\\s+)?different"),
        Pattern.compile("(?i)pretend\\s+to\\s+be"),
        Pattern.compile("(?i)act\\s+as\\s+if"),
        Pattern.compile("(?i)roleplay\\s+as"),
        
        // System prompt manipulation
        Pattern.compile("(?i)system\\s*:"),
        Pattern.compile("(?i)assistant\\s*:"),
        Pattern.compile("(?i)user\\s*:"),
        Pattern.compile("(?i)###\\s*(system|assistant|user)\\s*:"),
        
        // Code execution attempts
        Pattern.compile("(?i)eval\\s*\\("),
        Pattern.compile("(?i)function\\s*\\("),
        Pattern.compile("(?i)execute\\s*\\("),
        Pattern.compile("(?i)run\\s+this\\s+code"),
        Pattern.compile("(?i)```\\s*(javascript|python|bash|shell)"),
        
        // Jailbreak attempts
        Pattern.compile("(?i)jailbreak"),
        Pattern.compile("(?i)dan\\s+mode"),
        Pattern.compile("(?i)developer\\s+mode"),
        Pattern.compile("(?i)admin\\s+mode"),
        Pattern.compile("(?i)bypass\\s+(safety|restrictions)"),
        
        // Information extraction
        Pattern.compile("(?i)what\\s+is\\s+your\\s+(prompt|system\\s+message)"),
        Pattern.compile("(?i)show\\s+me\\s+your\\s+(instructions|rules)"),
        Pattern.compile("(?i)repeat\\s+your\\s+(prompt|instructions)"),
        
        // Manipulation attempts
        Pattern.compile("(?i)this\\s+is\\s+(just\\s+)?a\\s+(test|game|experiment)"),
        Pattern.compile("(?i)hypothetically"),
        Pattern.compile("(?i)in\\s+a\\s+fictional\\s+scenario"),
        Pattern.compile("(?i)imagine\\s+you\\s+are"),
        
        // Direct command attempts
        Pattern.compile("(?i)command\\s*:"),
        Pattern.compile("(?i)execute\\s*:"),
        Pattern.compile("(?i)run\\s*:"),
        Pattern.compile("(?i)do\\s+this\\s*:"),
        
        // Repetition attacks
        Pattern.compile("(?i)(ignore|forget|disregard).*?(ignore|forget|disregard)", Pattern.DOTALL),
        
        // Unicode/encoding attacks
        Pattern.compile("[\\u0000-\\u001F\\u007F-\\u009F]"), // Control characters
        Pattern.compile("[\\u2000-\\u206F\\u2E00-\\u2E7F\\u3000-\\u303F]"), // Various Unicode spaces/punctuation
        
        // Excessive repetition
        Pattern.compile("(.{1,20})\\1{10,}"), // Same pattern repeated 10+ times
        
        // Base64 encoding attempts
        Pattern.compile("(?i)[A-Za-z0-9+/]{50,}={0,2}"),
        
        // Hexadecimal encoding attempts
        Pattern.compile("(?i)[0-9A-Fa-f]{20,}")
    );
    
    // Suspicious keywords that might indicate injection attempts
    private static final List<String> SUSPICIOUS_KEYWORDS = List.of(
        "prompt", "injection", "jailbreak", "bypass", "override", "manipulate",
        "system", "admin", "root", "sudo", "execute", "eval", "function",
        "ignore", "forget", "disregard", "pretend", "roleplay", "act as"
    );
    
    /**
     * Analyzes input for prompt injection attempts.
     * 
     * @param input The text to analyze
     * @return DetectionResult containing threat level and details
     */
    public DetectionResult detectInjection(String input) {
        if (input == null || input.trim().isEmpty()) {
            return DetectionResult.safe();
        }
        
        String normalizedInput = input.toLowerCase().trim();
        
        // Check for pattern matches
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(normalizedInput).find()) {
                log.warn("Prompt injection detected: pattern match in input: {}", 
                    truncateForLog(input));
                return DetectionResult.threat(ThreatLevel.HIGH, 
                    "Suspicious pattern detected: " + pattern.pattern());
            }
        }
        
        // Check for suspicious keyword density
        long suspiciousCount = SUSPICIOUS_KEYWORDS.stream()
            .mapToLong(keyword -> countOccurrences(normalizedInput, keyword))
            .sum();
            
        if (suspiciousCount >= 3) {
            log.warn("High density of suspicious keywords detected: {}", suspiciousCount);
            return DetectionResult.threat(ThreatLevel.MEDIUM, 
                "High density of suspicious keywords: " + suspiciousCount);
        }
        
        // Check for unusual character patterns
        if (hasUnusualPatterns(input)) {
            log.warn("Unusual character patterns detected in input");
            return DetectionResult.threat(ThreatLevel.LOW, 
                "Unusual character patterns detected");
        }
        
        // Check input length (very long inputs might be attempts to overwhelm)
        if (input.length() > 10000) {
            log.warn("Extremely long input detected: {} characters", input.length());
            return DetectionResult.threat(ThreatLevel.LOW, 
                "Input length exceeds safe threshold");
        }
        
        return DetectionResult.safe();
    }
    
    /**
     * Checks if input contains unusual patterns that might indicate encoding attacks.
     */
    private boolean hasUnusualPatterns(String input) {
        // Check for excessive special characters
        long specialCharCount = input.chars()
            .filter(c -> !Character.isLetterOrDigit(c) && !Character.isWhitespace(c))
            .count();
            
        if (specialCharCount > input.length() * 0.3) {
            return true;
        }
        
        // Check for repeated patterns
        if (input.length() > 100) {
            String firstHalf = input.substring(0, input.length() / 2);
            String secondHalf = input.substring(input.length() / 2);
            if (firstHalf.equals(secondHalf)) {
                return true;
            }
        }
        
        // Check for excessive repetition of single characters
        for (char c : input.toCharArray()) {
            if (countOccurrences(input, String.valueOf(c)) > input.length() * 0.5) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Counts occurrences of a substring in text.
     */
    private long countOccurrences(String text, String substring) {
        if (text == null || substring == null || substring.isEmpty()) {
            return 0;
        }
        
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }
        return count;
    }
    
    /**
     * Truncates input for logging purposes to avoid log injection.
     */
    private String truncateForLog(String input) {
        if (input == null) {
            return "null";
        }
        if (input.length() <= 100) {
            return input;
        }
        return input.substring(0, 100) + "...";
    }
    
    /**
     * Threat levels for prompt injection detection.
     */
    public enum ThreatLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }
    
    /**
     * Result of prompt injection detection.
     */
    public static class DetectionResult {
        private final boolean isThreat;
        private final ThreatLevel threatLevel;
        private final String reason;
        
        private DetectionResult(boolean isThreat, ThreatLevel threatLevel, String reason) {
            this.isThreat = isThreat;
            this.threatLevel = threatLevel;
            this.reason = reason;
        }
        
        public static DetectionResult safe() {
            return new DetectionResult(false, null, null);
        }
        
        public static DetectionResult threat(ThreatLevel level, String reason) {
            return new DetectionResult(true, level, reason);
        }
        
        public boolean isThreat() {
            return isThreat;
        }
        
        public ThreatLevel getThreatLevel() {
            return threatLevel;
        }
        
        public String getReason() {
            return reason;
        }
        
        public boolean isHighThreat() {
            return isThreat && (threatLevel == ThreatLevel.HIGH || threatLevel == ThreatLevel.CRITICAL);
        }
    }
}
