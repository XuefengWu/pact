package com.thoughtworks.pact.verify

import java.io.File

import com.thoughtworks.pact.verify.junit.{JunitReport, TestSuites}
import com.thoughtworks.pact.verify.pact.PactFile
import org.apache.commons.logging.LogFactory
import org.apache.commons.logging.impl.LogFactoryImpl
/**
  * Created by xfwu on 12/07/2017.
  */
object Main extends App {

  println("welcome play pact v0.4.0")
  val start = System.currentTimeMillis()

  val root = if (args.length < 1) {
    println("Usage: java -jar pact-xx.jar pact_dir url_root")
    val _currentPath = new File("").getAbsolutePath    
    _currentPath
  } else args(0)

  val urlRoot = if (args.length < 2) {
    println("Usage: java -jar pact-xx.jar pact_dir url_root")
    val defaultRoot = "http://localhost:8080"
    println("Use default: " + defaultRoot)
    defaultRoot
  } else {
    val _url = args(1)
    if(_url.endsWith("/")) {
      _url.dropRight(1)
    }else{
      _url
    }
  }

  if(args.length > 2) {
    LogFactory.getFactory.setAttribute(LogFactoryImpl.LOG_PROPERTY, "org.apache.commons.logging.impl.SimpleLog")
    val logLevel = args(2)
    System.setProperty("org.apache.commons.logging.simplelog.defaultlog", logLevel)
  }

  //一个文件夹 - Pacts - TestSuites
  //一个文件 - Pact - TestSuite
  val reportDirPath = s"$root${File.separator}report"
  val pactDir = new File(s"$root")
  println("pact working directory: " + pactDir.getAbsolutePath)
  if (!pactDir.exists()) {
    println(s"${pactDir.getAbsolutePath} do not exists.")
    System.exit(-1)
  }
  new File(reportDirPath).mkdirs()
  val pactsList = PactFile.loadPacts(pactDir)
  if(pactsList.isEmpty){
    println(s"${pactDir.getAbsolutePath} do not contains pact json files")
    System.exit(-1)
  }

  val pactFs: Seq[TestSuites] = PactTestService.testPacts(pactsList, urlRoot)

  println(s"execute tests finished: spend: ${(System.currentTimeMillis() - start)/1000} s")

  for {
    f <- pactFs
    ts <- f.testSuites
    tc <- ts.cases if tc.failure.isDefined
    fail <- tc.failure
  } {
    println(s"\n${ts.name}::${tc.name} \n ${fail.message}")
  }
  JunitReport.dumpJUnitReport(reportDirPath, pactFs)
  println("dump report finished")
}
