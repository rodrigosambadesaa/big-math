package ch.obermuhlner.math.big.matrix;

import ch.obermuhlner.math.big.BigDecimalMath;
import ch.obermuhlner.math.big.algebra.BigDecimalField;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Numerical matrix algorithms whose ordering and square roots require real values. */
public final class BigDecimalMatrixMath {
    private BigDecimalMatrixMath() {
    }

    public static BigDecimal normOne(Matrix<BigDecimal> matrix, MathContext mathContext) {
        BigDecimal maximum = BigDecimal.ZERO;
        for (int column = 0; column < matrix.columns(); column++) {
            BigDecimal sum = BigDecimal.ZERO;
            for (int row = 0; row < matrix.rows(); row++) sum = sum.add(matrix.get(row, column).abs(), mathContext);
            maximum = maximum.max(sum);
        }
        return maximum;
    }

    public static BigDecimal normInfinity(Matrix<BigDecimal> matrix, MathContext mathContext) {
        BigDecimal maximum = BigDecimal.ZERO;
        for (int row = 0; row < matrix.rows(); row++) {
            BigDecimal sum = BigDecimal.ZERO;
            for (int column = 0; column < matrix.columns(); column++) sum = sum.add(matrix.get(row, column).abs(), mathContext);
            maximum = maximum.max(sum);
        }
        return maximum;
    }

    public static BigDecimal normMaximum(Matrix<BigDecimal> matrix) {
        BigDecimal maximum = BigDecimal.ZERO;
        for (BigDecimal value : matrix.flatten()) maximum = maximum.max(value.abs());
        return maximum;
    }

    public static BigDecimal normFrobenius(Matrix<BigDecimal> matrix, MathContext mathContext) {
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal value : matrix.flatten()) sum = sum.add(value.multiply(value, mathContext), mathContext);
        return BigDecimalMath.sqrt(sum, mathContext);
    }

    public static BigDecimal conditionNumberOne(Matrix<BigDecimal> matrix, MathContext mathContext) {
        return normOne(matrix, mathContext).multiply(normOne(matrix.inverse(), mathContext), mathContext);
    }

    public static BigDecimal conditionNumberInfinity(Matrix<BigDecimal> matrix, MathContext mathContext) {
        return normInfinity(matrix, mathContext).multiply(normInfinity(matrix.inverse(), mathContext), mathContext);
    }

    /** Cholesky decomposition A=L*L^T for a symmetric positive-definite matrix. */
    public static Matrix<BigDecimal> cholesky(Matrix<BigDecimal> matrix, MathContext mathContext) {
        requireSquare(matrix);
        BigDecimalField field = decimalField(matrix);
        if (!matrix.sameValues(matrix.transpose())) throw new IllegalArgumentException("Cholesky decomposition requires a symmetric matrix");
        int n = matrix.rows();
        List<List<BigDecimal>> lower = zeros(n, n);
        for (int row = 0; row < n; row++) {
            for (int column = 0; column <= row; column++) {
                BigDecimal sum = matrix.get(row, column);
                for (int k = 0; k < column; k++) sum = sum.subtract(lower.get(row).get(k).multiply(lower.get(column).get(k), mathContext), mathContext);
                if (row == column) {
                    if (sum.signum() <= 0) throw new ArithmeticException("Matrix is not positive definite");
                    lower.get(row).set(column, BigDecimalMath.sqrt(sum, mathContext));
                } else {
                    lower.get(row).set(column, sum.divide(lower.get(column).get(column), mathContext));
                }
            }
        }
        return new Matrix<>(field, lower);
    }

    /** Reduced QR decomposition using modified Gram-Schmidt. */
    public static QRDecomposition qr(Matrix<BigDecimal> matrix, MathContext mathContext) {
        BigDecimalField field = decimalField(matrix);
        int rows = matrix.rows();
        int columns = matrix.columns();
        if (rows < columns) throw new IllegalArgumentException("Reduced QR requires rows >= columns");
        List<List<BigDecimal>> q = zeros(rows, columns);
        List<List<BigDecimal>> r = zeros(columns, columns);
        for (int column = 0; column < columns; column++) {
            BigDecimal[] vector = new BigDecimal[rows];
            for (int row = 0; row < rows; row++) vector[row] = matrix.get(row, column);
            for (int previous = 0; previous < column; previous++) {
                BigDecimal projection = BigDecimal.ZERO;
                for (int row = 0; row < rows; row++) projection = projection.add(q.get(row).get(previous).multiply(vector[row], mathContext), mathContext);
                r.get(previous).set(column, projection);
                for (int row = 0; row < rows; row++) vector[row] = vector[row].subtract(projection.multiply(q.get(row).get(previous), mathContext), mathContext);
            }
            BigDecimal normSquare = BigDecimal.ZERO;
            for (BigDecimal value : vector) normSquare = normSquare.add(value.multiply(value, mathContext), mathContext);
            if (field.isZero(normSquare)) throw new ArithmeticException("Matrix columns are linearly dependent");
            BigDecimal norm = BigDecimalMath.sqrt(normSquare, mathContext);
            r.get(column).set(column, norm);
            for (int row = 0; row < rows; row++) q.get(row).set(column, vector[row].divide(norm, mathContext));
        }
        return new QRDecomposition(new Matrix<>(field, q), new Matrix<>(field, r));
    }

    /** Dominant real eigenpair by power iteration and a Rayleigh quotient. */
    public static Eigenpair dominantEigenpair(Matrix<BigDecimal> matrix, BigDecimal tolerance,
                                              int maxIterations, MathContext mathContext) {
        requireSquare(matrix);
        if (tolerance.signum() <= 0 || maxIterations <= 0) throw new IllegalArgumentException("Positive tolerance and iteration count required");
        BigDecimalField field = decimalField(matrix);
        List<BigDecimal> initial = new ArrayList<>(matrix.rows());
        for (int i = 0; i < matrix.rows(); i++) initial.add(BigDecimal.ONE);
        Matrix<BigDecimal> vector = Matrix.column(field, initial);
        vector = vector.divide(normFrobenius(vector, mathContext));
        for (int iteration = 1; iteration <= maxIterations; iteration++) {
            Matrix<BigDecimal> product = matrix.multiply(vector);
            BigDecimal norm = normFrobenius(product, mathContext);
            if (field.isZero(norm)) return new Eigenpair(BigDecimal.ZERO, vector, iteration);
            Matrix<BigDecimal> next = product.divide(norm);
            BigDecimal nextEigenvalue = next.transpose().multiply(matrix).multiply(next).get(0, 0);
            BigDecimal residual = normFrobenius(matrix.multiply(next).subtract(next.scale(nextEigenvalue)), mathContext);
            if (residual.compareTo(tolerance) <= 0) {
                return new Eigenpair(nextEigenvalue, next, iteration);
            }
            vector = next;
        }
        throw new ArithmeticException("Power iteration did not converge within " + maxIterations + " iterations");
    }

    private static BigDecimalField decimalField(Matrix<BigDecimal> matrix) {
        if (!(matrix.getField() instanceof BigDecimalField)) throw new IllegalArgumentException("Matrix must use BigDecimalField");
        return (BigDecimalField) matrix.getField();
    }
    private static void requireSquare(Matrix<BigDecimal> matrix) {
        Objects.requireNonNull(matrix, "matrix");
        if (!matrix.isSquare()) throw new IllegalArgumentException("Square matrix required");
    }
    private static List<List<BigDecimal>> zeros(int rows, int columns) {
        List<List<BigDecimal>> result = new ArrayList<>(rows);
        for (int row = 0; row < rows; row++) {
            List<BigDecimal> values = new ArrayList<>(columns);
            for (int column = 0; column < columns; column++) values.add(BigDecimal.ZERO);
            result.add(values);
        }
        return result;
    }

    public static final class QRDecomposition {
        private final Matrix<BigDecimal> q;
        private final Matrix<BigDecimal> r;
        private QRDecomposition(Matrix<BigDecimal> q, Matrix<BigDecimal> r) { this.q = q; this.r = r; }
        public Matrix<BigDecimal> getQ() { return q; }
        public Matrix<BigDecimal> getR() { return r; }
        public boolean verifies(Matrix<BigDecimal> original) { return q.multiply(r).sameValues(original); }
    }

    public static final class Eigenpair {
        private final BigDecimal eigenvalue;
        private final Matrix<BigDecimal> eigenvector;
        private final int iterations;
        private Eigenpair(BigDecimal eigenvalue, Matrix<BigDecimal> eigenvector, int iterations) {
            this.eigenvalue = eigenvalue; this.eigenvector = eigenvector; this.iterations = iterations;
        }
        public BigDecimal getEigenvalue() { return eigenvalue; }
        public Matrix<BigDecimal> getEigenvector() { return eigenvector; }
        public int getIterations() { return iterations; }
        public BigDecimal residual(Matrix<BigDecimal> matrix, MathContext mathContext) {
            return normFrobenius(matrix.multiply(eigenvector).subtract(eigenvector.scale(eigenvalue)), mathContext);
        }
    }
}
