Whether there is something that functional programming teaches us, is that we should always trust the 
signature of a function. Hence, when we use functional programming we prefer to define _ad-hoc_ types
to represent simple information such as an identifier, a description, or a currency. Ladies and 
gentlemen, please welcome the value types.

## 1. Idiomatic Value Types in Scala

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

Now, the problem is that there is nothing in our program that block us to use a description in the 
search by code, or a code in the search by description:

```scala
import in.rcard.value.ValuesTypes.ProductRepository
val aCode = "8-000137-001620"
val aDescription = "Multivitamin and minerals"

ProductRepository.findByCode(aDescription)
ProductRepository.findByDescription(aCode)
```

The compiler cannot warn us of our errors, because we are representing both information, the `code` 
and the `description`, using simple `Strings`. This fact can lead to subtle bugs, which are very 
difficult to intercept also at runtime.

However, we are smart developers, and we want the compiler helping us to identify such errors as
soon as possible. Fail fast, they said. Hence, to help the compiler in its work, we define two 
dedicated types, both to the `code`, and to the `description`:

```scala
case class BarCode(code: String)
case class Description(txt: String)
```

The new types, `BarCode` and `Description`, are nothing more than wrappers around strings. However,
they allow us to refine the functions of our repository to avoid the previous information mismatch:

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

