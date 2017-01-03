name := """scoring"""

version := "1.4"

scalaVersion := "2.11.8"

libraryDependencies += "com.github.scopt" %% "scopt" % "3.3.0"
libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.2.10"
libraryDependencies += "net.databinder" %% "unfiltered-filter" % "0.8.3"
libraryDependencies += "net.databinder" %% "unfiltered-jetty" % "0.8.3"
libraryDependencies += "com.quantifind" %% "sumac" % "0.3.0"
libraryDependencies += "org.apache.commons" % "commons-math3" % "3.4.1"
libraryDependencies += "commons-io" % "commons-io" % "2.4"
libraryDependencies += "com.github.OriolLopezMassaguer" % "dataframe_2.11" % "1.2.1" classifier "assembly"

enablePlugins(JavaAppPackaging)


