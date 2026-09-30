package io.github.vncntz.hris.sharedkernel;

import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/** A point on the UTC timeline, distinct from a local business date. */
public record UtcInstant(Instant value) {
    public UtcInstant {
        Objects.requireNonNull(value, "value");
    }

    public static UtcInstant of(Instant instant) {
        return new UtcInstant(instant);
    }

    public static UtcInstant parse(String isoInstant) {
        Objects.requireNonNull(isoInstant, "isoInstant");
        return of(Instant.parse(isoInstant));
    }

    public static UtcInstant now(Clock clock) {
        return of(Objects.requireNonNull(clock, "clock").instant());
    }

    public BusinessDate in(BusinessTimeZone zone) {
        return BusinessDate.from(this, zone);
    }

    @Override
    public String toString() {
        return DateTimeFormatter.ISO_INSTANT.format(value);
    }
}
