package zone.nilo
package domains

import helpers.SparkSessionHandler.getSparkSession

case class DBConnectionData(
    host: String,
    port: String,
    driver: String,
    db: String,
    user: String,
    password: String,
    appName: String = getSparkSession.conf.get("spark.app.name"),
    source: String = "jdbc") {

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
