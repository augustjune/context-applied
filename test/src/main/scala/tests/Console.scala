package tests

trait Console[F[_]] {
  def put(s: String): F[Unit]

  def read: F[String]
}
