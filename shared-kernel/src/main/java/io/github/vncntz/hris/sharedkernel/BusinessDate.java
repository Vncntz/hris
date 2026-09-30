package io.github.vncntz.hris.sharedkernel;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/** A calendar date in business context, without an implicit time or zone. */
public record BusinessDate(LocalDate value) {
    public BusinessDate {
        Objects.requireNonNull(value, "value");
    }

    public static BusinessDate of(LocalDate date) {
        return new BusinessDate(date);
    }

    public static BusinessDate parse(String isoDate) {
        Objects.requireNonNull(isoDate, "isoDate");
        return of(LocalDate.parse(isoDate, DateTimeFormatter.ISO_LOCAL_DATE));
    }

    public static BusinessDate today(Clock clock, BusinessTimeZone zone) {
        Objects.requireNonNull(clock, "clock");
        Objects.requireNonNull(zone, "zone");
        return of(clock.instant().atZone(zone.value()).toLocalDate());
    }

    public static BusinessDate from(UtcInstant instant, BusinessTimeZone zone) {
        Objects.requireNonNull(instant, "instant");
        Objects.requireNonNull(zone, "zone");
        return of(instant.value().atZone(zone.value()).toLocalDate());
    }

    /** Resolves midnight through the region's rules, including gaps at midnight. */
    public UtcInstant atStartOfDay(BusinessTimeZone zone) {
        Objects.requireNonNull(zone, "zone");
        return UtcInstant.of(value.atStartOfDay(zone.value()).toInstant());
    }

    @Override
    public String toString() {
        return value.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
}
