package io.github.vncntz.hris.syntheticdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;

class SyntheticDataGeneratorTest {
    private static final GeneratorRequest GOLDEN_REQUEST = new GeneratorRequest(
            PopulationProfile.S, -42L, "baseline", Map.of("density", "high", "edge", "overnight"));

    @Test
    void profileTargetsAreExact() {
        assertEquals(2_000, PopulationProfile.S.activePopulation());
        assertEquals(10_000, PopulationProfile.M.activePopulation());
        assertEquals(30_000, PopulationProfile.L.activePopulation());
        assertEquals(100_000, PopulationProfile.XL.activePopulation());
    }

    @Test
    void canonicalManifestAndGoldenFingerprintAreStable() {
        DatasetManifest manifest = new SyntheticDataGenerator(GOLDEN_REQUEST).manifest();
        assertEquals(1, manifest.generatorVersion());
        assertEquals("generator-version=1\nprofile=S\nactive-population=2000\nseed=-42\n"
                + "scenario=baseline\noption.density=high\noption.edge=overnight\n", manifest.canonicalText());
        assertEquals("de9923ed2b6f7f242b15dec853e0d7db7f8031e518e6edd13458f5beb3ed2251",
                manifest.fingerprint());
        assertEquals(manifest, new SyntheticDataGenerator(GOLDEN_REQUEST).manifest());
        assertEquals(manifest.fingerprint(), new SyntheticDataGenerator(GOLDEN_REQUEST).manifest().fingerprint());
    }

    @Test
    void optionOrderAndHostDefaultsDoNotAffectIdentityOrRecords() {
        Map<String, String> first = new LinkedHashMap<>();
        first.put("edge", "overnight");
        first.put("density", "high");
        Map<String, String> second = new LinkedHashMap<>();
        second.put("density", "high");
        second.put("edge", "overnight");
        GeneratorRequest request = new GeneratorRequest(PopulationProfile.S, -42L, "baseline", first);
        first.put("edge", "changed");
        SyntheticDataGenerator reference = new SyntheticDataGenerator(request);
        TimeZone originalZone = TimeZone.getDefault();
        Locale originalLocale = Locale.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Kiritimati"));
            Locale.setDefault(Locale.JAPAN);
            SyntheticDataGenerator alternate = new SyntheticDataGenerator(
                    new GeneratorRequest(PopulationProfile.S, -42L, "baseline", second));
            assertEquals(reference.manifest().canonicalText(), alternate.manifest().canonicalText());
            assertEquals(reference.manifest().fingerprint(), alternate.manifest().fingerprint());
            assertEquals(reference.recordAt(1), alternate.recordAt(1));
        } finally {
            TimeZone.setDefault(originalZone);
            Locale.setDefault(originalLocale);
        }
    }

    @Test
    void seedChangesDatasetAndRecordIdentity() {
        SyntheticDataGenerator first = new SyntheticDataGenerator(GOLDEN_REQUEST);
        SyntheticDataGenerator second = new SyntheticDataGenerator(new GeneratorRequest(
                PopulationProfile.S, -41L, "baseline", GOLDEN_REQUEST.options()));
        assertNotEquals(first.manifest().fingerprint(), second.manifest().fingerprint());
        assertNotEquals(first.recordAt(1).publicId(), second.recordAt(1).publicId());
        assertEquals(first.recordAt(1), new SyntheticDataGenerator(GOLDEN_REQUEST).recordAt(1));
    }

    @Test
    void knownRecordIdentitiesAndLabelsAreStable() {
        SyntheticDataGenerator generator = new SyntheticDataGenerator(GOLDEN_REQUEST);
        assertEquals("859d9274-5ebc-889e-b778-7822bdf4e978",
                generator.recordAt(1).publicId().toString());
        assertEquals("SYNTH-000001", generator.recordAt(1).label());
        assertEquals("43970ad4-798a-8661-acda-c267d668c8b6",
                generator.recordAt(2_000).publicId().toString());
        assertEquals("SYNTH-002000", generator.recordAt(2_000).label());
        assertThrows(IllegalArgumentException.class, () -> generator.recordAt(0));
        assertThrows(IllegalArgumentException.class, () -> generator.recordAt(2_001));
    }

    @Test
    void xlStreamIteratesExactPopulationWithUniquePublicIds() {
        SyntheticDataGenerator generator = new SyntheticDataGenerator(
                GeneratorRequest.baseline(PopulationProfile.XL, Long.MIN_VALUE));
        Set<String> ids = new HashSet<>();
        int[] count = {0};
        generator.records().forEach(record -> {
            count[0]++;
            assertEquals(count[0], record.ordinal());
            assertEquals("SYNTH-" + String.format(Locale.ROOT, "%06d", count[0]), record.label());
            assertTrue(ids.add(record.publicId().toString()));
        });
        assertEquals(100_000, count[0]);
        assertEquals(100_000, ids.size());
        assertEquals(100_000, generator.records().count());
    }

    @Test
    void ambiguousInputsAreRejected() {
        assertThrows(NullPointerException.class, () -> new GeneratorRequest(null, 0, "baseline", Map.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new GeneratorRequest(PopulationProfile.S, 0, "Baseline", Map.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new GeneratorRequest(PopulationProfile.S, 0, "baseline", Map.of("bad.key", "value")));
        assertThrows(IllegalArgumentException.class,
                () -> new GeneratorRequest(PopulationProfile.S, 0, "baseline", Map.of("key", "value\nother")));
        assertThrows(NullPointerException.class,
                () -> new GeneratorRequest(PopulationProfile.S, 0, "baseline", null));
    }
}
