package zone.nilo.helpers

import org.apache.spark.sql.{DataFrame, SparkSession}
import zone.nilo.domains.DBConnectionData.Query

object PgIO {

  def select(query: Query)(implicit sparkSession: SparkSession): DataFrame = {
    sparkSession.read
      .format(query.dbConf.source)
      .options(
        Map(
          "url" -> query.dbConf.getUrl,
          "user" -> query.dbConf.user,
          "password" -> query.dbConf.password,
          "driver" -> query.dbConf.driver,
          "query" -> query.query))
      .load()
  }

}
