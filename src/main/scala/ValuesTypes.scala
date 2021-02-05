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
}
