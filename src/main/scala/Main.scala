package io.opentargets.sitemap

import CommandLineConfig.cliParser

import com.google.cloud.bigquery.TableResult
import com.typesafe.scalalogging.LazyLogging
import scopt.OParser

import scala.collection.convert.ImplicitConversions.`iterable AsScalaIterable`
import scala.xml.{Elem, XML}

object Main extends App with LazyLogging {

  OParser.parse(cliParser, args, CommandLineConfig()) match {
    case Some(config) =>
      logger.info("Starting sitemap generation.")
      logger.info("Config parsed successfully.")
      run(config)

    case _ =>
      // arguments are bad, error message will have been displayed
      logger.error("Exiting without running due to bad configuration.")
      System.exit(-1)
  }

  def run(config: CommandLineConfig): Unit = {
    val siteAndIdQuery = Seq(
      ("target", s"SELECT id FROM `${config.bqProject}.${config.bqTables}.targets`"),
      ("disease", s"SELECT id FROM `${config.bqProject}.${config.bqTables}.diseases`"),
      ("drug", s"SELECT id FROM `${config.bqProject}.${config.bqTables}.molecule`")
    )

    val siteMapInputs: Seq[(String, Iterable[String])] = siteAndIdQuery
      .map { s =>
        logger.info(s"Querying BigQuery for ${s._1}")
        // assumes query is only returning the id field which is a string.
        val successCallback: TableResult => Iterable[String] =
          _.iterateAll.map(row => row.get("id").getStringValue)
        (s._1, BigQueryExecutor.executeQuery(s._2, successCallback, _ => ()))
      }
      .withFilter(_._2.isDefined)
      .map(it => (it._1, it._2.get))

    // for target and diseases we need both association and profile, so 'duplicate' the datasets here
    val hasAssociations = Set("target", "disease")
    val fullInputs: Seq[(String, Iterable[String])] = siteMapInputs.flatMap { smi =>
      smi._1 match {
        case idx if hasAssociations.contains(idx) =>
          val profile = (smi._1 + "_profile", smi._2)
          val association = (smi._1 + "_asssociation", smi._2.map(str => s"$str/associations"))
          Seq(profile, association)
        case _ => Seq((smi._1 + "_profile", smi._2))
      }
    }

    logger.info(s"Sitemap inputs generated for: ${fullInputs.map(_._1).mkString(",")}.")
    val siteMaps = SiteMapGenerator.generateSitesWithIndex(fullInputs.toMap)

    logger.info("Writing sitemaps to file.")
    config.outputDir match {
      case gs if gs.startsWith("gs://") =>
        GstorageWriter.gsPathToBucketAndObjectPath(gs) match {
          case Some(pathInfo) =>
            try {
              logger.info(s"Writing to GCP Bucket ${pathInfo._1}")
              val (bucket, path) = pathInfo
              val writer = new GstorageWriter(bucket, config.bqProject)
              siteMaps.foreach(sm => writer.writeXml(s"$path/${sm._1}.xml", sm._2))
            } catch {
              case ex: IllegalArgumentException =>
                logger.info(
                  s"Unable to find bucket, writing to local directory ${System.getProperty("user.dir")}"
                )
                writeXmlFiles(siteMaps)
            }
          case None =>
            logger.info(
              s"Unable to parse GCP path, writing to local directory ${System.getProperty("user.dir")}"
            )
            writeXmlFiles(siteMaps)
        }
      case _ =>
        writeXmlFiles(siteMaps)
    }
  }

  private def writeXmlFiles(siteMaps: Seq[(String, Elem)]): Unit =
    siteMaps.foreach(sm => XML.save(s"${sm._1}.xml", sm._2, "utf-8", xmlDecl = true, null))
}
