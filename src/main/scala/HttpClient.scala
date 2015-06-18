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
  val address = "localhost:10000,localhost:10001"
  val logger = Logger.getLogger("client")
  val stats = new SummarizingStatsReceiver()
  logger.setLevel(Level.ALL)

  def main(args: Array[String]): Unit = {
//    val client = makeVanillaClient()
    val clientName = if (args.size > 0) args(0) else ""
    val client = makeTuningClient()
    //val countdownHook = new AtomicInteger(5000)
    val countdownHook = new AtomicInteger(50)

    var tmwCount = new AtomicInteger(0)
    Range(0, countdownHook.get()).foreach{i =>
      println("Client making request " + i)
      val request = new DefaultHttpRequest(HTTP_1_1, GET, "/" + clientName + "/" + i)
      //Thread.sleep(25)
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
          case w: com.twitter.finagle.TooManyWaitersException =>
            ex.printStackTrace()
            tmwCount.getAndAdd(1)
          case m: Exception => ex.printStackTrace()
        }

        val responseCount = countdownHook.decrementAndGet()
        if (responseCount == 1) {
          shutdown()
        }
      }

      def shutdown(): Unit = {
        logger.info(stats.summary)

        println("TooManyWaiters received: " + tmwCount.get())
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


  def makeTuningClient() = {
    ClientBuilder()
      .codec(Http())
      .hosts(address)
      //.hostConnectionMaxWaiters(10)
      .hostConnectionLimit(2)
      .requestTimeout(Duration.fromMilliseconds(1100))
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
