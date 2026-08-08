package ch.obermuhlner.math.big.matrix;

import ch.obermuhlner.math.big.BigRational;
import ch.obermuhlner.math.big.algebra.BigRationalField;
import org.junit.Test;

import java.math.BigInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class Tensor3Test {
    private static final BigRationalField Q = BigRationalField.INSTANCE;

    @Test
    public void slicesPermutationsUnfoldingAndModeProducts() {
        Tensor3<BigRational> tensor = tensor(new int[][][] {{{1, 2}, {3, 4}}, {{5, 6}, {7, 8}}});
        assertEquals(r(7), tensor.layer(1).get(1, 0));
        assertEquals(r(6), tensor.sliceRows(0).get(1, 1));
        assertEquals(r(7), tensor.permuteAxes(1, 0, 2).get(1, 1, 0));
        assertEquals(2, tensor.unfold(0).rows());
        assertEquals(4, tensor.unfold(0).columns());
        assertTrue(tensor.modeProduct(Matrix.identity(Q, 2), 1).sameValues(tensor));
    }

    @Test
    public void genericThreeDimensionalConvolution() {
        Tensor3<BigRational> input = tensor(new int[][][] {{{1, 2}, {3, 4}}});
        Tensor3<BigRational> kernel = tensor(new int[][][] {{{1, 1}}});
        Tensor3<BigRational> full = input.convolve(kernel, Tensor3.ConvolutionMode.FULL);
        assertEquals(1, full.layers());
        assertEquals(2, full.rows());
        assertEquals(3, full.columns());
        assertEquals(r(1), full.get(0, 0, 0));
        assertEquals(r(3), full.get(0, 0, 1));
        assertEquals(r(2), full.get(0, 0, 2));
    }

    private static Tensor3<BigRational> tensor(int[][][] values) {
        BigRational[][][] result = new BigRational[values.length][values[0].length][values[0][0].length];
        for (int l = 0; l < values.length; l++) for (int r = 0; r < values[l].length; r++) for (int c = 0; c < values[l][r].length; c++) result[l][r][c] = r(values[l][r][c]);
        return new Tensor3<>(Q, result);
    }
    private static BigRational r(int value) { return BigRational.valueOf(BigInteger.valueOf(value)); }
}
