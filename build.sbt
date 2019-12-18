lazy val `context-applied` = project
  .in(file("."))
  .dependsOn(core, test)
  .aggregate(core, test)
  .settings(
    skip.in(publish) := true,
    projectSettings,
    crossScalaVersions := Nil
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
      "-deprecation",
      "-unchecked"
    ),
    console / initialCommands := "import plugin._",
    Compile / console / scalacOptions := Seq("-language:_", "-Xplugin:" + (Compile / packageBin).value)
  )

lazy val test = project
  .in(file("test"))
  .dependsOn(core)
  .settings(
    skip.in(publish) := true,
    projectSettings,
    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3"),
    scalacOptions ++= {
      val jar = (core / Compile / packageBin).value
      Seq(s"-Xplugin:${jar.getAbsolutePath}", s"-Jdummy=${jar.lastModified}") // ensures recompile
    },
    scalacOptions ++= Seq(
      "-Xfatal-warnings",
      "-language:higherKinds",
      "-language:postfixOps",
      "-language:implicitConversions",
      "-Ywarn-unused:implicits", // Warn if an implicit parameter is unused.
      "-Ywarn-unused:imports", // Warn if an import selector is not referenced.
      "-Ywarn-unused:locals", // Warn if a local definition is unused.
      "-Ywarn-unused:params", // Warn if a value parameter is unused.
      "-Ywarn-unused:patvars", // Warn if a variable bound in a pattern is unused.
      "-Ywarn-unused:privates", // Warn if a private member is unused.
      "-Ywarn-value-discard" // Warn when non-Unit expression results are unused.
    )
  )

lazy val projectSettings = Seq(
  organization := "org.augustjune",
  licenses ++= Seq(("MIT", url("http://opensource.org/licenses/MIT"))),
  homepage := Some(url("https://github.com/augustjune/context-applied")),
  developers := List(
    Developer("augustjune", "Yura Slinkin", "jurij.jurich@gmail.com", url("https://github.com/augustjune"))
  ),
  scalaVersion := "2.13.0",
  crossScalaVersions := Seq(scalaVersion.value, "2.12.10")
)
