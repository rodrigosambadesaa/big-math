package ch.obermuhlner.math.big.calculus;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** High precision numerical differentiation, integration, roots and ODE steps. */
public final class BigDecimalCalculus {
    private static final BigDecimal TWO = BigDecimal.valueOf(2);
    private static final BigDecimal THREE = BigDecimal.valueOf(3);
    private static final BigDecimal SIX = BigDecimal.valueOf(6);

    private BigDecimalCalculus() {
    }

    @FunctionalInterface
    public interface Function {
        BigDecimal apply(BigDecimal x, MathContext mathContext);
    }

    @FunctionalInterface
    public interface OdeFunction {
        BigDecimal apply(BigDecimal x, BigDecimal y, MathContext mathContext);
    }

    /** Five-point central derivative, with truncation error O(h^4). */
    public static BigDecimal derivative(Function function, BigDecimal x, BigDecimal step, MathContext mathContext) {
        requireStep(step);
        BigDecimal twoH = step.multiply(TWO, mathContext);
        BigDecimal numerator = function.apply(x.subtract(twoH, mathContext), mathContext)
                .subtract(function.apply(x.add(twoH, mathContext), mathContext), mathContext)
                .add(function.apply(x.add(step, mathContext), mathContext).multiply(BigDecimal.valueOf(8), mathContext), mathContext)
                .subtract(function.apply(x.subtract(step, mathContext), mathContext).multiply(BigDecimal.valueOf(8), mathContext), mathContext);
        return numerator.divide(step.multiply(BigDecimal.valueOf(12), mathContext), mathContext);
    }

    public static BigDecimal secondDerivative(Function function, BigDecimal x, BigDecimal step, MathContext mathContext) {
        requireStep(step);
        BigDecimal numerator = function.apply(x.add(step, mathContext), mathContext)
                .subtract(function.apply(x, mathContext).multiply(TWO, mathContext), mathContext)
                .add(function.apply(x.subtract(step, mathContext), mathContext), mathContext);
        return numerator.divide(step.multiply(step, mathContext), mathContext);
    }

    public static BigDecimal integrateTrapezoidal(Function function, BigDecimal lower, BigDecimal upper,
                                                   int intervals, MathContext mathContext) {
        requireIntervals(intervals);
        BigDecimal step = upper.subtract(lower, mathContext).divide(BigDecimal.valueOf(intervals), mathContext);
        BigDecimal sum = function.apply(lower, mathContext).add(function.apply(upper, mathContext), mathContext).divide(TWO, mathContext);
        for (int i = 1; i < intervals; i++) sum = sum.add(function.apply(lower.add(step.multiply(BigDecimal.valueOf(i), mathContext), mathContext), mathContext), mathContext);
        return sum.multiply(step, mathContext);
    }

    public static BigDecimal integrateSimpson(Function function, BigDecimal lower, BigDecimal upper,
                                               int intervals, MathContext mathContext) {
        requireIntervals(intervals);
        if ((intervals & 1) != 0) throw new IllegalArgumentException("Simpson integration requires an even interval count");
        BigDecimal step = upper.subtract(lower, mathContext).divide(BigDecimal.valueOf(intervals), mathContext);
        BigDecimal sum = function.apply(lower, mathContext).add(function.apply(upper, mathContext), mathContext);
        for (int i = 1; i < intervals; i++) {
            BigDecimal value = function.apply(lower.add(step.multiply(BigDecimal.valueOf(i), mathContext), mathContext), mathContext);
            sum = sum.add(value.multiply(BigDecimal.valueOf((i & 1) == 0 ? 2 : 4), mathContext), mathContext);
        }
        return sum.multiply(step, mathContext).divide(THREE, mathContext);
    }

    public static BigDecimal rootBisection(Function function, BigDecimal lower, BigDecimal upper,
                                           BigDecimal tolerance, int maxIterations, MathContext mathContext) {
        requireTolerance(tolerance); requireIterations(maxIterations);
        BigDecimal fLower = function.apply(lower, mathContext);
        BigDecimal fUpper = function.apply(upper, mathContext);
        if (fLower.signum() == 0) return lower;
        if (fUpper.signum() == 0) return upper;
        if (fLower.signum() == fUpper.signum()) throw new IllegalArgumentException("Bisection interval does not bracket a root");
        BigDecimal left = lower, right = upper;
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            BigDecimal middle = left.add(right, mathContext).divide(TWO, mathContext);
            BigDecimal fMiddle = function.apply(middle, mathContext);
            if (fMiddle.abs().compareTo(tolerance) <= 0 || right.subtract(left).abs().compareTo(tolerance) <= 0) return middle;
            if (fMiddle.signum() == fLower.signum()) { left = middle; fLower = fMiddle; }
            else right = middle;
        }
        throw new ArithmeticException("Bisection did not converge within " + maxIterations + " iterations");
    }

    public static BigDecimal rootNewton(Function function, Function derivative, BigDecimal initial,
                                        BigDecimal tolerance, int maxIterations, MathContext mathContext) {
        requireTolerance(tolerance); requireIterations(maxIterations);
        BigDecimal value = initial;
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            BigDecimal functionValue = function.apply(value, mathContext);
            if (functionValue.abs().compareTo(tolerance) <= 0) return value;
            BigDecimal derivativeValue = derivative.apply(value, mathContext);
            if (derivativeValue.signum() == 0) throw new ArithmeticException("Newton derivative became zero");
            BigDecimal next = value.subtract(functionValue.divide(derivativeValue, mathContext), mathContext);
            if (next.subtract(value).abs().compareTo(tolerance) <= 0) return next;
            value = next;
        }
        throw new ArithmeticException("Newton method did not converge within " + maxIterations + " iterations");
    }

    /** Classical fourth-order Runge-Kutta integration of y'=f(x,y). */
    public static List<Point> solveRungeKutta4(OdeFunction function, BigDecimal x0, BigDecimal y0,
                                               BigDecimal step, int steps, MathContext mathContext) {
        requireStep(step); requireIntervals(steps);
        List<Point> result = new ArrayList<>(steps + 1);
        BigDecimal x = Objects.requireNonNull(x0, "x0");
        BigDecimal y = Objects.requireNonNull(y0, "y0");
        result.add(new Point(x, y));
        BigDecimal halfStep = step.divide(TWO, mathContext);
        for (int i = 0; i < steps; i++) {
            BigDecimal k1 = function.apply(x, y, mathContext);
            BigDecimal k2 = function.apply(x.add(halfStep, mathContext), y.add(halfStep.multiply(k1, mathContext), mathContext), mathContext);
            BigDecimal k3 = function.apply(x.add(halfStep, mathContext), y.add(halfStep.multiply(k2, mathContext), mathContext), mathContext);
            BigDecimal k4 = function.apply(x.add(step, mathContext), y.add(step.multiply(k3, mathContext), mathContext), mathContext);
            BigDecimal weighted = k1.add(k2.multiply(TWO, mathContext), mathContext)
                    .add(k3.multiply(TWO, mathContext), mathContext).add(k4, mathContext);
            y = y.add(step.multiply(weighted, mathContext).divide(SIX, mathContext), mathContext);
            x = x.add(step, mathContext);
            result.add(new Point(x, y));
        }
        return Collections.unmodifiableList(result);
    }

    private static void requireStep(BigDecimal step) {
        if (Objects.requireNonNull(step, "step").signum() == 0) throw new IllegalArgumentException("Step must be non-zero");
    }
    private static void requireTolerance(BigDecimal tolerance) {
        if (Objects.requireNonNull(tolerance, "tolerance").signum() <= 0) throw new IllegalArgumentException("Tolerance must be positive");
    }
    private static void requireIntervals(int intervals) {
        if (intervals <= 0) throw new IllegalArgumentException("Interval/step count must be positive");
    }
    private static void requireIterations(int iterations) {
        if (iterations <= 0) throw new IllegalArgumentException("Maximum iterations must be positive");
    }

    public static final class Point {
        private final BigDecimal x;
        private final BigDecimal y;
        private Point(BigDecimal x, BigDecimal y) { this.x = x; this.y = y; }
        public BigDecimal getX() { return x; }
        public BigDecimal getY() { return y; }
    }
}
