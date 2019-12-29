package tests

class classbounds[F[_]: Monad: Console] {

  def fn: F[Int] = F.pure(12)

  def fn2[G[_]: Traverse: Monad]: F[G[Int]] =
    G.traverse(G.pure("Hello"))(s => F.pure(s.length))

  def fn3: F[Int] = F.map(F.read)(_.size)
}
