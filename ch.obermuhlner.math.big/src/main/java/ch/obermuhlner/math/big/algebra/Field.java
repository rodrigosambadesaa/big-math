package ch.obermuhlner.math.big.algebra;

/**
 * Defines the arithmetic needed by generic algebraic algorithms.
 *
 * <p>Implementations should be immutable (or effectively immutable) and must
 * agree on their zero test and equality semantics.  A matrix keeps the field
 * instance that created it, so precision and tolerance policies travel with
 * the values.</p>
 *
 * @param <T> element type
 */
public interface Field<T> {

    T zero();

    T one();

    T add(T left, T right);

    T subtract(T left, T right);

    T multiply(T left, T right);

    T divide(T left, T right);

    T negate(T value);

    boolean isZero(T value);

    T fromLong(long value);

    /** Returns the complex conjugate, or the value itself for real fields. */
    default T conjugate(T value) {
        return value;
    }

    /** Equality used by algebraic containers and algorithms. */
    default boolean areEqual(T left, T right) {
        return left == null ? right == null : left.equals(right);
    }
}
