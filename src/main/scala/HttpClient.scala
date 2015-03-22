import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.{Level, Logger}

import com.twitter.finagle.builder.ClientBuilder
import com.twitter.finagle.http.Http
import com.twitter.finagle.stats.{SummarizingStatsReceiver}
import com.twitter.util.{Duration, Future}
import org.jboss.netty.handler.codec.http.HttpVersion._
import org.jboss.netty.handler.codec.http.HttpMethod._
import org.jboss.netty.handler.codec.http.{DefaultHttpRequest, HttpResponse}

object HttpClient {
  val address = "localhost:10000"
  val logger = Logger.getLogger("client")
  val stats = new SummarizingStatsReceiver()
  logger.setLevel(Level.ALL)

  def main(args: Array[String]): Unit = {
    val client = makeVanillaClient()
    val countdownHook = new AtomicInteger(10000)

    Range(0, countdownHook.get()).foreach{i =>
      val request = new DefaultHttpRequest(HTTP_1_1, GET, "/")
      val responseFuture: Future[HttpResponse] = client(request)
      responseFuture.onSuccess {response =>
        val responseCount = countdownHook.decrementAndGet()
        if (responseCount == 1) {
          shutdown()
        }
      }
      responseFuture.onFailure { ex =>
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
        logger.info(stats.summary)
        client.release()
      }
    }
  }

  def makeVanillaClient() = {
    ClientBuilder()
      .codec(Http())
      .hosts(address)
      .hostConnectionLimit(1)
      .reportTo(stats)
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
      .reportTo(stats)
      .build()
  }
}
