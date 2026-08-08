package ch.obermuhlner.math.big;

import org.junit.Test;

import java.math.BigInteger;
import java.util.Arrays;

import static org.junit.Assert.*;

public class BigIntegerMathTest {
    @Test public void combinatorics() {
        assertEquals(new BigInteger("2432902008176640000"), BigIntegerMath.factorial(20));
        assertEquals(BigInteger.valueOf(120), BigIntegerMath.binomial(10, 3));
        assertEquals(BigInteger.valueOf(42), BigIntegerMath.catalan(5));
        assertEquals(BigInteger.valueOf(52), BigIntegerMath.bell(5));
        assertEquals(BigInteger.valueOf(42), BigIntegerMath.partition(10));
        assertEquals(new BigInteger("354224848179261915075"), BigIntegerMath.fibonacci(100));
    }

    @Test public void numberTheory() {
        BigIntegerMath.ExtendedGcd gcd = BigIntegerMath.extendedGcd(BigInteger.valueOf(240), BigInteger.valueOf(46));
        assertEquals(BigInteger.valueOf(2), gcd.getGcd());
        assertEquals(gcd.getGcd(), BigInteger.valueOf(240).multiply(gcd.getX()).add(BigInteger.valueOf(46).multiply(gcd.getY())));
        assertEquals(BigInteger.valueOf(23), BigIntegerMath.modularInverse(BigInteger.valueOf(7), BigInteger.valueOf(40)));
        assertEquals(BigInteger.valueOf(23), BigIntegerMath.chineseRemainder(
                Arrays.asList(BigInteger.valueOf(2), BigInteger.valueOf(3), BigInteger.valueOf(2)),
                Arrays.asList(BigInteger.valueOf(3), BigInteger.valueOf(5), BigInteger.valueOf(7))));
        assertEquals(Integer.valueOf(3), BigIntegerMath.primeFactorization(BigInteger.valueOf(360)).get(BigInteger.valueOf(2)));
        assertEquals(BigInteger.valueOf(96), BigIntegerMath.eulerPhi(BigInteger.valueOf(360)));
        assertTrue(BigIntegerMath.isPerfectSquare(new BigInteger("15241578750190521")));
    }
}
