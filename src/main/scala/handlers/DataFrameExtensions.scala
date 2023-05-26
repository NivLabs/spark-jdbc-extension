package zone.nilo
package handlers

import com.zaxxer.hikari.HikariDataSource
import domains.DBConnectionData
import handlers.ConnectionPoolManager.getDataSource
import helpers.PgIO
import helpers.SparkSessionHandler.getSparkSession
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.{DataFrame, Row}

import java.sql.{Connection, PreparedStatement, ResultSet}

object DataFrameExtensions {

  implicit class JDBCExtension(df1: DataFrame) {
    def joinJDBC(
        query: String,
        joinExpr: Seq[String],
        joinType: String = "inner",
        directJDBC: Boolean = false,
        dbConf: DBConnectionData): DataFrame = {

      val df2Processed = if (directJDBC) {
        directJoin(df1, joinExpr, query, dbConf)
      } else {
        PgIO.getData(query, dbConf)
      }
      df1.join(df2Processed, joinExpr, joinType)
    }
  }

  private def directJoin(
      df: DataFrame,
      joinExpr: Seq[String],
      query: String,
      dbConf: DBConnectionData,
      qtPools: Int = 0,
      securityRepartition: Boolean = true,
      securityLimit: Int = 8) = {

    val repartitionDelta =
      if (getSparkSession.conf.get("spark.submit.deployMode") == "cluster") {
        qtPools match {
          case x if x != 0 => securityManager(x, securityRepartition, securityLimit)
          case _ =>
            val numExecutors = getSparkSession.conf.get("spark.executor.instances").toInt
            val numCores = getSparkSession.conf.get("spark.executor.cores").toInt
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

    val appName = getSparkSession.conf.get("spark.app.name")

    val fetchedDataRDD = joinData.rdd.mapPartitions(partition => {
      val dataSource = getDataSource(dbConf)

      val result = fetchData(partition, joinColumnNames, query, dataSource)
      result

    })
    val fetchedDataSchema = PgIO.getData(query, dbConf).schema
    val fetchedDataStatement = getSparkSession.createDataFrame(fetchedDataRDD, fetchedDataSchema)

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

        keyValue.toSeq.zipWithIndex.foreach { case (value, index) =>
          statement.setObject(index + 1, value)
        }

        resultSet = statement.executeQuery()
        val metaData = resultSet.getMetaData
        val columnCount = metaData.getColumnCount

        Iterator
          .continually(resultSet)
          .takeWhile(_.next())
          .map { row =>
            val values = (1 to columnCount).map(row.getObject)
            Row.fromSeq(values)
          }
          .toList
      } finally {
        if (resultSet != null) resultSet.close()
        if (statement != null) statement.close()
        if (connection != null) connection.close()
      }
    }
  }
}
