package tests

/**
 * Checks if the plugin ignores the cases when it shouldn't be applied
 */
object ignore {

  def declarations[F[_] : Console, G[_] : Console] = {
    val F = "Sda"
    G.put(F)
  }

  def arguments[F[_] : Console, G[_] : Console](F: String) = {
    G.put(F)
  }
}
