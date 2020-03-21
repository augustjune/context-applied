package tests

class parent[F[_]: Monad] {
  def work: F[Int] = F.pure(21)
}

// starting from Scala 2.13.2 on, this would result in a warning if the class for context-applied had a name
// derived only from the bounds itself as both parent and shadowed would define a class with the same name
class shadowed[F[_]: Monad] extends parent[F] {
  override def work: F[Int] = F.pure(42)
}
