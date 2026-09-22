import com.tomshley.magicroot.sbt.projectsettings.PublishGitLabPlugin

lazy val projectName = "paste-surface-scala"
lazy val projectOrgName = "com.pastestack"

ThisBuild / versionScheme := Some("semver-spec")

lazy val `paste-surface-scala` = (project in file("."))
  .enablePlugins(SbtTwirl, PublishGitLabPlugin)
  .settings(
    name := projectName,
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
      val baseVersion = IO.read(baseDirectory.value / "VERSION").trim
      val tag = sys.env.get("CI_COMMIT_TAG").filter(_.nonEmpty)
      val revision = sys.env.get("TOMSHLEY_CICD_BUILD_REVISION").filter(_.nonEmpty)
      tag.map(_.stripPrefix("v"))
        .orElse(revision.map(r => s"$baseVersion-$r"))
        .getOrElse(baseVersion)
    },
    Compile / TwirlKeys.compileTemplates / sourceDirectories := Seq(
      (Compile / sourceDirectory).value / "twirl"
    ),
    libraryDependencies ++= Seq(
      "org.playframework.twirl" %% "twirl-api" % "2.0.1",
      "com.typesafe" % "config" % "1.4.3"
    ),
    scalacOptions ++= Seq(
      "-encoding", "utf8",
      "-deprecation",
      "-feature",
      "-unchecked"
    )
  )
