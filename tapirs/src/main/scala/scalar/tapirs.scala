package scalar

import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.server.model.ServerResponse
import sttp.tapir.server.netty.{FutureRoute, NettyFutureServer, NettyFutureServerBinding, Route}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.io.StdIn
import java.net.InetSocketAddress
import io.netty.buffer.{ByteBuf, Unpooled}
import sttp.tapir.server.netty.loom.{Id, IdRoute, NettyIdServer}

@main def main1(): Unit =
  val route: IdRoute = { request =>
    val body = s"Hello, ${request.queryParameters.get("name").getOrElse("undefined")}!"
    Some(ServerResponse(StatusCode.Ok, Nil, Some(Unpooled.wrappedBuffer(body.getBytes())), None))
  }

  val binding = NettyIdServer().addRoute(route).start()

  StdIn.readLine()
  binding.stop()

@main def main2(): Unit =
  val helloWorld = endpoint.get
    .in("hello")
    .in(query[String]("name"))
    .out(stringBody)
    .serverLogicSuccess[Id](name => s"Hello, $name!")

  val binding = NettyIdServer().addEndpoint(helloWorld).start()

  StdIn.readLine()
  binding.stop()

@main def main3(): Unit =
  import sttp.tapir.json.upickle.*
  import upickle.default.*
  import upickle.implicits.key
  import sttp.tapir.Schema.annotations.encodedName
  import sttp.tapir.swagger.bundle.SwaggerInterpreter

  case class Hello(@key("the_name") @encodedName("the_name") name: String, from: String)

  given ReadWriter[Hello] = macroRW
  given Schema[Hello] = Schema.derived[Hello]

  val helloJson = endpoint.get
    .in("hello")
    .out(jsonBody[Hello])
    .serverLogicSuccess[Id](_ => Hello("world", "scalar"))

  val swaggerEndpoints = SwaggerInterpreter().fromServerEndpoints[Id](List(helloJson), "Scalar", "1.0")

  val binding = NettyIdServer().addEndpoint(helloJson).addEndpoints(swaggerEndpoints).start()

  StdIn.readLine()
  binding.stop()
