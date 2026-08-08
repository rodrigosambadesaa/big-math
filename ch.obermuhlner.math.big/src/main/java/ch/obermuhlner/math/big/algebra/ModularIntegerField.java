package ch.obermuhlner.math.big.algebra;

import java.math.BigInteger;
import java.util.Objects;

/** Prime finite field GF(p), represented by canonical {@link BigInteger} residues. */
public final class ModularIntegerField implements Field<BigInteger> {
    private final BigInteger modulus;

    public ModularIntegerField(long primeModulus) {
        this(BigInteger.valueOf(primeModulus));
    }

    public ModularIntegerField(BigInteger primeModulus) {
        this.modulus = Objects.requireNonNull(primeModulus, "primeModulus");
        if (modulus.compareTo(BigInteger.valueOf(2)) < 0 || !modulus.isProbablePrime(100)) {
            throw new IllegalArgumentException("Modulus must be prime");
        }
    }

    public BigInteger getModulus() { return modulus; }
    public BigInteger normalize(BigInteger value) { return Objects.requireNonNull(value, "value").mod(modulus); }

    @Override public BigInteger zero() { return BigInteger.ZERO; }
    @Override public BigInteger one() { return BigInteger.ONE; }
    @Override public BigInteger add(BigInteger a, BigInteger b) { return a.add(b).mod(modulus); }
    @Override public BigInteger subtract(BigInteger a, BigInteger b) { return a.subtract(b).mod(modulus); }
    @Override public BigInteger multiply(BigInteger a, BigInteger b) { return a.multiply(b).mod(modulus); }
    @Override public BigInteger divide(BigInteger a, BigInteger b) { return multiply(a, normalize(b).modInverse(modulus)); }
    @Override public BigInteger negate(BigInteger a) { return a.negate().mod(modulus); }
    @Override public boolean isZero(BigInteger a) { return a.mod(modulus).signum() == 0; }
    @Override public BigInteger fromLong(long value) { return BigInteger.valueOf(value).mod(modulus); }
    @Override public boolean areEqual(BigInteger a, BigInteger b) { return a.subtract(b).mod(modulus).signum() == 0; }

    @Override public boolean equals(Object object) {
        return this == object || object instanceof ModularIntegerField && modulus.equals(((ModularIntegerField) object).modulus);
    }
    @Override public int hashCode() { return modulus.hashCode(); }
    @Override public String toString() { return "GF(" + modulus + ")"; }
}
