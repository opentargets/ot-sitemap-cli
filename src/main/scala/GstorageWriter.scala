package io.opentargets.sitemap

import com.google.cloud.storage.{Blob, BlobId, BlobInfo, StorageOptions}
import com.typesafe.scalalogging.LazyLogging

import java.nio.charset.StandardCharsets
import scala.xml.Elem

class GstorageWriter(bucket: String) extends LazyLogging {

  private lazy val storage = StorageOptions.getDefaultInstance.getService
  private lazy val prettyPrinter = new scala.xml.PrettyPrinter(200, 2)
  private val xmlDecl = "<?xml version='1.0' encoding='UTF-8'?>\n"

  def writeXml(name: String, data: Elem): Unit = {
    logger.info(s"Writing $name to $bucket.")
    val prettyXml = xmlDecl + prettyPrinter.format(data)
    val blobId = BlobId.of(bucket, name)
    val blobInfo = BlobInfo.newBuilder(blobId).setContentType("application/xml").build
    val blob: Blob = storage.create(blobInfo, prettyXml.getBytes(StandardCharsets.UTF_8))
  }

}

object GstorageWriter extends LazyLogging {
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
