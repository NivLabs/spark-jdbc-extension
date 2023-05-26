package zone.nilo
package helpers

import org.apache.spark.sql.SparkSession

object SparkSessionHandler {

  def getSparkSession: SparkSession = SparkSession.getActiveSession match {
    case Some(session) => session
    case None => throw new Exception("Spark Session not found")
  }

}
