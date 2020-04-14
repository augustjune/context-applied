package tests

object dotnotation {

  trait Witness {
    type T
    val value: T {}
  }

  object Witness {
    type Aux[T0] = Witness { type T = T0 }
  }

  class SomeClass[A <: String: Witness.Aux] {
    def fn: String = A.value
  }
}
