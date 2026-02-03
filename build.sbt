lazy val projectName = "paste-surface-scala"
lazy val projectOrgName = "com.pastestack"

lazy val `paste-surface-scala` = (project in file("."))
  .enablePlugins(SbtTwirl)
  .settings(
    name := projectName,
    organization := projectOrgName,
    scalaVersion := "3.3.3",
    
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
    
    // Scala compiler options
    scalacOptions ++= Seq(
      "-encoding", "utf8",
      "-deprecation",
      "-feature",
      "-unchecked"
    )
  )
