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
    new HttpServer(10000)
    new HttpServer(10001)
  }
}

class HttpServer(port: Int = 10000) {
  var requestCount = new AtomicInteger(0)
  val service: Service[HttpRequest, HttpResponse] = new Service[HttpRequest, HttpResponse] {
    def apply(req: HttpRequest) = {
      val currentCount = requestCount.getAndAdd(1)
      if (currentCount % 1000 == 0) {
        println("Server on port [%s] Receive count %s".format(port, currentCount))
      }
      Future(new DefaultHttpResponse(req.getProtocolVersion, OK))
    }
  }

  val address: SocketAddress = new InetSocketAddress(port)

  val server: Server = ServerBuilder()
    .codec(Http())
    .bindTo(address)
    .name("HttpServer")
    .build(service)

  println("Server started on port %s".format(port))
}