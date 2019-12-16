package tests

object ignore {

  def declarations[F[_] : Console, G[_] : Console] = {
    val F = "Sda"
    G.put(F)
  }

  def arguments[F[_] : Console, G[_] : Console](F: String) = {
    G.put(F)
  }
}
