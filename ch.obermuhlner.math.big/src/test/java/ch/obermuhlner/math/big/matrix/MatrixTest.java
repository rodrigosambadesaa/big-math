package ch.obermuhlner.math.big.matrix;

import ch.obermuhlner.math.big.BigComplex;
import ch.obermuhlner.math.big.BigRational;
import ch.obermuhlner.math.big.algebra.BigComplexField;
import ch.obermuhlner.math.big.algebra.BigRationalField;
import ch.obermuhlner.math.big.algebra.BinaryField;
import ch.obermuhlner.math.big.algebra.Expression;
import ch.obermuhlner.math.big.algebra.ExpressionField;
import ch.obermuhlner.math.big.algebra.ModularIntegerField;
import ch.obermuhlner.math.big.algebra.Polynomial;
import org.junit.Test;

import java.math.BigInteger;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Arrays;

import static org.junit.Assert.*;

public class MatrixTest {
    private static final BigRationalField Q = BigRationalField.INSTANCE;

    @Test
    public void symbolicDeterminantStaysAlgebraic() {
        Expression x = Expression.variable("x");
        Matrix<Expression> matrix = new Matrix<>(ExpressionField.INSTANCE, new Expression[][] {
                {x, Expression.constant(2)},
                {Expression.constant(1), Expression.constant(2)}
        });

        Expression determinant = matrix.determinant();

        assertEquals("2*x - 2", determinant.toString());
        assertEquals("8", determinant.substitute(java.util.Collections.singletonMap("x", Expression.constant(5))).toString());
        assertEquals("2", determinant.differentiate("x").toString());
    }

    @Test
    public void exactInverseRrefRankAndSolve() {
        Matrix<BigRational> matrix = rationalMatrix(new int[][] {{2, 1}, {5, 3}});
        Matrix<BigRational> inverse = matrix.inverse();

        assertTrue(matrix.multiply(inverse).sameValues(Matrix.identity(Q, 2)));
        assertEquals(BigRational.ONE, matrix.determinant());
        assertEquals(2, matrix.rank());
        assertEquals(0, matrix.nullity());

        Matrix<BigRational> solution = matrix.solve(Matrix.column(Q, Arrays.asList(r(1), r(2))));
        assertEquals(r(1), solution.get(0, 0));
        assertEquals(r(-1), solution.get(1, 0));
    }

    @Test
    public void characteristicPolynomialAndCayleyHamilton() {
        Matrix<BigRational> matrix = rationalMatrix(new int[][] {{1, 2}, {3, 4}});
        Polynomial<BigRational> characteristic = matrix.characteristicPolynomial();

        assertEquals(r(-2), characteristic.coefficient(0));
        assertEquals(r(-5), characteristic.coefficient(1));
        assertEquals(r(1), characteristic.coefficient(2));
        assertTrue(matrix.satisfiesCharacteristicPolynomial());
    }

    @Test
    public void lupDecompositionAndSolve() {
        Matrix<BigRational> matrix = rationalMatrix(new int[][] {{0, 2}, {3, 4}});
        LUDecomposition<BigRational> decomposition = LUDecomposition.decompose(matrix);
        assertTrue(decomposition.verifies(matrix));
        assertEquals(matrix.determinant(), decomposition.determinant());
        Matrix<BigRational> rhs = Matrix.column(Q, Arrays.asList(r(2), r(7)));
        assertTrue(matrix.multiply(decomposition.solve(rhs)).sameValues(rhs));
    }

    @Test
    public void polynomialArithmeticIsExact() {
        Polynomial<BigRational> left = Polynomial.of(Q, r(-1), r(1));
        Polynomial<BigRational> product = left.multiply(Polynomial.of(Q, r(1), r(1)));
        assertEquals(r(-1), product.coefficient(0));
        assertEquals(r(0), product.coefficient(1));
        assertEquals(r(1), product.coefficient(2));
        assertEquals(r(8), product.evaluate(r(3)));
        assertTrue(product.divideAndRemainder(left).getRemainder().isZero());
    }

    @Test
    public void matricesWorkOverFiniteFields() {
        ModularIntegerField field = new ModularIntegerField(5);
        Matrix<BigInteger> matrix = new Matrix<>(field, new BigInteger[][] {
                {BigInteger.valueOf(7), BigInteger.ONE},
                {BigInteger.valueOf(4), BigInteger.valueOf(3)}
        });
        assertTrue(field.areEqual(BigInteger.valueOf(2), matrix.determinant()));
        assertTrue(matrix.multiply(matrix.inverse()).isIdentity());

        Matrix<Boolean> binary = new Matrix<>(BinaryField.INSTANCE, new Boolean[][] {
                {true, true}, {true, false}
        });
        Polynomial<Boolean> characteristic = binary.characteristicPolynomial();
        assertEquals(Boolean.TRUE, characteristic.coefficient(0));
        assertEquals(Boolean.TRUE, characteristic.coefficient(1));
        assertEquals(Boolean.TRUE, characteristic.coefficient(2));
        assertTrue(binary.satisfiesCharacteristicPolynomial());
    }

    @Test
    public void complexMatricesUseConjugateAlgebra() {
        BigComplexField field = new BigComplexField(MathContext.DECIMAL64, new BigDecimal("1E-12"));
        Matrix<BigComplex> matrix = new Matrix<>(field, new BigComplex[][] {
                {BigComplex.ONE, BigComplex.I},
                {BigComplex.I.negate(), BigComplex.ONE}
        });
        assertTrue(matrix.isHermitian());
        assertTrue(field.isZero(matrix.determinant()));
        assertEquals(1, matrix.rank());
    }

    private static Matrix<BigRational> rationalMatrix(int[][] values) {
        BigRational[][] converted = new BigRational[values.length][values[0].length];
        for (int row = 0; row < values.length; row++) for (int column = 0; column < values[row].length; column++) converted[row][column] = r(values[row][column]);
        return new Matrix<>(Q, converted);
    }

    private static BigRational r(int value) { return BigRational.valueOf(BigInteger.valueOf(value)); }
}
