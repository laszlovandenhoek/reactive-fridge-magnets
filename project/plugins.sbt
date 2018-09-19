addSbtPlugin("io.spray" % "sbt-revolver" % "0.9.1")

// https://scalacenter.github.io/scalajs-bundler/
addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "0.13.1")

// http://www.scala-js.org/tutorial/basic/
// https://github.com/portable-scala/sbt-crossproject
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject"      % "0.6.0")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.25")
addSbtPlugin("org.portable-scala" % "sbt-scala-native-crossproject" % "0.6.0")
addSbtPlugin("org.scala-native"   % "sbt-scala-native"              % "0.3.7")