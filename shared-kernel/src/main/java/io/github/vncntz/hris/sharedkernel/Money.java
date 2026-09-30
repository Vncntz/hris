package io.github.vncntz.hris.sharedkernel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/** Decimal amount in one currency; no currency-specific scale or rounding is inferred. */
public final class Money {
    private final BigDecimal amount;
    private final Currency currency;

    private Money(BigDecimal amount, Currency currency) {
        this.amount = Objects.requireNonNull(amount, "amount");
        this.currency = Objects.requireNonNull(currency, "currency");
    }

    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }

    public static Money parse(String decimalAmount, Currency currency) {
        Objects.requireNonNull(decimalAmount, "decimalAmount");
        return of(new BigDecimal(decimalAmount), currency);
    }

    public BigDecimal amount() {
        return amount;
    }

    public Currency currency() {
        return currency;
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return of(amount.add(other.amount), currency);
    }

    public Money subtract(Money other) {
        requireSameCurrency(other);
        return of(amount.subtract(other.amount), currency);
    }

    public Money multiply(BigDecimal factor) {
        return of(amount.multiply(Objects.requireNonNull(factor, "factor")), currency);
    }

    /** Changes scale only under the caller's rounding policy; UNNECESSARY rejects lost digits. */
    public Money withScale(int scale, RoundingMode roundingMode) {
        return of(amount.setScale(scale, Objects.requireNonNull(roundingMode, "roundingMode")), currency);
    }

    private void requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "other");
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Currencies must match");
        }
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Money money
                && currency.equals(money.currency)
                && amount.compareTo(money.amount) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.signum() == 0 ? BigDecimal.ZERO : amount.stripTrailingZeros(), currency);
    }

    @Override
    public String toString() {
        return currency.getCurrencyCode() + " " + amount.toPlainString();
    }
}
