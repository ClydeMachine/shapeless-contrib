import sbt._
import Keys._

import sbtrelease._
import sbtrelease.ReleasePlugin._
import sbtrelease.ReleasePlugin.ReleaseKeys._
import sbtrelease.ReleaseStateTransformations._
import sbtrelease.Utilities._

import com.typesafe.sbt.pgp.PgpKeys._

object ShapelessContribBuild extends Build {

  val shapelessVersion = "1.2.4"
  val scalazVersion = "7.0.0-RC1"
  val scalacheckVersion = "1.10.0"


  lazy val publishSignedArtifacts = ReleaseStep(
    action = st => {
      val extracted = st.extract
      val ref = extracted.get(thisProjectRef)
      extracted.runAggregated(publishSigned in Global in ref, st)
    },
    check = st => {
      // getPublishTo fails if no publish repository is set up.
      val ex = st.extract
      val ref = ex.get(thisProjectRef)
      Classpaths.getPublishTo(ex.get(publishTo in Global in ref))
      st
    },
    enableCrossBuild = true
  )

  lazy val standardSettings = Defaults.defaultSettings ++ releaseSettings ++ Seq(
    organization := "org.typelevel",

    scalaVersion := "2.10.0",
    crossScalaVersions := Seq(/*"2.9.2",*/ "2.10.0"),
    scalacOptions ++= Seq("-unchecked", "-deprecation"),

    libraryDependencies += "com.chuusai" %% "shapeless" % shapelessVersion,

    resolvers += Resolver.sonatypeRepo("releases"),

    // https://github.com/sbt/sbt/issues/603
    conflictWarning ~= { cw =>
      cw.copy(filter = (id: ModuleID) => true, group = (id: ModuleID) => id.organization + ":" + id.name, level = Level.Error, failOnConflict = true)
    },

    sourceDirectory <<= baseDirectory(identity),

    publishTo <<= (version).apply { v =>
      val nexus = "https://oss.sonatype.org/"
      if (v.trim.endsWith("SNAPSHOT"))
        Some("Snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("Releases" at nexus + "service/local/staging/deploy/maven2")
    },
    credentials += Credentials(
      Option(System.getProperty("build.publish.credentials")) map (new File(_)) getOrElse (Path.userHome / ".ivy2" / ".credentials")
    ),

    // adapted from sbt-release defaults
    // * does not perform `pushChanges`
    // * performs `publish-signed` instead of `publish`
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      publishSignedArtifacts,
      setNextVersion,
      commitNextVersion
    ),

    pomIncludeRepository := Function.const(false),
    pomExtra :=
      <url>http://typelevel.org/</url>
        <licenses>
          <license>
            <name>MIT</name>
            <url>http://www.opensource.org/licenses/mit-license.php</url>
            <distribution>repo</distribution>
          </license>
        </licenses>
        <scm>
            <url>https://github.com/typelevel/shapeless-contrib</url>
            <connection>scm:git:git://github.com/typelevel/shapeless-contrib.git</connection>
            <developerConnection>scm:git:git@github.com:typelevel/shapeless-contrib.git</developerConnection>
        </scm>
        <developers>
          <developer>
            <id>larsrh</id>
            <name>Lars Hupel</name>
            <url>https://github.com/larsrh</url>
          </developer>
        </developers>
  )

  lazy val shapelessContrib = Project(
    id = "shapeless-contrib",
    base = file("."),
    settings = standardSettings ++ Seq(
      publishArtifact := false
    ),
    aggregate = Seq(common, scalacheck, scalaz, spire)
  )

  lazy val common = Project(
    id = "common",
    base = file("common"),
    settings = standardSettings ++ Seq(
      name := "shapeless-contrib-common"
    )
  )

  lazy val scalacheck = Project(
    id = "scalacheck",
    base = file("scalacheck"),
    settings = standardSettings ++ Seq(
      name := "shapeless-scalacheck",
      libraryDependencies ++= Seq(
        "org.scalaz" %% "scalaz-core" % scalazVersion,
        "org.scalacheck" %% "scalacheck" % scalacheckVersion,
        "org.scalaz" %% "scalaz-scalacheck-binding" % scalazVersion
      )
    )
  )

  lazy val scalaz = Project(
    id = "scalaz",
    base = file("scalaz"),
    dependencies = Seq(common, scalacheck % "test"),
    settings = standardSettings ++ Seq(
      name := "shapeless-scalaz",
      libraryDependencies ++= Seq(
        "org.scalaz" %% "scalaz-core" % scalazVersion,

        "org.specs2" %% "specs2" % "1.12.3" % "test",
        "org.scalacheck" %% "scalacheck" % scalacheckVersion % "test",
        "org.scalaz" %% "scalaz-scalacheck-binding" % scalazVersion % "test",
        "org.typelevel" %% "scalaz-specs2" % "0.1.3" % "test"
      )
    )
  )

  lazy val spire = Project(
    id = "spire",
    base = file("spire"),
    dependencies = Seq(common, scalacheck % "test"),
    settings = standardSettings ++ Seq(
      name := "shapeless-spire",
      libraryDependencies ++= Seq(
        "org.scalatest" %% "scalatest" % "1.9.1" % "test",
        "org.scalacheck" %% "scalacheck" % scalacheckVersion % "test"
      ),
      libraryDependencies <++= (scalaVersion) { sv =>
        val spireVersion = if (sv startsWith "2.9") "0.3.0" else "0.4.0-M3"
        Seq(
          "org.spire-math" %% "spire" % spireVersion,
          "org.spire-math" %% "spire-scalacheck-binding" % spireVersion % "test"
        )
      }
    )
  )

}

// vim: expandtab:ts=2:sw=2
