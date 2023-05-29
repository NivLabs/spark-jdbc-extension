package zone.nilo.handlers
import java.sql.PreparedStatement
import java.util.UUID

// 1. Define the trait and objects for each type.
trait TypeHandler {
  def setParam(statement: PreparedStatement, index: Int, value: Any): Unit
}

object TypeHandler {

  implicit object StringTypeHandler extends TypeHandler {
    def setParam(statement: PreparedStatement, index: Int, value: Any): Unit =
      statement.setString(index + 1, value.asInstanceOf[String])
  }

  implicit object IntegerTypeHandler extends TypeHandler {
    def setParam(statement: PreparedStatement, index: Int, value: Any): Unit =
      statement.setInt(index + 1, value.asInstanceOf[Int])
  }

  implicit object UUIDTypeHandler extends TypeHandler {
    def setParam(statement: PreparedStatement, index: Int, value: Any): Unit = {
      val uuid = UUID.fromString(value.asInstanceOf[String])
      statement.setObject(index + 1, uuid)
    }
  }

  implicit object BooleanTypeHandler extends TypeHandler {
    def setParam(statement: PreparedStatement, index: Int, value: Any): Unit =
      statement.setBoolean(index + 1, value.asInstanceOf[Boolean])
  }

}
