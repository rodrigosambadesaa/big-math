package ch.obermuhlner.math.big;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/** Exact combinatorics and elementary number theory for {@link BigInteger}. */
public final class BigIntegerMath {
    private static final BigInteger TWO = BigInteger.valueOf(2);

    private BigIntegerMath() {
    }

    public static BigInteger factorial(int n) {
        requireNonNegative(n, "n");
        BigInteger result = BigInteger.ONE;
        for (int i = 2; i <= n; i++) result = result.multiply(BigInteger.valueOf(i));
        return result;
    }

    public static BigInteger permutations(int n, int k) {
        requireRange(n, k);
        BigInteger result = BigInteger.ONE;
        for (int i = 0; i < k; i++) result = result.multiply(BigInteger.valueOf(n - i));
        return result;
    }

    public static BigInteger binomial(int n, int k) {
        requireRange(n, k);
        int effective = Math.min(k, n - k);
        BigInteger result = BigInteger.ONE;
        for (int i = 1; i <= effective; i++) {
            result = result.multiply(BigInteger.valueOf(n - effective + i)).divide(BigInteger.valueOf(i));
        }
        return result;
    }

    public static BigInteger multinomial(int... counts) {
        if (counts == null || counts.length == 0) throw new IllegalArgumentException("At least one count is required");
        int total = 0;
        BigInteger denominator = BigInteger.ONE;
        for (int count : counts) {
            requireNonNegative(count, "count");
            total = Math.addExact(total, count);
            denominator = denominator.multiply(factorial(count));
        }
        return factorial(total).divide(denominator);
    }

    public static BigInteger catalan(int n) {
        requireNonNegative(n, "n");
        return binomial(2 * n, n).divide(BigInteger.valueOf(n + 1L));
    }

    public static BigInteger stirlingSecondKind(int n, int k) {
        requireRange(n, k);
        BigInteger[] values = new BigInteger[k + 1];
        Arrays.fill(values, BigInteger.ZERO);
        values[0] = BigInteger.ONE;
        for (int i = 1; i <= n; i++) {
            for (int j = Math.min(i, k); j >= 1; j--) {
                values[j] = values[j].multiply(BigInteger.valueOf(j)).add(values[j - 1]);
            }
            values[0] = BigInteger.ZERO;
        }
        return values[k];
    }

    public static BigInteger bell(int n) {
        requireNonNegative(n, "n");
        BigInteger result = BigInteger.ZERO;
        for (int k = 0; k <= n; k++) result = result.add(stirlingSecondKind(n, k));
        return result;
    }

    public static BigInteger partition(int n) {
        requireNonNegative(n, "n");
        BigInteger[] partitions = new BigInteger[n + 1];
        Arrays.fill(partitions, BigInteger.ZERO);
        partitions[0] = BigInteger.ONE;
        for (int part = 1; part <= n; part++) {
            for (int sum = part; sum <= n; sum++) partitions[sum] = partitions[sum].add(partitions[sum - part]);
        }
        return partitions[n];
    }

    public static BigInteger fibonacci(int n) {
        requireNonNegative(n, "n");
        return fibonacciPair(n)[0];
    }

    public static BigInteger lucas(int n) {
        requireNonNegative(n, "n");
        if (n == 0) return TWO;
        BigInteger[] pair = fibonacciPair(n);
        return pair[1].shiftLeft(1).subtract(pair[0]);
    }

    private static BigInteger[] fibonacciPair(int n) {
        if (n == 0) return new BigInteger[] {BigInteger.ZERO, BigInteger.ONE};
        BigInteger[] pair = fibonacciPair(n >>> 1);
        BigInteger a = pair[0];
        BigInteger b = pair[1];
        BigInteger c = a.multiply(b.shiftLeft(1).subtract(a));
        BigInteger d = a.multiply(a).add(b.multiply(b));
        return (n & 1) == 0 ? new BigInteger[] {c, d} : new BigInteger[] {d, c.add(d)};
    }

    public static BigInteger gcd(BigInteger... values) {
        if (values == null || values.length == 0) throw new IllegalArgumentException("At least one value is required");
        BigInteger result = BigInteger.ZERO;
        for (BigInteger value : values) result = result.gcd(require(value));
        return result;
    }

    public static BigInteger lcm(BigInteger... values) {
        if (values == null || values.length == 0) throw new IllegalArgumentException("At least one value is required");
        BigInteger result = BigInteger.ONE;
        for (BigInteger value : values) {
            value = require(value);
            if (value.signum() == 0) return BigInteger.ZERO;
            result = result.divide(result.gcd(value)).multiply(value).abs();
        }
        return result;
    }

    public static ExtendedGcd extendedGcd(BigInteger a, BigInteger b) {
        a = require(a); b = require(b);
        BigInteger oldR = a.abs(), r = b.abs();
        BigInteger oldS = BigInteger.ONE, s = BigInteger.ZERO;
        BigInteger oldT = BigInteger.ZERO, t = BigInteger.ONE;
        while (r.signum() != 0) {
            BigInteger q = oldR.divide(r);
            BigInteger next = oldR.subtract(q.multiply(r)); oldR = r; r = next;
            next = oldS.subtract(q.multiply(s)); oldS = s; s = next;
            next = oldT.subtract(q.multiply(t)); oldT = t; t = next;
        }
        if (a.signum() < 0) oldS = oldS.negate();
        if (b.signum() < 0) oldT = oldT.negate();
        return new ExtendedGcd(oldR, oldS, oldT);
    }

    public static BigInteger modularInverse(BigInteger value, BigInteger modulus) {
        modulus = require(modulus);
        if (modulus.signum() <= 0) throw new IllegalArgumentException("Modulus must be positive");
        ExtendedGcd result = extendedGcd(require(value), modulus);
        if (!result.gcd.equals(BigInteger.ONE)) throw new ArithmeticException("Value and modulus are not coprime");
        return result.x.mod(modulus);
    }

    /** Chinese remainder theorem for pairwise coprime positive moduli. */
    public static BigInteger chineseRemainder(List<BigInteger> remainders, List<BigInteger> moduli) {
        if (remainders == null || moduli == null || remainders.isEmpty() || remainders.size() != moduli.size()) {
            throw new IllegalArgumentException("Remainders and moduli must have the same positive size");
        }
        BigInteger product = BigInteger.ONE;
        for (BigInteger modulus : moduli) {
            if (require(modulus).signum() <= 0) throw new IllegalArgumentException("Moduli must be positive");
            product = product.multiply(modulus);
        }
        BigInteger result = BigInteger.ZERO;
        for (int i = 0; i < moduli.size(); i++) {
            BigInteger modulus = moduli.get(i);
            BigInteger partial = product.divide(modulus);
            result = result.add(remainders.get(i).mod(modulus).multiply(partial).multiply(modularInverse(partial, modulus)));
        }
        return result.mod(product);
    }

    public static List<Integer> primesUpTo(int limit) {
        if (limit < 2) return Collections.emptyList();
        boolean[] composite = new boolean[limit + 1];
        for (int p = 2; (long) p * p <= limit; p++) if (!composite[p])
            for (int multiple = p * p; multiple <= limit; multiple += p) composite[multiple] = true;
        List<Integer> result = new ArrayList<>();
        for (int value = 2; value <= limit; value++) if (!composite[value]) result.add(value);
        return Collections.unmodifiableList(result);
    }

    /** Trial-division factorization, intended for exact moderate-sized integers. */
    public static SortedMap<BigInteger, Integer> primeFactorization(BigInteger value) {
        value = require(value);
        if (value.signum() == 0) throw new IllegalArgumentException("Zero has no prime factorization");
        SortedMap<BigInteger, Integer> result = new TreeMap<>();
        if (value.signum() < 0) { result.put(BigInteger.valueOf(-1), 1); value = value.negate(); }
        int twos = 0;
        while (!value.testBit(0)) { value = value.shiftRight(1); twos++; }
        if (twos > 0) result.put(TWO, twos);
        BigInteger divisor = BigInteger.valueOf(3);
        while (divisor.multiply(divisor).compareTo(value) <= 0) {
            int exponent = 0;
            while (value.mod(divisor).signum() == 0) { value = value.divide(divisor); exponent++; }
            if (exponent > 0) result.put(divisor, exponent);
            divisor = divisor.add(TWO);
        }
        if (value.compareTo(BigInteger.ONE) > 0) result.put(value, 1);
        return Collections.unmodifiableSortedMap(result);
    }

    public static BigInteger eulerPhi(BigInteger value) {
        value = require(value);
        if (value.signum() <= 0) throw new IllegalArgumentException("Value must be positive");
        BigInteger result = value;
        for (BigInteger prime : primeFactorization(value).keySet()) {
            if (prime.signum() > 0) result = result.divide(prime).multiply(prime.subtract(BigInteger.ONE));
        }
        return result;
    }

    public static int mobius(BigInteger value) {
        value = require(value);
        if (value.signum() <= 0) throw new IllegalArgumentException("Value must be positive");
        int factors = 0;
        for (Map.Entry<BigInteger, Integer> entry : primeFactorization(value).entrySet()) {
            if (entry.getValue() > 1) return 0;
            factors++;
        }
        return (factors & 1) == 0 ? 1 : -1;
    }

    public static BigInteger integerSqrt(BigInteger value) {
        value = require(value);
        if (value.signum() < 0) throw new ArithmeticException("Square root of negative integer");
        if (value.compareTo(BigInteger.ONE) <= 0) return value;
        BigInteger estimate = BigInteger.ONE.shiftLeft((value.bitLength() + 1) / 2);
        while (true) {
            BigInteger next = estimate.add(value.divide(estimate)).shiftRight(1);
            if (next.compareTo(estimate) >= 0) return estimate;
            estimate = next;
        }
    }

    public static boolean isPerfectSquare(BigInteger value) {
        if (require(value).signum() < 0) return false;
        BigInteger root = integerSqrt(value);
        return root.multiply(root).equals(value);
    }

    private static BigInteger require(BigInteger value) {
        if (value == null) throw new NullPointerException("value");
        return value;
    }
    private static void requireNonNegative(int value, String name) {
        if (value < 0) throw new IllegalArgumentException(name + " must be non-negative");
    }
    private static void requireRange(int n, int k) {
        requireNonNegative(n, "n");
        if (k < 0 || k > n) throw new IllegalArgumentException("k must be between 0 and n");
    }

    public static final class ExtendedGcd {
        private final BigInteger gcd;
        private final BigInteger x;
        private final BigInteger y;
        private ExtendedGcd(BigInteger gcd, BigInteger x, BigInteger y) { this.gcd = gcd; this.x = x; this.y = y; }
        public BigInteger getGcd() { return gcd; }
        public BigInteger getX() { return x; }
        public BigInteger getY() { return y; }
    }
}
