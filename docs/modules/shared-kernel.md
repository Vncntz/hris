# Shared kernel

`shared-kernel` provides five immutable, neutral Java value types. It has no production dependency outside the JDK. Domain modules own their entities, persistence mappings, policy, and installation configuration.

| Type | Meaning and boundary |
| --- | --- |
| `PublicId` | A `UUID` value. `of(UUID)` and `parse(String)` produce value-equal IDs; `toString()` is canonical UUID text. Invalid UUID text fails with `IllegalArgumentException`. No domain-specific ID scheme is implied. |
| `Money` | A `BigDecimal` and explicit `Currency`. `parse` accepts a locale-independent decimal string; `of` takes `BigDecimal`. `amount()` retains the input or arithmetic result's scale. Equality and hash code compare numeric amount and currency, so `1.0` equals `1.00` in the same currency. `toString()` is ISO currency code, one space, then plain decimal text. |
| `BusinessDate` | A `LocalDate` with ISO date parsing and formatting. It is a local calendar date, not a UTC instant. |
| `UtcInstant` | An `Instant` with ISO UTC parsing and formatting. `now(Clock)` uses only the supplied clock. |
| `BusinessTimeZone` | An explicitly supplied IANA region `ZoneId`. Fixed offsets and zone abbreviations are rejected. |

`Money.add` and `subtract` require matching currencies. `multiply` requires a `BigDecimal` factor. These operations do not round or silently choose a currency scale. `withScale(int, RoundingMode)` requires the caller's rounding policy; use `RoundingMode.UNNECESSARY` to reject precision loss. Callers must supply their own payroll, tax, and persistence rules. There is no `double` or `float` construction path and no exchange conversion.

`BusinessDate.from(UtcInstant, BusinessTimeZone)` and `UtcInstant.in(BusinessTimeZone)` interpret an instant in the supplied region. `BusinessDate.atStartOfDay(BusinessTimeZone)` uses the region's zone rules, including a possible midnight gap. `BusinessDate.today(Clock, BusinessTimeZone)` takes both inputs explicitly; the clock's own zone does not choose the business date. No primitive reads the host default zone or locale. Platform/Operations will supply the installation region later. D-123 names `Asia/Manila` as the planned Philippine default, but this module does not install that default.
