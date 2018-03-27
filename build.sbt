lazy val linebacker = project.in(file("."))
    .settings(commonSettings)
    .settings(
      name := "linebacker"
    )

/***********************************************************************\
                      Boilerplate below these lines
\***********************************************************************/




lazy val commonSettings = Seq(
  organization := "io.chrisdavenport",
  licenses += ("MIT", url("https://opensource.org/licenses/MIT")),

  scalaVersion := "2.12.4",
  crossScalaVersions := Seq(scalaVersion.value, "2.11.12"),

  addCompilerPlugin("org.spire-math" % "kind-projector" % "0.9.6" cross CrossVersion.binary),
  scalafmtOnCompile := true,
  scalafmtTestOnCompile := true,


  libraryDependencies ++= Seq(
    "org.typelevel"               %% "cats-effect"                % "0.10",
    "co.fs2"                      %% "fs2-core"                   % "0.10.3",
    "org.specs2"                  %% "specs2-core"                % "4.0.3"       % Test,
  )
)
