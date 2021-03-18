/*
 * Copyright (C) 2016-2020 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.stream.alpakka.google.firebase.fcm.impl
import akka.NotUsed
import akka.annotation.InternalApi
import akka.http.scaladsl.Http
import akka.stream.alpakka.google.auth.ServiceAccountCredentials
import akka.stream.alpakka.google.{GoogleAttributes, GoogleSettings}
import akka.stream.{Attributes, Materializer}
import akka.stream.alpakka.google.firebase.fcm._
import akka.stream.scaladsl.Flow
import com.github.ghik.silencer.silent

import scala.concurrent.Future

/**
 * INTERNAL API
 */
@InternalApi
private[fcm] object FcmFlows {

  private[fcm] def fcmWithData[T](conf: FcmSettings): Flow[(FcmNotification, T), (FcmResponse, T), NotUsed] =
    Flow
      .fromMaterializer { (mat, attr) =>
        implicit val settings = resolveSettings(conf)(mat, attr)
        val sender = new FcmSender()
        Flow[(FcmNotification, T)].mapAsync(conf.maxConcurrentConnections) {
          case (notification, data) =>
            sender
              .send(Http(mat.system), FcmSend(conf.isTest, notification))(mat, settings)
              .zip(Future.successful(data))
        }
      }
      .mapMaterializedValue(_ => NotUsed)

  private[fcm] def fcm(conf: FcmSettings): Flow[FcmNotification, FcmResponse, NotUsed] =
    Flow
      .fromMaterializer { (mat, attr) =>
        implicit val settings = resolveSettings(conf)(mat, attr)
        val sender = new FcmSender()
        Flow[FcmNotification].mapAsync(conf.maxConcurrentConnections) { notification =>
          sender.send(Http(mat.system), FcmSend(conf.isTest, notification))(mat, settings)
        }
      }
      .mapMaterializedValue(_ => NotUsed)

  @silent("deprecated")
  private def resolveSettings(conf: FcmSettings)(mat: Materializer, attr: Attributes): GoogleSettings = {
    val settings = GoogleAttributes.resolveSettings(mat, attr)
    val credentials =
      if (conf.privateKey == "deprecated")
        settings.credentials
      else
        ServiceAccountCredentials(
          conf.projectId,
          conf.clientEmail,
          conf.privateKey,
          List("https://www.googleapis.com/auth/firebase.messaging")
        )(mat.system)
    val forwardProxy = conf.forwardProxy.map(_.toCommonForwardProxy(mat.system)).orElse(settings.forwardProxy)
    settings.copy(
      credentials = credentials,
      forwardProxy = forwardProxy
    )
  }
}
