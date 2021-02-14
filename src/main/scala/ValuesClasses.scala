package in.rcard.value

import cats.Eq
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops.toCoercibleIdOps

object ValuesClasses {

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

  val anotherDescription: Description = Description("A fancy description")
  // AnotherProductRepository.findByCode(anotherDescription) Won't compile!

  val aFakeBarCode: BarCode = BarCode("I am a bar-code ;)") // WTF!

  sealed abstract class BarCodeWithSmartConstructor(code: String)
  object BarCodeWithSmartConstructor {
    def mkBarCode(code: String): Either[String, BarCodeWithSmartConstructor] =
      Either.cond(
        code.matches("\\d-\\d\\d\\d\\d\\d\\d-\\d\\d\\d\\d\\d\\d"),
        new BarCodeWithSmartConstructor(code) {},
        s"The given code $code has not the right format"
      )
  }

  val theBarCode: Either[String, BarCodeWithSmartConstructor] =
    BarCodeWithSmartConstructor.mkBarCode("8-000137-001620")

  case class BarCodeValueClass(val code: String) extends AnyVal {
    def countryCode: Char = code.charAt(0)
  }

  // Instantiation required
  implicit val eqBarCode: Eq[BarCodeValueClass] = Eq.fromUniversalEquals[BarCodeValueClass]

  object NewType {

    @newtype case class BarCode(code: String)

    @newtype class BarCodeWithCompanion(code: String)

    object BarCodeWithCompanion {
      def mkBarCode(code: String): Either[String, BarCodeWithCompanion] =
        Either.cond(
          code.matches("\\d-\\d\\d\\d\\d\\d\\d-\\d\\d\\d\\d\\d\\d"),
          code.coerce,
          s"The given code $code has not the right format")

      implicit val eq: Eq[BarCodeWithCompanion] = deriving
    }
  }
}
