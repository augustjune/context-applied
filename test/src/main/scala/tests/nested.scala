package tests

/**
 * Checks if the plugin provides the syntax for the nested functions
 */
object nested {

  implicit val listFunctor: Functor[List] = new Functor[List] {
    def map[A, B](fa: List[A])(f: A => B): List[B] = fa.map(f)
  }

  def fn[F[_]: Console, K[_]: Console](nt: F ~> List) = {
    def fk[G[_]: Functor](fa: G[String]) = G.map(fa)(_ => 1)

    fk(nt(F.read))
  }

}
