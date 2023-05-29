package zone.nilo.handlers

import org.apache.spark.sql.Row

import java.sql.{PreparedStatement, ResultSet, Types}
import org.apache.spark.sql.types._
import zone.nilo.helpers.Validator.isValidUUID

object PostgresHandler {

  private def getSparkType(sqlType: Int, sqlTypeName: String): DataType = (sqlType, sqlTypeName) match {
    case (Types.INTEGER, _) => IntegerType
    case (Types.VARCHAR, _) => StringType
    case (Types.DOUBLE, _) => DoubleType
    case (Types.DATE, _) => DateType
    case (Types.TIMESTAMP, _) => TimestampType
    case (Types.BOOLEAN, _) => BooleanType
    case (Types.BIT, _) => BooleanType
    case (Types.OTHER, "uuid") => StringType // PostgreSQL uses Types.OTHER for UUID
    case _ => throw new UnsupportedOperationException(s"Unsupported SQL type: $sqlType, $sqlTypeName")
  }

  def getSparkSchema(resultSet: ResultSet): StructType = {
    val metaData = resultSet.getMetaData
    val schema = for (i <- 1 to metaData.getColumnCount) yield {
      val columnName = metaData.getColumnName(i)
      val sqlType = metaData.getColumnType(i)
      val sqlTypeName = metaData.getColumnTypeName(i)
      StructField(columnName, getSparkType(sqlType, sqlTypeName), nullable = true)
    }
    StructType(schema)
  }

  def parseRow(resultSet: ResultSet, schema: StructType): Row = {
    val values = schema.fields.zipWithIndex.map { case (field, index) =>
      field.dataType match {
        case IntegerType => resultSet.getInt(index + 1)
        case StringType => resultSet.getString(index + 1)
        case DoubleType => resultSet.getDouble(index + 1)
        case DateType => resultSet.getDate(index + 1)
        case TimestampType => resultSet.getTimestamp(index + 1)
        case BooleanType => resultSet.getBoolean(index + 1)
        case _ => throw new UnsupportedOperationException(s"Unsupported Spark type: ${field.dataType}")
      }
    }
    Row.fromSeq(values)
  }

  def setParams(keyValue: Row, statement: PreparedStatement): Unit = {
    keyValue.toSeq.zipWithIndex.foreach { case (value, index) =>
      val handler: TypeHandler = value match {
        case uuid if isValidUUID(uuid.toString) => TypeHandler.UUIDTypeHandler
        case _: String => TypeHandler.StringTypeHandler
        case _: Int => TypeHandler.IntegerTypeHandler
        case _: Boolean => TypeHandler.BooleanTypeHandler
        case _ => throw new Exception(s"Unsupported type: ${value.getClass}")
      }
      handler.setParam(statement, index, value)
    }
  }

}
