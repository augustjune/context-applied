package plugin

import scala.reflect.internal.Flags._
import scala.reflect.internal.SymbolTable

trait Constructors {
  val global: SymbolTable

  import global._

  /**
   * New empty trait declaration.
   * Such trait doesn't contain any methods.
   *
   * @param name Name of the trait
   * @param priv Determines whether this object should have private modifier
   */
  def newEmptyTrait(name: TypeName, priv: Boolean): Tree =
    ClassDef(
      if (priv) Modifiers(ABSTRACT | INTERFACE | LOCAL | DEFAULTPARAM / TRAIT)
      else Modifiers(SYNTHETIC | ARTIFACT | ABSTRACT | INTERFACE | DEFAULTPARAM / TRAIT),
      name,
      List(),
      Template(List(Ident(tpnme.AnyRef)), noSelfType, List())
    )

  /**
   * New abstract class declaration.
   * E.g.: abstract class E {}
   *
   * @param name   Name of the class
   * @param parent Classes's parent
   * @param inside Method that this class contains
   */
  def newAbstractClass(name: String, parent: Option[String], inside: DefDef, priv: Boolean): Tree =
    ClassDef(
      if (priv) Modifiers(PRIVATE | LOCAL | SYNTHETIC | ARTIFACT | ABSTRACT)
      else Modifiers(SYNTHETIC | ARTIFACT | ABSTRACT),
      TypeName(name),
      List(),
      Template(List(Ident(parent.map(TypeName(_)).getOrElse(tpnme.AnyRef))), noSelfType, List(constructor, inside))
    )

  /**
   * New object declaration
   * E.g.: object Module {}
   *
   * @param name   Name of the object
   * @param parent Object's parent
   * @param inside Method that this trait contains
   * @param priv   Determines whether this object should have private modifier
   * @return
   */
  def newObject(name: String, parent: Option[String], inside: DefDef, priv: Boolean): ModuleDef =
    ModuleDef(
      if (priv) Modifiers(PRIVATE | LOCAL | SYNTHETIC | ARTIFACT)
      else Modifiers(SYNTHETIC | ARTIFACT),
      TermName(name),
      Template(
        List(Ident(parent.map(TypeName(_)).getOrElse(tpnme.AnyRef))),
        noSelfType,
        List(constructor, inside)
      )
    )

  /**
   * New implicit conversion method declaration.
   * This method always returns constant value.
   * Example: def magicInt(i: YourClass): Int = 12
   *
   * @param fromT Argument type
   * @param resT  Return type
   * @param resV  Constant value to be returned.
   */
  def newImplicitConversion(fromT: TypeName, resT: AppliedTypeTree, resV: String): DefDef =
    DefDef(
      Modifiers(IMPLICIT | SYNTHETIC | ARTIFACT),
      TermName(s"${fromT}_${resT.tpt}"),
      List(),
      List(List(ValDef(Modifiers(PARAM | SYNTHETIC | ARTIFACT), TermName("e"), Ident(fromT), EmptyTree))),
      resT,
      Ident(TermName(resV))
    )

  private def constructor: DefDef =
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
}