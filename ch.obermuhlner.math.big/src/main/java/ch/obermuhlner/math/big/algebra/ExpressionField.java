package ch.obermuhlner.math.big.algebra;

/** Exact field of symbolic rational expressions. */
public final class ExpressionField implements Field<Expression> {
    public static final ExpressionField INSTANCE = new ExpressionField();

    private ExpressionField() {
    }

    @Override public Expression zero() { return Expression.ZERO; }
    @Override public Expression one() { return Expression.ONE; }
    @Override public Expression add(Expression a, Expression b) { return a.add(b); }
    @Override public Expression subtract(Expression a, Expression b) { return a.subtract(b); }
    @Override public Expression multiply(Expression a, Expression b) { return a.multiply(b); }
    @Override public Expression divide(Expression a, Expression b) { return a.divide(b); }
    @Override public Expression negate(Expression a) { return a.negate(); }
    @Override public boolean isZero(Expression a) { return a.isZero(); }
    @Override public Expression fromLong(long value) { return Expression.constant(value); }
}
