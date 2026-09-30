package io.github.vncntz.hris.sharedkernel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Currency;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SharedPrimitivesTest {
    private static final Currency PHP = Currency.getInstance("PHP");
    private static final Currency USD = Currency.getInstance("USD");
    private static final BusinessTimeZone MANILA = BusinessTimeZone.of("Asia/Manila");
    private static final BusinessTimeZone NEW_YORK = BusinessTimeZone.of("America/New_York");

    @Test
    void publicIdRoundTripsAndRejectsMalformedValues() {
        UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        PublicId id = PublicId.of(uuid);
        assertEquals(uuid, id.value());
        assertEquals(id, PublicId.parse(id.toString()));
        assertEquals(id.hashCode(), PublicId.parse(id.toString()).hashCode());
        assertNotEquals(id, PublicId.of(new UUID(0, 0)));
        assertThrows(IllegalArgumentException.class, () -> PublicId.parse("invalid-id"));
        assertThrows(IllegalArgumentException.class, () -> PublicId.parse("1-1-1-1-1"));
        assertThrows(NullPointerException.class, () -> PublicId.parse(null));
        assertThrows(NullPointerException.class, () -> PublicId.of(null));
    }

    @Test
    void moneyKeepsPrecisionScaleAndValueEquality() {
        Money first = Money.parse("0.10", PHP);
        Money second = Money.parse("0.20", PHP);
        Money sum = first.add(second);
        assertEquals(new BigDecimal("0.30"), sum.amount());
        assertEquals(2, sum.amount().scale());
        assertEquals(Money.parse("0.3", PHP), sum);
        assertEquals(sum.hashCode(), Money.parse("0.3", PHP).hashCode());
        assertNotEquals(sum, Money.parse("0.30", USD));
        assertEquals(new BigDecimal("0.10"), sum.subtract(second).amount());
        assertEquals(new BigDecimal("0.030"), first.multiply(new BigDecimal("0.3")).amount());
        assertEquals("PHP 0.30", sum.toString());
        assertEquals(new BigDecimal("12345678901234567890.123456789"),
                Money.parse("12345678901234567890.123456789", PHP).amount());
    }

    @Test
    void moneyRequiresExplicitRoundingAndMatchingCurrency() {
        Money value = Money.parse("1.235", PHP);
        assertThrows(ArithmeticException.class, () -> value.withScale(2, RoundingMode.UNNECESSARY));
        assertEquals(new BigDecimal("1.24"), value.withScale(2, RoundingMode.HALF_UP).amount());
        assertEquals(new BigDecimal("1.23"), value.withScale(2, RoundingMode.DOWN).amount());
        assertThrows(IllegalArgumentException.class, () -> value.add(Money.parse("1", USD)));
        assertThrows(IllegalArgumentException.class, () -> value.subtract(Money.parse("1", USD)));
        assertThrows(NumberFormatException.class, () -> Money.parse("NaN", PHP));
        assertThrows(NumberFormatException.class, () -> Money.parse("1,234.00", PHP));
        assertThrows(NullPointerException.class, () -> Money.of(null, PHP));
        assertThrows(NullPointerException.class, () -> Money.of(BigDecimal.ONE, null));
        assertThrows(NullPointerException.class, () -> value.multiply(null));
    }

    @Test
    void dateAndInstantUseDistinctIsoRoundTrips() {
        BusinessDate date = BusinessDate.of(LocalDate.of(2024, 3, 10));
        UtcInstant instant = UtcInstant.of(Instant.parse("2024-03-10T04:30:00.123456789Z"));
        assertEquals(date, BusinessDate.parse(date.toString()));
        assertEquals(instant, UtcInstant.parse(instant.toString()));
        assertEquals("2024-03-10", date.toString());
        assertEquals("2024-03-10T04:30:00.123456789Z", instant.toString());
        assertThrows(RuntimeException.class, () -> BusinessDate.parse("03/10/2024"));
        assertThrows(RuntimeException.class, () -> UtcInstant.parse("2024-03-10"));
    }

    @Test
    void explicitRegionsHandleDateBoundariesAndOffsetTransitions() {
        UtcInstant beforeMidnight = UtcInstant.parse("2024-03-09T16:30:00Z");
        assertEquals(BusinessDate.parse("2024-03-10"), beforeMidnight.in(MANILA));
        assertEquals(BusinessDate.parse("2024-03-09"), beforeMidnight.in(NEW_YORK));
        assertEquals(UtcInstant.parse("2024-03-09T16:00:00Z"),
                BusinessDate.parse("2024-03-10").atStartOfDay(MANILA));
        assertEquals(UtcInstant.parse("2024-03-10T05:00:00Z"),
                BusinessDate.parse("2024-03-10").atStartOfDay(NEW_YORK));
        assertEquals(UtcInstant.parse("2024-03-11T04:00:00Z"),
                BusinessDate.parse("2024-03-11").atStartOfDay(NEW_YORK));
        assertEquals(BusinessDate.parse("2024-03-10"),
                UtcInstant.parse("2024-03-10T07:30:00Z").in(NEW_YORK));
        assertEquals(BusinessDate.parse("2024-03-10"),
                UtcInstant.parse("2024-03-10T08:30:00Z").in(NEW_YORK));
    }

    @Test
    void zonesMustBeExplicitIanaRegions() {
        assertEquals(ZoneId.of("Asia/Manila"), MANILA.value());
        assertEquals("Asia/Manila", MANILA.toString());
        assertEquals(MANILA, BusinessTimeZone.of("Asia/Manila"));
        assertThrows(IllegalArgumentException.class, () -> BusinessTimeZone.of("+08:00"));
        assertThrows(DateTimeException.class, () -> BusinessTimeZone.of("PST"));
        assertThrows(IllegalArgumentException.class, () -> new BusinessTimeZone(ZoneId.of("UTC+08:00")));
        assertThrows(NullPointerException.class, () -> BusinessTimeZone.of(null));
    }

    @Test
    void clocksAndBusinessZonesControlCurrentDateWithoutHostDefaults() {
        TimeZone originalZone = TimeZone.getDefault();
        Locale originalLocale = Locale.getDefault();
        Clock clock = Clock.fixed(Instant.parse("2024-01-01T01:00:00Z"), ZoneId.of("Pacific/Honolulu"));
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Kiritimati"));
            Locale.setDefault(Locale.FRANCE);
            assertEquals(UtcInstant.parse("2024-01-01T01:00:00Z"), UtcInstant.now(clock));
            assertEquals(BusinessDate.parse("2024-01-01"), BusinessDate.today(clock, MANILA));
            assertEquals(BusinessDate.parse("2023-12-31"), BusinessDate.today(clock, NEW_YORK));
            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Honolulu"));
            Locale.setDefault(Locale.JAPAN);
            assertEquals(BusinessDate.parse("2024-01-01"), BusinessDate.today(clock, MANILA));
            assertEquals(BusinessDate.parse("2023-12-31"), BusinessDate.today(clock, NEW_YORK));
            assertEquals("PHP 0.30", Money.parse("0.30", PHP).toString());
        } finally {
            TimeZone.setDefault(originalZone);
            Locale.setDefault(originalLocale);
        }
        assertThrows(NullPointerException.class, () -> UtcInstant.now(null));
        assertThrows(NullPointerException.class, () -> BusinessDate.today(null, MANILA));
        assertThrows(NullPointerException.class, () -> BusinessDate.today(clock, null));
    }
}
