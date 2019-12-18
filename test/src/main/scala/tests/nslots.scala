package tests

/**
 * Checks if the plugin is able to handle kind-projector and type lambda syntax
 */
object nslots {

  trait Console2[F[_], R[_]] {
    def put(s: String): F[Unit]

    def read: R[String]
  }

  def lambda[F[_] : ({type λ[T[_]] = Console2[T, List]})#λ]: F[Unit] = F.put("")

  def projector[F[_] : Console2[*[_], List]]: List[String] = F.read
}
