import java.net.{InetSocketAddress, SocketAddress}
import java.util.concurrent.atomic.AtomicInteger

import com.twitter.finagle.Service
import com.twitter.finagle.builder.{Server, ServerBuilder}
import com.twitter.finagle.http.Http
import com.twitter.util.Future
import org.jboss.netty.handler.codec.http.HttpResponseStatus._
import org.jboss.netty.handler.codec.http.{DefaultHttpResponse, HttpRequest, HttpResponse}

object HttpServer {
  def main(args: Array[String]) {
    new HttpServer(10000, 0)
    new HttpServer(10001, 1)
  }
}

class HttpServer(port: Int = 10000, serverId: Int) {
  var requestCount = new AtomicInteger(0)
  val service: Service[HttpRequest, HttpResponse] = new Service[HttpRequest, HttpResponse] {
    def apply(req: HttpRequest) = {
      val currentCount = requestCount.getAndAdd(1)
      //println("Server processing currentCount " + currentCount + " uri " + req.getUri)
      if (currentCount % 100 == 0) {
        println("Server on port [%s] Receive count %s".format(port, currentCount))
      }
      if (serverId == 1 && currentCount == 4)
        Thread.sleep(10)
      Future(new DefaultHttpResponse(req.getProtocolVersion, OK))
    }
  }

  val address: SocketAddress = new InetSocketAddress(port)

  val server: Server = ServerBuilder()
    .codec(Http())
    .bindTo(address)
    .name("HttpServer")
//    .maxConcurrentRequests(3)
    .build(service)

  println("Server started on port %s".format(port))
}