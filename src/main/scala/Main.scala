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
import scala.annotation.tailrec
import scala.collection.convert.ImplicitConversions.`iterable AsScalaIterable`
import scala.xml.{Elem, XML}

trait BigQueryT extends LazyLogging {

  val bigquery: BigQuery = BigQueryOptions.getDefaultInstance.getService
  private val logErrors: Seq[BigQueryError] => Unit = _.foreach(e => logger.warn(e.toString))

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
      Left(queryJob.getStatus.getExecutionErrors.toSeq)
    } else {
      Right(queryJob.getQueryResults())
    }
  }

}

object SiteMapGenerator {
  lazy val modified: String = java.time.LocalDateTime.now.toString
  val MAX_CHUNK = 50000
  val url = "https://www.targetvalidation.org"

  def generateSitesWithIndex(sites: Map[String, Iterable[String]]): Seq[(String, Elem)] = {
    val siteMaps = generateSites(sites)
    val index = generateIndex(siteMaps.map(_._1))
    siteMaps :+ ("index", index)
  }

  def generateSites(sites: Map[String, Iterable[String]],
                    chunkSize: Int = MAX_CHUNK): Seq[(String, Elem)] = {
    require(chunkSize <= MAX_CHUNK,
            s"Google limits sitemap files to a maximum of $MAX_CHUNK entries.")
    @tailrec
    def breakIntoChunks(iterable: Iterable[String],
                        acc: Seq[Iterable[String]] = Seq.empty): Seq[Iterable[String]] = {
      val (h, t) = iterable.splitAt(MAX_CHUNK)
      if (t.size > MAX_CHUNK) breakIntoChunks(t, h +: acc) else h +: t +: acc

    }
    sites.keySet
      .map(s => {
        val data = sites(s)
        if (data.size > chunkSize) {
          val chunks = breakIntoChunks(data).zipWithIndex.map(chunk => {
            (s"${s}_${chunk._2}", chunk._1)
          })
          chunks.map(ch => (ch._1, generateSite(s, ch._2)))
        } else Seq((s, generateSite(s, data)))
      })
      .toSeq
      .flatten
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

object Main extends App with BigQueryT with LazyLogging {
  logger.info("Starting sitemap generation...")

  val siteAndIdQuery = Seq(
    ("target", "SELECT id FROM `open-targets-eu-dev.platform_21_02.targets`"),
    ("disease", "SELECT id FROM `open-targets-eu-dev.platform_21_02.targets`"),
    ("drug", "SELECT id FROM `open-targets-eu-dev.platform_21_02.targets`"),
  )

  val siteMapInputs: Seq[(String, Iterable[String])] = siteAndIdQuery
    .map(s => {
      logger.info(s"Querying BigQuery for ${s._1}")
      // assumes query is only returning the id field which is a string.
      val successCallback: TableResult => Iterable[String] =
        _.iterateAll.map(row => row.get("id").getStringValue)
      (s._1, executeQuery(s._2, successCallback, _ => ()))
    })
    .withFilter(_._2.isDefined)
    .map(it => (it._1, it._2.get))

  // for target and diseases we need both association and profile, so 'duplicate' the datasets here
  val hasAssociations = Set("target", "disease")
  val fullInputs: Seq[(String, Iterable[String])] = siteMapInputs.flatMap(smi => {
    smi._1 match {
      case idx if hasAssociations.contains(idx) =>
        val profile = (smi._1 + "_profile", smi._2)
        val association = (smi._1 + "_asssociation", smi._2.map(str => s"$str/associations"))
        Seq(profile, association)
      case _ => Seq((smi._1 + "_profile", smi._2))
    }
  })

  logger.info(s"Sitemap inputs generated for: ${fullInputs.map(_._1).mkString(",")}.")
  val siteMaps = SiteMapGenerator.generateSitesWithIndex(fullInputs.toMap)

  logger.info("Writing sitemaps to file.")
  siteMaps.foreach(sm => {
    XML.save(s"${sm._1}_pages.xml", sm._2, "utf-8", xmlDecl = true, null)
  })
}
