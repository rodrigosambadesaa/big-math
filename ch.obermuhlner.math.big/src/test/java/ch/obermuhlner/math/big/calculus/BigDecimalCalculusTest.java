package ch.obermuhlner.math.big.calculus;

import org.junit.Test;

import java.math.BigDecimal;
import java.math.MathContext;

import static org.junit.Assert.assertTrue;

public class BigDecimalCalculusTest {
    private static final MathContext MC = MathContext.DECIMAL64;
    private static final BigDecimal EPSILON = new BigDecimal("1E-10");

    @Test public void differentiatesAndIntegrates() {
        BigDecimalCalculus.Function square = (x, mc) -> x.multiply(x, mc);
        BigDecimal derivative = BigDecimalCalculus.derivative(square, new BigDecimal("3"), new BigDecimal("0.001"), MC);
        BigDecimal integral = BigDecimalCalculus.integrateSimpson(square, BigDecimal.ZERO, BigDecimal.ONE, 100, MC);
        assertClose(new BigDecimal("6"), derivative);
        assertClose(BigDecimal.ONE.divide(new BigDecimal("3"), MC), integral);
    }

    @Test public void findsRootsAndSolvesOde() {
        BigDecimalCalculus.Function equation = (x, mc) -> x.multiply(x, mc).subtract(new BigDecimal("2"), mc);
        BigDecimal root = BigDecimalCalculus.rootBisection(equation, BigDecimal.ONE, new BigDecimal("2"), new BigDecimal("1E-14"), 100, MC);
        assertTrue(root.multiply(root, MC).subtract(new BigDecimal("2"), MC).abs().compareTo(EPSILON) < 0);
        java.util.List<BigDecimalCalculus.Point> points = BigDecimalCalculus.solveRungeKutta4((x, y, mc) -> y,
                BigDecimal.ZERO, BigDecimal.ONE, new BigDecimal("0.01"), 100, MC);
        assertTrue(points.get(100).getY().subtract(new BigDecimal("2.718281828459045"), MC).abs().compareTo(new BigDecimal("1E-8")) < 0);
    }

    private static void assertClose(BigDecimal expected, BigDecimal actual) {
        assertTrue(expected.subtract(actual, MC).abs().compareTo(EPSILON) < 0);
    }
}
