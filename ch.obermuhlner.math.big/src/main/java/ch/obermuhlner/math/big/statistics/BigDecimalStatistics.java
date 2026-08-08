package ch.obermuhlner.math.big.statistics;

import ch.obermuhlner.math.big.BigDecimalMath;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Descriptive and bivariate statistics without binary floating-point conversion. */
public final class BigDecimalStatistics {
    private BigDecimalStatistics() {
    }

    public static BigDecimal sum(List<BigDecimal> values, MathContext mathContext) {
        requireValues(values, 1);
        BigDecimal result = BigDecimal.ZERO;
        for (BigDecimal value : values) result = result.add(require(value), mathContext);
        return result;
    }

    public static BigDecimal mean(List<BigDecimal> values, MathContext mathContext) {
        return sum(values, mathContext).divide(BigDecimal.valueOf(values.size()), mathContext);
    }

    public static BigDecimal geometricMean(List<BigDecimal> values, MathContext mathContext) {
        requireValues(values, 1);
        BigDecimal product = BigDecimal.ONE;
        for (BigDecimal value : values) {
            if (require(value).signum() < 0) throw new IllegalArgumentException("Geometric mean requires non-negative values");
            product = product.multiply(value, mathContext);
        }
        return BigDecimalMath.root(product, BigDecimal.valueOf(values.size()), mathContext);
    }

    public static BigDecimal harmonicMean(List<BigDecimal> values, MathContext mathContext) {
        requireValues(values, 1);
        BigDecimal reciprocalSum = BigDecimal.ZERO;
        for (BigDecimal value : values) reciprocalSum = reciprocalSum.add(BigDecimal.ONE.divide(require(value), mathContext), mathContext);
        return BigDecimal.valueOf(values.size()).divide(reciprocalSum, mathContext);
    }

    public static BigDecimal populationVariance(List<BigDecimal> values, MathContext mathContext) {
        return centralMoment2(values, values.size(), mathContext);
    }

    public static BigDecimal sampleVariance(List<BigDecimal> values, MathContext mathContext) {
        requireValues(values, 2);
        return centralMoment2(values, values.size() - 1, mathContext);
    }

    private static BigDecimal centralMoment2(List<BigDecimal> values, int divisor, MathContext mathContext) {
        BigDecimal mean = mean(values, mathContext);
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal value : values) {
            BigDecimal deviation = value.subtract(mean, mathContext);
            sum = sum.add(deviation.multiply(deviation, mathContext), mathContext);
        }
        return sum.divide(BigDecimal.valueOf(divisor), mathContext);
    }

    public static BigDecimal populationStandardDeviation(List<BigDecimal> values, MathContext mathContext) {
        return BigDecimalMath.sqrt(populationVariance(values, mathContext), mathContext);
    }

    public static BigDecimal sampleStandardDeviation(List<BigDecimal> values, MathContext mathContext) {
        return BigDecimalMath.sqrt(sampleVariance(values, mathContext), mathContext);
    }

    public static BigDecimal covariance(List<BigDecimal> x, List<BigDecimal> y, boolean sample, MathContext mathContext) {
        requirePairs(x, y, sample ? 2 : 1);
        BigDecimal meanX = mean(x, mathContext);
        BigDecimal meanY = mean(y, mathContext);
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < x.size(); i++) {
            sum = sum.add(x.get(i).subtract(meanX, mathContext).multiply(y.get(i).subtract(meanY, mathContext), mathContext), mathContext);
        }
        return sum.divide(BigDecimal.valueOf(x.size() - (sample ? 1 : 0)), mathContext);
    }

    public static BigDecimal correlation(List<BigDecimal> x, List<BigDecimal> y, MathContext mathContext) {
        requirePairs(x, y, 2);
        BigDecimal denominator = sampleStandardDeviation(x, mathContext).multiply(sampleStandardDeviation(y, mathContext), mathContext);
        if (denominator.signum() == 0) throw new ArithmeticException("Correlation is undefined for constant data");
        return covariance(x, y, true, mathContext).divide(denominator, mathContext);
    }

    public static BigDecimal median(List<BigDecimal> values, MathContext mathContext) {
        return percentile(values, new BigDecimal("0.5"), mathContext);
    }

    /** Linear-interpolated percentile, where probability is in [0, 1]. */
    public static BigDecimal percentile(List<BigDecimal> values, BigDecimal probability, MathContext mathContext) {
        requireValues(values, 1);
        require(probability);
        if (probability.compareTo(BigDecimal.ZERO) < 0 || probability.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Probability must be in [0, 1]");
        }
        List<BigDecimal> sorted = new ArrayList<>(values);
        for (BigDecimal value : sorted) require(value);
        Collections.sort(sorted);
        BigDecimal position = probability.multiply(BigDecimal.valueOf(sorted.size() - 1L), mathContext);
        int lower = position.intValue();
        int upper = Math.min(lower + 1, sorted.size() - 1);
        BigDecimal fraction = position.subtract(BigDecimal.valueOf(lower), mathContext);
        return sorted.get(lower).add(sorted.get(upper).subtract(sorted.get(lower), mathContext).multiply(fraction, mathContext), mathContext);
    }

    public static List<BigDecimal> standardize(List<BigDecimal> values, MathContext mathContext) {
        BigDecimal mean = mean(values, mathContext);
        BigDecimal deviation = sampleStandardDeviation(values, mathContext);
        if (deviation.signum() == 0) throw new ArithmeticException("Cannot standardize constant data");
        List<BigDecimal> result = new ArrayList<>(values.size());
        for (BigDecimal value : values) result.add(value.subtract(mean, mathContext).divide(deviation, mathContext));
        return Collections.unmodifiableList(result);
    }

    public static LinearRegression linearRegression(List<BigDecimal> x, List<BigDecimal> y, MathContext mathContext) {
        requirePairs(x, y, 2);
        BigDecimal varianceX = sampleVariance(x, mathContext);
        if (varianceX.signum() == 0) throw new ArithmeticException("Regression requires non-constant x values");
        BigDecimal slope = covariance(x, y, true, mathContext).divide(varianceX, mathContext);
        BigDecimal intercept = mean(y, mathContext).subtract(slope.multiply(mean(x, mathContext), mathContext), mathContext);
        BigDecimal meanY = mean(y, mathContext);
        BigDecimal residualSquares = BigDecimal.ZERO;
        BigDecimal totalSquares = BigDecimal.ZERO;
        for (int i = 0; i < x.size(); i++) {
            BigDecimal predicted = slope.multiply(x.get(i), mathContext).add(intercept, mathContext);
            BigDecimal residual = y.get(i).subtract(predicted, mathContext);
            BigDecimal centered = y.get(i).subtract(meanY, mathContext);
            residualSquares = residualSquares.add(residual.multiply(residual, mathContext), mathContext);
            totalSquares = totalSquares.add(centered.multiply(centered, mathContext), mathContext);
        }
        BigDecimal rSquared = totalSquares.signum() == 0 ? BigDecimal.ONE
                : BigDecimal.ONE.subtract(residualSquares.divide(totalSquares, mathContext), mathContext);
        return new LinearRegression(slope, intercept, rSquared, mathContext);
    }

    private static void requireValues(List<BigDecimal> values, int minimumSize) {
        Objects.requireNonNull(values, "values");
        if (values.size() < minimumSize) throw new IllegalArgumentException("At least " + minimumSize + " values are required");
    }
    private static void requirePairs(List<BigDecimal> x, List<BigDecimal> y, int minimumSize) {
        requireValues(x, minimumSize); requireValues(y, minimumSize);
        if (x.size() != y.size()) throw new IllegalArgumentException("Data series must have equal size");
    }
    private static BigDecimal require(BigDecimal value) { return Objects.requireNonNull(value, "value"); }

    public static final class LinearRegression {
        private final BigDecimal slope;
        private final BigDecimal intercept;
        private final BigDecimal rSquared;
        private final MathContext mathContext;
        private LinearRegression(BigDecimal slope, BigDecimal intercept, BigDecimal rSquared, MathContext mathContext) {
            this.slope = slope; this.intercept = intercept; this.rSquared = rSquared; this.mathContext = mathContext;
        }
        public BigDecimal getSlope() { return slope; }
        public BigDecimal getIntercept() { return intercept; }
        public BigDecimal getRSquared() { return rSquared; }
        public BigDecimal predict(BigDecimal x) { return slope.multiply(x, mathContext).add(intercept, mathContext); }
    }
}
