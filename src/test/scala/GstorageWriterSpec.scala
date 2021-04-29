package io.opentargets.sitemap

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers.{convertToAnyMustWrapper, defined}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class GstorageWriterSpec extends AnyFlatSpec {
  "GstorageWriter object" should "extract the bucket and object path from an gsutil like string" in {
    // given
    val input = "gs://ot-team/someone/save/my/files/here"
    // when
    val results = GstorageWriter.gsPathToBucketAndObjectPath(input)
    // then
    results shouldBe defined
    val (bucket, path) = results.get
    bucket mustBe "ot-team"
    path mustBe "someone/save/my/files/here"

  }
  "It" should "return None when the input is malformed" in {
    // given
    val input = "gs://ot-team"
    // when
    val results = GstorageWriter.gsPathToBucketAndObjectPath(input)

    // then
    results shouldBe None
  }
}
