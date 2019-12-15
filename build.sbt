lazy val root = project
  .in(file("."))
  .dependsOn(core, test)
  .aggregate(core, test)
  .settings(
    skip.in(publish) := true,
    projectSettings
  )

lazy val core = project
  .in(file("core"))
  .settings(
    name := "context-applied",
    projectSettings,
    libraryDependencies += scalaOrganization.value % "scala-compiler" % scalaVersion.value,
    scalacOptions ++= Seq(
      "-Xfatal-warnings",
      "-Xlint",
      "-feature",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-deprecation",
      "-unchecked"
    ),
    Test / scalacOptions ++= {
      val jar = (Compile / packageBin).value
      Seq(s"-Xplugin:${jar.getAbsolutePath}", s"-Jdummy=${jar.lastModified}") // ensures recompile
    },
    Test / scalacOptions += "-Yrangepos",
    console / initialCommands := "import plugin._",
    Compile / console / scalacOptions := Seq("-language:_", "-Xplugin:" + (Compile / packageBin).value),
    Test / console / scalacOptions := (Compile / console / scalacOptions).value,
    Test / fork := true,
    //Test / scalacOptions ++= Seq("-Xprint:typer", "-Xprint-pos"), // Useful for debugging
  )

lazy val test = project
  .in(file("test"))
  .dependsOn(core)
  .settings(
    skip.in(publish) := true,
    projectSettings,
    libraryDependencies += "org.typelevel" %% "cats-core" % "2.0.0",
    scalacOptions ++= {
      val jar = (core / Compile / packageBin).value
      Seq(s"-Xplugin:${jar.getAbsolutePath}", s"-Jdummy=${jar.lastModified}") // ensures recompile
    }
  )

lazy val projectSettings = Seq(
  organization := "org.augustjune",
  licenses ++= Seq(("MIT", url("http://opensource.org/licenses/MIT"))),
  homepage := Some(url("https://github.com/augustjune/context-applied")),
  developers := List(
    Developer("augustjune", "Yura Slinkin", "jurij.jurich@gmail.com", url("https://github.com/augustjune"))
  ),
  scalaVersion := "2.13.0"
)
