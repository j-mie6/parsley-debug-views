// https://typelevel.org/sbt-typelevel/faq.html#what-is-a-base-version-anyway
ThisBuild / tlBaseVersion := "0.0" // your current series x.y

ThisBuild / organization     := "io.github.mf42-dzh"
ThisBuild / organizationName := "Fawwaz Abdullah"
ThisBuild / startYear        := Some(2023)
ThisBuild / licenses   := Seq("BSD-3-Clause" -> url("https://opensource.org/licenses/BSD-3-Clause"))
ThisBuild / developers := List(
  // your GitHub handle and name
  tlGitHubDev("mf42-dzh", "Fawwaz Abdullah")
)

// publish to s01.oss.sonatype.org (set to true to publish to oss.sonatype.org instead)
ThisBuild / tlSonatypeUseLegacyHost := false

val Scala212 = "2.12.10"
val Scala213 = "2.13.12"
val Scala3   = "3.3.1"

ThisBuild / crossScalaVersions := Seq(Scala212, Scala213, Scala3)
ThisBuild / scalaVersion       := Scala213 // the default Scala

// Java version for CI and support
ThisBuild / githubWorkflowJavaVersions := Seq(
  JavaSpec.temurin("8"),
  JavaSpec.temurin("11"),
  JavaSpec.temurin("17")
)

lazy val root = tlCrossRootProject
  .aggregate(conui)
  .settings(
    headerLicenseStyle   := HeaderLicenseStyle.SpdxSyntax,
    headerEmptyLine      := false,
    resolvers           ++= Opts.resolver.sonatypeOssSnapshots,
    libraryDependencies ++= Seq(
      "com.github.j-mie6" %%% "parsley"         % "4.4-de8306a-SNAPSHOT",
      "com.github.j-mie6" %%% "parsley-debug"   % "4.4-de8306a-SNAPSHOT",
      "org.scalactic"     %%% "scalactic"       % "3.2.15"   % Test,
      "org.scalatest"     %%% "scalatest"       % "3.2.15"   % Test,
      "org.scalatestplus" %%% "scalacheck-1-15" % "3.2.11.0" % Test
    )
  )

lazy val conui = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .withoutSuffixFor(JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("conui"))
  .settings(
    name := "parsley-debug-conui"
  )

lazy val sfxui = crossProject(JVMPlatform)
  .withoutSuffixFor(JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("sfxui"))
  .settings(
    name                := "parsley-debug-sfxui",
    libraryDependencies += "org.scalafx" %%% "scalafx" % "19.0.0-R30"
  )

Test / parallelExecution := false

// Scoverage settings.
coverageFailOnMinimum := false
coverageHighlighting  := true
val defaultCoverageMinimum = 80

coverageMinimumStmtTotal        := defaultCoverageMinimum
coverageMinimumBranchTotal      := defaultCoverageMinimum
coverageMinimumStmtPerPackage   := defaultCoverageMinimum
coverageMinimumBranchPerPackage := defaultCoverageMinimum
coverageMinimumStmtPerFile      := defaultCoverageMinimum
coverageMinimumBranchPerFile    := defaultCoverageMinimum
