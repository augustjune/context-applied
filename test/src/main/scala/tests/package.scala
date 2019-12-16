package object tests {

  trait ~>[F[_], G[_]] {
    def apply[A](fa: F[A]): G[A]
  }

  trait Functor[F[_]] {
    def map[A, B](fa: F[A])(f: A => B): F[B]
  }

  trait Monad[F[_]] extends Functor[F] {
    def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]

    def pure[A](a: A): F[A]
  }

  trait Traverse[F[_]] extends Functor[F] {
    def traverse[G[_], A, B](fa: F[A])(f: A => G[B]): G[F[A]]
  }

  trait Console[F[_]] {
    def put(s: String): F[Unit]

    def read: F[String]
  }
}
