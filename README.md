This POC is the outcome of https://wiki.hq.tni01.com/display/~phivale/Explore+use+of+finagle+http+client


This is a test-bed for carrying out experiments with finagle zookeeper cluster.

There are two main classes:
* HttpClient - Connects to a zookeeper cluster using zk client and discovers the service address. Disconnects the zk client and closes the client channel on completion.
* HttpServer - Registers with a given zookeeper instance. The ip address and port number for the service are published in ZK.

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
$ sbt compile
$ sbt "run-main HttpServer"
```

Check zk. The service should be registered under services -> Fnagle-HttpServer. A host and port number should be exposed.
```
sbt "run-main HttpClient"
```

You should see something like below for the server : 
```
Server on port [10000] Receive count 0
Server on port [10000] Receive count 1000
Server on port [10000] Receive count 2000
```

You should see the below for client :
```
(Received response from server ,DefaultHttpResponse(chunked: false)
HTTP/1.1 200 OK Content-Length: 0)
```

Next steps based on this POC
- https://wiki.hq.tni01.com/pages/viewpage.action?pageId=91425061
- https://jira.tendrilinc.com/browse/PAIIN-377


Learning and Recommendations:-
- The finagle/finatra artifact ID version (finagle-serversets_2.10) has to match the scala version. SBT throws exceptions if multiple library versions are pulled in.

- Currently the weather service Java code pulls in Netty version 3.8.x and 3.10.x. It is recommended to use the default version of Netty (3.10.x) that finagle pulls in.

- To avoid "java.lang.InterruptedException" when the client shuts down, always close the established zookeeper client connection before closing the finagle client connection.

- Based on the POC the finagle zookeeper server set approach seems worth pursuing. It will help us to get away from Amazon ELB. More questions such as how it handles RPC communincation & its benefits over the current "service-deps-registry" will be answered in the follow-up spikes.

