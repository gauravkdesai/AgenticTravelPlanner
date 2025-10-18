package com.agentictravel.validation;

import com.agentictravel.model.TripRequest;
import com.agentictravel.security.InputSanitizer;
import com.agentictravel.security.PromptInjectionDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates TripRequest objects for security, completeness, and business rules.
 * Ensures all inputs are safe for processing and meet application requirements.
 */
@Component
public class TripRequestValidator {
    
    private static final Logger log = LoggerFactory.getLogger(TripRequestValidator.class);
    
    private final InputSanitizer inputSanitizer;
    private final PromptInjectionDetector injectionDetector;
    
    public TripRequestValidator(InputSanitizer inputSanitizer, PromptInjectionDetector injectionDetector) {
        this.inputSanitizer = inputSanitizer;
        this.injectionDetector = injectionDetector;
    }
    
    /**
     * Validates a TripRequest and returns validation results.
     * 
     * @param request The request to validate
     * @return ValidationResult containing validation status and any errors
     */
    public ValidationResult validate(TripRequest request) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        if (request == null) {
            return ValidationResult.error("Trip request cannot be null");
        }
        
        // Validate trip title
        if (request.tripTitle != null) {
            String sanitizedTitle = inputSanitizer.sanitizeTripTitle(request.tripTitle);
            if (!sanitizedTitle.equals(request.tripTitle)) {
                warnings.add("Trip title was sanitized");
                request.tripTitle = sanitizedTitle;
            }
            
            var injectionResult = injectionDetector.detectInjection(request.tripTitle);
            if (injectionResult.isThreat()) {
                errors.add("Trip title contains suspicious content: " + injectionResult.getReason());
            }
        }
        
        // Validate region
        if (request.region != null) {
            String sanitizedRegion = inputSanitizer.sanitizeRegion(request.region);
            if (!sanitizedRegion.equals(request.region)) {
                warnings.add("Region was sanitized");
                request.region = sanitizedRegion;
            }
            
            var injectionResult = injectionDetector.detectInjection(request.region);
            if (injectionResult.isThreat()) {
                errors.add("Region contains suspicious content: " + injectionResult.getReason());
            }
        }
        
        // Validate days
        if (request.days <= 0) {
            errors.add("Number of days must be positive");
        } else if (request.days > 365) {
            errors.add("Number of days cannot exceed 365");
        }
        
        // Validate people count
        if (request.people <= 0) {
            errors.add("Number of people must be positive");
        } else if (request.people > 50) {
            errors.add("Number of people cannot exceed 50");
        }
        
        // Validate budget format
        if (request.budget != null && !request.budget.trim().isEmpty()) {
            if (!isValidBudgetFormat(request.budget)) {
                warnings.add("Budget format may be invalid. Expected format: '1500 USD' or '€2000'");
            }
        }
        
        // Validate interests
        if (request.interests != null) {
            request.interests = inputSanitizer.sanitizeStringList(request.interests, 10, 50);
            if (request.interests.size() > 5) {
                warnings.add("Too many interests specified, using first 5");
            }
        }
        
        // Validate food preferences
        if (request.foodPreferences != null) {
            request.foodPreferences = inputSanitizer.sanitizeStringList(request.foodPreferences, 10, 100);
        }
        
        // Validate booking preferences
        if (request.bookingPreferences != null) {
            List<String> validPreferences = List.of("flight", "train", "car", "bus");
            request.bookingPreferences = request.bookingPreferences.stream()
                .filter(validPreferences::contains)
                .distinct()
                .toList();
        }
        
        // Validate notes
        if (request.notes != null) {
            String sanitizedNotes = inputSanitizer.sanitizeNotes(request.notes);
            if (!sanitizedNotes.equals(request.notes)) {
                warnings.add("Notes were sanitized");
                request.notes = sanitizedNotes;
            }
            
            var injectionResult = injectionDetector.detectInjection(request.notes);
            if (injectionResult.isThreat()) {
                errors.add("Notes contain suspicious content: " + injectionResult.getReason());
            }
        }
        
        // Validate amendments
        if (request.amendments != null) {
            String sanitizedAmendments = inputSanitizer.sanitizeAmendments(request.amendments);
            if (!sanitizedAmendments.equals(request.amendments)) {
                warnings.add("Amendments were sanitized");
                request.amendments = sanitizedAmendments;
            }
            
            var injectionResult = injectionDetector.detectInjection(request.amendments);
            if (injectionResult.isThreat()) {
                errors.add("Amendments contain suspicious content: " + injectionResult.getReason());
            }
        }
        
        // Validate tentative dates format
        if (request.tentativeDates != null) {
            if (!isValidDateFormat(request.tentativeDates.toString())) {
                warnings.add("Tentative dates format may be invalid. Expected: '2025-12-20 to 2025-12-27'");
            }
        }
        
        // Validate weather preference
        if (request.weatherPreference != null) {
            List<String> validPreferences = List.of("any", "warm", "mild", "cool", "cold", "rainy");
            if (!validPreferences.contains(request.weatherPreference)) {
                warnings.add("Invalid weather preference, using 'any'");
                request.weatherPreference = "any";
            }
        }
        
        // Check for minimum required fields
        if (request.region == null || request.region.trim().isEmpty()) {
            warnings.add("Region is recommended for better results");
        }
        
        if (request.interests == null || request.interests.isEmpty()) {
            warnings.add("Interests are recommended for personalized recommendations");
        }
        
        if (errors.isEmpty()) {
            log.info("Trip request validation successful. Warnings: {}", warnings.size());
            return ValidationResult.success(warnings);
        } else {
            log.warn("Trip request validation failed. Errors: {}, Warnings: {}", errors.size(), warnings.size());
            return ValidationResult.error(errors, warnings);
        }
    }
    
    /**
     * Validates budget format (e.g., "1500 USD", "€2000", "2000 CAD").
     */
    private boolean isValidBudgetFormat(String budget) {
        if (budget == null || budget.trim().isEmpty()) {
            return true;
        }
        
        String trimmed = budget.trim();
        // Allow formats like: "1500 USD", "€2000", "$2000", "2000 CAD", "2000"
        return trimmed.matches("^[€$]?\\d+\\s*[A-Z]{3}?$|^\\d+\\s*[€$]?$|^\\d+$");
    }
    
    /**
     * Validates date format (e.g., "2025-12-20 to 2025-12-27" or "2025-12-20,2025-12-27").
     */
    private boolean isValidDateFormat(String dates) {
        if (dates == null || dates.trim().isEmpty()) {
            return true;
        }
        
        String trimmed = dates.trim();
        // Allow formats like: "2025-12-20 to 2025-12-27" or "2025-12-20,2025-12-27"
        return trimmed.matches("^\\d{4}-\\d{2}-\\d{2}\\s+(to|,)\\s+\\d{4}-\\d{2}-\\d{2}$") ||
               trimmed.matches("^\\d{4}-\\d{2}-\\d{2}$");
    }
    
    /**
     * Result of validation containing status and any messages.
     */
    public static class ValidationResult {
        private final boolean isValid;
        private final List<String> errors;
        private final List<String> warnings;
        
        private ValidationResult(boolean isValid, List<String> errors, List<String> warnings) {
            this.isValid = isValid;
            this.errors = errors != null ? errors : new ArrayList<>();
            this.warnings = warnings != null ? warnings : new ArrayList<>();
        }
        
        public static ValidationResult success(List<String> warnings) {
            return new ValidationResult(true, new ArrayList<>(), warnings);
        }
        
        public static ValidationResult success() {
            return new ValidationResult(true, new ArrayList<>(), new ArrayList<>());
        }
        
        public static ValidationResult error(String error) {
            return new ValidationResult(false, List.of(error), new ArrayList<>());
        }
        
        public static ValidationResult error(List<String> errors, List<String> warnings) {
            return new ValidationResult(false, errors, warnings);
        }
        
        public boolean isValid() {
            return isValid;
        }
        
        public List<String> getErrors() {
            return errors;
        }
        
        public List<String> getWarnings() {
            return warnings;
        }
        
        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }
        
        public String getFirstError() {
            return errors.isEmpty() ? null : errors.get(0);
        }
    }
}
