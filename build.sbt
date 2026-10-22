import com.tomshley.magicroot.sbt.projectsettings.PublishGitLabPlugin

lazy val projectName = "paste-surface-scala"
lazy val projectOrgName = "com.pastestack"

ThisBuild / versionScheme := Some("semver-spec")

// The renderer checks load the paste-assetgraph native library through JNA in
// the forked test JVM — an explicitly exported PASTE_ASSETGRAPH_LIB is
// inherited, otherwise the runtime's bundled library is used.
ThisBuild / Test / fork := true

lazy val commonSettings = Seq(
  organization := projectOrgName,
  // The house Scala 3 line (magicroot's ProjectSettingDefinitions.Scala3).
  scalaVersion := "3.4.2",
  magicRootPublishGitLabProjectId := sys.env.get("TOMSHLEY_CICD_PUBLISH_PROJECT_ID").map(_.toInt).getOrElse(70289607),
  // The plugin looks for .credentials.gitlab in the build root; local runs get
  // it as a secure file, so read it from there as well. In CI the plugin already
  // authenticates with the job token, and a second credential for the same
  // realm would override it.
  credentials ++= {
    val secureFile = (ThisBuild / baseDirectory).value / ".secure_files" / ".credentials.gitlab"
    if (sys.env.contains("CI_JOB_TOKEN") || !secureFile.exists()) Seq.empty else Seq(Credentials(secureFile))
  },
  version := {
    // VERSION is the single source of the released version, as it is for every
    // other project in the family; CI checks a release tag against the same file.
    val baseVersion = IO.read((ThisBuild / baseDirectory).value / "VERSION").trim
    val tag = sys.env.get("CI_COMMIT_TAG").filter(_.nonEmpty)
    val revision = sys.env.get("TOMSHLEY_CICD_BUILD_REVISION").filter(_.nonEmpty)
    tag.map(_.stripPrefix("v"))
      .orElse(revision.map(r => s"$baseVersion-$r"))
      .getOrElse(baseVersion)
  },
  scalacOptions ++= Seq(
    "-encoding", "utf8",
    "-deprecation",
    "-feature",
    "-unchecked"
  )
)

lazy val `paste-surface-scala` = (project in file("."))
  .enablePlugins(SbtTwirl, PublishGitLabPlugin)
  .settings(commonSettings)
  .settings(
    name := projectName,
    Compile / TwirlKeys.compileTemplates / sourceDirectories := Seq(
      (Compile / sourceDirectory).value / "twirl"
    ),
    libraryDependencies ++= Seq(
      "org.playframework.twirl" %% "twirl-api" % "2.0.1",
      "com.typesafe" % "config" % "1.4.3",
      // The image-sequence catalog serializes itself into an inert
      // script[type="application/json"] block — its own serializer,
      // not a transitive accident.
      "com.fasterxml.jackson.core" % "jackson-databind" % "2.15.2",
      // Its POM already declares the JNA the loader needs.
      "com.pastestack" %% "paste-assetgraph-runtime" % "0.3.1"
    )
  )
  // Referenced by id rather than the lazy val: pekko depends on this project,
  // and a direct reference in both directions never finishes initializing.
  .aggregate(LocalProject("pekko"))

lazy val pekko = (project in file("pekko"))
  .dependsOn(`paste-surface-scala`)
  .enablePlugins(PublishGitLabPlugin)
  .settings(commonSettings)
  .settings(
    name := "paste-surface-pekko",
    // The host application carries its own Pekko, so the glue only compiles
    // against it.
    libraryDependencies ++= Seq(
      "org.apache.pekko" %% "pekko-http" % "1.3.0" % Provided,
      "org.apache.pekko" %% "pekko-stream" % "1.5.0" % Provided,
      "org.apache.pekko" %% "pekko-actor-typed" % "1.5.0" % Provided,
      "org.apache.pekko" %% "pekko-http-testkit" % "1.3.0" % Test,
      "org.apache.pekko" %% "pekko-stream-testkit" % "1.5.0" % Test,
      "org.scalatest" %% "scalatest" % "3.2.19" % Test
    )
  )
