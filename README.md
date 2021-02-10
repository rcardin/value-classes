Whether there is something that functional programming teaches us, is that we should always trust the 
signature of a function. Hence, when we use functional programming we prefer to define _ad-hoc_ types
to represent simple information such as an identifier, a description, or a currency. Ladies and 
gentlemen, please welcome the value types.

## 1. The Problem

First, let's define an example to work with. Imagine we have an e-commerce business, and that we 
model a product to sell using the following representation:

```scala
case class Product(code: String, description: String)
```

So, every product is represented by a `code`, for example some alphanumerics barcode, and a 
`description`. So far so good. Now, we want to implement a repository that retrieves products from
a persistent store, and we want to give our users the possibility to search by `code` and by 
`description`:

```scala
trait ProductRepository {
  def findByCode(code: String): Option[Product]
  def findByDescription(description: String): List[Product]
}
```

For sake of completeness, the search by `description` performs some smart search based on some 
distance measure between strings.

Now, the problem is that there is nothing in our program that blocks us to use a description in the 
search by code, or a code in the search by description:

```scala
import in.rcard.value.ValuesClasses.ProductRepository

val aCode = "8-000137-001620"
val aDescription = "Multivitamin and minerals"

ProductRepository.findByCode(aDescription)
ProductRepository.findByDescription(aCode)
```

The compiler cannot warn us of our errors, because we are representing both information, the `code` 
and the `description`, using simple `Strings`. This fact can lead to subtle bugs, which are very 
difficult to intercept also at runtime.

## 2. First Try of Solution

However, we are smart developers, and we want the compiler helping us to identify such errors as
soon as possible. Fail fast, they said. Hence, to help the compiler in its work, we define two 
dedicated types, both to the `code`, and to the `description`:

```scala
case class BarCode(code: String)
case class Description(txt: String)
```

The new types, `BarCode` and `Description`, are nothing more than wrappers around strings. In 
jargon, we call them _value types_. However, they allow us to refine the functions of our repository 
to avoid the previous information mismatch:

```scala
trait AnotherProductRepository {
  def findByCode(barCode: BarCode): Option[Product] =
    Some(Product(barCode.code, "Some description"))
  def findByDescription(description: Description): List[Product] =
    List(Product("some-code", description.txt))
}
```

As we can see, it is not possible anymore to search a product by code, passing accidentally a 
description. Indeed, we can try to pass a `Description` instead of a `BarCode` instead:

```scala
val anotherDescription = Description("A fancy description")
AnotherProductRepository.findByCode(anotherDescription)
```

As desired, the compiler diligently warns us that we are doing it wrong:

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

To overcome this issue we must revamp the _smart constructor_ design pattern. Though the description
of the pattern is behind the scope of this article, the smart constructor pattern hides to developers
the main constructor of the class, and adds a factory method that performs any needed validation on
input information. In its final form, smart constructor pattern for the `BarCode` type is the 
following:

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

Awesome! We reach our main goal. Now, we have fewer problems to worry about...or not? 

## 3. An Idiomatic Solution: Value Classes

The above approach resolves the problems we already mentioned, but it adds many others. In fact,
since we are using a regular type to wrap `String`s, the compiler must instantiate a new instance of
the types `BarCode` and `Description` more or less every time we use them. The over instantiation of
object can lead to problem concerning performances, and the amount of consumed memory.

Fortunately, Scala provides us an idiomatic way to implement value types: _Value classes_. Value
classes avoid allocating runtime objects, avoiding the problems we just enumerated.

A value class is a `class` (or a `case class`) that extends the type `AnyVal`, and declares only one 
single public `val` attribute in the constructor. Moreover, a value class can declare `def`:

```scala
case class BarCodeValueClass(val code: String) extends AnyVal {
  def countryCode: Char = code.charAt(0)
}
```

Value classes can define `def`, but not `val` other than the constructor's attribute, cannot be 
extended and cannot extend anything but _universal traits_. For sake of completeness, a universal 
trait is a trait that extends the `Any` type, has only `def` as members and does no initialization.

The main characteristic of a value class is that the compiler treats it as a regular type at compile
time, but at runtime its representation is equal to the basic type it declares in the constructor.
In our example, the `BarCodeValueClass` type is transformed as a simple `String` at runtime.

Hence, due to the lack of runtime overhead, value classes are a valuable tool of Scala, and are use
also in the SDK in the definition of extension methods for basic types such as `Int`, `Double`, 
`Char`, and so forth.

On the other hand, we must remember that the JVM does not support value classes directly. So, there
are cases in which the runtime must perform an extra allocation of memory for the wrapper type, as
in the case of our `BarCode` class.

The Scala [documentation](https://docs.scala-lang.org/overviews/core/value-classes.html) reports the
following use cases that need an extra memory allocation:

* A value class is treated as another type.
* A value class is assigned to an array.
* Doing runtime type tests, such as pattern matching.

Unfortunately, a concrete case of the first rule concerns the use of a value class as a type 
argument. So, every time we want to implement the type class pattern for a value class, we cannot 
avoid its instantiation as a wrapper.

In our specific case, imagine we have to implement the `cats.Eq` type class for the 
`BarCodeValueClass`:

```scala
implicit val eqBarCode: Eq[BarCodeValueClass] = Eq.fromUniversalEquals[BarCodeValueClass]
```

The problem here that we love type classes, as functional developers, and there are many Scala 
libraries, such as Cats, which are based at the root on the use of the type classes pattern. 

Due to these limitations, the Scala community searched for a better solution. Ladies and gentlemen,
please welcome the [NewType](https://github.com/estatico/scala-newtype) library.

## 4. The NewType Library

The NewType library allow us to create new types without the overhead of extra runtime allocations,
avoiding in this way all the pitfalls of using Scala values classes:

```sbt
libraryDependencies += "io.estatico" %% "newtype" % "0.4.4"
```

It uses the experimental feature of Scala macros. So, it is necessary to enable such a feature at 
compile time, using the `-Ymacro-annotations`. In details, the library defines the `@newtype` 
annotation macro:

```scala
import io.estatico.newtype.macros.newtype
@newtype case class BarCode(code: String)
```

Since the macro expansion generates during compile time a new `type` definition, and an associated 
companion object, we must define the new type inside an `object` or a `package object`. Moreover, 
the library expands the class marked with the `@newtype` annotation with its underlying value at 
runtime. So, it's not possible for a `@newtype` class to extend any other type.

Despite these limitations, the NewType library works like a charm, and interacts smoothly with IDEs.

What about smart constructors? If we choose to use a `case class`, the library will generate for us
the `apply` method in the companion object. If we want to avoid the access to the `apply` 
method, we can use a `class` instead, and create our smart constructor in a dedicated companion 
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

Wait. What is the `code.coerce` statement? Unfortunately, using a `class` instead of a `case class`
removes the chance to use the `apply` method both for other developers and for us :( So, we have to
use type coercion.

As we know, the Scala community considers type coercion a bad practice, because it requires a cast
(via the `asInstanceOf` method). The NewType library tries to make this operation safer using a type
class approach. 

Hence, the compiler will let us use the `coerce` extension method if and only if an
instance of the `Coercible[R, N]` type class exists in the scope for types `R` and `N`. However, 
[it's proven](https://github.com/estatico/scala-newtype/issues/64) that the scope resolution of the 
`Coercible` type class (a.k.a., the coercible trick) is an operations with a very high compile-time 
cost, and should be avoided.

### 4.2. Automatically Deriving Type Classes

The NewType library offers a very nice mechanism to deriving type classes for our `newtype`. Taking
an idea coming from Haskell (as the library itself), the companion objecy of a `newtype` contains 
two methods, called `deriving` and `derivingK`.

So, we can call the first method, `deriving`, if we want to derive an instance of a type class with 
the type parameter that is not higher kinded. For example, we want to use our `BarCodeWithCompanion` 
type together with the `cats.Eq` type class:

```scala
implicit val eq: Eq[BarCodeWithCompanion] = deriving
```

Whereas, if we want to derive an instance of a type class with the type parameter that is higher 
kinded, we can use the `derivingK` method, instead.

Therefore, the fact that we can easily implement type classes for newtypes should dispel any doubt 
whether using them to value classes.

However, with the advent of Dotty (a.k.a. Scala 3) a new competitor came in town: The opaque types.

## 5. Scala 3 Opaque Types Aliases

As many of us might already know, Dotty is the former name of the new major version of Scala. Dotty
introduces many changes and enhancement to the language. One of this is _opaque type aliases_, which
addresses the same issue as the previous value classes: Creating zero cost abstraction. 

In effect, opaque types let us define a new `type` alias with an associated scope. Hence, Dotty 
introduces a new reserved word for opaque type aliases, `opaque`:

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

Inside `BarCodes` scope, the `type` alias `BarCode` works as a `String`: We can assign a `String` to
a variable of type `BarCode`, and we have access to the full API of `String` through an object of 
type `BarCode`. So, there is no distinction between the two types.

Whereas outside the `BarCodes` scope, the compiler treats a `String` and a `BarCode` as completely 
different types.

For example, defining a type alias `BarCode` for the type `String`, we have access to the whole API of the latter through a `BarCode`, 

From this point of view, opaque types seem to be the idiomatic replacement to the NewType library.





