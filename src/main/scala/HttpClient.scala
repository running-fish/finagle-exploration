import java.util.concurrent.atomic.AtomicInteger

import com.twitter.finagle.FailFastException
import com.twitter.finagle.builder.ClientBuilder
import com.twitter.finagle.http.Http
import com.twitter.util.{Duration, Future}
import org.jboss.netty.handler.codec.http.HttpVersion._
import org.jboss.netty.handler.codec.http.HttpMethod._
import org.jboss.netty.handler.codec.http.{DefaultHttpRequest, HttpResponse}

object HttpClient {
  val address = "localhost:10000"
  def main(args: Array[String]): Unit = {
    val client = makeVanillaClient()
    val countdownHook = new AtomicInteger(20001)
    val successCount = new AtomicInteger(0)
    val errorCount = new AtomicInteger(0)

    Range(1, countdownHook.get()).foreach{i =>
      val request = new DefaultHttpRequest(HTTP_1_1, GET, "/")
      val responseFuture: Future[HttpResponse] = client(request)
      responseFuture.onSuccess {response =>
        successCount.incrementAndGet()
        val responseCount = countdownHook.decrementAndGet()
        if (responseCount == 1) {
          shutdown()
        }
      }
      responseFuture.onFailure { ex =>
        errorCount.incrementAndGet()
        ex match {
          case f: com.twitter.finagle.FailedFastException => {/* ignore; no message */}
          case m: Exception => ex.printStackTrace()
        }

        val responseCount = countdownHook.decrementAndGet()
        if (responseCount == 1) {
          shutdown()
        }
      }

      def shutdown(): Unit = {
        println("SuccessCount: %s".format(successCount.intValue()))
        println("ErrorCount: %s".format(errorCount.intValue()))
        client.release()
      }
    }
  }

  def makeVanillaClient() = {
    ClientBuilder()
      .codec(Http())
      .hosts(address)
      .tcpConnectTimeout(Duration.fromSeconds(1))
      .hostConnectionLimit(1)
      .build()
  }

  def makeOverConfiguredClient() = {
    ClientBuilder()
      .codec(Http())
      .hosts(address)
      .hostConnectionMaxWaiters(20)
      .requestTimeout(Duration.fromMilliseconds(7000))
      .hostConnectionCoresize(10)
      .hostConnectionLimit(50)
      .build()
  }
}
