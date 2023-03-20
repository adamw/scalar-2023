package scalar

import jdk.incubator.concurrent.StructuredTaskScope
import org.slf4j.LoggerFactory
import ox.Ox
import ox.Ox.*
import ox.channels.{orThrow, select, Channel, ChannelState, Sink, Source}

import java.util.concurrent.Semaphore
import scala.annotation.tailrec
import scala.concurrent.duration.DurationInt
import scala.util.Random

val log = LoggerFactory.getLogger("oxes")

def findUser(): String =
  Thread.sleep(1000)
  "user"

def fetchOrder(): String =
  Thread.sleep(1000)
  "order"

@main def main0(): Unit =
  log.info("Starting ...")
  val result =
    try {
      val scope = new StructuredTaskScope.ShutdownOnFailure()
      try {
        val user = scope.fork(() => findUser())
        val order = scope.fork(() => fetchOrder())
        scope.join()
        scope.throwIfFailed()
        (user.resultNow, order.resultNow)
      } finally scope.close()
    }
  log.info(result.toString)

@main def main1(): Unit =
  log.info("Starting ...")

  def forkFindUser(using Ox) = fork(findUser())

  val result = scoped {
    val user = forkFindUser
    val order = fork(fetchOrder())
    (user.join(), order.join())
  }
  log.info(result.toString())

@main def main2(): Unit =
  scoped {
    class RateLimiter(perSecond: Int):
      val s = new Semaphore(perSecond)
      fork {
        forever {
          s.release(perSecond - s.availablePermits())
          Thread.sleep(1000)
        }
      }

      def run[T](t: => T): T =
        s.acquire()
        t

    val rl = RateLimiter(3)
    for (i <- 1 to 10) rl.run(log.info(s"Task$i"))
  }

@main def main3(): Unit =
  val c = Channel[String]()
  scoped {
    fork {
      c.send("Hello,")
      c.send("world")
      c.send("from Scalar")
      c.done()
    }

    val t = fork {
      foreverWhile {
        c.receive() match
          case Left(e: ChannelState.Error) =>
            log.error("Channel error", e)
            false
          case Left(ChannelState.Done) => false
          case Right(v) =>
            log.info(s"Got: $v")
            true
      }
    }

    t.join()
  }

@main def main4(): Unit =
  @tailrec
  def producer(s: Sink[String]): Nothing =
    s.send(Random.nextString(Random.nextInt(100)))
    Thread.sleep(Random.nextInt(200))
    producer(s)

  case object Tick
  def consumer(strings: Source[String]): Nothing =
    scoped {
      val stringLengths = strings.map(_.length)
      val tick = Source.tick(1.second, Tick)

      @tailrec
      def doConsume(acc: Int): Nothing =
        select(stringLengths, tick).orThrow match
          case Tick =>
            log.info(s"Total length received during the last second: ${acc}")
            doConsume(0)
          case length: Int => doConsume(acc + length)

      doConsume(0)
    }

  val c = Channel[String]()
  scoped {
    fork(producer(c))
    fork(consumer(c))
    log.info("Press any key to exit ...")
    // readline
    System.in.read()
  }
