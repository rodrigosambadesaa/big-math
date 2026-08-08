package ch.obermuhlner.math.big.matrix;

import ch.obermuhlner.math.big.algebra.BigDecimalField;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.MathContext;

import static org.junit.Assert.assertTrue;

public class BigDecimalMatrixMathTest {
    private static final MathContext MC = MathContext.DECIMAL64;
    private static final BigDecimalField FIELD = new BigDecimalField(MC, new BigDecimal("1E-12"));

    @Test public void choleskyAndQrReconstructInputs() {
        Matrix<BigDecimal> positive = matrix(new String[][] {{"4", "2"}, {"2", "3"}});
        Matrix<BigDecimal> lower = BigDecimalMatrixMath.cholesky(positive, MC);
        assertTrue(lower.multiply(lower.transpose()).sameValues(positive));

        Matrix<BigDecimal> rectangular = matrix(new String[][] {{"1", "1"}, {"1", "0"}, {"0", "1"}});
        BigDecimalMatrixMath.QRDecomposition qr = BigDecimalMatrixMath.qr(rectangular, MC);
        assertTrue(qr.verifies(rectangular));
        assertTrue(qr.getQ().transpose().multiply(qr.getQ()).sameValues(Matrix.identity(FIELD, 2)));
    }

    @Test public void normsAndDominantEigenpair() {
        Matrix<BigDecimal> matrix = matrix(new String[][] {{"2", "0"}, {"0", "1"}});
        assertTrue(BigDecimalMatrixMath.normOne(matrix, MC).compareTo(new BigDecimal("2")) == 0);
        BigDecimalMatrixMath.Eigenpair pair = BigDecimalMatrixMath.dominantEigenpair(matrix, new BigDecimal("1E-12"), 100, MC);
        assertTrue(pair.getEigenvalue().subtract(new BigDecimal("2"), MC).abs().compareTo(new BigDecimal("1E-10")) < 0);
        assertTrue(pair.residual(matrix, MC).compareTo(new BigDecimal("1E-10")) < 0);
    }

    private static Matrix<BigDecimal> matrix(String[][] values) {
        BigDecimal[][] result = new BigDecimal[values.length][values[0].length];
        for (int row = 0; row < values.length; row++) for (int column = 0; column < values[row].length; column++) result[row][column] = new BigDecimal(values[row][column]);
        return new Matrix<>(FIELD, result);
    }
}
