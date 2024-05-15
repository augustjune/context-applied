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

  override val runsBefore = List("namer")
  val runsAfter = List("parser")
  /**
   * Name of the phase starts with 'x' to make it run
   * after kind-projector phase if such exists
   */
  val phaseName = "xcontext-applied"

  def newTransformer(unit: CompilationUnit): MyTransformer =
    new MyTransformer(unit)

  class MyTransformer(unit: CompilationUnit) extends TypingTransformer(unit) with Extractors with Constructors {
    val global: ContextPlugin.this.global.type = ContextPlugin.this.global
    private var inVclass: Boolean = false
    private var resultTypeLambdas = 0L

    override def transform(tree: Tree): Tree =
      tree match {
        case VClass(_) =>
          // Going to wait until the first complain with a broken codebase
          inVclass = true
          val t = super.transform(tree)
          inVclass = false
          t

        case DefDef(mods, _, _, _, _, _)
          if mods.isDeferred =>
          super.transform(tree)

        case ContextBounds(bounds) =>
          if (inVclass) super.transform(tree)
          else super.transform(injectComponents(tree, bounds))
        case _ => super.transform(tree)
      }

    private def injectComponents(tree: Tree, bounds: List[ContextBound]): Tree =
      tree match {
        case d: DefDef =>
          d.rhs match {
            case b: Block =>
              val legalBounds = bounds.filterNot(cb => containsDeclaration(cb.typ.decode, b.stats ++ d.vparamss.flatten))
              val insert = legalBounds.flatMap(createComponents(_, inclass = false, None))
              d.copy(rhs = b.copy(stats = insert ::: b.stats))

            case value =>
              val legalBounds = bounds.filterNot(cb => containsDeclaration(cb.typ.decode, d.vparamss.flatten))
              val insert = legalBounds.flatMap(createComponents(_, inclass = false, None))
              d.copy(rhs = Block(insert, value))
          }

        case d @ ClassDef(_, name, _, Template(_, _, body)) =>
          val legalBounds = bounds.filterNot(cb => containsDeclaration(cb.typ.decode, body))
          val insert = legalBounds.flatMap(createComponents(_, inclass = true, className = Some(name.decode)))
          val updatedBody = insertAfterConstructor(body, insert)
          d.copy(impl = d.impl.copy(body = updatedBody))

        case _ => tree
      }

    private def containsDeclaration(s: String, trees: List[Tree]): Boolean =
      trees.exists {
        case ValOrDefDef(_, TermName(str), _, _) if str == s => true
        case _ => false
      }

    private def createComponents(bound: ContextBound, inclass: Boolean, className: Option[String]): List[Tree] = {
      val trees = new ListBuffer[Tree]

      val empty = TypeName(s"E$$${bound.typ.decode}$$${className.getOrElse("Def")}")
      trees.append(newEmptyTrait(empty, inclass))

      val lastParent = bound.evs.tail.foldRight(Option.empty[String]) { case (ev, parent) =>
        val resTName = resultTypeName(ev.tree.tpt)
        val name = s"$resTName$$${bound.typ.decode}"
        trees.append(newAbstractClass(name, parent, newImplicitConversion(empty, resTName, ev.tree, ev.variable), inclass))
        Some(name)
      }

      val resTName = resultTypeName(bound.evs.head.tree.tpt)
      val moduleName = s"$resTName$$${bound.typ.decode}"
      val module = newObject(moduleName, lastParent, newImplicitConversion(empty, resTName, bound.evs.head.tree, bound.evs.head.variable), inclass)
      trees.append(module)

      val imp = importModule(moduleName)
      trees.append(imp)

      trees.append(nullVal(bound.typ.decode, empty, inclass))

      trees.toList
    }

    /**
     * Solves the problem with the name of applied type tree constructed using type lambda.
     * For example, following type name will be simplified to meet the requirements of scala naming:
     * {{{scala.AnyRef {
     *     type ?[T[_]] = Console2[T, List]
     *   }#?[F]
     * }}}
     */
    private def resultTypeName(t: Tree): String =
      if (!t.toString.contains("{")) t.toString.replace(".", "")
      else {
        resultTypeLambdas += 1
        s"TLambda$resultTypeLambdas"
      }

    private def nullVal(name: String, typeName: TypeName, inclass: Boolean): Tree =
      if (inclass) ValDef(Modifiers(PRIVATE | LOCAL | SYNTHETIC | ARTIFACT), TermName(name), Ident(typeName), Literal(Constant(null)))
      else ValDef(Modifiers(SYNTHETIC | ARTIFACT), TermName(name), Ident(typeName), Literal(Constant(null)))

    private def importModule(name: String): Tree =
      Import(Ident(TermName(name)), ImportSelector.wildList)

    private def insertAfterConstructor(body: List[Tree], insert: List[Tree]): List[Tree] =
      body match {
        case DefDef(_, termNames.CONSTRUCTOR, _, _, _, _) :: t => body.head :: insert ::: t
        case h :: t => h :: insertAfterConstructor(t, insert)
        case Nil => insert
      }
  }

}
