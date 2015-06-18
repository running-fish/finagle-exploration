Finagle provides connection-pooling on the client-side. This acts as a form of latency-based backpressure.

Client settings which influence this behavior:
* hostConnectionCoresize - minimum connections per host
* hostConnectionLimit - maximum number of connections per host
* hostConnectionMaxWaiters - maximum number of queued requests
* requestTimeout - time limit a request can be "in flight" before it is cancelled

For x connections and y maxWaiters, x + y requests can be "in flight". The (x + y + 1)th will throw TooManyWaitersException

Experiments we performed:
* 1 connection, 10 maxWaiters, 15 requests. We receive 4 TooManyWaitersExceptions
* 2 connections, 10 maxWaiters, 15 requests. Receive 3 TooManyWaitersExceptions
* 5 connections, 10 maxWaiters, 15 requests. No TooManyWaitersExceptions
* Two services, 2 connections, 10 maxWaiters, 20 requests. No TooManyWaitersExceptions. Therefore queue is per-host.
* Server processes 3 requests, client has 5 connections, 10 maxWaiters, 15 requests. No TooManyWaitersExceptions but service
clearly processes in batches of 3.
* 2 clients, server does 3 concurrent requests. Server processes batches of 3, intermixed from the clients
* client timeouts a little longer than service time. Server does 3 concurrent, client has 5 connections. The requests that
are in flight in a connection but not running do time out, indicating that timeout starts from when the request is made, until
the time it is resolved (e.g. working and queueing do not stop the timer)
* Two services, requests are distributed more-or-less evenly
* Two services, one is slower, we seem to be equally distributing work with one processing slowly (if work is queued immediately)
* Two services, one is slower, request traffic is slow enough to see the slowness from the server, client-side load balancing
happens (slow service gets fewer requests)
* Two services, one has periodic lag (e.g. large requests), client-side load balancing happens
* Two services, one has periodic lag at the beginning, faster service gets more traffic, then when the slow one "recovers" they
get roughly equal traffic (i.e. both output progress counts at similar rate)


Other notes:
Timeouts are a tricky thing. They seem to work fairly well in general terms, but tuning these too finely seems confusing
or unpredictable. We were unable to find experiments that we could accurately predict exactly how many requests would time
out in a very detailed way. At a larger scale, we were able to predict, for example, if the service is slow and the timeouts
are tight you will have requests time out.



