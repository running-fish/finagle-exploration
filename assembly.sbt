import AssemblyKeys._ // put this at the top of the file

assemblySettings

// your assembly settings here

jarName in assembly := "finagle-server.jar"

mainClass in assembly := Some("HttpServer")

