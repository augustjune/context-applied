package tests

object basic {
  def block[F[_]: Console] = {
    val x = "das"
    F.put(x)
  }

  def appl[F[_]: Console] = F.read
}
