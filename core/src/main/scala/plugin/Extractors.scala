package plugin

import scala.reflect.internal.SymbolTable

trait Extractors {
  val global: SymbolTable

  import global._

  case class ContextBound(typ: TypeName, evs: List[Evidence])

  //Evidence(Traverse[A],A,evidence$5)
  case class Evidence(name: AppliedTypeTree, typ: TypeName, variable: String)

  object VClass {
    /**
     * Matches a tree which represents a value class
     */
    def unapply(tree: Tree): Option[Unit] = tree match {
      case ClassDef(_, _, _, Template(parents, _, _))
        if parents.exists { case Ident(tpnme.AnyVal) => true; case _ => false } =>
        Some(())

      case _ => None
    }
  }

  object ContextBounds {
    /**
     * Matches an occurrence of context bounds (in function or class/trait constructor), e.g.:
     * def fn[A: B]
     * class Foo[A: B]
     */
    def unapply(tree: Tree): Option[List[ContextBound]] = tree match {
      case DefDef(_, _, tparams, vparamss, _, _) =>
        val tpars = tparams.collect { case TypeDef(_, tp, _, _) => tp }
        val evs = vparamss.lastOption.toList.flatMap { params =>
          params.collect { case Evidence(e) => e }
        }

        val bounds = matchBounds(tpars, evs)
        if (bounds.isEmpty) None
        else Some(bounds)

      case ClassDef(_, _, tparams, Template(_, _, body)) =>
        val tpars = tparams.collect { case TypeDef(_, tp, _, _) => tp }
        val evs = body.collect { case Evidence(e) => e }

        val bounds = matchBounds(tpars, evs)
        if (bounds.isEmpty) None
        else Some(bounds)

      case _ => None
    }

    private def matchBounds(tpars: List[TypeName], evidences: List[Evidence]): List[ContextBound] =
      tpars.flatMap { s =>
        val imps = evidences.filter(ev => ev.typ == s)
        if (imps.isEmpty) List()
        else List(ContextBound(s, imps))
      }
  }

  object Evidence {
    def unapply(valDef: Tree): Option[Evidence] = valDef match {
      case ValDef(mods, TermName(variable), ap @ AppliedTypeTree(_, List(Ident(typ @ TypeName(_)))), _)
        if mods.isImplicit => Some(Evidence(ap, typ, variable))
      case _ => None
    }
  }

}
