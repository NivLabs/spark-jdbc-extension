package zone.nilo
package helpers

import org.apache.spark.sql.SparkSession

object SparkSessionHandler {

  def getSparkSession: SparkSession = SparkSession.getActiveSession match {
    case Some(_) => _
    case None => throw new Exception("Spark Session not found")
  }

}
