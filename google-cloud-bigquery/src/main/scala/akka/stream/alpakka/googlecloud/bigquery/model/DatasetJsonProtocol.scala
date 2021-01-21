/*
 * Copyright (C) 2016-2020 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.stream.alpakka.googlecloud.bigquery.model

import akka.stream.alpakka.googlecloud.bigquery.scaladsl.Paginated
import akka.stream.alpakka.googlecloud.bigquery.scaladsl.spray.BigQueryApiJsonProtocol._
import spray.json.{JsonFormat, RootJsonFormat}

import java.util
import scala.collection.JavaConverters._
import scala.collection.immutable.Seq
import scala.compat.java8.OptionConverters._

object DatasetJsonProtocol {

  final case class Dataset(datasetReference: DatasetReference,
                           friendlyName: Option[String],
                           labels: Option[Map[String, String]],
                           location: Option[String]) {

    def getDatasetReference = datasetReference
    def getFriendlyName = friendlyName.asJava
    def getLabels = labels.map(_.asJava).asJava
    def getLocation = location.asJava

    def withDatasetReference(datasetReference: DatasetReference) =
      copy(datasetReference = datasetReference)
    def withFriendlyName(friendlyName: util.Optional[String]) =
      copy(friendlyName = friendlyName.asScala)
    def withLabels(labels: util.Optional[util.Map[String, String]]) =
      copy(labels = labels.asScala.map(_.asScala.toMap))
    def withLocation(location: util.Optional[String]) =
      copy(location = location.asScala)
  }

  /**
   * Java API
   */
  def createDataset(datasetReference: DatasetReference,
                    friendlyName: util.Optional[String],
                    labels: util.Optional[util.Map[String, String]],
                    location: util.Optional[String]) =
    Dataset(datasetReference, friendlyName.asScala, labels.asScala.map(_.asScala.toMap), location.asScala)

  final case class DatasetReference(datasetId: String, projectId: Option[String]) {

    def getDatasetId = datasetId
    def getProjectId = projectId.asJava

    def withDatasetId(datasetId: String) =
      copy(datasetId = datasetId)
    def withProjectId(projectId: util.Optional[String]) =
      copy(projectId = projectId.asScala)
  }

  /**
   * Java API
   */
  def createDatasetReference(datasetId: String, projectId: util.Optional[String]) =
    DatasetReference(datasetId, projectId.asScala)

  final case class DatasetListResponse(nextPageToken: Option[String], datasets: Option[Seq[Dataset]]) {

    def getNextPageToken = nextPageToken.asJava
    def getDatasets = datasets.map(_.asJava).asJava

    def withNextPageToken(nextPageToken: util.Optional[String]) =
      copy(nextPageToken = nextPageToken.asScala)
    def withDatasets(datasets: util.Optional[util.List[Dataset]]) =
      copy(datasets = datasets.asScala.map(_.asScala.toList))
  }

  /**
   * Java API
   */
  def createDatasetListResponse(nextPageToken: util.Optional[String], datasets: util.Optional[util.List[Dataset]]) =
    DatasetListResponse(nextPageToken.asScala, datasets.asScala.map(_.asScala.toList))

  implicit val referenceFormat: JsonFormat[DatasetReference] = jsonFormat2(DatasetReference)
  implicit val format: RootJsonFormat[Dataset] = jsonFormat4(Dataset)
  implicit val listResponseFormat: RootJsonFormat[DatasetListResponse] = jsonFormat2(DatasetListResponse)
  implicit val paginated: Paginated[DatasetListResponse] = _.nextPageToken
}