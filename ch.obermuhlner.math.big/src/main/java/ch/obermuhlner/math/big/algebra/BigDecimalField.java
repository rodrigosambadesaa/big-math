package ch.obermuhlner.math.big.algebra;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Objects;

/** A {@link BigDecimal} field with an explicit precision and zero tolerance. */
public final class BigDecimalField implements Field<BigDecimal> {
    private final MathContext mathContext;
    private final BigDecimal tolerance;

    public BigDecimalField(MathContext mathContext) {
        this(mathContext, BigDecimal.ZERO);
    }

    public BigDecimalField(MathContext mathContext, BigDecimal tolerance) {
        this.mathContext = Objects.requireNonNull(mathContext, "mathContext");
        this.tolerance = Objects.requireNonNull(tolerance, "tolerance").abs();
    }

    public MathContext getMathContext() {
        return mathContext;
    }

    public BigDecimal getTolerance() {
        return tolerance;
    }

    @Override public BigDecimal zero() { return BigDecimal.ZERO; }
    @Override public BigDecimal one() { return BigDecimal.ONE; }
    @Override public BigDecimal add(BigDecimal a, BigDecimal b) { return a.add(b, mathContext); }
    @Override public BigDecimal subtract(BigDecimal a, BigDecimal b) { return a.subtract(b, mathContext); }
    @Override public BigDecimal multiply(BigDecimal a, BigDecimal b) { return a.multiply(b, mathContext); }
    @Override public BigDecimal divide(BigDecimal a, BigDecimal b) { return a.divide(b, mathContext); }
    @Override public BigDecimal negate(BigDecimal a) { return a.negate(mathContext); }
    @Override public boolean isZero(BigDecimal a) { return a.abs().compareTo(tolerance) <= 0; }
    @Override public BigDecimal fromLong(long value) { return BigDecimal.valueOf(value); }
    @Override public boolean areEqual(BigDecimal a, BigDecimal b) {
        return a.subtract(b).abs().compareTo(tolerance) <= 0;
    }

    @Override public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof BigDecimalField)) return false;
        BigDecimalField other = (BigDecimalField) object;
        return mathContext.equals(other.mathContext) && tolerance.compareTo(other.tolerance) == 0;
    }

    @Override public int hashCode() { return 31 * mathContext.hashCode() + tolerance.stripTrailingZeros().hashCode(); }
}
