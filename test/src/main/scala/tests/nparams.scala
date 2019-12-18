package tests

/**
 * Checks if the plugin is able to handle more than one
 * type constructor parameter with own context bounds
 */
object nparams {

  def two[F[_] : Console, G[_] : Console] = {
    F.read
    G.put("sda")
  }
}
