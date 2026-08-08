package ch.obermuhlner.math.big.algebra;

/** The two-element field GF(2), useful for binary matrices and coding theory. */
public final class BinaryField implements Field<Boolean> {
    public static final BinaryField INSTANCE = new BinaryField();
    private BinaryField() { }
    @Override public Boolean zero() { return false; }
    @Override public Boolean one() { return true; }
    @Override public Boolean add(Boolean a, Boolean b) { return a ^ b; }
    @Override public Boolean subtract(Boolean a, Boolean b) { return a ^ b; }
    @Override public Boolean multiply(Boolean a, Boolean b) { return a && b; }
    @Override public Boolean divide(Boolean a, Boolean b) { if (!b) throw new ArithmeticException("Division by zero"); return a; }
    @Override public Boolean negate(Boolean a) { return a; }
    @Override public boolean isZero(Boolean a) { return !a; }
    @Override public Boolean fromLong(long value) { return (value & 1L) != 0; }
}
