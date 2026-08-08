package ch.obermuhlner.math.big.statistics;

import org.junit.Test;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class BigDecimalStatisticsTest {
    private static final MathContext MC = MathContext.DECIMAL64;
    private static final List<BigDecimal> VALUES = values("1", "2", "3", "4", "5");

    @Test public void descriptiveStatistics() {
        assertEquals(0, BigDecimalStatistics.mean(VALUES, MC).compareTo(new BigDecimal("3")));
        assertEquals(0, BigDecimalStatistics.populationVariance(VALUES, MC).compareTo(new BigDecimal("2")));
        assertEquals(0, BigDecimalStatistics.median(VALUES, MC).compareTo(new BigDecimal("3")));
        assertEquals(0, BigDecimalStatistics.percentile(VALUES, new BigDecimal("0.25"), MC).compareTo(new BigDecimal("2")));
    }

    @Test public void linearRegression() {
        BigDecimalStatistics.LinearRegression result = BigDecimalStatistics.linearRegression(VALUES, values("3", "5", "7", "9", "11"), MC);
        assertEquals(0, result.getSlope().compareTo(new BigDecimal("2")));
        assertEquals(0, result.getIntercept().compareTo(BigDecimal.ONE));
        assertEquals(0, result.getRSquared().compareTo(BigDecimal.ONE));
        assertEquals(0, result.predict(BigDecimal.TEN).compareTo(new BigDecimal("21")));
    }

    private static List<BigDecimal> values(String... values) {
        BigDecimal[] result = new BigDecimal[values.length];
        for (int i = 0; i < values.length; i++) result[i] = new BigDecimal(values[i]);
        return Arrays.asList(result);
    }
}
