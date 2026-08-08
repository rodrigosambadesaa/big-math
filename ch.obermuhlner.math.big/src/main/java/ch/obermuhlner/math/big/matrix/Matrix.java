package ch.obermuhlner.math.big.matrix;

import ch.obermuhlner.math.big.algebra.Field;
import ch.obermuhlner.math.big.algebra.Polynomial;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Immutable dense matrix over an arbitrary algebraic field.
 *
 * <p>No operation converts values to {@code double}.  Consequently the same
 * algorithms work with exact rationals, high precision decimals and complex
 * numbers, as well as symbolic expressions.</p>
 */
public final class Matrix<T> {
    private final Field<T> field;
    private final int rowCount;
    private final int columnCount;
    private final Object[][] values;

    public Matrix(Field<T> field, T[][] values) {
        this(field, copyArray(values), true);
    }

    public Matrix(Field<T> field, List<? extends List<? extends T>> rows) {
        this.field = Objects.requireNonNull(field, "field");
        Objects.requireNonNull(rows, "rows");
        if (rows.isEmpty()) throw new IllegalArgumentException("A matrix must have at least one row");
        this.rowCount = rows.size();
        this.columnCount = Objects.requireNonNull(rows.get(0), "row").size();
        if (columnCount == 0) throw new IllegalArgumentException("A matrix must have at least one column");
        this.values = new Object[rowCount][columnCount];
        for (int row = 0; row < rowCount; row++) {
            List<? extends T> source = Objects.requireNonNull(rows.get(row), "row");
            if (source.size() != columnCount) throw new IllegalArgumentException("Matrix rows must have equal length");
            for (int column = 0; column < columnCount; column++) {
                this.values[row][column] = Objects.requireNonNull(source.get(column), "element");
            }
        }
    }

    private Matrix(Field<T> field, Object[][] trustedValues, boolean trusted) {
        this.field = Objects.requireNonNull(field, "field");
        if (trustedValues.length == 0 || trustedValues[0].length == 0) {
            throw new IllegalArgumentException("A matrix must have positive dimensions");
        }
        this.rowCount = trustedValues.length;
        this.columnCount = trustedValues[0].length;
        this.values = trustedValues;
    }

    private static <T> Object[][] copyArray(T[][] source) {
        Objects.requireNonNull(source, "values");
        if (source.length == 0) throw new IllegalArgumentException("A matrix must have at least one row");
        Objects.requireNonNull(source[0], "row");
        if (source[0].length == 0) throw new IllegalArgumentException("A matrix must have at least one column");
        Object[][] result = new Object[source.length][source[0].length];
        for (int row = 0; row < source.length; row++) {
            Objects.requireNonNull(source[row], "row");
            if (source[row].length != source[0].length) throw new IllegalArgumentException("Matrix rows must have equal length");
            for (int column = 0; column < source[row].length; column++) {
                result[row][column] = Objects.requireNonNull(source[row][column], "element");
            }
        }
        return result;
    }

    public static <T> Matrix<T> filled(Field<T> field, int rows, int columns, T value) {
        checkDimensions(rows, columns);
        Object[][] result = new Object[rows][columns];
        for (int row = 0; row < rows; row++) Arrays.fill(result[row], Objects.requireNonNull(value, "value"));
        return new Matrix<>(field, result, true);
    }

    public static <T> Matrix<T> zeros(Field<T> field, int rows, int columns) {
        return filled(field, rows, columns, field.zero());
    }

    public static <T> Matrix<T> identity(Field<T> field, int size) {
        checkDimensions(size, size);
        Object[][] result = zeroArray(field, size, size);
        for (int i = 0; i < size; i++) result[i][i] = field.one();
        return new Matrix<>(field, result, true);
    }

    public static <T> Matrix<T> identity(Field<T> field, int rows, int columns) {
        checkDimensions(rows, columns);
        Object[][] result = zeroArray(field, rows, columns);
        for (int i = 0; i < Math.min(rows, columns); i++) result[i][i] = field.one();
        return new Matrix<>(field, result, true);
    }

    public static <T> Matrix<T> diagonal(Field<T> field, List<T> diagonal) {
        Objects.requireNonNull(diagonal, "diagonal");
        if (diagonal.isEmpty()) throw new IllegalArgumentException("Diagonal must not be empty");
        Object[][] result = zeroArray(field, diagonal.size(), diagonal.size());
        for (int i = 0; i < diagonal.size(); i++) result[i][i] = Objects.requireNonNull(diagonal.get(i));
        return new Matrix<>(field, result, true);
    }

    public static <T> Matrix<T> row(Field<T> field, List<T> values) {
        return new Matrix<>(field, Collections.singletonList(values));
    }

    public static <T> Matrix<T> column(Field<T> field, List<T> values) {
        Objects.requireNonNull(values, "values");
        List<List<T>> rows = new ArrayList<>(values.size());
        for (T value : values) rows.add(Collections.singletonList(value));
        return new Matrix<>(field, rows);
    }

    public static <T> Matrix<T> vandermonde(Field<T> field, List<T> points, int columns) {
        Objects.requireNonNull(points, "points");
        checkDimensions(points.size(), columns);
        Object[][] result = new Object[points.size()][columns];
        for (int row = 0; row < points.size(); row++) {
            T power = field.one();
            for (int column = 0; column < columns; column++) {
                result[row][column] = power;
                power = field.multiply(power, points.get(row));
            }
        }
        return new Matrix<>(field, result, true);
    }

    public static <T> Matrix<T> circulant(Field<T> field, List<T> firstRow) {
        Objects.requireNonNull(firstRow, "firstRow");
        int size = firstRow.size();
        checkDimensions(size, size);
        Object[][] result = new Object[size][size];
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) result[row][column] = firstRow.get((column - row + size) % size);
        }
        return new Matrix<>(field, result, true);
    }

    public static <T> Matrix<T> permutation(Field<T> field, int... permutation) {
        Objects.requireNonNull(permutation, "permutation");
        checkDimensions(permutation.length, permutation.length);
        Object[][] result = zeroArray(field, permutation.length, permutation.length);
        boolean[] seen = new boolean[permutation.length];
        for (int row = 0; row < permutation.length; row++) {
            int column = permutation[row];
            if (column < 0 || column >= permutation.length || seen[column]) throw new IllegalArgumentException("Not a permutation");
            seen[column] = true;
            result[row][column] = field.one();
        }
        return new Matrix<>(field, result, true);
    }

    public static <T> Matrix<T> companion(Polynomial<T> polynomial) {
        Objects.requireNonNull(polynomial, "polynomial");
        if (polynomial.degree() < 1) throw new IllegalArgumentException("Companion matrix requires degree at least one");
        Field<T> field = polynomial.getField();
        int size = polynomial.degree();
        Object[][] result = zeroArray(field, size, size);
        for (int row = 1; row < size; row++) result[row][row - 1] = field.one();
        T leading = polynomial.leadingCoefficient();
        for (int row = 0; row < size; row++) {
            result[row][size - 1] = field.negate(field.divide(polynomial.coefficient(row), leading));
        }
        return new Matrix<>(field, result, true);
    }

    public Field<T> getField() { return field; }
    public int getRowCount() { return rowCount; }
    public int getColumnCount() { return columnCount; }
    public int rows() { return rowCount; }
    public int columns() { return columnCount; }

    @SuppressWarnings("unchecked")
    public T get(int row, int column) {
        checkIndex(row, column);
        return (T) values[row][column];
    }

    public List<T> row(int row) {
        if (row < 0 || row >= rowCount) throw new IndexOutOfBoundsException("Row: " + row);
        List<T> result = new ArrayList<>(columnCount);
        for (int column = 0; column < columnCount; column++) result.add(get(row, column));
        return Collections.unmodifiableList(result);
    }

    public List<T> column(int column) {
        if (column < 0 || column >= columnCount) throw new IndexOutOfBoundsException("Column: " + column);
        List<T> result = new ArrayList<>(rowCount);
        for (int row = 0; row < rowCount; row++) result.add(get(row, column));
        return Collections.unmodifiableList(result);
    }

    public List<List<T>> toList() {
        List<List<T>> result = new ArrayList<>(rowCount);
        for (int row = 0; row < rowCount; row++) result.add(row(row));
        return Collections.unmodifiableList(result);
    }

    public Matrix<T> with(int row, int column, T value) {
        checkIndex(row, column);
        Object[][] result = copyValues();
        result[row][column] = Objects.requireNonNull(value, "value");
        return new Matrix<>(field, result, true);
    }

    public Matrix<T> add(Matrix<T> other) { return zip(other, field::add); }
    public Matrix<T> subtract(Matrix<T> other) { return zip(other, field::subtract); }
    public Matrix<T> hadamard(Matrix<T> other) { return zip(other, field::multiply); }

    public Matrix<T> negate() { return map(field, field::negate); }
    public Matrix<T> scale(T scalar) { return map(field, value -> field.multiply(value, scalar)); }
    public Matrix<T> divide(T scalar) { return map(field, value -> field.divide(value, scalar)); }
    public Matrix<T> conjugate() { return map(field, field::conjugate); }

    public Matrix<T> multiply(Matrix<T> other) {
        requireSameField(other);
        if (columnCount != other.rowCount) throw new IllegalArgumentException("Incompatible matrix dimensions for multiplication");
        Object[][] result = zeroArray(field, rowCount, other.columnCount);
        for (int row = 0; row < rowCount; row++) {
            for (int column = 0; column < other.columnCount; column++) {
                T sum = field.zero();
                for (int k = 0; k < columnCount; k++) sum = field.add(sum, field.multiply(get(row, k), other.get(k, column)));
                result[row][column] = sum;
            }
        }
        return new Matrix<>(field, result, true);
    }

    public Matrix<T> transpose() {
        Object[][] result = new Object[columnCount][rowCount];
        for (int row = 0; row < rowCount; row++) for (int column = 0; column < columnCount; column++) result[column][row] = get(row, column);
        return new Matrix<>(field, result, true);
    }

    public Matrix<T> conjugateTranspose() { return conjugate().transpose(); }

    public T trace() {
        requireSquare();
        T result = field.zero();
        for (int i = 0; i < rowCount; i++) result = field.add(result, get(i, i));
        return result;
    }

    public T sum() {
        T result = field.zero();
        for (int row = 0; row < rowCount; row++) for (int column = 0; column < columnCount; column++) result = field.add(result, get(row, column));
        return result;
    }

    public T product() {
        T result = field.one();
        for (int row = 0; row < rowCount; row++) for (int column = 0; column < columnCount; column++) result = field.multiply(result, get(row, column));
        return result;
    }

    public List<T> diagonal() {
        List<T> result = new ArrayList<>(Math.min(rowCount, columnCount));
        for (int i = 0; i < Math.min(rowCount, columnCount); i++) result.add(get(i, i));
        return Collections.unmodifiableList(result);
    }

    /** Frobenius inner product sum(conjugate(a_ij) * b_ij). */
    public T frobeniusInnerProduct(Matrix<T> other) {
        requireSameDimensions(other);
        T result = field.zero();
        for (int row = 0; row < rowCount; row++) for (int column = 0; column < columnCount; column++) {
            result = field.add(result, field.multiply(field.conjugate(get(row, column)), other.get(row, column)));
        }
        return result;
    }

    public Matrix<T> commutator(Matrix<T> other) {
        requireSquare();
        return multiply(other).subtract(other.multiply(this));
    }

    public Matrix<T> anticommutator(Matrix<T> other) {
        requireSquare();
        return multiply(other).add(other.multiply(this));
    }

    /** Division-free Laplace determinant, preserving polynomial symbolic results. */
    public T determinant() {
        requireSquare();
        return determinant(values, rowCount);
    }

    @SuppressWarnings("unchecked")
    private T determinant(Object[][] data, int size) {
        if (size == 0) return field.one();
        if (size == 1) return (T) data[0][0];
        if (size == 2) {
            return field.subtract(field.multiply((T) data[0][0], (T) data[1][1]),
                    field.multiply((T) data[0][1], (T) data[1][0]));
        }
        int expansionRow = 0;
        int mostZeros = -1;
        for (int row = 0; row < size; row++) {
            int zeros = 0;
            for (int column = 0; column < size; column++) if (field.isZero((T) data[row][column])) zeros++;
            if (zeros > mostZeros) { mostZeros = zeros; expansionRow = row; }
        }
        T sum = field.zero();
        for (int column = 0; column < size; column++) {
            T element = (T) data[expansionRow][column];
            if (field.isZero(element)) continue;
            T term = field.multiply(element, determinant(deleteRowAndColumn(data, size, expansionRow, column), size - 1));
            sum = ((expansionRow + column) & 1) == 0 ? field.add(sum, term) : field.subtract(sum, term);
        }
        return sum;
    }

    public T minorValue(int row, int column) {
        requireSquare();
        checkIndex(row, column);
        return determinant(deleteRowAndColumn(values, rowCount, row, column), rowCount - 1);
    }

    public T cofactor(int row, int column) {
        T minor = minorValue(row, column);
        return ((row + column) & 1) == 0 ? minor : field.negate(minor);
    }

    public Matrix<T> adjugate() {
        requireSquare();
        Object[][] result = new Object[rowCount][columnCount];
        for (int row = 0; row < rowCount; row++) for (int column = 0; column < columnCount; column++) result[column][row] = cofactor(row, column);
        return new Matrix<>(field, result, true);
    }

    public Matrix<T> inverse() {
        requireSquare();
        int size = rowCount;
        Object[][] augmented = new Object[size][size * 2];
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) augmented[row][column] = get(row, column);
            for (int column = 0; column < size; column++) augmented[row][size + column] = row == column ? field.one() : field.zero();
        }
        int pivots = reduceInPlace(augmented, size, size * 2, size);
        if (pivots != size) throw new ArithmeticException("Singular matrix");
        Object[][] result = new Object[size][size];
        for (int row = 0; row < size; row++) System.arraycopy(augmented[row], size, result[row], 0, size);
        return new Matrix<>(field, result, true);
    }

    public Matrix<T> solve(Matrix<T> rightHandSide) {
        requireSquare();
        requireSameField(rightHandSide);
        if (rightHandSide.rowCount != rowCount) throw new IllegalArgumentException("Right-hand side has incompatible row count");
        return inverse().multiply(rightHandSide);
    }

    public Matrix<T> leastSquares(Matrix<T> rightHandSide) {
        requireSameField(rightHandSide);
        if (rightHandSide.rowCount != rowCount) throw new IllegalArgumentException("Right-hand side has incompatible row count");
        Matrix<T> adjoint = conjugateTranspose();
        return adjoint.multiply(this).inverse().multiply(adjoint).multiply(rightHandSide);
    }

    public Matrix<T> pseudoInverseFullColumnRank() {
        Matrix<T> adjoint = conjugateTranspose();
        return adjoint.multiply(this).inverse().multiply(adjoint);
    }

    public Matrix<T> rref() {
        Object[][] result = copyValues();
        reduceInPlace(result, rowCount, columnCount, columnCount);
        return new Matrix<>(field, result, true);
    }

    public int rank() {
        Matrix<T> reduced = rref();
        int rank = 0;
        for (int row = 0; row < rowCount; row++) {
            boolean nonzero = false;
            for (int column = 0; column < columnCount; column++) nonzero |= !field.isZero(reduced.get(row, column));
            if (nonzero) rank++;
        }
        return rank;
    }

    public int nullity() { return columnCount - rank(); }

    @SuppressWarnings("unchecked")
    private int reduceInPlace(Object[][] data, int rows, int columns, int pivotColumnLimit) {
        int pivotRow = 0;
        for (int column = 0; column < pivotColumnLimit && pivotRow < rows; column++) {
            int selected = pivotRow;
            while (selected < rows && field.isZero((T) data[selected][column])) selected++;
            if (selected == rows) continue;
            Object[] temporary = data[pivotRow]; data[pivotRow] = data[selected]; data[selected] = temporary;
            T pivot = (T) data[pivotRow][column];
            for (int j = 0; j < columns; j++) data[pivotRow][j] = field.divide((T) data[pivotRow][j], pivot);
            for (int row = 0; row < rows; row++) {
                if (row == pivotRow) continue;
                T factor = (T) data[row][column];
                if (field.isZero(factor)) continue;
                for (int j = 0; j < columns; j++) data[row][j] = field.subtract((T) data[row][j], field.multiply(factor, (T) data[pivotRow][j]));
            }
            pivotRow++;
        }
        return pivotRow;
    }

    public Matrix<T> pow(int exponent) {
        requireSquare();
        if (exponent < 0) return inverse().powPositive(-(long) exponent);
        return powPositive(exponent);
    }

    private Matrix<T> powPositive(long exponent) {
        Matrix<T> result = identity(field, rowCount);
        Matrix<T> base = this;
        long remaining = exponent;
        while (remaining > 0) {
            if ((remaining & 1L) != 0) result = result.multiply(base);
            remaining >>>= 1;
            if (remaining != 0) base = base.multiply(base);
        }
        return result;
    }

    /** Characteristic polynomial det(xI-A), computed without division. */
    public Polynomial<T> characteristicPolynomial() {
        requireSquare();
        Object[][] polynomialMatrix = new Object[rowCount][columnCount];
        Polynomial<T> x = Polynomial.of(field, field.zero(), field.one());
        for (int row = 0; row < rowCount; row++) {
            for (int column = 0; column < columnCount; column++) {
                Polynomial<T> constant = Polynomial.of(field, field.negate(get(row, column)));
                polynomialMatrix[row][column] = row == column ? x.add(constant) : constant;
            }
        }
        return polynomialDeterminant(polynomialMatrix, rowCount);
    }

    @SuppressWarnings("unchecked")
    private Polynomial<T> polynomialDeterminant(Object[][] data, int size) {
        if (size == 0) return Polynomial.of(field, field.one());
        if (size == 1) return (Polynomial<T>) data[0][0];
        Polynomial<T> sum = Polynomial.of(field, field.zero());
        for (int column = 0; column < size; column++) {
            Polynomial<T> element = (Polynomial<T>) data[0][column];
            if (element.isZero()) continue;
            Polynomial<T> term = element.multiply(polynomialDeterminant(deleteRowAndColumn(data, size, 0, column), size - 1));
            sum = (column & 1) == 0 ? sum.add(term) : sum.subtract(term);
        }
        return sum;
    }

    public boolean satisfiesCharacteristicPolynomial() {
        Polynomial<T> polynomial = characteristicPolynomial();
        Matrix<T> result = zeros(field, rowCount, columnCount);
        for (int i = polynomial.degree(); i >= 0; i--) {
            result = result.multiply(this).add(identity(field, rowCount).scale(polynomial.coefficient(i)));
        }
        return result.isZero();
    }

    public Matrix<T> kronecker(Matrix<T> other) {
        requireSameField(other);
        Object[][] result = new Object[rowCount * other.rowCount][columnCount * other.columnCount];
        for (int row = 0; row < rowCount; row++) for (int column = 0; column < columnCount; column++)
            for (int otherRow = 0; otherRow < other.rowCount; otherRow++) for (int otherColumn = 0; otherColumn < other.columnCount; otherColumn++)
                result[row * other.rowCount + otherRow][column * other.columnCount + otherColumn] = field.multiply(get(row, column), other.get(otherRow, otherColumn));
        return new Matrix<>(field, result, true);
    }

    public Matrix<T> directSum(Matrix<T> other) {
        requireSameField(other);
        Object[][] result = zeroArray(field, rowCount + other.rowCount, columnCount + other.columnCount);
        for (int row = 0; row < rowCount; row++) for (int column = 0; column < columnCount; column++) result[row][column] = get(row, column);
        for (int row = 0; row < other.rowCount; row++) for (int column = 0; column < other.columnCount; column++) result[rowCount + row][columnCount + column] = other.get(row, column);
        return new Matrix<>(field, result, true);
    }

    public Matrix<T> concatHorizontal(Matrix<T> other) {
        requireSameField(other);
        if (rowCount != other.rowCount) throw new IllegalArgumentException("Matrices must have equal row counts");
        Object[][] result = new Object[rowCount][columnCount + other.columnCount];
        for (int row = 0; row < rowCount; row++) {
            System.arraycopy(values[row], 0, result[row], 0, columnCount);
            System.arraycopy(other.values[row], 0, result[row], columnCount, other.columnCount);
        }
        return new Matrix<>(field, result, true);
    }

    public Matrix<T> concatVertical(Matrix<T> other) {
        requireSameField(other);
        if (columnCount != other.columnCount) throw new IllegalArgumentException("Matrices must have equal column counts");
        Object[][] result = new Object[rowCount + other.rowCount][columnCount];
        for (int row = 0; row < rowCount; row++) System.arraycopy(values[row], 0, result[row], 0, columnCount);
        for (int row = 0; row < other.rowCount; row++) System.arraycopy(other.values[row], 0, result[rowCount + row], 0, columnCount);
        return new Matrix<>(field, result, true);
    }

    public Matrix<T> subMatrix(int fromRow, int toRow, int fromColumn, int toColumn) {
        if (fromRow < 0 || fromRow >= toRow || toRow > rowCount || fromColumn < 0 || fromColumn >= toColumn || toColumn > columnCount) {
            throw new IndexOutOfBoundsException("Invalid half-open matrix range");
        }
        Object[][] result = new Object[toRow - fromRow][toColumn - fromColumn];
        for (int row = fromRow; row < toRow; row++) System.arraycopy(values[row], fromColumn, result[row - fromRow], 0, toColumn - fromColumn);
        return new Matrix<>(field, result, true);
    }

    public Matrix<T> reshape(int newRows, int newColumns) {
        checkDimensions(newRows, newColumns);
        if (newRows * newColumns != rowCount * columnCount) throw new IllegalArgumentException("Reshape must preserve element count");
        Object[][] result = new Object[newRows][newColumns];
        for (int index = 0; index < rowCount * columnCount; index++) result[index / newColumns][index % newColumns] = values[index / columnCount][index % columnCount];
        return new Matrix<>(field, result, true);
    }

    public List<T> flatten() {
        List<T> result = new ArrayList<>(rowCount * columnCount);
        for (int row = 0; row < rowCount; row++) for (int column = 0; column < columnCount; column++) result.add(get(row, column));
        return Collections.unmodifiableList(result);
    }

    public Matrix<T> swapRows(int first, int second) {
        if (first < 0 || first >= rowCount || second < 0 || second >= rowCount) throw new IndexOutOfBoundsException("Row index");
        Object[][] result = copyValues();
        Object[] temporary = result[first]; result[first] = result[second]; result[second] = temporary;
        return new Matrix<>(field, result, true);
    }

    public Matrix<T> swapColumns(int first, int second) {
        if (first < 0 || first >= columnCount || second < 0 || second >= columnCount) throw new IndexOutOfBoundsException("Column index");
        Object[][] result = copyValues();
        for (int row = 0; row < rowCount; row++) {
            Object temporary = result[row][first]; result[row][first] = result[row][second]; result[row][second] = temporary;
        }
        return new Matrix<>(field, result, true);
    }

    public <R> Matrix<R> map(Field<R> resultField, Function<? super T, ? extends R> function) {
        Objects.requireNonNull(resultField, "resultField");
        Objects.requireNonNull(function, "function");
        Object[][] result = new Object[rowCount][columnCount];
        for (int row = 0; row < rowCount; row++) for (int column = 0; column < columnCount; column++) result[row][column] = Objects.requireNonNull(function.apply(get(row, column)));
        return new Matrix<>(resultField, result, true);
    }

    public Matrix<T> zip(Matrix<T> other, BiFunction<? super T, ? super T, ? extends T> function) {
        requireSameDimensions(other);
        Object[][] result = new Object[rowCount][columnCount];
        for (int row = 0; row < rowCount; row++) for (int column = 0; column < columnCount; column++) result[row][column] = Objects.requireNonNull(function.apply(get(row, column), other.get(row, column)));
        return new Matrix<>(field, result, true);
    }

    public boolean isSquare() { return rowCount == columnCount; }
    public boolean isZero() {
        for (int row = 0; row < rowCount; row++) for (int column = 0; column < columnCount; column++) if (!field.isZero(get(row, column))) return false;
        return true;
    }
    public boolean isDiagonal() {
        if (!isSquare()) return false;
        for (int row = 0; row < rowCount; row++) for (int column = 0; column < columnCount; column++) if (row != column && !field.isZero(get(row, column))) return false;
        return true;
    }
    public boolean isIdentity() { return isSquare() && sameValues(identity(field, rowCount)); }
    public boolean isRectangularIdentity() { return sameValues(identity(field, rowCount, columnCount)); }
    public boolean isUpperTriangular() {
        if (!isSquare()) return false;
        for (int row = 1; row < rowCount; row++) for (int column = 0; column < row; column++) if (!field.isZero(get(row, column))) return false;
        return true;
    }
    public boolean isLowerTriangular() {
        if (!isSquare()) return false;
        for (int row = 0; row < rowCount; row++) for (int column = row + 1; column < columnCount; column++) if (!field.isZero(get(row, column))) return false;
        return true;
    }
    public boolean isSymmetric() { return isSquare() && sameValues(transpose()); }
    public boolean isHermitian() { return isSquare() && sameValues(conjugateTranspose()); }
    public boolean isNormal() { return isSquare() && multiply(conjugateTranspose()).sameValues(conjugateTranspose().multiply(this)); }
    public boolean commutesWith(Matrix<T> other) { return commutator(other).isZero(); }
    public boolean isIdempotent() { return isSquare() && multiply(this).sameValues(this); }
    public boolean isInvolutory() { return isSquare() && multiply(this).isIdentity(); }
    public boolean isNilpotent() {
        if (!isSquare()) return false;
        Matrix<T> power = this;
        for (int exponent = 1; exponent <= rowCount; exponent++) {
            if (power.isZero()) return true;
            power = power.multiply(this);
        }
        return false;
    }
    public boolean isInvertible() { return isSquare() && !field.isZero(determinant()); }

    /** Compares entries using the field's tolerance/equality policy. */
    public boolean sameValues(Matrix<T> other) {
        if (other == null || rowCount != other.rowCount || columnCount != other.columnCount) return false;
        requireSameField(other);
        for (int row = 0; row < rowCount; row++) for (int column = 0; column < columnCount; column++) if (!field.areEqual(get(row, column), other.get(row, column))) return false;
        return true;
    }

    private void requireSameDimensions(Matrix<T> other) {
        requireSameField(other);
        if (rowCount != other.rowCount || columnCount != other.columnCount) throw new IllegalArgumentException("Matrix dimensions differ");
    }

    private void requireSameField(Matrix<T> other) {
        Objects.requireNonNull(other, "other");
        if (field != other.field && !field.equals(other.field)) throw new IllegalArgumentException("Matrices use different fields");
    }

    private void requireSquare() { if (!isSquare()) throw new IllegalStateException("Operation requires a square matrix"); }
    private void checkIndex(int row, int column) {
        if (row < 0 || row >= rowCount || column < 0 || column >= columnCount) throw new IndexOutOfBoundsException("Matrix index (" + row + ", " + column + ")");
    }
    private static void checkDimensions(int rows, int columns) {
        if (rows <= 0 || columns <= 0) throw new IllegalArgumentException("Matrix dimensions must be positive");
    }
    private static <T> Object[][] zeroArray(Field<T> field, int rows, int columns) {
        Object[][] result = new Object[rows][columns];
        for (int row = 0; row < rows; row++) Arrays.fill(result[row], field.zero());
        return result;
    }
    private Object[][] copyValues() {
        Object[][] result = new Object[rowCount][columnCount];
        for (int row = 0; row < rowCount; row++) System.arraycopy(values[row], 0, result[row], 0, columnCount);
        return result;
    }
    private static Object[][] deleteRowAndColumn(Object[][] source, int size, int deletedRow, int deletedColumn) {
        Object[][] result = new Object[size - 1][size - 1];
        int targetRow = 0;
        for (int row = 0; row < size; row++) {
            if (row == deletedRow) continue;
            int targetColumn = 0;
            for (int column = 0; column < size; column++) {
                if (column == deletedColumn) continue;
                result[targetRow][targetColumn++] = source[row][column];
            }
            targetRow++;
        }
        return result;
    }

    @Override public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof Matrix)) return false;
        Matrix<?> other = (Matrix<?>) object;
        return rowCount == other.rowCount && columnCount == other.columnCount && field.equals(other.field) && Arrays.deepEquals(values, other.values);
    }
    @Override public int hashCode() { return 31 * (31 * field.hashCode() + rowCount) + Arrays.deepHashCode(values); }
    @Override public String toString() { return toList().toString(); }
}

