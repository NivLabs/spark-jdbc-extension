package zone.nilo
package helpers

import domains.DBConnectionData
import helpers.SparkSessionHandler.getSparkSession
import org.apache.spark.sql.DataFrame

object PgIO {

  def getData(query: String, postgresConf: DBConnectionData): DataFrame = {
    getSparkSession.read
      .format(postgresConf.source)
      .options(
        Map(
          "url" -> postgresConf.getUrl,
          "user" -> postgresConf.user,
          "password" -> postgresConf.password,
          "driver" -> postgresConf.driver,
          "query" -> query))
      .load()
  }

}
