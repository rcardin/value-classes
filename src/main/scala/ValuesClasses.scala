package in.rcard.value

import cats.Eq
import io.estatico.newtype.Coercible
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
        code.matches("\\d-\\d{6}-\\d{6}"),
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

  def show[T](obj: T): String = obj.toString
  println(show(BarCodeValueClass("1-234567-890234")))

  // Instantiation required
  val macBookBarCode = BarCodeValueClass("1-234567-890234")
  val iPhone12ProBarCode = BarCodeValueClass("0-987654-321098")
  val barCodes = Array[BarCodeValueClass](macBookBarCode, iPhone12ProBarCode)

  def madeInItaly(barCode: BarCodeValueClass): Boolean = barCode match {
    case BarCodeValueClass(code) => code.charAt(0) == '8'
  }

  object NewType {

    @newtype case class BarCode(code: String)
    @newtype case class Description(descr: String)

    case class Product(code: BarCode, description: Description)

    val iPhoneBarCode: BarCode = BarCode("1-234567-890123")
    val iPhoneDescription: Description = Description("Apple iPhone 12 Pro")
    val iPhone12Pro: Product = Product(iPhoneBarCode, iPhoneDescription)

    @newtype class BarCodeWithCompanion(code: String)

    object BarCodeWithCompanion {
      def mkBarCode(code: String): Either[String, BarCodeWithCompanion] =
        Either.cond(
          code.matches("\\d-\\d{6}-\\d{6}"),
          code.coerce,
          s"The given code $code has not the right format")

      implicit val eq: Eq[BarCodeWithCompanion] = deriving

      val barCodeToString: Coercible[BarCode, String] = Coercible[BarCode, String]
      val stringToBarCode: Coercible[String, BarCode] = Coercible[String, BarCode]

      val code: String = barCodeToString(iPhoneBarCode)
      val iPhone12BarCode: BarCode = stringToBarCode("1-234567-890123")

      // Won't compile
      // val doubleToBarCode: Coercible[Double, BarCode] = Coercible[Double, BarCode]

      val anotherCode: String = iPhoneBarCode.coerce
      val anotherIPhone12BarCode: BarCode = "1-234567-890123".coerce
    }
  }
}
