package io.opentargets.sitemap

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
import scala.collection.JavaConversions._
import scala.xml.dtd.DocType
import scala.xml.{Elem, XML}

trait BigQuery extends LazyLogging {

  val bigquery = BigQueryOptions.getDefaultInstance.getService

  def executeQuery[T](sqlQuery: String,
                      onSuccess: TableResult => T,
                      onError: Seq[BigQueryError] => Unit = logErrors,
                      legacySql: Boolean = false): Option[T] = {
    executeQuery(sqlQuery, legacySql) match {
      case Left(errors)  => onError(errors); None
      case Right(result) => Some(onSuccess(result))
    }
  }

  def executeQuery(sqlQuery: String,
                   legacySql: Boolean): Either[Seq[BigQueryError], TableResult] = {
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
      Left(queryJob.getStatus.getExecutionErrors)
    } else {
      Right(queryJob.getQueryResults())
    }
  }

  private val logErrors: Seq[BigQueryError] => Unit = _.foreach(e => logger.warn(e.toString))

}

object SiteMapGenerator {
  lazy val modified = java.time.LocalDateTime.now.toString
  val url = "https://www.targetvalidation.org"

  def generateSitesWithIndex(sites: Map[String, Iterable[String]]): Seq[(String, Elem)] = {
    generateSites(sites) :+ ("index", generateIndex(sites.keySet))
  }

  def generateSites(sites: Map[String, Iterable[String]]): Seq[(String, Elem)] = {
    sites.keySet.map(s => (s, generateSite(s, sites(s)))).toSeq
  }

  def generateSite(site: String, pages: Iterable[String]): Elem = {
    <urlset xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://www.sitemaps.org/schemas/sitemap/0.9" xsi:schemaLocation="http://www.sitemaps.org/schemas/sitemap/0.9 http://www.sitemaps.org/schemas/sitemap/0.9/sitemap.xsd">
        {for (page <- pages) yield
        <url>
          <loc>{url}/{site}/{page}</loc>
          <lastmod>{modified}</lastmod>
        </url>
        }
      </urlset>
  }

  def generateIndex(pages: Iterable[String]): Elem = {
    <sitemapindex xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
      {for (page <- pages) yield
      <sitemap>
        <loc>{url}/sitemaps/{page}_pages.xml</loc>
        <lastmod>{modified}</lastmod>
      </sitemap> 
      }
    </sitemapindex>
  }
}

object Main extends App with BigQuery with LazyLogging {
  logger.info("Starting sitemap generation...")

  val siteAndIdQuery = Seq(
    ("targets", "SELECT id FROM `open-targets-eu-dev.platform_21_02.targets`"),
    ("diseases", "SELECT id FROM `open-targets-eu-dev.platform_21_02.targets`"),
    ("drugs", "SELECT id FROM `open-targets-eu-dev.platform_21_02.targets`"),
  )

  val siteMapInputs: Seq[(String, Iterable[String])] = siteAndIdQuery
    .map(s => {
      logger.info(s"Querying BigQuery for ${s._1}")
      val successCallback: TableResult => Iterable[String] =
        _.iterateAll.map(row => row.get("id").getStringValue)
      (s._1, executeQuery(s._2, successCallback, _ => ()))
    })
    .withFilter(_._2.isDefined)
    .map(it => (it._1, it._2.get))

  val hasAssociations = Set("targets", "diseases")
  // for target and diseases we need both association and profile, so 'duplicate' the datasets here
  val fullInputs: Seq[(String, Iterable[String])] = siteMapInputs.flatMap(smi => {
    smi._1 match {
      case idx if hasAssociations.contains(idx) => {
        val profile = smi
        val association = (smi._1 + "_asssociations", smi._2.map(str => s"$str/associations"))
        Seq(profile, association)
      }
      case _ => Seq(smi)
    }
  })

  val siteMaps = SiteMapGenerator.generateSitesWithIndex(fullInputs.toMap)
  logger.info("Writing sitemaps to file.")
  siteMaps.foreach(sm => {
    XML.save(s"${sm._1}.xml", sm._2, "utf-8", xmlDecl = true, null)
  })
}
