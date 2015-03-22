import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.{Level, ConsoleHandler, Logger}

import com.twitter.finagle.FailFastException
import com.twitter.finagle.builder.ClientBuilder
import com.twitter.finagle.http.Http
import com.twitter.finagle.stats.{SummarizingStatsReceiver, JavaLoggerStatsReceiver}
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
        logger.info("SuccessCount: %s".format(successCount.intValue()))
        logger.info("ErrorCount: %s".format(errorCount.intValue()))
        logger.info("")
        logger.info(stats.summary())
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
      .reportHostStats(stats)
      .logger(logger)
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
