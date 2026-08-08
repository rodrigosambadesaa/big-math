package ch.obermuhlner.math.big.algebra;

import ch.obermuhlner.math.big.BigRational;

/** Exact arithmetic over {@link BigRational}. */
public final class BigRationalField implements Field<BigRational> {
    public static final BigRationalField INSTANCE = new BigRationalField();

    private BigRationalField() {
    }

    @Override public BigRational zero() { return BigRational.ZERO; }
    @Override public BigRational one() { return BigRational.ONE; }
    @Override public BigRational add(BigRational a, BigRational b) { return a.add(b).reduce(); }
    @Override public BigRational subtract(BigRational a, BigRational b) { return a.subtract(b).reduce(); }
    @Override public BigRational multiply(BigRational a, BigRational b) { return a.multiply(b).reduce(); }
    @Override public BigRational divide(BigRational a, BigRational b) { return a.divide(b).reduce(); }
    @Override public BigRational negate(BigRational a) { return a.negate().reduce(); }
    @Override public boolean isZero(BigRational a) { return a.isZero(); }
    @Override public BigRational fromLong(long value) { return BigRational.valueOf(java.math.BigInteger.valueOf(value)); }
    @Override public boolean areEqual(BigRational a, BigRational b) { return a.compareTo(b) == 0; }
}
