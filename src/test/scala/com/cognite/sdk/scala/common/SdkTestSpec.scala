package com.cognite.sdk.scala.common

import java.util.UUID

import cats.Id
import com.cognite.sdk.scala.v1._
import com.softwaremill.sttp.{MonadError, Request, Response, SttpBackend}
import org.scalatest.{FlatSpec, Matchers}

class LoggingSttpBackend[R[_], S](delegate: SttpBackend[R, S]) extends SttpBackend[R, S] {
  override def send[T](request: Request[T, S]): R[Response[T]] =
    responseMonad.map(responseMonad.handleError(delegate.send(request)) {
      case e: Exception =>
        println(s"Exception when sending request: $request, ${e.toString}") // scalastyle:ignore
        responseMonad.error(e)
    }) { response =>
      println(s"request ${request.body.toString}") // scalastyle:ignore
      println(s"response ${response.toString()}") // scalastyle:ignore
      if (response.isSuccess) {
        println(s"For request: $request got response: $response") // scalastyle:ignore
      } else {
        println(s"For request: $request got response: $response") // scalastyle:ignore
      }
      response
    }
  override def close(): Unit = delegate.close()
  override def responseMonad: MonadError[R] = delegate.responseMonad
}

abstract class SdkTestSpec extends FlatSpec with Matchers {

  val client: GenericClient[Id, Nothing] = GenericClient.forAuth[Id, Nothing](
    "scala-sdk-test", auth)(implicitly, sttpBackend)

  val greenfieldClient: GenericClient[Id, Nothing] = GenericClient.forAuth[Id, Nothing](
    "scala-sdk-test", greenfieldAuth, "https://greenfield.cognitedata.com")(implicitly, sttpBackend)

  val projectName: String = client.login.status().project

  def shortRandom(): String = UUID.randomUUID().toString.substring(0, 8)

  private lazy val apiKey = Option(System.getenv("TEST_API_KEY_READ"))
    .getOrElse(throw new RuntimeException("TEST_API_KEY_READ not set"))
  implicit lazy val auth: Auth = ApiKeyAuth(apiKey)
  private lazy val greenfieldApiKey = Option(System.getenv("TEST_API_KEY_GREENFIELD"))
    .getOrElse(throw new RuntimeException("TEST_API_KEY_GREENFIELD not set"))
  implicit lazy val greenfieldAuth: Auth = ApiKeyAuth(greenfieldApiKey)
}