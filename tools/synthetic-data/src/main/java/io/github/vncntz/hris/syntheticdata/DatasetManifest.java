package io.github.vncntz.hris.syntheticdata;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;

/** Canonical dataset identity. The text format is part of generator version 1. */
public record DatasetManifest(int generatorVersion, PopulationProfile profile,
                              int activePopulation, long seed, String scenario,
                              Map<String, String> options) {
    public DatasetManifest {
        if (generatorVersion < 1) {
            throw new IllegalArgumentException("generatorVersion must be positive");
        }
        Objects.requireNonNull(profile, "profile");
        if (activePopulation != profile.activePopulation()) {
            throw new IllegalArgumentException("activePopulation must match profile");
        }
        GeneratorRequest normalized = new GeneratorRequest(profile, seed, scenario, options);
        scenario = normalized.scenario();
        options = normalized.options();
    }

    /** ASCII fields in fixed order, LF separators, and options sorted by Java String order. */
    public String canonicalText() {
        StringBuilder text = new StringBuilder()
                .append("generator-version=").append(generatorVersion).append('\n')
                .append("profile=").append(profile.name()).append('\n')
                .append("active-population=").append(activePopulation).append('\n')
                .append("seed=").append(seed).append('\n')
                .append("scenario=").append(scenario).append('\n');
        options.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(entry -> text.append("option.").append(entry.getKey())
                        .append('=').append(entry.getValue()).append('\n'));
        return text.toString();
    }

    public String fingerprint() {
        return HexFormat.of().formatHex(sha256(canonicalText().getBytes(StandardCharsets.UTF_8)));
    }

    static byte[] sha256(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (NoSuchAlgorithmException impossibleOnSupportedJdk) {
            throw new IllegalStateException("SHA-256 unavailable", impossibleOnSupportedJdk);
        }
    }
}
