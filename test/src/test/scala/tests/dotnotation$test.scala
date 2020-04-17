package tests

import org.junit.Test
import tests.dotnotation.{SomeClass, Witness}

class dotnotation$test {

  @Test
  def run(): Unit = {
    implicit def m[A]: Monoid[A] = new Monoid[A] {}
    implicit val witness: Witness.Aux[Unit] = new Witness {
      type T = Unit
      val value: T = ()
    }

    new SomeClass[Unit]().fn
  }
}
