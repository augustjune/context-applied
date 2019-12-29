package tests

class classbounds[F[_]: Monad] {

  def fn: F[Int] = F.pure(12)

  def fn2[G[_]: Traverse: Monad]: F[G[Int]] =
    G.traverse(G.pure("Hello"))(s => F.pure(s.length))
}
