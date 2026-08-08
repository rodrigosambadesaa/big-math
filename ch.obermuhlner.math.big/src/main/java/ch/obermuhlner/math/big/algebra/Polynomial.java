package ch.obermuhlner.math.big.algebra;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Immutable univariate polynomial with coefficients in an arbitrary field. */
public final class Polynomial<T> {
    private final Field<T> field;
    private final List<T> coefficients; // ascending order

    @SafeVarargs
    public static <T> Polynomial<T> of(Field<T> field, T... coefficients) {
        List<T> values = new ArrayList<>(coefficients.length);
        Collections.addAll(values, coefficients);
        return new Polynomial<>(field, values);
    }

    public Polynomial(Field<T> field, List<T> coefficients) {
        this.field = Objects.requireNonNull(field, "field");
        Objects.requireNonNull(coefficients, "coefficients");
        List<T> copy = new ArrayList<>(coefficients.size());
        for (T coefficient : coefficients) copy.add(Objects.requireNonNull(coefficient, "coefficient"));
        int size = copy.size();
        while (size > 0 && field.isZero(copy.get(size - 1))) size--;
        this.coefficients = Collections.unmodifiableList(new ArrayList<>(copy.subList(0, size)));
    }

    public Field<T> getField() { return field; }
    public int degree() { return coefficients.size() - 1; }
    public boolean isZero() { return coefficients.isEmpty(); }
    public T coefficient(int degree) {
        if (degree < 0) throw new IndexOutOfBoundsException("Negative degree: " + degree);
        return degree < coefficients.size() ? coefficients.get(degree) : field.zero();
    }
    public T leadingCoefficient() { return isZero() ? field.zero() : coefficients.get(coefficients.size() - 1); }
    public List<T> coefficients() { return coefficients; }

    public Polynomial<T> add(Polynomial<T> other) {
        checkField(other);
        int size = Math.max(coefficients.size(), other.coefficients.size());
        List<T> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) result.add(field.add(coefficient(i), other.coefficient(i)));
        return new Polynomial<>(field, result);
    }

    public Polynomial<T> subtract(Polynomial<T> other) {
        checkField(other);
        int size = Math.max(coefficients.size(), other.coefficients.size());
        List<T> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) result.add(field.subtract(coefficient(i), other.coefficient(i)));
        return new Polynomial<>(field, result);
    }

    public Polynomial<T> negate() { return scale(field.fromLong(-1)); }

    public Polynomial<T> scale(T scalar) {
        Objects.requireNonNull(scalar, "scalar");
        List<T> result = new ArrayList<>(coefficients.size());
        for (T coefficient : coefficients) result.add(field.multiply(coefficient, scalar));
        return new Polynomial<>(field, result);
    }

    public Polynomial<T> multiply(Polynomial<T> other) {
        checkField(other);
        if (isZero() || other.isZero()) return new Polynomial<>(field, Collections.<T>emptyList());
        List<T> result = new ArrayList<>(Collections.nCopies(degree() + other.degree() + 1, field.zero()));
        for (int i = 0; i <= degree(); i++) {
            for (int j = 0; j <= other.degree(); j++) {
                result.set(i + j, field.add(result.get(i + j), field.multiply(coefficient(i), other.coefficient(j))));
            }
        }
        return new Polynomial<>(field, result);
    }

    public Polynomial<T> pow(int exponent) {
        if (exponent < 0) throw new IllegalArgumentException("Negative polynomial exponent: " + exponent);
        Polynomial<T> result = Polynomial.of(field, field.one());
        Polynomial<T> base = this;
        int remaining = exponent;
        while (remaining > 0) {
            if ((remaining & 1) != 0) result = result.multiply(base);
            remaining >>>= 1;
            if (remaining != 0) base = base.multiply(base);
        }
        return result;
    }

    public T evaluate(T value) {
        Objects.requireNonNull(value, "value");
        T result = field.zero();
        for (int i = degree(); i >= 0; i--) result = field.add(field.multiply(result, value), coefficient(i));
        return result;
    }

    public Polynomial<T> compose(Polynomial<T> inner) {
        checkField(inner);
        Polynomial<T> result = new Polynomial<>(field, Collections.<T>emptyList());
        for (int i = degree(); i >= 0; i--) {
            result = result.multiply(inner).add(Polynomial.of(field, coefficient(i)));
        }
        return result;
    }

    public Polynomial<T> derivative() {
        if (degree() <= 0) return new Polynomial<>(field, Collections.<T>emptyList());
        List<T> result = new ArrayList<>(degree());
        for (int i = 1; i <= degree(); i++) result.add(field.multiply(coefficient(i), field.fromLong(i)));
        return new Polynomial<>(field, result);
    }

    public Polynomial<T> integral(T integrationConstant) {
        List<T> result = new ArrayList<>(coefficients.size() + 1);
        result.add(Objects.requireNonNull(integrationConstant, "integrationConstant"));
        for (int i = 0; i < coefficients.size(); i++) {
            result.add(field.divide(coefficients.get(i), field.fromLong(i + 1L)));
        }
        return new Polynomial<>(field, result);
    }

    public DivisionResult<T> divideAndRemainder(Polynomial<T> divisor) {
        checkField(divisor);
        if (divisor.isZero()) throw new ArithmeticException("Polynomial division by zero");
        Polynomial<T> remainder = this;
        List<T> quotient = new ArrayList<>(Collections.nCopies(Math.max(0, degree() - divisor.degree() + 1), field.zero()));
        while (!remainder.isZero() && remainder.degree() >= divisor.degree()) {
            int power = remainder.degree() - divisor.degree();
            T factor = field.divide(remainder.leadingCoefficient(), divisor.leadingCoefficient());
            quotient.set(power, factor);
            List<T> termValues = new ArrayList<>(Collections.nCopies(power + 1, field.zero()));
            termValues.set(power, factor);
            remainder = remainder.subtract(divisor.multiply(new Polynomial<>(field, termValues)));
        }
        return new DivisionResult<>(new Polynomial<>(field, quotient), remainder);
    }

    public Polynomial<T> gcd(Polynomial<T> other) {
        checkField(other);
        Polynomial<T> a = this;
        Polynomial<T> b = other;
        while (!b.isZero()) {
            Polynomial<T> remainder = a.divideAndRemainder(b).getRemainder();
            a = b;
            b = remainder;
        }
        return a.isZero() ? a : a.scale(field.divide(field.one(), a.leadingCoefficient()));
    }

    private void checkField(Polynomial<T> other) {
        Objects.requireNonNull(other, "other");
        if (field != other.field && !field.equals(other.field)) {
            throw new IllegalArgumentException("Polynomials use different fields");
        }
    }

    @Override public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof Polynomial)) return false;
        Polynomial<?> other = (Polynomial<?>) object;
        return field.equals(other.field) && coefficients.equals(other.coefficients);
    }
    @Override public int hashCode() { return 31 * field.hashCode() + coefficients.hashCode(); }

    @Override public String toString() {
        if (isZero()) return "0";
        StringBuilder result = new StringBuilder();
        for (int i = degree(); i >= 0; i--) {
            if (field.isZero(coefficient(i))) continue;
            if (result.length() > 0) result.append(" + ");
            result.append(coefficient(i));
            if (i > 0) result.append("*x");
            if (i > 1) result.append('^').append(i);
        }
        return result.toString();
    }

    public static final class DivisionResult<T> {
        private final Polynomial<T> quotient;
        private final Polynomial<T> remainder;
        private DivisionResult(Polynomial<T> quotient, Polynomial<T> remainder) {
            this.quotient = quotient;
            this.remainder = remainder;
        }
        public Polynomial<T> getQuotient() { return quotient; }
        public Polynomial<T> getRemainder() { return remainder; }
    }
}
