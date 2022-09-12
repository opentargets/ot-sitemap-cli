package io.opentargets.sitemap

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper

class SiteMapGeneratorSpec extends AnyFlatSpec {
  "SitemapGenerator" should "create a site xml representation of a series of URLs" in {
    // given
    val urls = Map("page" -> Seq("url1", "url2", "url3"))
    // when
    val sites = SiteMapGenerator.generateSites(urls)
    // then
    (sites.head._2 \\ "loc").map(_.text) sameElements urls("page")
  }
  "it" should "generate a separate xml for each page" in {
    // given
    val urls = Map("page1" -> Seq("url1", "url2", "url3"), "page2" -> Seq("url1", "url2", "url3"))
    // when
    val sites = SiteMapGenerator.generateSites(urls)
    // then
    sites.length mustEqual urls.keySet.size
    sites.map(s => (s._2 \\ "loc").map(_.text) sameElements urls(s._1))
  }

  "it" should "split entries into specified chunk sizes" in {
    val urlCount = 100
    val desiredChunks = 2
    val urls =
      Map("page" -> Array.fill(urlCount)(scala.util.Random.alphanumeric.take(10).toString).toSeq)
    // when
    val sites = SiteMapGenerator.generateSites(urls, urlCount / desiredChunks)
    // then
    sites.size mustEqual desiredChunks
  }

  "it" should "automatically split a page of greater than the maximum allowable chunk size into smaller pages" in {
    val urlCount = SiteMapGenerator.MAX_CHUNK * 1.5 toInt
    val urls =
      Map(
        "page" -> Array
          .fill(urlCount) {
            scala.util.Random.alphanumeric.take(10).toString
          }
          .toSeq
      )
    // when
    val sites = SiteMapGenerator.generateSites(urls)
    // then
    // we have two chunks
    sites.size mustEqual 2
    // all inputs are included in those two chunks
    sites.map(_._2).foldLeft(0)((acc, nxt) => acc + (nxt \\ "loc").length) mustEqual urlCount
  }

  "GenerateIndex" should "create an entry in the index for each sitemap" in {
    // given
    val sites = Seq("target_association",
                    "target_profiles",
                    "disease_association",
                    "disease_profile",
                    "drug_profile_pages"
    )
    // when
    val index = SiteMapGenerator.generateIndex(sites)
    // then
    (index \\ "loc").length mustEqual sites.length
  }
}
