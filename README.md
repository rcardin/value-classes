Whether there is something that functional programming teaches us is that we should always trust the signature of a function. Hence, when we use functional programming, we prefer to define _ad-hoc_ types to represent simple information such as an identifier, a description, or a currency. Ladies and gentlemen, please welcome the value classes.

## 1. The Problem

First, let's define an example to work with. Imagine we have an e-commerce business and that we model a product to sell using the following representation:

```scala
case class Product(code: String, description: String)
```

So, every product is represented by a `code`, some alphanumerics bar-code, and a `description`. So far, so good. Now, we want to implement a repository that retrieves products from a persistent store, and we want to allow our users to search by `code` and by `description`:

```scala
trait ProductRepository {
  def findByCode(code: String): Option[Product]
  def findByDescription(description: String): List[Product]
}
```

The problem is we cannot avoid using a description in the search by code or a code in the search by description:

```scala
import in.rcard.value.ValuesClasses.ProductRepository

val aCode = "8-000137-001620"
val aDescription = "Multivitamin and minerals"

ProductRepository.findByCode(aDescription)
ProductRepository.findByDescription(aCode)
```

The compiler cannot warn us of our errors because we represent both information, the `code` and the `description`, using simple `Strings`. This fact can lead to subtle bugs, which are very difficult to intercept also at runtime.

## 2. Using Straight Case Classes

However, we are smart developers, and we want the compiler to help us identify such errors as soon as possible. Fail fast, they said. Hence, we define two dedicated types, both for the `code`, and for the `description`:

```scala
case class BarCode(code: String)
case class Description(txt: String)
```

The new types, `BarCode` and `Description`, are nothing more than wrappers around strings. In jargon, we call them _value classes_. However, they allow us to refine the functions of our repository to avoid the previous information mismatch:

```scala
trait AnotherProductRepository {
  def findByCode(barCode: BarCode): Option[Product] =
    Some(Product(barCode.code, "Some description"))
  def findByDescription(description: Description): List[Product] =
    List(Product("some-code", description.txt))
}
```

As we can see, it is not possible anymore to search a product by code, passing a description accidentally. Indeed, we can try to pass a `Description` instead of a `BarCode` instead:

```scala
val anotherDescription = Description("A fancy description")
AnotherProductRepository.findByCode(anotherDescription)
```

As desired, the compiler diligently warns us that we are bad developers:

```shell
[error] /Users/rcardin/Documents/value-types/src/main/scala/ValuesTypes.scala:33:39: type mismatch;
[error]  found   : in.rcard.value.ValuesTypes.Description
[error]  required: in.rcard.value.ValuesTypes.BarCode
[error]   AnotherProductRepository.findByCode(anotherDescription)
[error]                                       ^
```

However, we can still create a `BarCode` using a `String` representing a description:

```scala
val aFakeBarCode: BarCode = BarCode("I am a bar-code ;)")
```

To overcome this issue we must revamp the _smart constructor_ design pattern. Though the description of the pattern is behind the scope of this article, the smart constructor pattern hides to developers the main constructor of the class, and adds a factory method that performs any needed validation. In its final form, smart constructor pattern for the `BarCode` type is the following:

```scala
sealed abstract class BarCodeWithSmartConstructor(code: String)
object BarCodeWithSmartConstructor {
  def mkBarCode(code: String): Either[String, BarCodeWithSmartConstructor] =
    Either.cond(
      code.matches("d-dddddd-dddddd"),
      new BarCodeWithSmartConstructor(code) {},
      s"The given code $code has not the right format"
    )
}

val theBarCode: Either[String, BarCodeWithSmartConstructor] =
  BarCodeWithSmartConstructor.mkBarCode("8-000137-001620")
```

Awesome! We reach our primary goal. Now, we have fewer problems to worry about...or not?

## 3. An Idiomatic Approach

The above approach resolves some problems, but it adds many others. In fact, since we are using a regular type to wrap `String`s, the compiler must instantiate a new instance of `BarCode` and `Description` more or less every time we use them. The over instantiation of objects can lead to a problem concerning performances and the amount of consumed memory.

Fortunately, Scala provides an idiomatic way to implement value classes: _Value classes_ ;). Idiomatic value classes avoid allocating runtime objects and the problems we just enumerated.

A value class is a `class` (or a `case class`) that extends the type `AnyVal`, and declares only one single public `val` attribute in the constructor. Moreover, a value class can declare `def`:

```scala
case class BarCodeValueClass(val code: String) extends AnyVal {
  def countryCode: Char = code.charAt(0)
}
```

However, value classes have many constraints: They can define `def`, but not `val` other than the constructor's attribute, cannot be extended, and cannot extend anything but _universal traits_ (for the sake of completeness, a universal trait is a trait that extends the `Any` type, has only `def` as members, and does no initialization).

The main characteristic of a value class is that the compiler treats it as a regular type at compile-time. Still, at runtime, its representation is equal to the basic type it declares in the constructor. Roughly speaking, the `BarCodeValueClass` type is transformed as a simple `String` at runtime.

Hence, due to the lack of runtime overhead, value classes are a valuable tool used in the SDK to define extension methods for basic types such as `Int`, `Double`, `Char`, etc.

### 3.1. The Problem With the Idiomatic Approach

We must remember that the JVM does not support value classes directly. So, there are cases in which the runtime environment must perform an extra allocation of memory for the wrapper type.

The Scala [documentation](https://docs.scala-lang.org/overviews/core/value-classes.html) reports the following use cases that need an extra memory allocation:

* A value class is treated as another type.
* A value class is assigned to an array.
* Doing runtime type tests, such as pattern matching.

Unfortunately, the first rule's concrete case also concerns using a value class as a type argument. Every time we want to implement the type classes pattern for a value class, we cannot avoid its instantiation as a wrapper.

In our specific case, imagine we have to implement the `cats.Eq` type class for the `BarCodeValueClass`:

```scala
implicit val eqBarCode: Eq[BarCodeValueClass] = Eq.fromUniversalEquals[BarCodeValueClass]
```

We love type classes, as functional developers, and there are many Scala libraries, such as Cats, which are based on the root of the type classes pattern.

Due to these limitations, the Scala community searched for a better solution. Ladies and gentlemen, please welcome the [NewType](https://github.com/estatico/scala-newtype) library.

## 4. The NewType Library

The NewType library allows us to create new types without the overhead of extra runtime allocations, avoiding in this way all the pitfalls of using Scala values classes:

```scala
import io.estatico.newtype.macros.newtype
@newtype case class BarCode(code: String)
```

It uses the experimental feature of Scala macros. So, it is necessary to enable it at compile-time, using the `-Ymacro-annotations`. In details, the library defines the `@newtype` annotation macro:

```sbt
libraryDependencies += "io.estatico" %% "newtype" % "0.4.4"
```

Since the macro expansion generates a new `type` definition, we must define the new type inside an `object` or a `package object`. Moreover, the library expands the class marked with the `@newtype` annotation with its underlying value at runtime. So, a `@newtype` class can't extend any other type.

Despite these limitations, the NewType library works like a charm and interacts smoothly with IDEs.

What about smart constructors? If we choose to use a `case class`, the library will generate the `apply` method in the companion object. If we want to avoid access to the `apply` method, we can use a `class` instead and create our smart constructor in a dedicated companion
object:

```scala
@newtype class BarCodeWithCompanion(code: String)

object BarCodeWithCompanion {
  def mkBarCode(code: String): Either[String, BarCodeWithCompanion] =
    Either.cond(
      code.matches("d-dddddd-dddddd"),
      code.coerce,
      s"The given code $code has not the right format")
}
```

### 4.1. Type Coercion

Wait. What is the `code.coerce` statement? Unfortunately, using a `class` instead of a `case class` removes the chance to use the `apply` method both for other developers and for us :( So, we have to use type coercion.

As we know, the Scala community considers type coercion a bad practice because it requires a cast (via the `asInstanceOf` method). The NewType library tries to make this operation safer using a type class approach.

Hence, the compiler will let us use the `coerce` extension method if and only if an instance of the `Coercible[R, N]` type class exists in the scope for types `R` and `N`. However, [it's proven](https://github.com/estatico/scala-newtype/issues/64) that the scope resolution of the `Coercible` type class (a.k.a., the coercible trick) is an operation with a very high compile-time cost and should be avoided.

### 4.2. Automatically Deriving Type Classes

The NewType library offers a very nice mechanism for deriving type classes for our `newtype`. Taking an idea coming from Haskell (as the library itself), the generated companion object of a `newtype` contains two methods, called `deriving` and `derivingK`.

We can call the first method `deriving`, if we want to derive an instance of a type class with the type parameter that is not higher kinded. For example, we want to use our `BarCodeWithCompanion` type together with the `cats.Eq` type class:

```scala
implicit val eq: Eq[BarCodeWithCompanion] = deriving
```

Whereas, if we want to derive an instance of a type class with the type parameter that is higher kinded, we can use the `derivingK` method instead.

Therefore, we can quickly implement type classes for newtypes should dispel any doubt whether using them to value classes.

However, with Dotty's advent (a.k.a. Scala 3), a new competitor came in town: The opaque types.

## 5. Scala 3 Opaque Types Aliases

As many of us might already know, Dotty is the former name of the new major version of Scala. Dotty introduces many changes and enhancements to the language. One of these is _opaque type aliases_, which addresses the same issue as the previous value classes: Creating zero-cost abstraction.

In effect, opaque types let us define a new `type` alias with an associated scope. Hence, Dotty introduces a new reserved word for opaque type aliases, `opaque`:

```scala
object BarCodes {
  opaque type BarCode = String
}
```

To create a `BarCode` from a `String`, we must provide one or many smart constructors:

```scala
object BarCodes {
  opaque type BarCode = String
  
  object BarCode {
    def mkBarCode(code: String): Either[String, BarCode] = {
      Either.cond(
        code.matches("d-dddddd-dddddd"),
        code,
        s"The given code $code has not the right format"
      )   
    }
  }
}
```

Inside the `BarCodes` scope, the `type` alias `BarCode` works as a `String`: We can assign a `String` to a variable of type `BarCode`, and we have access to the full API of `String` through an object of type `BarCode`. So, there is no distinction between the two types:

```scala
object BarCodes {
  opaque type BarCode = String
  val barCode: BarCode = "8-000137-001620"
  
  extension (b: BarCode) {
    def country: Char = b.head
  }
}
```

As we can see, if we want to add a method to an opaque type alias, we can use the extension method mechanism, which is another new feature of Dotty.

Outside the `BarCodes` scope, the compiler treats a `String` and a `BarCode` as completely different types. In other words, the `BarCode` type is opaque with respect to the `String` type outside the definition scope:

```scala
object BarCodes {
  opaque type BarCode = String
}
val anotherBarCode: BarCode = "8-000137-001620"
```

Hence, in the above example, the compiler diligently warns us that the two types are incompatible:

```shell
[error] 20 |  val anotherBarCode: BarCode = "8-000137-001620"
[error]    |                      ^^^^^^^
[error]    |                      Not found: type BarCode
```

Finally, we can say that the opaque type aliases seem to be the idiomatic replacement to the NewType
library in Dotty / Scala 3. Awesome!

## 6. Conclusion

Summing up, in this article, we have first introduced the reason why we need the so-called value classes. The first attempt to give a solution uses directly `case classes`. However, due to performance concerns, we introduced the idiomatic solution provided by Scala. This approach, too, had limitations due to random memory allocations.

Then, we turned to additional libraries, and we found the NewType library. Through the use of a mix of `type` and companion objects definition, the library solved the value classes problem in a very brilliant way.

Finally, we looked at the future, introducing opaque type aliases from Dotty that give us the idiomatic language solution we were searching for.
