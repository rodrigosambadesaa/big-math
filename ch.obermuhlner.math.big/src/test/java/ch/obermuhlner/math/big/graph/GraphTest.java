package ch.obermuhlner.math.big.graph;

import ch.obermuhlner.math.big.BigRational;
import ch.obermuhlner.math.big.algebra.BigRationalField;
import ch.obermuhlner.math.big.matrix.Matrix;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class GraphTest {
    @Test public void traversesAndMeasuresDirectedGraphs() {
        Graph graph = Graph.fromEdges(5, true, new int[][] {{0, 1}, {0, 2}, {1, 3}, {2, 3}, {3, 4}});
        assertEquals(Arrays.asList(0, 1, 2, 3, 4), graph.breadthFirst(0));
        assertEquals(Arrays.asList(0, 1, 3, 4), graph.shortestPath(0, 4));
        assertEquals(3, graph.distances()[0][4]);
        assertTrue(graph.transitiveClosure()[0][4]);
        assertFalse(graph.hasCycle());
        assertEquals(Arrays.asList(0, 1, 2, 3, 4), graph.topologicalOrder());
    }

    @Test public void createsGraphMatricesOverExactFields() {
        Graph triangle = Graph.fromEdges(3, false, new int[][] {{0, 1}, {1, 2}, {2, 0}});
        assertEquals(3, triangle.edgeCount());
        assertTrue(triangle.isConnected());
        assertTrue(triangle.hasCycle());
        Matrix<BigRational> laplacian = triangle.laplacianMatrix(BigRationalField.INSTANCE);
        assertEquals(BigRational.ZERO, laplacian.multiply(Matrix.column(BigRationalField.INSTANCE,
                Arrays.asList(BigRational.ONE, BigRational.ONE, BigRational.ONE))).get(0, 0));
        assertEquals(BigRational.valueOf(2), triangle.walkCounts(BigRationalField.INSTANCE, 2).get(0, 0));
    }
}
