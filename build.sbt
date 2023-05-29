ThisBuild / scalaVersion := "2.12.17"

lazy val root = (project in file("."))
  .settings(
    licenses := List("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt")),
    version := "1.0.1",
    versionScheme := Some("early-semver"),
    developers := List(
      Developer(
        id = "daniloarodrigues",
        name = "Danilo Rodrigues",
        email = "hello@nilo.zone",
        url = url("https://github.com/daniloarodrigues"))),
    description := "An spark extension that amplifies the power of JDBC",
    organizationHomepage := Some(url("https://github.com/NivLabs/spark-jdbc-extension")),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/NivLabs/spark-jdbc-extension"),
        "scm:git@github.com:NivLabs/spark-jdbc-extension.git")),
    homepage := Some(url("https://github.com/NivLabs/spark-jdbc-extension")),
    organization := "zone.nilo",
    organizationName := "Nilo Zone",
    publishMavenStyle := true,
    pomIncludeRepository := { _ => false },
    publishTo := {
      val nexus = "https://s01.oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    credentials += Credentials(Path.userHome / ".sbt" / ".credentials"),
    name := "spark-jdbc-extension",
    assembly / assemblyJarName := s"${name.value}-${version.value}.jar",
    assembly / assemblyShadeRules := {
      val rules =
        Seq("com.zaxxer.hikari", "org.apache.spark", "com.github.jnr").map { packageName =>
          ShadeRule
            .rename(s"$packageName.**" -> s"${name.value}_shade_package.$packageName.@1")
            .inAll
        }
      rules
    },
    libraryDependencies ++= Seq(
      "org.apache.spark" %% "spark-core" % "3.3.2",
      "org.apache.spark" %% "spark-sql" % "3.3.2",
      "com.zaxxer" % "HikariCP" % "4.0.3",
      "org.scalameta" %% "munit" % "0.7.29" % Test,
      "org.postgresql" % "postgresql" % "42.5.4" % Test))

ThisBuild / assemblyMergeStrategy := {
  case x if Assembly.isConfigFile(x) =>
    MergeStrategy.concat
  case PathList(ps @ _*) if Assembly.isReadme(ps.last) || Assembly.isLicenseFile(ps.last) =>
    MergeStrategy.rename
  case PathList("META-INF", "versions", xs @ _*) =>
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
