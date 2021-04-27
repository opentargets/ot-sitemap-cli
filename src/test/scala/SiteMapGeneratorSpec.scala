package io.opentargets.sitemap

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper

import scala.xml.Elem

object TestObjects {
  val urlsXML: Elem =
    <urlset xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://www.sitemaps.org/schemas/sitemap/0.9" xsi:schemaLocation="http://www.sitemaps.org/schemas/sitemap/0.9 http://www.sitemaps.org/schemas/sitemap/0.9/sitemap.xsd">
  <url>
    <loc>https://www.targetvalidation.org/target/ENSG00000094914</loc>
    <lastmod>2021-02-15</lastmod>
    <changefreq>monthly</changefreq>
    <priority>0.9</priority>
  </url>
  <url>
    <loc>https://www.targetvalidation.org/target/ENSG00000103591</loc>
    <lastmod>2021-02-15</lastmod>
    <changefreq>monthly</changefreq>
    <priority>0.9</priority>
  </url>
    <url>
    <loc>https://www.targetvalidation.org/target/ENSG00000276863</loc>
    <lastmod>2021-02-15</lastmod>
    <changefreq>monthly</changefreq>
    <priority>0.9</priority>
  </url>
</urlset>
}

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
  "GenerateIndex" should "create an entry in the index for each sitemap" in {
    // given
    val sites = Seq("target_association",
                    "target_profiles",
                    "disease_association",
                    "disease_profile",
                    "drug_profile_pages")
    // when
    val index = SiteMapGenerator.generateIndex(sites)
    // then
    (index \\ "loc").length mustEqual sites.length
  }
}
