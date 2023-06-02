package zone.nilo.handlers

import com.zaxxer.hikari.HikariDataSource
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.{DataFrame, Row, SparkSession}
import zone.nilo.domains.DBConnectionData.{DF, DirectJoin, Query}
import zone.nilo.helpers.PgIO
import zone.nilo.handlers.ConnectionPoolManager.getDataSource
import zone.nilo.handlers.PostgresHandler.{getSparkSchema, parseRow, setParams}

import java.sql.{Connection, PreparedStatement, ResultSet}

object DataFrameExtensions {

  implicit class JDBCExtension(df1: DataFrame) {
    def joinJDBC(pack: DirectJoin, joinExpr: Seq[String], joinType: String = "inner")(implicit
        sparkSession: SparkSession): DataFrame = {
      pack match {
        case query: Query =>
          df1.join(directJoin(sparkSession, df1, joinExpr, query), joinExpr, joinType)
        case DF(dataFrame) => df1.join(dataFrame, joinExpr, joinType)
        case _ => throw new Exception("Invalid query type")
      }
    }
  }

  private def directJoin(implicit
      sparkSession: SparkSession,
      df: DataFrame,
      joinExpr: Seq[String],
      query: Query,
      qtPools: Int = 0,
      securityRepartition: Boolean = true,
      securityLimit: Int = 8): DataFrame = {

    val repartitionDelta =
      if (sparkSession.conf.get("spark.master") == "yarn") {
        qtPools match {
          case x if x != 0 => securityManager(x, securityRepartition, securityLimit)
          case _ =>
            val numExecutors = sparkSession.conf.get("spark.executor.instances").toInt
            val numCores = sparkSession.conf.get("spark.executor.cores").toInt
            val result = (numExecutors * numCores) * 3
            securityManager(result, securityRepartition, securityLimit)
        }
      } else {
        securityManager(qtPools, securityRepartition, securityLimit)
      }

    val joinData = df
      .select(joinExpr.map(col): _*)
      .distinct()
      .repartition(repartitionDelta)

    val joinColumnNames = joinData.schema.fields
      .map(_.name)

    val fetchedDataRDD = joinData.rdd.mapPartitions(partition => {
      val dataSource = getDataSource(query.dbConf)

      val result = fetchData(partition, joinColumnNames, query.query, dataSource)
      result

    })
    val fetchedDataSchema = PgIO.select(query).schema
    val fetchedDataStatement = sparkSession.createDataFrame(fetchedDataRDD, fetchedDataSchema)

    fetchedDataStatement
  }

  private def securityManager(poolCalc: Int, securityFlag: Boolean, securityLimit: Int) = {
    if (securityFlag && (poolCalc >= securityLimit || poolCalc <= 0)) {
      securityLimit
    } else {
      poolCalc
    }
  }
  private def fetchData(
      keyValues: Iterator[Row],
      joinColumnNames: Array[String],
      query: String,
      dataSource: HikariDataSource = null): Iterator[Row] = {

    keyValues.flatMap { keyValue =>
      var connection: Connection = null
      var statement: PreparedStatement = null
      var resultSet: ResultSet = null

      try {
        connection = dataSource.getConnection
        val where =
          s"""select *
             |from ($query) as tab
             |where ${joinColumnNames
              .map(colName => s"$colName = ?")
              .mkString(" and ")}""".stripMargin

        statement = connection.prepareStatement(where)

        setParams(keyValue, statement)

        resultSet = statement.executeQuery()

        val schema = getSparkSchema(resultSet)

        Iterator
          .continually(resultSet)
          .takeWhile(_.next())
          .map(row => parseRow(row, schema))
          .toList

      } finally {
        if (resultSet != null) resultSet.close()
        if (statement != null) statement.close()
        if (connection != null) connection.close()
      }
    }
  }

}
