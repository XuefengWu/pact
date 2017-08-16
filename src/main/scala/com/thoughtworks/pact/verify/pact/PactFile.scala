package com.thoughtworks.pact.verify.pact

import java.io.File

import org.apache.commons.logging.LogFactory
import play.api.libs.json.Json

import scala.io.{Codec, Source}
import scala.util.{Failure, Success, Try}

/**
  * Created by xfwu on 12/07/2017.
  */
object PactFile {
  implicit val pactRequestFormat = Json.format[PactRequest]
  implicit val pactResponseFormat = Json.format[PactResponse]
  implicit val interactionFormat = Json.format[Interaction]
  implicit val providerFormat = Json.format[Provider]
  implicit val consumerFormat = Json.format[Consumer]
  implicit val pactFormat = Json.format[Pact]

  private val logger = LogFactory.getFactory.getInstance(this.getClass)

  def loadPacts(dir: File): List[Pacts] = {
    val (subDirs, files) = listFiles(dir).partition(_.isDirectory)
    val pacts: Seq[Try[Pact]] = parsePacts(files)
    val subPacts: List[Pacts] = loadParsePacts(subDirs)
    if (pacts.isEmpty) {
      subPacts
    } else {
      val pactsName = s"${Option(dir.getParentFile).map(_.getName + "_").getOrElse("")}${dir.getName}"
      subPacts :+ Pacts(pactsName, pacts)
    }
  }

  private def loadParsePacts(subDirs: Seq[File]): List[Pacts] = {
    if(subDirs != null && !subDirs.isEmpty) {
      subDirs.flatMap(subDir => loadPacts(subDir)).toList
    } else {
      Nil
    }
  }

  private def parsePacts(files: Seq[File]): Seq[Try[Pact]] = {
    if(files != null && !files.isEmpty) {
      val before = beforeInteraction(files.head.getParentFile).get
      files.filter(_.getName.endsWith(".json"))
        .filterNot(_.getName.startsWith("_"))
        .map(parsePactFile)
        .map(pt => pt.map(p => p.copy(interactions = before ::: p.interactions.toList)))
    } else {
      Nil
    }
  }

  private def beforeInteraction(dir: File): Try[List[Interaction]] = {
    listFiles(dir).find(_.getName.equalsIgnoreCase("_before.json")) match {
      case Some(f) => parsePactFile(f).map(_.interactions.map(v => v.copy(description = s"_before_${v.description}")).toList)
      case None => Success(Nil)
    }
  }

  private def listFiles(dir: File): Seq[File] = {
    dir.listFiles().toSeq
  }

  private def parsePactFile(f: File): Try[Pact] = {
    logger.debug(s"parsePactFile: ${f.getAbsolutePath}")
    val sTry = Try(Source.fromFile(f)(Codec.UTF8).getLines().mkString("\n"))
    sTry match {
      case Failure(t) => t.addSuppressed(new Exception(s"read file by UTF8: ${f.getAbsolutePath}"))
      case _ =>
    }
    val pactTry = sTry.flatMap(parsePact)
    pactTry match {
      case Failure(t) => t.addSuppressed(new Exception(f.getAbsolutePath))
      case _ =>
    }
    pactTry.map(_.copy(source = Some(s"${f.getParentFile.getName}/${f.getName}")))
  }

  private def parsePact(s: String): Try[Pact] = {
    Try(Json.parse(s).as[Pact])
  }

}
