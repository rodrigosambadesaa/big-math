package ch.obermuhlner.math.big.algebra;

import ch.obermuhlner.math.big.BigRational;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable exact algebraic expression.
 *
 * <p>The small expression tree deliberately performs only identities that are
 * always valid.  This makes it suitable for symbolic matrix algorithms without
 * silently turning them into floating-point calculations.</p>
 */
public final class Expression {
    private enum Kind { CONSTANT, VARIABLE, ADD, SUBTRACT, MULTIPLY, DIVIDE, POWER, NEGATE }

    public static final Expression ZERO = new Expression(Kind.CONSTANT, BigRational.ZERO, null, null, null, 0);
    public static final Expression ONE = new Expression(Kind.CONSTANT, BigRational.ONE, null, null, null, 0);

    private final Kind kind;
    private final BigRational constant;
    private final String name;
    private final Expression left;
    private final Expression right;
    private final int exponent;

    private Expression(Kind kind, BigRational constant, String name, Expression left, Expression right, int exponent) {
        this.kind = kind;
        this.constant = constant;
        this.name = name;
        this.left = left;
        this.right = right;
        this.exponent = exponent;
    }

    public static Expression constant(long value) {
        return constant(BigRational.valueOf(java.math.BigInteger.valueOf(value)));
    }

    public static Expression constant(BigRational value) {
        Objects.requireNonNull(value, "value");
        BigRational reduced = value.reduce();
        if (reduced.isZero()) return ZERO;
        if (reduced.equals(BigRational.ONE)) return ONE;
        return new Expression(Kind.CONSTANT, reduced, null, null, null, 0);
    }

    public static Expression decimal(String value) {
        return constant(BigRational.valueOf(value));
    }

    public static Expression variable(String name) {
        Objects.requireNonNull(name, "name");
        if (!name.matches("[A-Za-z_$][A-Za-z0-9_$]*")) {
            throw new IllegalArgumentException("Invalid variable name: " + name);
        }
        return new Expression(Kind.VARIABLE, null, name, null, null, 0);
    }

    /** Parses numbers, variables, parentheses and the operators +, -, *, / and integer powers. */
    public static Expression parse(String text) {
        Parser parser = new Parser(text);
        Expression result = parser.parseExpression();
        parser.skipSpaces();
        if (!parser.atEnd()) throw parser.error("Unexpected input");
        return result;
    }

    public Expression add(Expression other) {
        Objects.requireNonNull(other, "other");
        if (isZero()) return other;
        if (other.isZero()) return this;
        if (isConstant() && other.isConstant()) return constant(constant.add(other.constant));
        if (equals(other)) return constant(2).multiply(this);
        return node(Kind.ADD, this, other);
    }

    public Expression subtract(Expression other) {
        Objects.requireNonNull(other, "other");
        if (other.isZero()) return this;
        if (equals(other)) return ZERO;
        if (isZero()) return other.negate();
        if (isConstant() && other.isConstant()) return constant(constant.subtract(other.constant));
        return node(Kind.SUBTRACT, this, other);
    }

    public Expression multiply(Expression other) {
        Objects.requireNonNull(other, "other");
        if (isZero() || other.isZero()) return ZERO;
        if (isOne()) return other;
        if (other.isOne()) return this;
        if (isConstant() && other.isConstant()) return constant(constant.multiply(other.constant));
        if (!isConstant() && other.isConstant()) return other.multiply(this);
        if (isConstant() && other.kind == Kind.MULTIPLY && other.left.isConstant()) {
            return constant(constant.multiply(other.left.constant)).multiply(other.right);
        }
        if (equals(other)) return pow(2);
        return node(Kind.MULTIPLY, this, other);
    }

    public Expression divide(Expression other) {
        Objects.requireNonNull(other, "other");
        if (other.isZero()) throw new ArithmeticException("Division by zero expression");
        if (isZero()) return ZERO;
        if (other.isOne()) return this;
        if (equals(other)) return ONE;
        if (isConstant() && other.isConstant()) return constant(constant.divide(other.constant));
        return node(Kind.DIVIDE, this, other);
    }

    public Expression negate() {
        if (isZero()) return ZERO;
        if (isConstant()) return constant(constant.negate());
        if (kind == Kind.NEGATE) return left;
        return new Expression(Kind.NEGATE, null, null, this, null, 0);
    }

    public Expression pow(int power) {
        if (power == 0) return ONE;
        if (power == 1) return this;
        if (isZero() && power < 0) throw new ArithmeticException("Zero cannot have a negative exponent");
        if (isZero()) return ZERO;
        if (isOne()) return ONE;
        if (isConstant()) return constant(constant.pow(power));
        return new Expression(Kind.POWER, null, null, this, null, power);
    }

    public Expression differentiate(String variable) {
        Objects.requireNonNull(variable, "variable");
        switch (kind) {
            case CONSTANT: return ZERO;
            case VARIABLE: return name.equals(variable) ? ONE : ZERO;
            case ADD: return left.differentiate(variable).add(right.differentiate(variable));
            case SUBTRACT: return left.differentiate(variable).subtract(right.differentiate(variable));
            case MULTIPLY:
                return left.differentiate(variable).multiply(right)
                        .add(left.multiply(right.differentiate(variable)));
            case DIVIDE:
                return left.differentiate(variable).multiply(right)
                        .subtract(left.multiply(right.differentiate(variable)))
                        .divide(right.pow(2));
            case POWER:
                return constant(exponent).multiply(left.pow(exponent - 1)).multiply(left.differentiate(variable));
            case NEGATE: return left.differentiate(variable).negate();
            default: throw new AssertionError(kind);
        }
    }

    public Expression substitute(Map<String, Expression> replacements) {
        Objects.requireNonNull(replacements, "replacements");
        switch (kind) {
            case CONSTANT: return this;
            case VARIABLE: return replacements.containsKey(name) ? Objects.requireNonNull(replacements.get(name)) : this;
            case ADD: return left.substitute(replacements).add(right.substitute(replacements));
            case SUBTRACT: return left.substitute(replacements).subtract(right.substitute(replacements));
            case MULTIPLY: return left.substitute(replacements).multiply(right.substitute(replacements));
            case DIVIDE: return left.substitute(replacements).divide(right.substitute(replacements));
            case POWER: return left.substitute(replacements).pow(exponent);
            case NEGATE: return left.substitute(replacements).negate();
            default: throw new AssertionError(kind);
        }
    }

    public BigDecimal evaluate(Map<String, BigDecimal> values, MathContext mathContext) {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(mathContext, "mathContext");
        switch (kind) {
            case CONSTANT: return constant.toBigDecimal(mathContext);
            case VARIABLE:
                BigDecimal value = values.get(name);
                if (value == null) throw new IllegalArgumentException("No value supplied for variable " + name);
                return value.round(mathContext);
            case ADD: return left.evaluate(values, mathContext).add(right.evaluate(values, mathContext), mathContext);
            case SUBTRACT: return left.evaluate(values, mathContext).subtract(right.evaluate(values, mathContext), mathContext);
            case MULTIPLY: return left.evaluate(values, mathContext).multiply(right.evaluate(values, mathContext), mathContext);
            case DIVIDE: return left.evaluate(values, mathContext).divide(right.evaluate(values, mathContext), mathContext);
            case POWER:
                BigDecimal base = left.evaluate(values, mathContext);
                if (exponent >= 0) return base.pow(exponent, mathContext);
                return BigDecimal.ONE.divide(base.pow(-exponent, mathContext), mathContext);
            case NEGATE: return left.evaluate(values, mathContext).negate(mathContext);
            default: throw new AssertionError(kind);
        }
    }

    public Set<String> variables() {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        collectVariables(result);
        return Collections.unmodifiableSet(result);
    }

    private void collectVariables(Set<String> result) {
        if (kind == Kind.VARIABLE) result.add(name);
        if (left != null) left.collectVariables(result);
        if (right != null) right.collectVariables(result);
    }

    public boolean isConstant() { return kind == Kind.CONSTANT; }
    public boolean isZero() { return kind == Kind.CONSTANT && constant.isZero(); }
    public boolean isOne() { return kind == Kind.CONSTANT && constant.equals(BigRational.ONE); }
    public BigRational constantValue() {
        if (!isConstant()) throw new IllegalStateException("Expression is not constant");
        return constant;
    }

    private static Expression node(Kind kind, Expression left, Expression right) {
        return new Expression(kind, null, null, left, right, 0);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof Expression)) return false;
        Expression other = (Expression) object;
        return exponent == other.exponent && kind == other.kind
                && Objects.equals(constant, other.constant) && Objects.equals(name, other.name)
                && Objects.equals(left, other.left) && Objects.equals(right, other.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, constant, name, left, right, exponent);
    }

    @Override
    public String toString() {
        return format(0);
    }

    private String format(int parentPrecedence) {
        int precedence;
        String text;
        switch (kind) {
            case CONSTANT: return constant.toRationalString();
            case VARIABLE: return name;
            case ADD:
                precedence = 1; text = left.format(precedence) + " + " + right.format(precedence); break;
            case SUBTRACT:
                precedence = 1; text = left.format(precedence) + " - " + right.format(precedence + 1); break;
            case MULTIPLY:
                precedence = 2; text = left.format(precedence) + "*" + right.format(precedence); break;
            case DIVIDE:
                precedence = 2; text = left.format(precedence) + "/" + right.format(precedence + 1); break;
            case POWER:
                precedence = 3; text = left.format(precedence) + "^" + exponent; break;
            case NEGATE:
                precedence = 4; text = "-" + left.format(precedence); break;
            default: throw new AssertionError(kind);
        }
        return precedence < parentPrecedence ? "(" + text + ")" : text;
    }

    private static final class Parser {
        private final String text;
        private int index;

        private Parser(String text) { this.text = Objects.requireNonNull(text, "text"); }
        private boolean atEnd() { return index >= text.length(); }
        private void skipSpaces() { while (!atEnd() && Character.isWhitespace(text.charAt(index))) index++; }
        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at position " + index + " in '" + text + "'");
        }

        private Expression parseExpression() {
            Expression value = parseTerm();
            while (true) {
                skipSpaces();
                if (consume('+')) value = value.add(parseTerm());
                else if (consume('-')) value = value.subtract(parseTerm());
                else return value;
            }
        }

        private Expression parseTerm() {
            Expression value = parsePower();
            while (true) {
                skipSpaces();
                if (consume('*')) value = value.multiply(parsePower());
                else if (consume('/')) value = value.divide(parsePower());
                else return value;
            }
        }

        private Expression parsePower() {
            Expression value = parseUnary();
            skipSpaces();
            if (consume('^')) {
                skipSpaces();
                boolean negative = consume('-');
                int start = index;
                while (!atEnd() && Character.isDigit(text.charAt(index))) index++;
                if (start == index) throw error("Expected integer exponent");
                int power = Integer.parseInt(text.substring(start, index));
                return value.pow(negative ? -power : power);
            }
            return value;
        }

        private Expression parseUnary() {
            skipSpaces();
            if (consume('-')) return parseUnary().negate();
            if (consume('+')) return parseUnary();
            if (consume('(')) {
                Expression value = parseExpression();
                skipSpaces();
                if (!consume(')')) throw error("Expected ')'");
                return value;
            }
            if (atEnd()) throw error("Expected expression");
            char first = text.charAt(index);
            if (Character.isLetter(first) || first == '_' || first == '$') {
                int start = index++;
                while (!atEnd()) {
                    char c = text.charAt(index);
                    if (!Character.isLetterOrDigit(c) && c != '_' && c != '$') break;
                    index++;
                }
                return variable(text.substring(start, index));
            }
            int start = index;
            boolean dot = false;
            while (!atEnd()) {
                char c = text.charAt(index);
                if (Character.isDigit(c)) index++;
                else if (c == '.' && !dot) { dot = true; index++; }
                else break;
            }
            if (start == index) throw error("Expected number or variable");
            return decimal(text.substring(start, index));
        }

        private boolean consume(char expected) {
            if (!atEnd() && text.charAt(index) == expected) { index++; return true; }
            return false;
        }
    }
}
