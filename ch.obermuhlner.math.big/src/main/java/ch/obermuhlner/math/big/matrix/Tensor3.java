package ch.obermuhlner.math.big.matrix;

import ch.obermuhlner.math.big.algebra.Field;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/** Immutable rank-3 tensor using the stable axis order layer × row × column. */
public final class Tensor3<T> {
    public enum ConvolutionMode { FULL, SAME, VALID }

    private final Field<T> field;
    private final int layers;
    private final int rows;
    private final int columns;
    private final Object[][][] values;

    public Tensor3(Field<T> field, T[][][] values) {
        this(field, copy(values), true);
    }

    private Tensor3(Field<T> field, Object[][][] values, boolean trusted) {
        this.field = Objects.requireNonNull(field, "field");
        if (values.length == 0 || values[0].length == 0 || values[0][0].length == 0) throw new IllegalArgumentException("Tensor dimensions must be positive");
        this.layers = values.length;
        this.rows = values[0].length;
        this.columns = values[0][0].length;
        this.values = values;
    }

    public static <T> Tensor3<T> zeros(Field<T> field, int layers, int rows, int columns) {
        return filled(field, layers, rows, columns, field.zero());
    }

    public static <T> Tensor3<T> filled(Field<T> field, int layers, int rows, int columns, T value) {
        checkDimensions(layers, rows, columns);
        Object[][][] result = new Object[layers][rows][columns];
        for (int layer = 0; layer < layers; layer++) for (int row = 0; row < rows; row++) Arrays.fill(result[layer][row], value);
        return new Tensor3<>(field, result, true);
    }

    public static <T> Tensor3<T> stack(List<Matrix<T>> matrices) {
        Objects.requireNonNull(matrices, "matrices");
        if (matrices.isEmpty()) throw new IllegalArgumentException("At least one matrix is required");
        Matrix<T> first = Objects.requireNonNull(matrices.get(0));
        Object[][][] result = new Object[matrices.size()][first.rows()][first.columns()];
        for (int layer = 0; layer < matrices.size(); layer++) {
            Matrix<T> matrix = Objects.requireNonNull(matrices.get(layer));
            if (matrix.rows() != first.rows() || matrix.columns() != first.columns()) throw new IllegalArgumentException("All matrices must have equal dimensions");
            if (matrix.getField() != first.getField() && !matrix.getField().equals(first.getField())) throw new IllegalArgumentException("All matrices must use the same field");
            for (int row = 0; row < first.rows(); row++) for (int column = 0; column < first.columns(); column++) result[layer][row][column] = matrix.get(row, column);
        }
        return new Tensor3<>(first.getField(), result, true);
    }

    public Field<T> getField() { return field; }
    public int layers() { return layers; }
    public int rows() { return rows; }
    public int columns() { return columns; }
    public int size() { return layers * rows * columns; }

    @SuppressWarnings("unchecked")
    public T get(int layer, int row, int column) {
        checkIndex(layer, row, column);
        return (T) values[layer][row][column];
    }

    public Tensor3<T> with(int layer, int row, int column, T value) {
        checkIndex(layer, row, column);
        Object[][][] result = copyValues();
        result[layer][row][column] = Objects.requireNonNull(value, "value");
        return new Tensor3<>(field, result, true);
    }

    public Matrix<T> layer(int layer) {
        if (layer < 0 || layer >= layers) throw new IndexOutOfBoundsException("Layer: " + layer);
        List<List<T>> result = new ArrayList<>(rows);
        for (int row = 0; row < rows; row++) {
            List<T> values = new ArrayList<>(columns);
            for (int column = 0; column < columns; column++) values.add(get(layer, row, column));
            result.add(values);
        }
        return new Matrix<>(field, result);
    }

    public Matrix<T> sliceRows(int row) {
        if (row < 0 || row >= rows) throw new IndexOutOfBoundsException("Row: " + row);
        List<List<T>> result = new ArrayList<>(layers);
        for (int layer = 0; layer < layers; layer++) {
            List<T> values = new ArrayList<>(columns);
            for (int column = 0; column < columns; column++) values.add(get(layer, row, column));
            result.add(values);
        }
        return new Matrix<>(field, result);
    }

    public Matrix<T> sliceColumns(int column) {
        if (column < 0 || column >= columns) throw new IndexOutOfBoundsException("Column: " + column);
        List<List<T>> result = new ArrayList<>(layers);
        for (int layer = 0; layer < layers; layer++) {
            List<T> values = new ArrayList<>(rows);
            for (int row = 0; row < rows; row++) values.add(get(layer, row, column));
            result.add(values);
        }
        return new Matrix<>(field, result);
    }

    public Tensor3<T> add(Tensor3<T> other) { return zip(other, field::add); }
    public Tensor3<T> subtract(Tensor3<T> other) { return zip(other, field::subtract); }
    public Tensor3<T> hadamard(Tensor3<T> other) { return zip(other, field::multiply); }
    public Tensor3<T> negate() { return map(field, field::negate); }
    public Tensor3<T> conjugate() { return map(field, field::conjugate); }
    public Tensor3<T> scale(T scalar) { return map(field, value -> field.multiply(value, scalar)); }

    public Tensor3<T> multiplyLayers(Tensor3<T> other) {
        requireSameField(other);
        if (layers != other.layers || columns != other.rows) throw new IllegalArgumentException("Incompatible layer multiplication dimensions");
        List<Matrix<T>> result = new ArrayList<>(layers);
        for (int layer = 0; layer < layers; layer++) result.add(layer(layer).multiply(other.layer(layer)));
        return stack(result);
    }

    public T sum() {
        T result = field.zero();
        for (int layer = 0; layer < layers; layer++) for (int row = 0; row < rows; row++) for (int column = 0; column < columns; column++) result = field.add(result, get(layer, row, column));
        return result;
    }

    public T frobeniusInnerProduct(Tensor3<T> other) {
        requireSameShape(other);
        T result = field.zero();
        for (int layer = 0; layer < layers; layer++) for (int row = 0; row < rows; row++) for (int column = 0; column < columns; column++)
            result = field.add(result, field.multiply(field.conjugate(get(layer, row, column)), other.get(layer, row, column)));
        return result;
    }

    public Tensor3<T> permuteAxes(int first, int second, int third) {
        int[] axes = {first, second, third};
        boolean[] seen = new boolean[3];
        for (int axis : axes) {
            if (axis < 0 || axis > 2 || seen[axis]) throw new IllegalArgumentException("Axes must be a permutation of 0, 1, 2");
            seen[axis] = true;
        }
        int[] shape = {layers, rows, columns};
        Object[][][] result = new Object[shape[first]][shape[second]][shape[third]];
        for (int layer = 0; layer < layers; layer++) for (int row = 0; row < rows; row++) for (int column = 0; column < columns; column++) {
            int[] source = {layer, row, column};
            result[source[first]][source[second]][source[third]] = get(layer, row, column);
        }
        return new Tensor3<>(field, result, true);
    }

    /** Mode-n unfolding; rows are the selected axis and columns are the other axes in natural order. */
    public Matrix<T> unfold(int mode) {
        if (mode < 0 || mode > 2) throw new IllegalArgumentException("Mode must be 0, 1 or 2");
        int[] shape = {layers, rows, columns};
        int resultRows = shape[mode];
        int resultColumns = size() / resultRows;
        List<List<T>> result = new ArrayList<>(resultRows);
        for (int i = 0; i < resultRows; i++) result.add(new ArrayList<T>(Collections.nCopies(resultColumns, field.zero())));
        for (int layer = 0; layer < layers; layer++) for (int row = 0; row < rows; row++) for (int column = 0; column < columns; column++) {
            int targetRow;
            int targetColumn;
            if (mode == 0) { targetRow = layer; targetColumn = row * columns + column; }
            else if (mode == 1) { targetRow = row; targetColumn = layer * columns + column; }
            else { targetRow = column; targetColumn = layer * rows + row; }
            result.get(targetRow).set(targetColumn, get(layer, row, column));
        }
        return new Matrix<>(field, result);
    }

    /** Multiplies a tensor by a matrix along one axis. */
    public Tensor3<T> modeProduct(Matrix<T> matrix, int mode) {
        Objects.requireNonNull(matrix, "matrix");
        if (matrix.getField() != field && !matrix.getField().equals(field)) throw new IllegalArgumentException("Different fields");
        int oldDimension = mode == 0 ? layers : mode == 1 ? rows : mode == 2 ? columns : -1;
        if (oldDimension < 0 || matrix.columns() != oldDimension) throw new IllegalArgumentException("Incompatible mode product dimensions");
        int newLayers = mode == 0 ? matrix.rows() : layers;
        int newRows = mode == 1 ? matrix.rows() : rows;
        int newColumns = mode == 2 ? matrix.rows() : columns;
        Object[][][] result = zerosArray(field, newLayers, newRows, newColumns);
        for (int layer = 0; layer < newLayers; layer++) for (int row = 0; row < newRows; row++) for (int column = 0; column < newColumns; column++) {
            int targetIndex = mode == 0 ? layer : mode == 1 ? row : column;
            T sum = field.zero();
            for (int source = 0; source < oldDimension; source++) {
                T value = mode == 0 ? get(source, row, column) : mode == 1 ? get(layer, source, column) : get(layer, row, source);
                sum = field.add(sum, field.multiply(matrix.get(targetIndex, source), value));
            }
            result[layer][row][column] = sum;
        }
        return new Tensor3<>(field, result, true);
    }

    public Tensor3<T> convolve(Tensor3<T> kernel, ConvolutionMode mode) {
        requireSameField(kernel);
        Objects.requireNonNull(mode, "mode");
        Object[][][] full = zerosArray(field, layers + kernel.layers - 1, rows + kernel.rows - 1, columns + kernel.columns - 1);
        for (int l = 0; l < layers; l++) for (int r = 0; r < rows; r++) for (int c = 0; c < columns; c++)
            for (int kl = 0; kl < kernel.layers; kl++) for (int kr = 0; kr < kernel.rows; kr++) for (int kc = 0; kc < kernel.columns; kc++) {
                @SuppressWarnings("unchecked") T previous = (T) full[l + kl][r + kr][c + kc];
                full[l + kl][r + kr][c + kc] = field.add(previous, field.multiply(get(l, r, c), kernel.get(kl, kr, kc)));
            }
        if (mode == ConvolutionMode.FULL) return new Tensor3<>(field, full, true);
        int resultLayers = mode == ConvolutionMode.SAME ? layers : layers - kernel.layers + 1;
        int resultRows = mode == ConvolutionMode.SAME ? rows : rows - kernel.rows + 1;
        int resultColumns = mode == ConvolutionMode.SAME ? columns : columns - kernel.columns + 1;
        if (resultLayers <= 0 || resultRows <= 0 || resultColumns <= 0) throw new IllegalArgumentException("Kernel is larger than input in VALID mode");
        int startLayer = mode == ConvolutionMode.SAME ? (kernel.layers - 1) / 2 : kernel.layers - 1;
        int startRow = mode == ConvolutionMode.SAME ? (kernel.rows - 1) / 2 : kernel.rows - 1;
        int startColumn = mode == ConvolutionMode.SAME ? (kernel.columns - 1) / 2 : kernel.columns - 1;
        Object[][][] result = new Object[resultLayers][resultRows][resultColumns];
        for (int l = 0; l < resultLayers; l++) for (int r = 0; r < resultRows; r++)
            System.arraycopy(full[l + startLayer][r + startRow], startColumn, result[l][r], 0, resultColumns);
        return new Tensor3<>(field, result, true);
    }

    public List<T> flatten() {
        List<T> result = new ArrayList<>(size());
        for (int layer = 0; layer < layers; layer++) for (int row = 0; row < rows; row++) for (int column = 0; column < columns; column++) result.add(get(layer, row, column));
        return Collections.unmodifiableList(result);
    }

    public <R> Tensor3<R> map(Field<R> resultField, Function<? super T, ? extends R> function) {
        Object[][][] result = new Object[layers][rows][columns];
        for (int layer = 0; layer < layers; layer++) for (int row = 0; row < rows; row++) for (int column = 0; column < columns; column++) result[layer][row][column] = Objects.requireNonNull(function.apply(get(layer, row, column)));
        return new Tensor3<>(resultField, result, true);
    }

    public Tensor3<T> zip(Tensor3<T> other, BiFunction<? super T, ? super T, ? extends T> function) {
        requireSameShape(other);
        Object[][][] result = new Object[layers][rows][columns];
        for (int layer = 0; layer < layers; layer++) for (int row = 0; row < rows; row++) for (int column = 0; column < columns; column++) result[layer][row][column] = Objects.requireNonNull(function.apply(get(layer, row, column), other.get(layer, row, column)));
        return new Tensor3<>(field, result, true);
    }

    public boolean sameValues(Tensor3<T> other) {
        if (other == null || layers != other.layers || rows != other.rows || columns != other.columns) return false;
        requireSameField(other);
        for (int layer = 0; layer < layers; layer++) for (int row = 0; row < rows; row++) for (int column = 0; column < columns; column++)
            if (!field.areEqual(get(layer, row, column), other.get(layer, row, column))) return false;
        return true;
    }

    private void requireSameShape(Tensor3<T> other) {
        requireSameField(other);
        if (layers != other.layers || rows != other.rows || columns != other.columns) throw new IllegalArgumentException("Tensor shapes differ");
    }
    private void requireSameField(Tensor3<T> other) {
        Objects.requireNonNull(other, "other");
        if (field != other.field && !field.equals(other.field)) throw new IllegalArgumentException("Tensors use different fields");
    }
    private void checkIndex(int layer, int row, int column) {
        if (layer < 0 || layer >= layers || row < 0 || row >= rows || column < 0 || column >= columns) throw new IndexOutOfBoundsException("Tensor index");
    }
    private static void checkDimensions(int layers, int rows, int columns) {
        if (layers <= 0 || rows <= 0 || columns <= 0) throw new IllegalArgumentException("Tensor dimensions must be positive");
    }
    private static <T> Object[][][] zerosArray(Field<T> field, int layers, int rows, int columns) {
        Object[][][] result = new Object[layers][rows][columns];
        for (int layer = 0; layer < layers; layer++) for (int row = 0; row < rows; row++) Arrays.fill(result[layer][row], field.zero());
        return result;
    }
    private Object[][][] copyValues() {
        Object[][][] result = new Object[layers][rows][columns];
        for (int layer = 0; layer < layers; layer++) for (int row = 0; row < rows; row++) System.arraycopy(values[layer][row], 0, result[layer][row], 0, columns);
        return result;
    }
    private static <T> Object[][][] copy(T[][][] source) {
        Objects.requireNonNull(source, "values");
        if (source.length == 0 || source[0].length == 0 || source[0][0].length == 0) throw new IllegalArgumentException("Tensor dimensions must be positive");
        Object[][][] result = new Object[source.length][source[0].length][source[0][0].length];
        for (int layer = 0; layer < source.length; layer++) {
            if (source[layer].length != source[0].length) throw new IllegalArgumentException("Jagged tensor");
            for (int row = 0; row < source[layer].length; row++) {
                if (source[layer][row].length != source[0][0].length) throw new IllegalArgumentException("Jagged tensor");
                for (int column = 0; column < source[layer][row].length; column++) result[layer][row][column] = Objects.requireNonNull(source[layer][row][column]);
            }
        }
        return result;
    }

    @Override public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof Tensor3)) return false;
        Tensor3<?> other = (Tensor3<?>) object;
        return field.equals(other.field) && Arrays.deepEquals(values, other.values);
    }
    @Override public int hashCode() { return 31 * field.hashCode() + Arrays.deepHashCode(values); }
    @Override public String toString() { return flatten().toString(); }
}
