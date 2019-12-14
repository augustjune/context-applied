package plugin

import scala.tools.nsc
import nsc.Global
import nsc.plugins.Plugin
import nsc.plugins.PluginComponent
import nsc.transform.Transform
import nsc.transform.TypingTransformers
import nsc.ast.TreeDSL

class ContextApplied(val global: Global) extends Plugin {
  val name = "context-applied"
  val description = "Apply your context"
  val components = List(new ContextPlugin(this, global))
}

class ContextPlugin(plugin: Plugin, val global: Global)
  extends PluginComponent with Transform with TypingTransformers with TreeDSL {

  import global._

  val runsAfter = List("parser")
  override val runsBefore = List("namer")
  val phaseName = "context-applied"

  lazy val useAsciiNames: Boolean =
    System.getProperty("kp:genAsciiNames") == "true"

  def newTransformer(unit: CompilationUnit): MyTransformer =
    new MyTransformer(unit)

  class MyTransformer(unit: CompilationUnit) extends TypingTransformer(unit) {
    val global: ContextPlugin.this.global.type = ContextPlugin.this.global

    // The transform method -- this is where the magic happens.
    override def transform(tree: Tree): Tree = {
      val result = tree match {
        case ContextBounds(bounds) =>
          println(s"Discovered context bounds: $bounds")
          super.transform(tree)
        case _ => super.transform(tree)
      }

      // cache the result so we don't have to recompute it again later.
      result
    }
  }

  object ContextBounds {
    def unapply(tree: Tree): Option[List[ContextBound]] = tree match {
      case DefDef(mods, name, tparams, vparamss, tpt, rhs) =>
        val tpars = tparams.collect { case TypeDef(_, TypeName(str), _, _) => str }
        val evs = vparamss.lastOption.toList.flatMap { params => params.collect { case Evidence(e) => e } }
        val bounds = tpars.flatMap { s =>
          val imps = evs.filter(ev => ev.typ == s)
          if (imps.isEmpty) Nil
          else List(ContextBound(s, imps))
        }
        if (bounds.isEmpty) None
        else Some(bounds)

      case _ => None
    }
  }

  case class ContextBound(typ: String, evs: List[Evidence])

  object Evidence {
    def unapply(valDef: ValDef): Option[Evidence] = valDef match {
      case ValDef(mods, TermName(variable), AppliedTypeTree(Ident(instanceType), List(Ident(TypeName(typ)))), _)
        if mods.isImplicit => Some(Evidence(instanceType.decoded, typ, variable))
      case _ => None
    }
  }

  case class Evidence(name: String, typ: String, variable: String)

}
