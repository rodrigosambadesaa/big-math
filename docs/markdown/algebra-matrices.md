# Algebra, matrices and tensors

## Design

`Matrix<T>`, `Tensor3<T>` and `Polynomial<T>` are immutable and use an explicit `Field<T>`.
The field owns arithmetic, equality, precision and zero semantics. This avoids the central
limitation of a matrix implementation tied to one numeric class: algorithms do not inspect,
cast or convert their entries.

Built-in fields:

| Field | Element | Arithmetic |
| --- | --- | --- |
| `BigDecimalField` | `BigDecimal` | Configurable `MathContext` and tolerance |
| `BigRationalField` | `BigRational` | Exact, normalized rational arithmetic |
| `BigComplexField` | `BigComplex` | Configurable precision, tolerance and conjugation |
| `ExpressionField` | `Expression` | Exact symbolic rational expressions |
| `ModularIntegerField` | `BigInteger` | Prime finite field GF(p) |
| `BinaryField` | `Boolean` | GF(2), using XOR and AND |

## Symbolic matrices

```java
Expression x = Expression.variable("x");
Matrix<Expression> a = new Matrix<>(ExpressionField.INSTANCE, new Expression[][] {
    {x, Expression.constant(2)},
    {Expression.ONE, Expression.constant(2)}
});

Expression determinant = a.determinant();       // 2*x - 2
Expression derivative = determinant.differentiate("x"); // 2
BigDecimal atFive = determinant.evaluate(
        Collections.singletonMap("x", new BigDecimal("5")),
        MathContext.DECIMAL128);                 // 8
```

The determinant uses a division-free expansion. It therefore returns an algebraic expression
instead of introducing floating-point values or rational denominators. Symbolic inverse and
row-reduction are also supported; as usual, selecting a symbolic pivot assumes that expression
is non-zero.

## Matrix coverage

The implementation was compared with
[`rodrigosambadesaa/BibliotecaMatrices`](https://github.com/rodrigosambadesaa/BibliotecaMatrices)
at commit `d0cc5144e619439156203ca3eb0372ef7b495da7`.

| Area | Implemented here |
| --- | --- |
| Storage | Generic immutable dense matrices, rows/columns, replacement, submatrices, reshape, flatten and concatenation |
| Arithmetic | Add/subtract, scalar, matrix, Hadamard, Kronecker, direct-sum, commutator and Frobenius inner product |
| Symmetries | Transpose, conjugate, adjoint, and symmetric/Hermitian/normal/triangular predicates |
| Elimination | RREF, rank, nullity, inverse and exact system solving |
| Determinants | Division-free determinant, minors, cofactors and adjugate |
| Factorizations | Generic LUP; `BigDecimal` Cholesky and modified Gram-Schmidt QR |
| Spectrum | Characteristic polynomial, Cayley-Hamilton verification and dominant real eigenpair |
| Least squares | Normal-equation solve and full-column-rank pseudoinverse |
| Structured factories | Zero, identity/rectangular identity, diagonal, rows, columns, Vandermonde, circulant, permutation and companion |
| Finite fields | Matrices over GF(p) and GF(2) through normal matrix algorithms |
| Rank-3 tensors | Layers/slices, elementwise operations, layer products, axis permutations, unfolding, mode products and convolution modes |

Algorithms that intrinsically need ordering, square roots or convergence are intentionally in
`BigDecimalMatrixMath`; algebraic algorithms remain generic. Advanced numerical SVD/Schur/Jordan,
matrix logarithms and HOSVD/t-SVD are not represented as exact symbolic operations and are not
silently approximated by the generic API.

## Other new mathematical areas

- `Polynomial<T>`: arithmetic, powers, Horner evaluation, composition, formal derivative and
  integral, Euclidean division and monic GCD.
- `BigIntegerMath`: factorials, permutations, binomial/multinomial/Catalan/Stirling/Bell/partition
  numbers, fast Fibonacci/Lucas, GCD/LCM/Bézout, modular inverses, Chinese remainder theorem,
  prime sieves/factorization, Euler's totient, Möbius and exact integer square roots.
- `BigDecimalStatistics`: arithmetic/geometric/harmonic means, population/sample variance and
  deviation, covariance, correlation, percentiles, standardization and linear regression.
- `BigDecimalCalculus`: five-point differentiation, second derivatives, trapezoidal and Simpson
  integration, bisection/Newton roots and fourth-order Runge-Kutta ODE integration.
- `Graph`: immutable directed/undirected graphs, degrees, BFS/DFS, shortest paths, all-pairs
  distances, transitive closure, components, topological order, cycle detection, and adjacency,
  Laplacian and exact-length walk-count matrices over any configured field.
