package scalar

@main def main0(): Unit =
  import sttp.client4.quick.*
  println(quickRequest.get(uri"https://httpbin.org/ip").send())

@main def main1(): Unit =
  import sttp.client4.{Request, Response}
  import sttp.client4.quick.*
  val req: Request[String] = quickRequest.get(uri"https://httpbin.org/ip")
  val resp: Response[String] = req.send()
  println(resp)

@main def main2(): Unit =
  import sttp.client4.{Request, Response}
  import sttp.client4.*
  import scala.concurrent.duration.Duration
  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.{Await, Future}

  val backend: WebSocketBackend[Future] = DefaultFutureBackend()
  val req: WebSocketRequest[Future, Either[String, Unit]] = basicRequest
    .get(uri"wss://ws.postman-echo.com/raw")
    .response(asWebSocket[Future, Unit] { ws =>
      for {
        _ <- ws.sendText("Hello")
        _ <- ws.sendText("Scalar")
        msg1 <- ws.receiveText()
        msg2 <- ws.receiveText()
      } yield println(s"Received: $msg1, $msg2")
    })
  println("Sending ...")
  val resp: Future[Response[Either[String, Unit]]] = req.send(backend)
  Await.result(resp, Duration.Inf)
  println("Done")
