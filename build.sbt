import scala.io.Source

lazy val projectName = "paste-surface-scala"
lazy val projectOrgName = "com.pastestack"

ThisBuild / versionScheme := Some("semver-spec")

lazy val `paste-surface-scala` = (project in file("."))
  .enablePlugins(SbtTwirl)
  .settings(
    name := projectName,
    organization := projectOrgName,
    scalaVersion := "3.3.3",
    version := {
      val versionFile = baseDirectory.value / "VERSION"
      val base = if (versionFile.exists()) IO.readLines(versionFile).filter(_.nonEmpty).mkString.trim
                 else "0.1.0"
      if (sys.env.contains("CI_COMMIT_TAG")) base
      else s"$base-SNAPSHOT"
    },
    
    // Twirl settings
    Compile / TwirlKeys.compileTemplates / sourceDirectories := Seq(
      (Compile / sourceDirectory).value / "twirl"
    ),
    
    libraryDependencies ++= Seq(
      "org.playframework.twirl" %% "twirl-api" % "2.0.1",
      "com.typesafe" % "config" % "1.4.3"
    ),
    
    // Publishing
    publishMavenStyle := true,
    publishTo := Some("gitlab-maven" at
      s"https://gitlab.com/api/v4/projects/${sys.env.getOrElse("GITLAB_PUBLISH_PROJECT_ID", "70289607")}/packages/maven"),
    credentials ++= {
      val projectCredentials = file(".secure_files") / ".credentials.gitlab"
      val homeCredentials = Path.userHome / ".sbt" / "gitlab-token"

      if (projectCredentials.exists()) {
        Seq(Credentials(projectCredentials))
      } else if (homeCredentials.exists()) {
        Seq(Credentials(
          "GitLab Packages Registry",
          "gitlab.com",
          "Private-Token",
          Source.fromFile(homeCredentials).getLines().mkString.trim
        ))
      } else {
        sys.env.get("GITLAB_TOKEN").map { token =>
          Credentials("GitLab Packages Registry", "gitlab.com", "Private-Token", token)
        }.toSeq
      }
    },
    
    // Scala compiler options
    scalacOptions ++= Seq(
      "-encoding", "utf8",
      "-deprecation",
      "-feature",
      "-unchecked"
    )
  )
