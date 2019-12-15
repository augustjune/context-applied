package tests

object nparams {

  def two[F[_] : Console, G[_] : Console] = {
    F.read
    G.put("sda")
  }
}
