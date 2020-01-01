package tests

object virtual {

  trait T {
    def fn[F[_]: Monad]: F[Unit]
  }

  abstract class AC[F[_]: Monad] {
    def concrete: F[String] = F.pure("")

    def fn[G[_]: Console]: F[Unit]
  }
}
