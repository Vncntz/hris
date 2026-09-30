package io.github.vncntz.hris.syntheticdata;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Inputs that identify a logical synthetic dataset. */
public record GeneratorRequest(PopulationProfile profile, long seed, String scenario,
                               Map<String, String> options) {
    public static final String BASELINE = "baseline";

    public GeneratorRequest {
        Objects.requireNonNull(profile, "profile");
        validateName(scenario, "scenario");
        Objects.requireNonNull(options, "options");
        TreeMap<String, String> sorted = new TreeMap<>();
        options.forEach((key, value) -> {
            validateName(key, "option key");
            if (value == null || !value.matches("[A-Za-z0-9._-]+")) {
                throw new IllegalArgumentException("option values must use ASCII letters, digits, dot, underscore, or hyphen");
            }
            sorted.put(key, value);
        });
        options = Collections.unmodifiableSortedMap(sorted);
    }

    public static GeneratorRequest baseline(PopulationProfile profile, long seed) {
        return new GeneratorRequest(profile, seed, BASELINE, Map.of());
    }

    private static void validateName(String value, String field) {
        if (value == null || !value.matches("[a-z][a-z0-9-]*")) {
            throw new IllegalArgumentException(field + " must be a lower-case ASCII name");
        }
    }
}
