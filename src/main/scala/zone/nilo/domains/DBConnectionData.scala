package zone.nilo.domains

import org.apache.spark.sql.{DataFrame, SparkSession}
import zone.nilo.domains.DBConnectionData.{DF, DirectJoin, Query}
import zone.nilo.helpers.PgIO

case class DBConnectionData(
    host: String,
    port: String,
    driver: String,
    db: String,
    user: String,
    password: String,
    appName: String = "Spark JDBC Extension",
    source: String = "jdbc")(implicit sparkSession: SparkSession) extends DirectJoin {

  def toDF(query: Query): DF  = {
    DF(PgIO.select(query))
  }

  def toDF(query: String): DF = {
    DF(PgIO.select(Query(this, query)))
  }
  def toQ(q: String): Query = {
    query(q)
  }

  def toQ(q: Query): Query = {
    q
  }

  private def query(query: String): Query = {
    Query(this, query)
  }

  def getUrl: String = {
    val url = s"$source:${getDriverConf(driver)}://$host/$db?ApplicationName=$appName"
    url
  }

  private def getDriverConf(driver: String): String = {
    driver match {
      case "org.postgresql.Driver" => "postgresql"
      case "com.microsoft.sqlserver.jdbc.SQLServerDriver" => "sqlserver"
      case "com.mysql.jdbc.Driver" => "mysql"
      case "oracle.jdbc.OracleDriver" => "oracle"
      case _ => throw new Exception("Unknown driver class name")
    }
  }
}

object DBConnectionData {
  sealed trait DirectJoin
  final case class DF(df: DataFrame) extends DirectJoin {
    def get: DataFrame = df
  }
  final case class Query(dbConf: DBConnectionData, query: String) extends DirectJoin

}
