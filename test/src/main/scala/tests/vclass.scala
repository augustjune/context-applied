package tests

/**
 * Checks if specifying context bounds in value classes does not break the plugin
 */
class vclass(private val b: Boolean) extends AnyVal {
  def fn[F[_] : Console] = ""
}
