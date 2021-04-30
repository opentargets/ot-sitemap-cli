package io.opentargets.sitemap

import com.google.cloud.storage.{Bucket, Storage}
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.when
import org.scalatest.PrivateMethodTester
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers.{convertToAnyMustWrapper, defined}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.mockito.MockitoSugar

class GstorageWriterSpec extends AnyFlatSpec with PrivateMethodTester with MockitoSugar {

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

  val gsBucketExistsPM: PrivateMethod[Boolean] = PrivateMethod[Boolean]('gsBucketExists)

  "gsBucketExists" should "return false if a bucket does not exist" in {
    // given
    implicit val storage: Storage = mock[Storage]
    val path = "gs://not-a-bucket/files"
    when(storage.get(anyString)).thenReturn(null)

    // when
    val results = GstorageWriter invokePrivate gsBucketExistsPM(path, storage)

    // then
    results mustBe false
  }

  "It" should "return true when a bucket exists" in {
    // given
    implicit val storage: Storage = mock[Storage]
    val bucket: Bucket = mock[Bucket]
    val path = "gs://a-bucket/files"
    when(storage.get(anyString)).thenReturn(bucket)

    // when
    val results = GstorageWriter invokePrivate gsBucketExistsPM(path, storage)

    // then
    results mustBe true
  }
}
