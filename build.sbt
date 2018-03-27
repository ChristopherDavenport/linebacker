lazy val linebacker = project.in(file("."))
    .settings(commonSettings)
    .settings(
      name := "linebacker"
    )

lazy val contributors = Seq(
  "ChristopherDavenport" -> "Christopher Davenport"
)


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

lazy val releaseSettings = Seq(
  releaseCrossBuild := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  publishArtifact in Test := false
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/functional-streams-for-scala/fs2"),
      "git@github.com:ChristopherDavenport/linebacker.git"
    )
  ),
  homepage := Some(url("https://github.com/ChristopherDavenport/linebacker")),
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  publishMavenStyle := true,
  pomIncludeRepository := { _ => false},
  pomExtra := {
    <developers>
      {for ((username, name) <- contributors) yield
      <developer>
        <id>{username}</id>
        <name>{name}</name>
        <url>http://github.com/{username}</url>
      </developer>
      }
    </developers>
  }
)
