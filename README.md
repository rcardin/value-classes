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

```
