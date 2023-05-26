package zone.nilo
package handlers

import domains.DBConnectionData
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}

object ConnectionPoolManager {

  @transient private var dataSource: HikariDataSource = _

  def getDataSource(dbConf: DBConnectionData): HikariDataSource = {
    synchronized(if (dataSource == null) {

      val hikariConfig = new HikariConfig()
      hikariConfig.setDataSourceClassName(getDataSourceClassName(dbConf.driver))
      hikariConfig.addDataSourceProperty("serverName", dbConf.host)
      hikariConfig.addDataSourceProperty("portNumber", dbConf.port)
      hikariConfig.addDataSourceProperty("databaseName", dbConf.db)
      hikariConfig.addDataSourceProperty("user", dbConf.user)
      hikariConfig.addDataSourceProperty("password", dbConf.password)
      hikariConfig.addDataSourceProperty("ApplicationName", dbConf.appName)
      hikariConfig.setMaximumPoolSize(1)
      new HikariDataSource(hikariConfig)
    } else {
      dataSource
    })
  }

  private def getDataSourceClassName(driverClassName: String): String = {
    driverClassName match {
      case "org.apache.derby.jdbc.ClientDriver" => "org.apache.derby.jdbc.ClientDataSource"
      case "org.firebirdsql.jdbc.FBDriver" => "org.firebirdsql.ds.FBSimpleDataSource"
      case "com.google.cloud.spanner.jdbc.JdbcDriver" =>
        "com.google.cloud.spanner.jdbc.JdbcDriver"
      case "org.h2.Driver" => "org.h2.jdbcx.JdbcDataSource"
      case "org.hsqldb.jdbc.JDBCDriver" => "org.hsqldb.jdbc.JDBCDataSource"
      case "com.ibm.db2.jcc.DB2Driver" => "com.ibm.db2.jcc.DB2SimpleDataSource"
      case "com.informix.jdbc.IfxDriver" => "com.informix.jdbcx.IfxDataSource"
      case "com.microsoft.sqlserver.jdbc.SQLServerDriver" =>
        "com.microsoft.sqlserver.jdbc.SQLServerDataSource"
      case "com.mysql.jdbc.Driver" => "com.mysql.jdbc.jdbc2.optional.MysqlDataSource"
      case "org.mariadb.jdbc.Driver" => "org.mariadb.jdbc.MariaDbDataSource"
      case "oracle.jdbc.OracleDriver" => "oracle.jdbc.pool.OracleDataSource"
      case "com.orientechnologies.orient.jdbc.OrientJdbcDriver" =>
        "com.orientechnologies.orient.jdbc.OrientDataSource"
      case "com.impossibl.postgres.jdbc.PGDriver" => "com.impossibl.postgres.jdbc.PGDataSource"
      case "org.postgresql.Driver" => "org.postgresql.ds.PGSimpleDataSource"
      case "com.sap.dbtech.jdbc.DriverSapDB" => "com.sap.dbtech.jdbc.DriverSapDB"
      case "org.sqlite.JDBC" => "org.sqlite.SQLiteDataSource"
      case "com.sybase.jdbc4.jdbc.SybDriver" => "com.sybase.jdbc4.jdbc.SybDataSource"
      case _ => throw new Exception("Unknown driver class name")
    }
  }

}
