This POC is the outcome of https://wiki.hq.tni01.com/display/~phivale/Explore+use+of+finagle+http+client


This is a test-bed for carrying out experiments with finagle basics.

There are two main classes:
* HttpClient
* HttpServer

The HttpServer spins up two finagle services using the HTTP codec. These echo a request, emitting messages periodically to stdout.

The HttpClient spins up a finagle client, connecting to the HttpServer service, sends a number of requests, and shuts down after receiving responses.
Stats are captured and output at the completion. The ClientBuilder configuration is set up in separate methods to make it easy to experiment with different
configurations.

Both client and server can be run via sbt:
```sh
$ sbt run
```
This will prompt for selecting which main to run.

Alternately, these can be run directly via sbt args:
```sh
$ sbt "run-main HttpServer"
$ sbt "run-main HttpClient"
```


