package io.github.vncntz.hris.syntheticdata;

import io.github.vncntz.hris.sharedkernel.PublicId;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/** Lazily derives fictional identities from one immutable manifest. */
public final class SyntheticDataGenerator {
    public static final int GENERATOR_VERSION = 1;

    private final DatasetManifest manifest;

    public SyntheticDataGenerator(GeneratorRequest request) {
        Objects.requireNonNull(request, "request");
        manifest = new DatasetManifest(GENERATOR_VERSION, request.profile(),
                request.profile().activePopulation(), request.seed(), request.scenario(), request.options());
    }

    public DatasetManifest manifest() {
        return manifest;
    }

    /** A fresh lazy stream; no population list is retained. */
    public Stream<SyntheticRecord> records() {
        return IntStream.rangeClosed(1, manifest.activePopulation()).mapToObj(this::recordAt);
    }

    public SyntheticRecord recordAt(int ordinal) {
        if (ordinal < 1 || ordinal > manifest.activePopulation()) {
            throw new IllegalArgumentException("ordinal outside profile population");
        }
        // SHA-256 over UTF-8 canonical manifest text followed by a fixed, one-based ordinal line.
        byte[] digest = DatasetManifest.sha256((manifest.canonicalText() + "record-ordinal=" + ordinal + "\n")
                .getBytes(StandardCharsets.UTF_8));
        // RFC 9562 custom UUID version 8; use the first 128 digest bits and set version/variant.
        ByteBuffer bytes = ByteBuffer.wrap(digest);
        long most = (bytes.getLong() & 0xffffffffffff0fffL) | 0x0000000000008000L;
        long least = (bytes.getLong() & 0x3fffffffffffffffL) | 0x8000000000000000L;
        return new SyntheticRecord(ordinal, PublicId.of(new UUID(most, least)),
                "SYNTH-" + String.format(java.util.Locale.ROOT, "%06d", ordinal));
    }
}
