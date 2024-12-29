Global / onChangedBuildSource := ReloadOnSourceChanges

// https://typelevel.org/sbt-typelevel/faq.html#what-is-a-base-version-anyway
ThisBuild / tlBaseVersion := "0.1" // your current series x.y

ThisBuild / organization := "com.github.j-mie6"
ThisBuild / organizationName := "Parsley Contributors <https://github.com/j-mie6/Parsley/graphs/contributors> + Fawwaz Abdullah"
ThisBuild / startYear  := Some(2023)
ThisBuild / licenses   := Seq("BSD-3-Clause" -> url("https://opensource.org/licenses/BSD-3-Clause"))
ThisBuild / developers := List(
  tlGitHubDev("j-mie6", "Jamie Willis"),
  tlGitHubDev("mf42-dzh", "Fawwaz Abdullah")
)

val Scala212 = "2.12.18"
val Scala213 = "2.13.14"
val Scala3   = "3.3.3"

ThisBuild / crossScalaVersions := Seq( /*Scala212,*/ Scala213, Scala3)
ThisBuild / scalaVersion       := Scala213 // the default Scala

// Java version for CI and support
ThisBuild / githubWorkflowJavaVersions := Seq(
  JavaSpec.temurin("11"),
  JavaSpec.temurin("17"),
  JavaSpec.temurin("21")
)

// Shared dependencies for all frontends:
val baseParsleyVersion = "5.0.0-M8"

lazy val commonSettings = Seq(
  headerLicenseStyle   := HeaderLicenseStyle.SpdxSyntax,
  headerEmptyLine      := false,
  resolvers           ++= Opts.resolver.sonatypeOssSnapshots,
  libraryDependencies ++= Seq(
    "com.github.j-mie6" %%% "parsley"       % baseParsleyVersion,
    "com.github.j-mie6" %%% "parsley-debug" % baseParsleyVersion,
    "org.scalactic"     %%% "scalactic"     % "3.2.19" % Test,
    "org.scalatest"     %%% "scalatest"     % "3.2.19" % Test
  )
)

lazy val root = tlCrossRootProject.aggregate(json_info, sfx_ui, http_server)

// Circe JSON library.
val circeVersion = "0.14.10"
lazy val circe   = Seq(
  libraryDependencies ++= Seq(
    "io.circe" %%% "circe-core",
    "io.circe" %%% "circe-generic",
    "io.circe" %%% "circe-parser"
  ).map(_ % circeVersion)
)

lazy val json_info = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .withoutSuffixFor(JVMPlatform)
  .crossType(CrossType.Full)
  .in(file("json-info"))
  .settings(
    commonSettings,
    name := "parsley-debug-json",
    circe
  )

lazy val sfx_ui = crossProject(JVMPlatform)
  .withoutSuffixFor(JVMPlatform)
  .crossType(CrossType.Full)
  .in(file("sfx-ui"))
  .settings(
    commonSettings,
    name := "parsley-debug-sfx",
    libraryDependencies += "org.scalafx" %%% "scalafx" % "19.0.0-R30" // Later versions unsupported by Java 8. (TODO: I don't really mind this anymore)
  )

// Here's hoping the stable version of Http4S works fine!
val http4sVersion   = "0.23.30" // For Scala 2.12 compatibility, this version is needed.
val log4catsVersion = "2.6.0"

// native is out for http4s, because it doesn't support 0.5 yet...
lazy val http_server = crossProject(JVMPlatform, JSPlatform /*, NativePlatform*/ )
  .withoutSuffixFor(JVMPlatform)
  .crossType(CrossType.Full)
  .dependsOn(json_info) // We want the CJson type class here too.
  .in(file("http-server"))
  .settings(
    commonSettings,
    name                 := "parsley-debug-http",
    circe,
    libraryDependencies ++= Seq(
      "org.http4s" %%% "http4s-ember-client",
      "org.http4s" %%% "http4s-ember-server",
      "org.http4s" %%% "http4s-dsl",
      "org.http4s" %%% "http4s-circe"
    ).map(_ % http4sVersion) ++ Seq(
      "org.typelevel" %%% "log4cats-core",
      "org.typelevel" %%% "log4cats-noop"
    ).map(_ % log4catsVersion) ++ Seq(
      "org.scala-lang.modules" %%% "scala-xml"   % "2.3.0",
      // FIXME: find a replacement for this minifier. N.B. This is licensed under the Apache License 2.0.
      "dev.i10416"             %%% "cssminifier" % "0.0.3"
    )
  )
  .jvmSettings(
    libraryDependencies += "org.typelevel" %%% "log4cats-slf4j" % log4catsVersion
  )

Test / parallelExecution := false
