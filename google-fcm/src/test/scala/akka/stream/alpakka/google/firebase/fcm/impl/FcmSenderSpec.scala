/*
 * Copyright (C) 2016-2020 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.stream.alpakka.google.firebase.fcm.impl

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.alpakka.google.GoogleSettings
import akka.stream.alpakka.google.firebase.fcm.{FcmErrorResponse, FcmNotification, FcmSettings, FcmSuccessResponse}
import akka.stream.alpakka.google.http.GoogleHttp
import akka.stream.alpakka.testkit.scaladsl.LogCapturing
import akka.testkit.TestKit
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class FcmSenderSpec
    extends TestKit(ActorSystem())
    with AnyWordSpecLike
    with Matchers
    with ScalaFutures
    with MockitoSugar
    with BeforeAndAfterAll
    with LogCapturing {

  import FcmJsonSupport._

  override def afterAll: Unit =
    TestKit.shutdownActorSystem(system)

  implicit val defaultPatience =
    PatienceConfig(timeout = 2.seconds, interval = 50.millis)

  implicit val executionContext: ExecutionContext = system.dispatcher

  implicit val conf = FcmSettings.create("test-XXX@test-XXXXX.iam.gserviceaccount.com", "RSA KEY", "projectId")

  "FcmSender" should {

    "call the api as the docs want to" in {
      val sender = new FcmSender
      val gHttp = mock[GoogleHttp]
      when(
        gHttp.singleRequestWithOAuth(any[HttpRequest]())(any[GoogleSettings]())
      ).thenReturn(
        Future.successful(HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, """{"name": ""}""")))
      )

      Await.result(sender.send(gHttp, FcmSend(false, FcmNotification.empty)), defaultPatience.timeout)

      val captor: ArgumentCaptor[HttpRequest] = ArgumentCaptor.forClass(classOf[HttpRequest])
      verify(gHttp).singleRequestWithOAuth(captor.capture())(any[GoogleSettings]())
      val request: HttpRequest = captor.getValue
      Unmarshal(request.entity).to[FcmSend].futureValue shouldBe FcmSend(false, FcmNotification.empty)
      request.uri.toString shouldBe "https://fcm.googleapis.com/v1/projects/projectId/messages:send"
    }

    "parse the success response correctly" in {
      val sender = new FcmSender
      val gHttp = mock[GoogleHttp]
      when(
        gHttp.singleRequestWithOAuth(any[HttpRequest]())(any[GoogleSettings]())
      ).thenReturn(
        Future.successful(HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, """{"name": "test"}""")))
      )

      sender
        .send(gHttp, FcmSend(false, FcmNotification.empty))
        .futureValue shouldBe FcmSuccessResponse("test")
    }

    "parse the error response correctly" in {
      val sender = new FcmSender
      val gHttp = mock[GoogleHttp]
      when(
        gHttp.singleRequestWithOAuth(any[HttpRequest]())(any[GoogleSettings]())
      ).thenReturn(
        Future.successful(
          HttpResponse(status = StatusCodes.BadRequest,
                       entity = HttpEntity(ContentTypes.`application/json`, """{"name": "test"}"""))
        )
      )

      sender
        .send(gHttp, FcmSend(false, FcmNotification.empty))
        .futureValue shouldBe FcmErrorResponse(
        """{"name":"test"}"""
      )
    }

  }
}
