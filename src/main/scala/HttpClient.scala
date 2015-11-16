import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.{Level, Logger}

import com.twitter.common.quantity.{Amount, Time}
import com.twitter.common.zookeeper.{ServerSetImpl, ZooKeeperClient}
import com.twitter.finagle.builder.ClientBuilder
import com.twitter.finagle.http.Http
import com.twitter.finagle.stats.SummarizingStatsReceiver
import com.twitter.finagle.zookeeper.ZookeeperServerSetCluster
import com.twitter.util.{Duration, Future}
import org.jboss.netty.handler.codec.http.HttpMethod._
import org.jboss.netty.handler.codec.http.HttpVersion._
import org.jboss.netty.handler.codec.http.{DefaultHttpRequest, HttpResponse}

import scala.collection.JavaConverters._


object HttpClient {
  val address = "localhost:10000"
  val logger = Logger.getLogger("client")
  val stats = new SummarizingStatsReceiver()
  logger.setLevel(Level.ALL)

  def main(args: Array[String]): Unit = {
    val client = makeFinagleProxy()
    val countdownHook = new AtomicInteger(10000)

    Range(0, countdownHook.get()).foreach{i =>
      val request = new DefaultHttpRequest(HTTP_1_1, GET, "/")
      val responseFuture: Future[HttpResponse] = client(request)
      responseFuture.onSuccess {response =>
        val responseCount = countdownHook.decrementAndGet()
        if (responseCount == 1) {
          println("Received response from server ", response)
        //  shutdown()
        }
      }
      responseFuture.onFailure { ex =>
        ex match {
          case f: com.twitter.finagle.FailedFastException => {/* ignore; no message */}

          case m: Exception => ex.printStackTrace()
        }

        val responseCount = countdownHook.decrementAndGet()
        if (responseCount == 1) {
        //  shutdown()
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

  def makeFinagleProxy() ={
    var zookeeperAddr = new InetSocketAddress("zk1", 2181)
    val zkaddrlist: List[InetSocketAddress] = List(zookeeperAddr)
    val timeout = Amount.of(10, Time.SECONDS)
    val zkClient = new ZooKeeperClient(timeout,  zkaddrlist.asJava)
    val path="/services/Finagle-HttpServer"

    val serverSet = new ServerSetImpl(zkClient, path)
    val cluster = new ZookeeperServerSetCluster(serverSet)

    ClientBuilder()
      .codec(Http())
      .cluster(cluster)
      .requestTimeout(Duration.fromMilliseconds(7000))
      .hostConnectionCoresize(10)
      .hostConnectionLimit(50)
      .reportTo(stats)
      .build()

  }
}
