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
  case class LambdaClass[F[_] : ({type λ[T[_]] = Console2[T, List]})#λ]()

  def projector[F[_] : Console2[*[_], List]]: List[String] = F.read
  case class ProjectorClass[F[_] : Console2[*[_], List]]()

  def projectorLambda[F[_]: λ[T[_] => Console[Trace[T, *]]]]: Trace[F, String] = F.read
  case class ProjectorLambdaClass[F[_]: λ[T[_] => Console[Trace[T, *]]]]()
}
