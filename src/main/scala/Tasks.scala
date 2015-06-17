package ideaplugin

import sbt._
import sbt.Keys._
import scala.util._
import scala.xml._


object Tasks {
  import Downloader._

  def updateIdea(baseDir: File, build: String, externalPlugins: Seq[(String, URL)], streams: TaskStreams): Unit = {
    val externalPluginsDir = baseDir / "externalPlugins"
    IO.createDirectory(baseDir)
    IO.createDirectory(externalPluginsDir)
    val downloads =
      createIdeaDownloads(baseDir, build, streams.log) ++
      createExternalPluginsDownloads(externalPluginsDir, externalPlugins, streams.log)
    downloads.foreach(d => download(d, streams.log))
  }

  def createPluginsClasspath(pluginsBase: File, pluginsUsed: Seq[String]): Classpath = {
    val pluginsDirs = pluginsUsed.foldLeft(PathFinder.empty) { (paths, plugin) =>
      paths +++ (pluginsBase / plugin / "lib")
    }
    (pluginsDirs * (globFilter("*.jar") -- "*asm*.jar")).classpath
  }

  private def createIdeaDownloads(baseDir: File, build: String, log: Logger): Seq[Download] = {
    val ideaUrl = s"https://www.jetbrains.com/intellij-repository/releases/com/jetbrains/intellij/idea/ideaIC/$build/ideaIC-$build.zip"
    val ideaSourcesUrl = s"https://www.jetbrains.com/intellij-repository/releases/com/jetbrains/intellij/idea/ideaIC/$build/ideaIC-$build-sources.zip"
    Seq(
      DownloadAndUnpack(
        url(ideaUrl),
        baseDir.getParentFile / s"ideaIC-$build.zip",
        baseDir),
      DownloadOnly(
        url(ideaSourcesUrl),
        baseDir / "sources.zip")
    )
  }

  private def createExternalPluginsDownloads(baseDir: File, plugins: Seq[(String, URL)], log: Logger): Seq[Download] =
    plugins.map { case (pluginName, pluginUrl) =>
      DownloadAndUnpack(
        pluginUrl,
        baseDir / s"$pluginName.zip",
        baseDir / pluginName
      )
    }
}

private object Downloader {
  sealed trait Download
  final case class DownloadOnly(from: URL, to: File) extends Download
  final case class DownloadAndUnpack(from: URL, to: File, unpackTo: File) extends Download

  def download(d: Download, log: Logger): Unit = d match {
    case DownloadOnly(from, to) =>
      download(log, from, to)
    case DownloadAndUnpack(from, to, unpackTo) =>
      download(log, from, to)
      unpack(log, to, unpackTo)
  }

  private def download(log: Logger, from: URL, to: File): Unit = {
    if (to.isFile) {
      log.info(s"$to exists, download aborted")
    } else {
      log.info(s"Downloading $from to $to")
      IO.download(from, to)
    }
  }

  private def unpack(log: Logger, from: File, to: File): Unit = {
    log.info(s"Unpacking $from to $to")
    IO.unzip(from, to)
  }
}
