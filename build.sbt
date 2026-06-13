import xerial.sbt.Sonatype.sonatypeCentralHost

// ---- shared settings for every module in the suite ----

ThisBuild / licenses               := Seq("ISC" -> url("https://opensource.org/licenses/ISC"))
ThisBuild / versionScheme          := Some("semver-spec")
ThisBuild / evictionErrorLevel     := Level.Warn
ThisBuild / scalaVersion           := "3.8.4"
ThisBuild / organization           := "io.github.edadma"
ThisBuild / organizationName       := "edadma"
ThisBuild / organizationHomepage   := Some(url("https://github.com/edadma"))
ThisBuild / version                := "0.2.6"
ThisBuild / sonatypeCredentialHost := sonatypeCentralHost

ThisBuild / publishConfiguration := publishConfiguration.value.withOverwrite(true).withChecksums(Vector.empty)
ThisBuild / resolvers += Resolver.mavenLocal
ThisBuild / resolvers += Resolver.sonatypeCentralSnapshots
ThisBuild / resolvers += Resolver.sonatypeCentralRepo("releases")

ThisBuild / sonatypeProfileName := "io.github.edadma"

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/edadma/sdl3"),
    "scm:git@github.com:edadma/sdl3.git",
  ),
)
ThisBuild / developers := List(
  Developer(
    id = "edadma",
    name = "Edward A. Maxedon, Sr.",
    email = "edadma@gmail.com",
    url = url("https://github.com/edadma"),
  ),
)

ThisBuild / homepage := Some(url("https://github.com/edadma/sdl3"))

// Settings every published module shares: Scala Native, scalac/scaladoc flags,
// the test dependency, and Maven Central publishing.
lazy val commonSettings = Seq(
  scalacOptions ++= Seq(
    "-deprecation",
    "-feature",
    "-unchecked",
    "-language:postfixOps",
    "-language:implicitConversions",
    "-language:existentials",
  ),
  // scaladoc doesn't support the Scala Native compiler plugin (-Xplugin: nscplugin)
  // that sbt-scala-native adds for compilation; drop it from the doc task so
  // `doc` (and therefore `publishSigned`) is warning-free.
  Compile / doc / scalacOptions ~= { _.filterNot(_.startsWith("-Xplugin")) },
  libraryDependencies += "org.scalatest" %%% "scalatest" % "3.2.19" % "test",
  publishMavenStyle      := true,
  Test / publishArtifact := false,
  publishTo              := sonatypePublishToBundle.value,
)

// ---- modules: one system library per binding ----

// Pure SDL3: window, float render, texture, surface, events, keyboard/mouse,
// hints, render targets. Published as `io.github.edadma::sdl3`.
lazy val core = project
  .in(file("core"))
  .enablePlugins(ScalaNativePlugin)
  .settings(commonSettings)
  .settings(
    name        := "sdl3",
    description := "Scala Native bindings for SDL3 (2D rendering and input), with a pure-Scala wrapper layer",
  )

// SDL_ttf 3 text rendering, layered on core via dependsOn (no intermediate
// publish needed). Published as `io.github.edadma::sdl3_ttf`.
lazy val ttf = project
  .in(file("ttf"))
  .enablePlugins(ScalaNativePlugin)
  .dependsOn(core)
  .settings(commonSettings)
  .settings(
    name        := "sdl3_ttf",
    description := "Scala Native bindings for SDL_ttf 3, built on the sdl3 core binding",
  )

// SDL_image 3 surface loading (PNG/JPEG/etc.). Published as
// `io.github.edadma::sdl3_image`.
lazy val image = project
  .in(file("image"))
  .enablePlugins(ScalaNativePlugin)
  .dependsOn(core)
  .settings(commonSettings)
  .settings(
    name        := "sdl3_image",
    description := "Scala Native bindings for SDL_image 3, built on the sdl3 core binding",
  )

// SDL_mixer 3 audio playback (effects + music). Published as
// `io.github.edadma::sdl3_mixer`.
lazy val mixer = project
  .in(file("mixer"))
  .enablePlugins(ScalaNativePlugin)
  .dependsOn(core)
  .settings(commonSettings)
  .settings(
    name        := "sdl3_mixer",
    description := "Scala Native bindings for SDL_mixer 3, built on the sdl3 core binding",
  )

lazy val root = project
  .in(file("."))
  .aggregate(core, ttf, image, mixer)
  .settings(
    name           := "sdl3-suite",
    publish / skip := true,
  )
