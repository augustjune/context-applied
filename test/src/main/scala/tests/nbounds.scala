package tests

/**
 * Checks if the plugin is able to handle more than one algebra
 */
object nbounds {

  def combined[F[_]: Monad: Traverse] = {
    F.traverse(F.pure(12))(_ => F.pure(12))
  }

  def common[F[_]: Monad: Traverse] = {
    F.map(F.pure(12))(_ + 1)
  }
}
