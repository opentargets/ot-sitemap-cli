package io.opentargets.sitemap

import com.google.cloud.storage.{BlobId, BlobInfo, Storage, StorageException, StorageOptions}
import com.typesafe.scalalogging.LazyLogging
import GstorageWriter.gsBucketExists

import java.nio.charset.StandardCharsets
import scala.xml.Elem

class GstorageWriter(bucket: String, projectId: String) extends LazyLogging {
  implicit private lazy val storage: Storage =
    StorageOptions.newBuilder().setProjectId(projectId).build().getService
  private lazy val prettyPrinter = new scala.xml.PrettyPrinter(200, 2)
  private val xmlDecl = "<?xml version='1.0' encoding='UTF-8'?>\n"

  require(gsBucketExists(bucket), s"Bucket $bucket does not exists.")

  def writeXml(name: String, data: Elem): Unit = {
    logger.info(s"Writing $name to $bucket.")
    try {
      val prettyXml = xmlDecl + prettyPrinter.format(data)
      val blobId = BlobId.of(bucket, name)
      val blobInfo = BlobInfo.newBuilder(blobId).setContentType("application/xml").build
      storage.create(blobInfo, prettyXml.getBytes(StandardCharsets.UTF_8))
    } catch {
      case e: StorageException =>
        logger.error(s"Unable to write $name. Operation failed with exception: ${e.getMessage}")
    }

  }
}

object GstorageWriter extends LazyLogging {
  def gsBucketExists(bucket: String)(implicit storage: Storage): Boolean =
    storage.get(bucket) != null

  def gsPathToBucketAndObjectPath(gsPath: String): Option[(String, String)] = {
    val path = gsPath.stripPrefix("gs://").split("/", 2)
    if (path.length != 2) {
      logger.warn(s"gsPath $gsPath could not be parsed into bucket and object format.")
      None
    } else {
      Some((path.head, path.tail.head))
    }
  }
}
