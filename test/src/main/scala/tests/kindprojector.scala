package tests

object kindprojector {

  trait Console2[F[_], R[_]] {
    def put(s: String): F[Unit]

    def read: R[String]
  }

  def f2[F[_]: Console2[*[_], List]]: List[String] = F.read
}
