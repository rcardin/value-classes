package in.rcard.value

object ValuesTypes {

  case class Product(code: String, description: String)

  trait ProductRepository {
    def findByCode(code: String): Option[Product] =
      Some(Product(code, "Some description"))
    def findByDescription(description: String): List[Product] =
      List(Product("some-code", description))
  }
  object ProductRepository extends ProductRepository

  val aCode = "8-000137-001620"
  val aDescription = "Multivitamin and minerals"

  ProductRepository.findByCode(aDescription)
  ProductRepository.findByDescription(aCode)

  case class BarCode(code: String)
  case class Description(txt: String)

  trait AnotherProductRepository {
    def findByCode(barCode: BarCode): Option[Product] =
      Some(Product(barCode.code, "Some description"))
    def findByDescription(description: Description): List[Product] =
      List(Product("some-code", description.txt))
  }
  object AnotherProductRepository extends AnotherProductRepository

  val anotherDescription = Description("A fancy description")
  // AnotherProductRepository.findByCode(anotherDescription) Won't compile!
}
