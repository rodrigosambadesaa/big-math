package ch.obermuhlner.math.big.algebra;

import ch.obermuhlner.math.big.BigComplex;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Objects;

/** High precision complex arithmetic for generic matrices and polynomials. */
public final class BigComplexField implements Field<BigComplex> {
    private final MathContext mathContext;
    private final BigDecimal tolerance;

    public BigComplexField(MathContext mathContext) {
        this(mathContext, BigDecimal.ZERO);
    }

    public BigComplexField(MathContext mathContext, BigDecimal tolerance) {
        this.mathContext = Objects.requireNonNull(mathContext, "mathContext");
        this.tolerance = Objects.requireNonNull(tolerance, "tolerance").abs();
    }

    public MathContext getMathContext() { return mathContext; }
    public BigDecimal getTolerance() { return tolerance; }

    @Override public BigComplex zero() { return BigComplex.ZERO; }
    @Override public BigComplex one() { return BigComplex.ONE; }
    @Override public BigComplex add(BigComplex a, BigComplex b) { return a.add(b, mathContext); }
    @Override public BigComplex subtract(BigComplex a, BigComplex b) { return a.subtract(b, mathContext); }
    @Override public BigComplex multiply(BigComplex a, BigComplex b) { return a.multiply(b, mathContext); }
    @Override public BigComplex divide(BigComplex a, BigComplex b) { return a.divide(b, mathContext); }
    @Override public BigComplex negate(BigComplex a) { return a.negate(); }
    @Override public boolean isZero(BigComplex a) { return a.abs(mathContext).compareTo(tolerance) <= 0; }
    @Override public BigComplex fromLong(long value) { return BigComplex.valueOf(BigDecimal.valueOf(value)); }
    @Override public BigComplex conjugate(BigComplex value) { return value.conjugate(); }
    @Override public boolean areEqual(BigComplex a, BigComplex b) {
        return a.subtract(b, mathContext).abs(mathContext).compareTo(tolerance) <= 0;
    }

    @Override public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof BigComplexField)) return false;
        BigComplexField other = (BigComplexField) object;
        return mathContext.equals(other.mathContext) && tolerance.compareTo(other.tolerance) == 0;
    }

    @Override public int hashCode() { return 31 * mathContext.hashCode() + tolerance.stripTrailingZeros().hashCode(); }
}
