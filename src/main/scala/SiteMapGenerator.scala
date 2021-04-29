package io.opentargets.sitemap

import scala.annotation.tailrec
import scala.xml.Elem

object SiteMapGenerator {
  lazy val modified: String = java.time.LocalDateTime.now.toString
  val MAX_CHUNK = 50000
  val url = "https://beta.targetvalidation.org/"

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
        val uri = s"$url${s.takeWhile(_ != '_')}"
        val data = sites(s)
        if (data.size > chunkSize) {
          val chunks = breakIntoChunks(data).zipWithIndex.map(chunk => {
            (s"${s}_${chunk._2}", chunk._1)
          })
          chunks.map(ch => (ch._1, generateSite(uri, ch._2)))
        } else Seq((s, generateSite(uri, data)))
      })
      .toSeq
      .flatten
  }

  def generateSite(uriBase: String, pages: Iterable[String]): Elem = {
    <urlset xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://www.sitemaps.org/schemas/sitemap/0.9" xsi:schemaLocation="http://www.sitemaps.org/schemas/sitemap/0.9 http://www.sitemaps.org/schemas/sitemap/0.9/sitemap.xsd">
        {for (page <- pages) yield
        <url>
          <loc>{uriBase}/{page}</loc>
          <lastmod>{modified}</lastmod>
        </url>
        }
      </urlset>
  }

  def generateIndex(pages: Iterable[String]): Elem = {
    <sitemapindex xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
      {for (page <- pages) yield
      <sitemap>
        <loc>{url}sitemaps/{page}_pages.xml</loc>
        <lastmod>{modified}</lastmod>
      </sitemap> 
      }
    </sitemapindex>
  }
}
