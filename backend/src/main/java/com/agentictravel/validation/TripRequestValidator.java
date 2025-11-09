package com.agentictravel.validation;

import com.agentictravel.model.TripRequest;
import com.agentictravel.security.InputSanitizer;
import com.agentictravel.security.PromptInjectionDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TripRequestValidator {

    private static final int MAX_TRAVELERS = 50;
    private static final int MAX_DURATION = 365;
    private static final int MAX_BUDGET = 1000000;

    private static final Logger LOG = LoggerFactory.getLogger(TripRequestValidator.class);

    private final InputSanitizer sanitizer;
    private final PromptInjectionDetector promptInjectionDetector;

    public TripRequestValidator(InputSanitizer sanitizer, PromptInjectionDetector promptInjectionDetector) {
        this.sanitizer = sanitizer;
        this.promptInjectionDetector = promptInjectionDetector;
    }

    public ValidationResult validate(TripRequest request) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // 1. Sanitize all fields first
        request.tripTitle = sanitizer.sanitizeTripTitle(request.tripTitle);
        request.region = sanitizer.sanitizeRegion(request.region);
        request.days = sanitizer.sanitizePositiveInt(String.valueOf(request.days), 7, MAX_DURATION);
        request.people = sanitizer.sanitizePositiveInt(String.valueOf(request.people), 1, MAX_TRAVELERS);
        request.budget = sanitizer.sanitizeText(request.budget, 50);
        request.interests = sanitizer.sanitizeStringList(request.interests, 10, 50);
        request.foodPreferences = sanitizer.sanitizeStringList(request.foodPreferences, 5, 50);
        request.notes = sanitizer.sanitizeNotes(request.notes);
        request.amendments = sanitizer.sanitizeAmendments(request.amendments);

        // 2. Perform validation checks
        if (request.tripTitle == null || request.tripTitle.isBlank()) {
            errors.add("Trip title is required.");
        }
        if (request.region == null || request.region.isBlank()) {
            errors.add("Region is required.");
        }
        if (request.days <= 0) {
            errors.add("Duration must be a positive number of days.");
        }
        if (request.people <= 0) {
            errors.add("Number of travelers must be positive.");
        }

        // 3. Check for prompt injection risks
        if (promptInjectionDetector.assessRisk(request.notes).isHighRisk()) {
            errors.add("Potential prompt injection detected in notes.");
        }
        if (promptInjectionDetector.assessRisk(request.amendments).isHighRisk()) {
            errors.add("Potential prompt injection detected in amendments.");
        }
        if (promptInjectionDetector.assessRisk(request.tripTitle).isHighRisk()) {
            errors.add("Potential prompt injection detected in trip title.");
        }

        // 4. Check for safety in other free-text fields
        if (!sanitizer.isSafeForLLM(request.notes)) {
            warnings.add("Notes contain patterns that may be unsafe for the LLM.");
        }
        if (!sanitizer.isSafeForLLM(request.region)) {
            warnings.add("Region contains patterns that may be unsafe for the LLM.");
        }

        if (errors.isEmpty()) {
            LOG.info("Trip request validation successful. Warnings: {}", warnings.size());
        } else {
            LOG.warn("Trip request validation failed. Errors: {}, Warnings: {}", errors.size(), warnings.size());
        }

        return new ValidationResult(errors, warnings);
    }

    public static record ValidationResult(List<String> errors, List<String> warnings) {
        public boolean isValid() {
            return errors.isEmpty();
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }

        public String getFirstError() {
            return errors.isEmpty() ? "" : errors.get(0);
        }
    }
}
