package io.opentargets.sitemap

import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.{
  BigQueryError,
  BigQueryOptions,
  JobId,
  JobInfo,
  QueryJobConfiguration,
  TableResult
}
import com.typesafe.scalalogging.LazyLogging

import java.util.UUID
import scala.collection.convert.ImplicitConversions.`collection AsScalaIterable`

object BigQueryExecutor extends LazyLogging {

  val bigquery: BigQuery = BigQueryOptions.getDefaultInstance.getService

  private val logErrors: Seq[BigQueryError] => Unit = _.foreach(e => logger.warn(e.toString))

  def executeQuery[T](sqlQuery: String,
                      onSuccess: TableResult => T,
                      onError: Seq[BigQueryError] => Unit = logErrors,
                      legacySql: Boolean = false
  ): Option[T] =
    executeQuery(sqlQuery, legacySql) match {
      case Left(errors)  => onError(errors); None
      case Right(result) => Some(onSuccess(result))
    }

  def executeQuery(sqlQuery: String,
                   legacySql: Boolean
  ): Either[Seq[BigQueryError], TableResult] = {
    val queryConfig = QueryJobConfiguration
      .newBuilder(sqlQuery)
      .setUseLegacySql(legacySql)
      .build

    // Create a job ID so that we can safely retry.
    val jobId = JobId.of(UUID.randomUUID.toString)
    val queryJob = bigquery.create(JobInfo.newBuilder(queryConfig).setJobId(jobId).build)

    // Wait for the query to complete.
    queryJob.waitFor()

    // Check for errors
    if (queryJob == null) throw new RuntimeException("Job no longer exists")
    if (queryJob.getStatus.getError != null) {
      Left(queryJob.getStatus.getExecutionErrors.toSeq)
    } else {
      Right(queryJob.getQueryResults())
    }
  }

}
