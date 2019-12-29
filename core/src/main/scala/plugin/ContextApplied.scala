package plugin

import scala.tools.nsc
import nsc.Global
import nsc.plugins.Plugin
import nsc.plugins.PluginComponent
import nsc.transform.Transform
import nsc.transform.TypingTransformers
import nsc.ast.TreeDSL
import scala.collection.mutable.ListBuffer
import scala.reflect.internal.Flags._

class ContextApplied(val global: Global) extends Plugin {
  val name = "context-applied"
  val description = "May the F be with you"
  val components = List(new ContextPlugin(this, global))
}

class ContextPlugin(plugin: Plugin, val global: Global)
  extends PluginComponent with Transform with TypingTransformers with TreeDSL {

  import global._

  val runsAfter = List("parser")
  override val runsBefore = List("namer")

  /**
   * Name of the phase starts with 'x' to make it run
   * after kind-projector phase if such exists
   */
  val phaseName = "xcontext-applied"

  def newTransformer(unit: CompilationUnit): MyTransformer =
    new MyTransformer(unit)

  class MyTransformer(unit: CompilationUnit) extends TypingTransformer(unit) {
    val global: ContextPlugin.this.global.type = ContextPlugin.this.global

    def nullVal(name: String, typeName: TypeName): Tree =
      ValDef(Modifiers(SYNTHETIC | ARTIFACT), TermName(name), Ident(typeName), Literal(Constant(null)))

    def defineTrait(name: String, parent: Option[String], inside: DefDef): Tree =
      ClassDef(
        Modifiers(SYNTHETIC | ARTIFACT | ABSTRACT),
        TypeName(name),
        List(),
        Template(List(Ident(parent.map(TypeName(_)).getOrElse(tpnme.AnyRef))), noSelfType, List(constructor, inside))
      )

    def constructor: DefDef =
      DefDef(
        Modifiers(SYNTHETIC | ARTIFACT),
        termNames.CONSTRUCTOR,
        List(),
        List(List()),
        TypeTree(),
        Block(
          List(
            Apply(
              Select(
                Super(
                  This(typeNames.EMPTY),
                  typeNames.EMPTY
                ),
                termNames.CONSTRUCTOR
              ),
              List()
            )
          ),
          Literal(Constant(()))
        )
      )

    def defineEmptyTrait(tpName: TypeName): Tree =
      ClassDef(
        Modifiers(SYNTHETIC | ARTIFACT | ABSTRACT | INTERFACE | DEFAULTPARAM / TRAIT),
        tpName,
        List(),
        Template(List(Ident(tpnme.AnyRef)), noSelfType, List())
      )

    def defineObject(name: String, parent: Option[String], inside: DefDef): ModuleDef =
      ModuleDef(
        Modifiers(SYNTHETIC | ARTIFACT),
        TermName(name),
        Template(
          List(Ident(parent.map(TypeName(_)).getOrElse(tpnme.AnyRef))),
          noSelfType,
          List(constructor, inside)
        )
      )

    def importModule(name: String): Tree =
      Import(Ident(TermName(name)), List(ImportSelector.wild))

    def defineImplicitConv(fromT: TypeName, resT: AppliedTypeTree, resV: String): DefDef =
      DefDef(
        Modifiers(IMPLICIT | SYNTHETIC | ARTIFACT),
        TermName(s"${fromT}_${resT.tpt}"),
        List(),
        List(List(ValDef(Modifiers(PARAM | SYNTHETIC | ARTIFACT), TermName("e"), Ident(fromT), EmptyTree))),
        resT,
        Ident(TermName(resV))
      )

    def createTraits(bound: ContextBound): List[Tree] = {
      val trees = new ListBuffer[Tree]

      val empty = TypeName("E$" + bound.typ.decode)
      trees.append(defineEmptyTrait(empty))

      val lastParent = bound.evs.tail.foldRight(Option.empty[String]) { case (ev, parent) =>
        val name = s"${ev.name.tpt}$$${bound.typ.decode}"
        trees.append(defineTrait(name, parent, defineImplicitConv(empty, ev.name, ev.variable)))
        Some(name)
      }

      val moduleName = s"${bound.evs.head.name.tpt}$$${bound.typ.decode}"
      val module = defineObject(moduleName, lastParent, defineImplicitConv(empty, bound.evs.head.name, bound.evs.head.variable))
      trees.append(module)

      val imp = importModule(moduleName)
      trees.append(imp)

      trees.append(nullVal(bound.typ.decode, empty))

      trees.toList
    }

    def containsDeclaration(s: String, trees: List[Tree]): Boolean =
      trees.exists {
        case ValOrDefDef(_, TermName(str), _, _) if str == s => true
        case _ => false
      }

    def injectTransformations(tree: Tree, bounds: List[ContextBound]): Tree =
      tree match {
        case d: DefDef =>
          d.rhs match {
            case b: Block =>
              val legalBounds = bounds.filterNot(cb => containsDeclaration(cb.typ.decode, b.stats ++ d.vparamss.flatten))
              val insert = legalBounds.flatMap(createTraits)
              d.copy(rhs = b.copy(stats = insert ::: b.stats))

            case value =>
              val legalBounds = bounds.filterNot(cb => containsDeclaration(cb.typ.decode, d.vparamss.flatten))
              val insert = legalBounds.flatMap(createTraits)
              d.copy(rhs = Block(insert, value))
          }

        case d @ ClassDef(_, _, _, Template(_, _, body)) =>
          val legalBounds = bounds.filterNot(cb => containsDeclaration(cb.typ.decode, body))
          val insert = legalBounds.flatMap(createTraits)
          d.copy(impl = d.impl.copy(body = insert ::: body))

        case _ => tree
      }

    var inVclass: Boolean = false

    override def transform(tree: Tree): Tree =
      tree match {
        case VClass(_) =>
          // Seems like a horrible thing to do. Will wait until the first complain with a broken codebase
          inVclass = true
          val t = super.transform(tree)
          inVclass = false
          t
        case ContextBounds(bounds) =>
          if (inVclass) super.transform(tree)
          else super.transform(injectTransformations(tree, bounds))
        case _ => super.transform(tree)
      }
  }

  object VClass {
    def unapply(tree: Tree): Option[Unit] = tree match {
      case ClassDef(_, _, _, Template(parents, _, _))
        if parents.exists { case Ident(tpnme.AnyVal) => true; case _ => false } => Some(())

      case _ => None
    }
  }

  object ContextBounds {
    def unapply(tree: Tree): Option[List[ContextBound]] = tree match {
      case DefDef(_, _, tparams, vparamss, _, _) =>
        val tpars = tparams.collect { case TypeDef(_, tp, _, _) => tp }
        val evs = vparamss.lastOption.toList.flatMap { params => params.collect { case Evidence(e) => e } }
        val bounds = tpars.flatMap { s =>
          val imps = evs.filter(ev => ev.typ == s)
          if (imps.isEmpty) List()
          else List(ContextBound(s, imps))
        }
        if (bounds.isEmpty) None
        else Some(bounds)

      case ClassDef(_, _, tparams, Template(_, _, body)) =>
        val tpars = tparams.collect { case TypeDef(_, tp, _, _) => tp }
        val evs = body.collect { case Evidence(e) => e }
        val bounds = tpars.flatMap { s =>
          val imps = evs.filter(ev => ev.typ == s)
          if (imps.isEmpty) List()
          else List(ContextBound(s, imps))
        }
        if (bounds.isEmpty) None
        else Some(bounds)

      case _ => None
    }
  }

  case class ContextBound(typ: TypeName, evs: List[Evidence])

  object Evidence {
    def unapply(valDef: Tree): Option[Evidence] = valDef match {
      case ValDef(mods, TermName(variable), ap @ AppliedTypeTree(_, List(Ident(typ @ TypeName(_)))), _)
        if mods.isImplicit => Some(Evidence(ap, typ, variable))
      case _ => None
    }
  }

  //Evidence(Traverse[A],A,evidence$5)
  case class Evidence(name: AppliedTypeTree, typ: TypeName, variable: String)

}
