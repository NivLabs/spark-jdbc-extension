ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.12.17"

lazy val root = (project in file("."))
  .settings(
    licenses := List("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt")),
    idePackagePrefix := Some("zone.nilo"),
    organization := "zone.nilo",
    organizationName := "Nilo Zone",
    publishMavenStyle := true,
    pomIncludeRepository := { _ => false },
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    credentials += Credentials(Path.userHome / ".sbt" / ".credentials"),
    name := "spark-jdbc-extension",
    assembly / assemblyJarName := s"${name.value}-${version.value}.jar",
    assembly / assemblyShadeRules := {
      println(Path.userHome)
      val rules =
        Seq("com.zaxxer.hikari", "org.apache.spark", "com.github.jnr", "com.typesafe").map {
          packageName =>
            ShadeRule
              .rename(s"$packageName.**" -> s"${name.value}_shade_package.$packageName.@1")
              .inAll
        }
      rules
    },
    libraryDependencies ++= Seq(
      "org.apache.spark" %% "spark-core" % "3.3.2",
      "org.apache.spark" %% "spark-sql" % "3.3.2",
      "com.zaxxer" % "HikariCP" % "4.0.3"))

ThisBuild / assemblyMergeStrategy := {
  case x if Assembly.isConfigFile(x) =>
    MergeStrategy.concat
  case PathList(ps @ _*) if Assembly.isReadme(ps.last) || Assembly.isLicenseFile(ps.last) =>
    MergeStrategy.rename
  case PathList("META-INF", "versions", xs@_*) =>
    MergeStrategy.discard
  case PathList("META-INF", xs @ _*) =>
    xs map { _.toLowerCase } match {
      case "manifest.mf" :: Nil | "index.list" :: Nil | "dependencies" :: Nil =>
        MergeStrategy.discard
      case ps @ x :: xs if ps.last.endsWith(".sf") || ps.last.endsWith(".dsa") =>
        MergeStrategy.discard
      case "plexus" :: xs =>
        MergeStrategy.discard
      case "services" :: xs =>
        MergeStrategy.filterDistinctLines
      case "spring.schemas" :: Nil | "spring.handlers" :: Nil =>
        MergeStrategy.filterDistinctLines
      case _ => MergeStrategy.first
    }
  case _ => MergeStrategy.first
}