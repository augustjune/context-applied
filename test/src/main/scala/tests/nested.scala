package tests

import cats.{Functor, ~>}
import cats.instances.list._


object nested {

  def fn[F[_]: Console, K[_]: Console](nt: F ~> List) = {
    def fk[G[_]: Functor](fa: G[String]) = G.map(fa)(_ => 1)

    fk(nt(F.read))
  }

}
