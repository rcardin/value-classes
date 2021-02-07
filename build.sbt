name := "value-types"

version := "0.1"

scalaVersion := "2.13.4"

idePackagePrefix := Some("in.rcard.value")

scalacOptions += "-Ymacro-annotations"

libraryDependencies += "io.estatico" %% "newtype" % "0.4.4"
