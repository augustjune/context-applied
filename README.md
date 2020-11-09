context-applied
=============

[![Build Status](https://travis-ci.com/augustjune/context-applied.svg?branch=master)](https://travis-ci.com/augustjune/context-applied)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.augustjune/context-applied_2.13/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.augustjune/context-applied_2.13)

### Overview
**context-applied** is a Scala compiler plugin that gives you a handle to the value 
that has the abilities specified by type parameter context bounds. 

Example: 
```scala
def fn[F[_]: Monad]: F[Int] = F.pure(12)
``` 

This scales across multiple contexts as well as multiple type parameters:
```scala
def fn[F[_]: Applicative: Traverse, G[_]: Applicative]: G[F[Int]] = 
  F.traverse(F.pure(""))(s => G.pure(s.size))
```

*This doesn't require any type class specific syntax nor "summoner" method*.

In fact it is achieved by introducing implicit conversions to the 
appropriate value from the implicit scope. 

Roughly speaking, you can pretend like you have a value named after the type parameter 
of the type that combines specified contexts: 
```scala
def fn[A: B: C: D] = {
  val A: B[A] with C[A] with D[A] = ???

// In reality A can be either B[A] or C[A] or D[A] in a particular moment
}
```

### Usage
Plugin is available for Scala 2.11, 2.12 and 2.13.
```scala
addCompilerPlugin("org.augustjune" %% "context-applied" % "0.1.4")
```

### Use cases
1. **Custom algebras.**
    ```scala
    trait Console[F[_]] {
      def read: F[String]
      
      def write(s: String): F[Unit]
    }
    
    def reply[F[_]: Console: FlatMap]: F[String] =
      for {
        s <- F.read
        _ <- F.write(s)
      } yield s
    ```

1. **Non-linear type class hierarchy.** 

    If you specify two algebras that derive from the same parent, 
    because of ambiguity you cannot use that parent's syntax.
    Typical example of this problem is `Monad` and `Traverse` from *cats* 
    since they are both subtypes of `Functor`.
    ```scala
     import cats.syntax.all._
     def fn[F[_]: Monad: Traverse](fs: F[String]) = 
       fs.map(_.size) // Compiler error
     ```
    With **context-applied** the first context that has *map* method
    in function's context bounds is used.
    ```scala
    def fn[F[_]: Monad: Traverse](fs: F[String]) = 
       F.map(fs)(_.size)  // Monad's map is used
    ```

### Supported features
1. Kind-projector support.
   ```scala
   def fn[F[_]: ApplicativeError[*[_], Throwable]]: F[Nothing] = 
     F.raiseError(new RuntimeException)
   ```
1. Type parameters of any kinds. 
    ```scala
    def fn[F[_]: Applicative, B[_, _]: Bifunctor, A: Monoid] = {
      val fa: F[A] = F.pure(A.empty)
      val rf: Functor[B[A, *]] = B.rightFunctor[A]
    }
    ```
1. Nested scopes.

    Syntax is available for any context bounds: in classes, methods and nested methods.
    ```scala
    class Foo[F[_]: Applicative] {
   
      def bar[A: Monoid] = {
        def baz[G[_]: Functor](ga: G[A]) = 
          G.map(ga)(F.pure)
          
        baz(List(A.empty))
      }
    }
    ```
 
### Special cases
Since **context-applied** introduces additional syntax to your program 
it is important not to break any existing code or change its meaning.
For this reason there are cases when the plugin 
just gracefully skips parts of the program.
It happens when:

1. Name of type parameter is already taken.
    ```scala
    class Foo[F[_]: Functor] {
      val F: Int = 12                  // F: Functor[F] will not be introduced inside Foo 
      def f1[A: Monoid](A: Int) = ()   // A: Monoid[A] will not be introduced inside f1
      def f2[F[_]: Monad] = ???        // F: Monad[F] will be available inside f2 as local value
    }
    ```
1. Inside value classes.
    ```scala
    class Foo(val dummy: Boolean) extends AnyVal {
      def fn[F[_]: Monad] = ???        // F: Monad[F] will not be introduced inside fn
    }
    ```
