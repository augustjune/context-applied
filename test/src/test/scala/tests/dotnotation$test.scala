package tests

import org.junit.Test
import tests.dotnotation.{SomeClass, Witness}

class dotnotation$test {

  @Test
  def run(): Unit = {
    implicit val witness: Witness.Aux[Unit] = new Witness {
      type T = Unit
      val value: T = ()
    }

    new SomeClass[Unit]().fn
  }
}
