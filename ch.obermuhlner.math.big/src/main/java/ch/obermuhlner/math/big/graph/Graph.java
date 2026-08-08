package ch.obermuhlner.math.big.graph;

import ch.obermuhlner.math.big.algebra.Field;
import ch.obermuhlner.math.big.matrix.Matrix;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;

/** Immutable unweighted directed or undirected graph with matrix adapters. */
public final class Graph {
    private final boolean directed;
    private final boolean[][] adjacency;

    public Graph(boolean[][] adjacency, boolean directed) {
        Objects.requireNonNull(adjacency, "adjacency");
        if (adjacency.length == 0) throw new IllegalArgumentException("A graph needs at least one vertex");
        this.adjacency = new boolean[adjacency.length][adjacency.length];
        for (int row = 0; row < adjacency.length; row++) {
            if (adjacency[row] == null || adjacency[row].length != adjacency.length) throw new IllegalArgumentException("Adjacency matrix must be square");
            System.arraycopy(adjacency[row], 0, this.adjacency[row], 0, adjacency.length);
        }
        if (!directed) {
            for (int row = 0; row < adjacency.length; row++) for (int column = row + 1; column < adjacency.length; column++) {
                if (adjacency[row][column] != adjacency[column][row]) throw new IllegalArgumentException("Undirected adjacency matrix must be symmetric");
            }
        }
        this.directed = directed;
    }

    public static Graph empty(int vertices, boolean directed) {
        if (vertices <= 0) throw new IllegalArgumentException("Vertex count must be positive");
        return new Graph(new boolean[vertices][vertices], directed);
    }

    public static Graph fromEdges(int vertices, boolean directed, int[][] edges) {
        if (vertices <= 0) throw new IllegalArgumentException("Vertex count must be positive");
        boolean[][] adjacency = new boolean[vertices][vertices];
        for (int[] edge : Objects.requireNonNull(edges, "edges")) {
            if (edge == null || edge.length != 2) throw new IllegalArgumentException("Every edge needs two vertices");
            checkVertex(edge[0], vertices); checkVertex(edge[1], vertices);
            adjacency[edge[0]][edge[1]] = true;
            if (!directed) adjacency[edge[1]][edge[0]] = true;
        }
        return new Graph(adjacency, directed);
    }

    public int vertexCount() { return adjacency.length; }
    public boolean isDirected() { return directed; }
    public boolean hasEdge(int from, int to) { checkVertex(from); checkVertex(to); return adjacency[from][to]; }

    public int edgeCount() {
        int count = 0;
        for (int from = 0; from < vertexCount(); from++) for (int to = 0; to < vertexCount(); to++) if (adjacency[from][to]) count++;
        if (!directed) {
            int loops = 0;
            for (int vertex = 0; vertex < vertexCount(); vertex++) if (adjacency[vertex][vertex]) loops++;
            count = (count - loops) / 2 + loops;
        }
        return count;
    }

    public Graph withEdge(int from, int to) {
        checkVertex(from); checkVertex(to);
        boolean[][] result = copy();
        result[from][to] = true;
        if (!directed) result[to][from] = true;
        return new Graph(result, directed);
    }

    public Graph withoutEdge(int from, int to) {
        checkVertex(from); checkVertex(to);
        boolean[][] result = copy();
        result[from][to] = false;
        if (!directed) result[to][from] = false;
        return new Graph(result, directed);
    }

    public List<Integer> successors(int vertex) {
        checkVertex(vertex);
        List<Integer> result = new ArrayList<>();
        for (int candidate = 0; candidate < vertexCount(); candidate++) if (adjacency[vertex][candidate]) result.add(candidate);
        return Collections.unmodifiableList(result);
    }

    public List<Integer> predecessors(int vertex) {
        checkVertex(vertex);
        List<Integer> result = new ArrayList<>();
        for (int candidate = 0; candidate < vertexCount(); candidate++) if (adjacency[candidate][vertex]) result.add(candidate);
        return Collections.unmodifiableList(result);
    }

    public int outDegree(int vertex) { return successors(vertex).size(); }
    public int inDegree(int vertex) { return predecessors(vertex).size(); }
    public int degree(int vertex) {
        if (directed) return inDegree(vertex) + outDegree(vertex);
        int result = outDegree(vertex);
        if (adjacency[vertex][vertex]) result++;
        return result;
    }

    public List<Integer> breadthFirst(int start) {
        checkVertex(start);
        List<Integer> order = new ArrayList<>();
        boolean[] visited = new boolean[vertexCount()];
        Deque<Integer> queue = new ArrayDeque<>();
        visited[start] = true; queue.add(start);
        while (!queue.isEmpty()) {
            int vertex = queue.remove(); order.add(vertex);
            for (int next : successors(vertex)) if (!visited[next]) { visited[next] = true; queue.add(next); }
        }
        return Collections.unmodifiableList(order);
    }

    public List<Integer> depthFirst(int start) {
        checkVertex(start);
        List<Integer> order = new ArrayList<>();
        depthFirst(start, new boolean[vertexCount()], order);
        return Collections.unmodifiableList(order);
    }

    private void depthFirst(int vertex, boolean[] visited, List<Integer> order) {
        visited[vertex] = true; order.add(vertex);
        for (int next : successors(vertex)) if (!visited[next]) depthFirst(next, visited, order);
    }

    /** Returns an inclusive shortest path, or an empty list when unreachable. */
    public List<Integer> shortestPath(int start, int target) {
        checkVertex(start); checkVertex(target);
        int[] previous = new int[vertexCount()];
        java.util.Arrays.fill(previous, -1);
        boolean[] visited = new boolean[vertexCount()];
        Deque<Integer> queue = new ArrayDeque<>();
        visited[start] = true; queue.add(start);
        while (!queue.isEmpty() && !visited[target]) {
            int vertex = queue.remove();
            for (int next : successors(vertex)) if (!visited[next]) {
                visited[next] = true; previous[next] = vertex; queue.add(next);
            }
        }
        if (!visited[target]) return Collections.emptyList();
        List<Integer> path = new ArrayList<>();
        for (int vertex = target; vertex != -1; vertex = previous[vertex]) path.add(vertex);
        Collections.reverse(path);
        return Collections.unmodifiableList(path);
    }

    /** Floyd-Warshall distances, with -1 representing unreachable pairs. */
    public int[][] distances() {
        int n = vertexCount();
        int[][] result = new int[n][n];
        for (int row = 0; row < n; row++) for (int column = 0; column < n; column++) result[row][column] = row == column ? 0 : adjacency[row][column] ? 1 : -1;
        for (int middle = 0; middle < n; middle++) for (int from = 0; from < n; from++) for (int to = 0; to < n; to++) {
            if (result[from][middle] >= 0 && result[middle][to] >= 0) {
                int distance = result[from][middle] + result[middle][to];
                if (result[from][to] < 0 || distance < result[from][to]) result[from][to] = distance;
            }
        }
        return result;
    }

    public boolean[][] transitiveClosure() {
        boolean[][] result = copy();
        for (int vertex = 0; vertex < vertexCount(); vertex++) result[vertex][vertex] = true;
        for (int middle = 0; middle < vertexCount(); middle++) for (int from = 0; from < vertexCount(); from++) for (int to = 0; to < vertexCount(); to++)
            result[from][to] |= result[from][middle] && result[middle][to];
        return result;
    }

    /** Weak components for directed graphs, ordinary components for undirected graphs. */
    public List<List<Integer>> connectedComponents() {
        List<List<Integer>> result = new ArrayList<>();
        boolean[] visited = new boolean[vertexCount()];
        for (int start = 0; start < vertexCount(); start++) if (!visited[start]) {
            List<Integer> component = new ArrayList<>();
            Deque<Integer> queue = new ArrayDeque<>(); visited[start] = true; queue.add(start);
            while (!queue.isEmpty()) {
                int vertex = queue.remove(); component.add(vertex);
                for (int candidate = 0; candidate < vertexCount(); candidate++) {
                    if (!visited[candidate] && (adjacency[vertex][candidate] || adjacency[candidate][vertex])) { visited[candidate] = true; queue.add(candidate); }
                }
            }
            result.add(Collections.unmodifiableList(component));
        }
        return Collections.unmodifiableList(result);
    }

    public boolean isConnected() { return connectedComponents().size() == 1; }

    /** Kahn topological order; rejects undirected or cyclic graphs. */
    public List<Integer> topologicalOrder() {
        if (!directed) throw new IllegalStateException("Topological order is defined for directed graphs");
        int[] indegree = new int[vertexCount()];
        PriorityQueue<Integer> ready = new PriorityQueue<>();
        for (int vertex = 0; vertex < vertexCount(); vertex++) { indegree[vertex] = inDegree(vertex); if (indegree[vertex] == 0) ready.add(vertex); }
        List<Integer> result = new ArrayList<>();
        while (!ready.isEmpty()) {
            int vertex = ready.remove(); result.add(vertex);
            for (int next : successors(vertex)) if (--indegree[next] == 0) ready.add(next);
        }
        if (result.size() != vertexCount()) throw new IllegalStateException("Graph contains a directed cycle");
        return Collections.unmodifiableList(result);
    }

    public boolean hasCycle() {
        if (directed) {
            int[] state = new int[vertexCount()];
            for (int vertex = 0; vertex < vertexCount(); vertex++) if (directedCycle(vertex, state)) return true;
            return false;
        }
        boolean[] visited = new boolean[vertexCount()];
        for (int vertex = 0; vertex < vertexCount(); vertex++) if (!visited[vertex] && undirectedCycle(vertex, -1, visited)) return true;
        return false;
    }

    private boolean directedCycle(int vertex, int[] state) {
        if (state[vertex] == 1) return true;
        if (state[vertex] == 2) return false;
        state[vertex] = 1;
        for (int next : successors(vertex)) if (directedCycle(next, state)) return true;
        state[vertex] = 2;
        return false;
    }

    private boolean undirectedCycle(int vertex, int parent, boolean[] visited) {
        visited[vertex] = true;
        for (int next : successors(vertex)) {
            if (!visited[next] && undirectedCycle(next, vertex, visited)) return true;
            if (visited[next] && next != parent) return true;
        }
        return false;
    }

    public <T> Matrix<T> adjacencyMatrix(Field<T> field) {
        List<List<T>> result = new ArrayList<>(vertexCount());
        for (int row = 0; row < vertexCount(); row++) {
            List<T> values = new ArrayList<>(vertexCount());
            for (int column = 0; column < vertexCount(); column++) values.add(adjacency[row][column] ? field.one() : field.zero());
            result.add(values);
        }
        return new Matrix<>(field, result);
    }

    /** Out-degree Laplacian D-A (ordinary Laplacian for undirected graphs). */
    public <T> Matrix<T> laplacianMatrix(Field<T> field) {
        Matrix<T> result = adjacencyMatrix(field).negate();
        for (int vertex = 0; vertex < vertexCount(); vertex++) result = result.with(vertex, vertex,
                field.add(result.get(vertex, vertex), field.fromLong(outDegree(vertex))));
        return result;
    }

    /** Number of walks of an exact length, represented in the requested field. */
    public <T> Matrix<T> walkCounts(Field<T> field, int length) {
        if (length < 0) throw new IllegalArgumentException("Length must be non-negative");
        return adjacencyMatrix(field).pow(length);
    }

    private boolean[][] copy() {
        boolean[][] result = new boolean[vertexCount()][vertexCount()];
        for (int row = 0; row < vertexCount(); row++) System.arraycopy(adjacency[row], 0, result[row], 0, vertexCount());
        return result;
    }
    private void checkVertex(int vertex) { checkVertex(vertex, vertexCount()); }
    private static void checkVertex(int vertex, int count) {
        if (vertex < 0 || vertex >= count) throw new IndexOutOfBoundsException("Vertex: " + vertex);
    }

    @Override public boolean equals(Object object) {
        return this == object || object instanceof Graph && directed == ((Graph) object).directed && java.util.Arrays.deepEquals(adjacency, ((Graph) object).adjacency);
    }
    @Override public int hashCode() { return 31 * Boolean.valueOf(directed).hashCode() + java.util.Arrays.deepHashCode(adjacency); }
}
