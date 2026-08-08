package ch.obermuhlner.math.big.matrix;

import ch.obermuhlner.math.big.algebra.Field;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Generic LUP decomposition {@code P*A = L*U} using the first known non-zero pivot. */
public final class LUDecomposition<T> {
    private final Field<T> field;
    private final Matrix<T> lower;
    private final Matrix<T> upper;
    private final Matrix<T> permutation;
    private final int swaps;

    private LUDecomposition(Field<T> field, Matrix<T> lower, Matrix<T> upper, Matrix<T> permutation, int swaps) {
        this.field = field;
        this.lower = lower;
        this.upper = upper;
        this.permutation = permutation;
        this.swaps = swaps;
    }

    public static <T> LUDecomposition<T> decompose(Matrix<T> matrix) {
        Objects.requireNonNull(matrix, "matrix");
        if (!matrix.isSquare()) throw new IllegalArgumentException("LU decomposition requires a square matrix");
        Field<T> field = matrix.getField();
        int n = matrix.rows();
        List<List<T>> upper = mutableCopy(matrix);
        List<List<T>> lower = mutableZeros(field, n, n);
        List<List<T>> permutation = mutableCopy(Matrix.identity(field, n));
        int swaps = 0;
        for (int column = 0; column < n; column++) {
            int pivot = column;
            while (pivot < n && field.isZero(upper.get(pivot).get(column))) pivot++;
            if (pivot == n) throw new ArithmeticException("Singular matrix");
            if (pivot != column) {
                java.util.Collections.swap(upper, pivot, column);
                java.util.Collections.swap(permutation, pivot, column);
                for (int previous = 0; previous < column; previous++) {
                    T temporary = lower.get(pivot).get(previous);
                    lower.get(pivot).set(previous, lower.get(column).get(previous));
                    lower.get(column).set(previous, temporary);
                }
                swaps++;
            }
            lower.get(column).set(column, field.one());
            for (int row = column + 1; row < n; row++) {
                T factor = field.divide(upper.get(row).get(column), upper.get(column).get(column));
                lower.get(row).set(column, factor);
                for (int j = column; j < n; j++) {
                    upper.get(row).set(j, field.subtract(upper.get(row).get(j), field.multiply(factor, upper.get(column).get(j))));
                }
            }
        }
        return new LUDecomposition<>(field, new Matrix<>(field, lower), new Matrix<>(field, upper), new Matrix<>(field, permutation), swaps);
    }

    public Matrix<T> getLower() { return lower; }
    public Matrix<T> getUpper() { return upper; }
    public Matrix<T> getPermutation() { return permutation; }
    public int getSwapCount() { return swaps; }

    public T determinant() {
        T result = (swaps & 1) == 0 ? field.one() : field.fromLong(-1);
        for (int i = 0; i < upper.rows(); i++) result = field.multiply(result, upper.get(i, i));
        return result;
    }

    public Matrix<T> solve(Matrix<T> rightHandSide) {
        if (rightHandSide.rows() != lower.rows()) throw new IllegalArgumentException("Right-hand side has incompatible dimensions");
        Matrix<T> permuted = permutation.multiply(rightHandSide);
        int n = lower.rows();
        int columns = rightHandSide.columns();
        List<List<T>> y = mutableZeros(field, n, columns);
        for (int row = 0; row < n; row++) for (int column = 0; column < columns; column++) {
            T value = permuted.get(row, column);
            for (int k = 0; k < row; k++) value = field.subtract(value, field.multiply(lower.get(row, k), y.get(k).get(column)));
            y.get(row).set(column, value);
        }
        List<List<T>> x = mutableZeros(field, n, columns);
        for (int row = n - 1; row >= 0; row--) for (int column = 0; column < columns; column++) {
            T value = y.get(row).get(column);
            for (int k = row + 1; k < n; k++) value = field.subtract(value, field.multiply(upper.get(row, k), x.get(k).get(column)));
            x.get(row).set(column, field.divide(value, upper.get(row, row)));
        }
        return new Matrix<>(field, x);
    }

    public boolean verifies(Matrix<T> original) {
        return permutation.multiply(original).sameValues(lower.multiply(upper));
    }

    private static <T> List<List<T>> mutableCopy(Matrix<T> matrix) {
        List<List<T>> result = new ArrayList<>(matrix.rows());
        for (int row = 0; row < matrix.rows(); row++) result.add(new ArrayList<>(matrix.row(row)));
        return result;
    }

    private static <T> List<List<T>> mutableZeros(Field<T> field, int rows, int columns) {
        List<List<T>> result = new ArrayList<>(rows);
        for (int row = 0; row < rows; row++) {
            List<T> values = new ArrayList<>(columns);
            for (int column = 0; column < columns; column++) values.add(field.zero());
            result.add(values);
        }
        return result;
    }
}
