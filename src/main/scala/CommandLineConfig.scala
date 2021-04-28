package io.opentargets.sitemap

import com.google.cloud.resourcemanager.Project
import scopt.{OParser, OParserBuilder}
import com.google.cloud.resourcemanager.ResourceManager
import com.google.cloud.resourcemanager.ResourceManagerOptions
import com.typesafe.scalalogging.LazyLogging

import scala.collection.JavaConverters._

case class CommandLineConfig(bqTables: String = "platform_21_02",
                             bqProject: String = "open-targets-eu-dev",
                             outputDir: String = System.getProperty("user.dir"))

/**
  * Container to hold longer command line interface help strings to avoid cluttering the specification of the CLI
  * interface in CommandLineConfig.
  */
trait CommandLineTextStrings {

  val outputHelp: String =
    """
      |Directory to save generated sitemaps into. Selected directory can be either an absolute or relative path or a 
      |gcloud storage bucket location.
      |
      |The default directory is the current working directory. 
      |
      |If a storage bucket is selected then you must have authorisation set up on the machine running the program to 
      |access those resources.
      |""".stripMargin

  val helpHelp: String =
    s"""
       |Generates sitemaps for open targets platform releases based on data currently available in Big Query. 
       |
       |Output directory:
       |
       |$outputHelp
       |""".stripMargin
}

/** Command Line Interface using scopt project.
  *
  *  See <a href="https://github.com/scopt/scopt">documentation</a> for
  *  examples and usage of scopt library.
  */
object CommandLineConfig extends CommandLineTextStrings with LazyLogging {

  lazy val resourceManager: ResourceManager = ResourceManagerOptions.getDefaultInstance.getService
  val builder: OParserBuilder[CommandLineConfig] = OParser.builder[CommandLineConfig]
  val cliParser: OParser[Unit, CommandLineConfig] = {
    import builder._
    OParser.sequence(
      programName("ot-sitemap"),
      head("Generate SEO sitemaps for open targets platform.",
           this.getClass.getPackage.getImplementationVersion),
      arg[String]("<bigQueryTable>")
        .action((x, c) => c.copy(bqTables = x))
        .text("BigQuery table to query for results, eg. platform_21_02"),
      arg[String]("<bigQueryProject>")
        .optional()
        .action((x, c) => c.copy(bqProject = x))
        .text("GCP project containing BQ tables. Default: open-targets-eu-dev")
        .validate(pId =>
          if (validateProjectId(pId)) success else failure(s"Project $pId not found.")),
      opt[String]('o', "output-dir")
        .action((x, c) => c.copy(outputDir = x))
        .text(outputHelp),
      help("help").text("prints this usage text")
    )

  }

  def validateProjectId(str: String): Boolean = {
    logger.debug(s"Validating $str is valid GCP project")
    val project: Option[Project] = Option(resourceManager.get(str))
    project.isDefined
  }

}
