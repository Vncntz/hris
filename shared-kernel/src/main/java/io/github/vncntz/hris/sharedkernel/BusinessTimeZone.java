package io.github.vncntz.hris.sharedkernel;

import java.time.ZoneId;
import java.util.Objects;

/** Explicit IANA region, never a host default or fixed numeric offset. */
public record BusinessTimeZone(ZoneId value) {
    public BusinessTimeZone {
        Objects.requireNonNull(value, "value");
        if (!ZoneId.getAvailableZoneIds().contains(value.getId())) {
            throw new IllegalArgumentException("Not an IANA region: " + value);
        }
    }

    public static BusinessTimeZone of(String region) {
        Objects.requireNonNull(region, "region");
        return new BusinessTimeZone(ZoneId.of(region));
    }

    @Override
    public String toString() {
        return value.getId();
    }
}
