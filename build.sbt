val Scala213 = "2.13.14"
val Scala212 = "2.12.18"
val Scala3 = "3.3.3"
val Java11 = JavaSpec.temurin("11")
val Java17 = JavaSpec.temurin("17")
val Java21 = JavaSpec.temurin("21")

val mainBranch = "master"
val baseParsleyVersion = "5.0.0-M12" // NEEDS TO BE NEXT VERSION WITH BREAKPOINTS
val circeVersion = "0.14.10"
val scalatestVersion = "3.2.19"
// Here's hoping the stable version of Http4S works fine!
val http4sVersion   = "0.23.30" // For Scala 2.12 compatibility, this version is needed.
val log4catsVersion = "2.6.0"
val scalaXmlVersion = "2.3.0"
val scalafxVersion = "19.0.0-R30" // Later versions unsupported by Java 8. (I don't really mind this anymore)

Global / onChangedBuildSource := ReloadOnSourceChanges

inThisBuild(List(
  tlBaseVersion := "0.1",
  organization := "com.github.j-mie6",
  organizationName := "Parsley Debug View Contributors <https://github.com/j-mie6/parsley-debug-views/graphs/contributors>",
  startYear := Some(2023), // true start is 2018, but license is from 2020
  licenses := List("BSD-3-Clause" -> url("https://opensource.org/licenses/BSD-3-Clause")),
  developers := List(
    tlGitHubDev("j-mie6", "Jamie Willis"),
    tlGitHubDev("mf42-dzh", "Fawwaz Abdullah"),
    tlGitHubDev("Riley-horrix", "Riley Horrix"),
    tlGitHubDev("PriyanshC", "Priyansh Chugh")
  ),
  versionScheme := Some("early-semver"),
  crossScalaVersions := Seq(Scala213, Scala212, Scala3),
  scalaVersion := Scala213,
  // CI Configuration
  tlCiReleaseBranches := Seq(mainBranch),
  tlCiScalafmtCheck := false,
  tlCiHeaderCheck := true,
  githubWorkflowJavaVersions := Seq(Java11, Java17, Java21),
  githubWorkflowConcurrency := None, // this allows us to not fail the pipeline on double commit
  // Website Configuration
  //tlSitePublishBranch := Some(mainBranch),
))

lazy val root = tlCrossRootProject.aggregate(remoteView, jsonInfo, sfxUi/*, http4sServer*/, unidocs)

lazy val commonSettings = Seq(
  headerLicenseStyle := HeaderLicenseStyle.SpdxSyntax,
  headerEmptyLine := false,
  resolvers ++= Opts.resolver.sonatypeOssReleases, // Will speed up MiMA during fast back-to-back releases
  resolvers ++= Opts.resolver.sonatypeOssSnapshots, // needed during flux periods

  libraryDependencies ++= Seq(
    "com.github.j-mie6" %%% "parsley"       % baseParsleyVersion,
    "com.github.j-mie6" %%% "parsley-debug" % baseParsleyVersion,
    "org.scalatest"     %%% "scalatest"     % scalatestVersion % Test
  ),

  Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-oI"),
  Test / parallelExecution := false,
)

lazy val jsonInfo = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .withoutSuffixFor(JVMPlatform)
  .crossType(CrossType.Full)
  .in(file("json-info"))
  .settings(
    commonSettings,
    name := "parsley-debug-json",
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-core" % circeVersion,
      "io.circe" %%% "circe-generic" % circeVersion,
      "io.circe" %%% "circe-parser" % circeVersion,
    )
  )

// this only works for java, obviously.
lazy val sfxUi = project
  .in(file("sfx-ui"))
  .settings(
    commonSettings,
    name := "parsley-debug-sfx",
    libraryDependencies += "org.scalafx" %%% "scalafx" % scalafxVersion,
  )

// native is out for http4s, because it doesn't support 0.5 yet...
// a bit useless on Scala.js, except if its being ran within Node.js...
lazy val http4sServer = crossProject(JVMPlatform, JSPlatform /*, NativePlatform*/ )
  .withoutSuffixFor(JVMPlatform)
  .crossType(CrossType.Full)
  .dependsOn(jsonInfo) // We want the CJson type class here too.
  .in(file("http4s-server"))
  .settings(
    commonSettings,
    name := "parsley-debug-http",
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-core" % circeVersion,
      "io.circe" %%% "circe-generic" % circeVersion,
      "io.circe" %%% "circe-parser" % circeVersion,
      "org.http4s" %%% "http4s-ember-client" % http4sVersion,
      "org.http4s" %%% "http4s-ember-server" % http4sVersion,
      "org.http4s" %%% "http4s-dsl" % http4sVersion,
      "org.http4s" %%% "http4s-circe" % http4sVersion,
      "org.typelevel" %%% "log4cats-core" % log4catsVersion,
      "org.typelevel" %%% "log4cats-noop" % log4catsVersion,
      "org.scala-lang.modules" %%% "scala-xml" % scalaXmlVersion,
      // FIXME: find a replacement for this minifier. N.B. This is licensed under the Apache License 2.0.
      "dev.i10416"             %%% "cssminifier" % "0.0.3",
    )
  )
  .jvmSettings(
    libraryDependencies += "org.typelevel" %%% "log4cats-slf4j" % log4catsVersion
  )

lazy val unidocs = project
  .in(file("unidoc"))
  .enablePlugins(TypelevelUnidocPlugin)
  .settings(
    name := "parsley-debug-view-docs",
    ScalaUnidoc / unidoc / unidocProjectFilter := inProjects(jsonInfo.jvm, sfxUi/*, http4sServer*/),
  )

lazy val remoteView = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .withoutSuffixFor(JVMPlatform)
  .crossType(CrossType.Full)
  .in(file("remote-view"))
  .settings(
    commonSettings,
    name := "parsley-debug-remote",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.client3" %% "core" % "3.10.2",
      "com.softwaremill.sttp.client3" %% "upickle" % "3.10.3",
      "com.lihaoyi" %% "upickle" % "3.3.1"
    )
  )
