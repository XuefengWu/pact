import play.api.libs.json._
import play.api.libs.ws.WSResponse

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Try

object PactTester {

  private val PlaceHolderR = """"\$([a-zA-Z]+)\$"""".r

  private def testPact(urlRoot: String, pact: Pact): TestSuite = {
    val startPact = System.currentTimeMillis()
    val pactWS = new PactWS(urlRoot)

    var responseOpt: Option[WSResponse] = None
    var cookiesOpt: Option[Seq[String]] = None
    val result: Seq[TestCase] = for {
      i <- 1 to pact.repeat.getOrElse(1)
      interaction <- pact.interactions
    } yield {
        val start = System.currentTimeMillis()

        //TODO: 创建参数，参数的连续使用
        val request = rebuildRequest(interaction.request, responseOpt)

        //cookies
        if (responseOpt.isDefined) {
          val response = responseOpt.get
          val cookies: Seq[String] = response.cookies.filter(_.value.isDefined).map(c => s"${c.name.getOrElse("")}=${c.value.getOrElse("")}")
          if (cookies.size > 0) {
            cookiesOpt = cookiesOpt.fold(Some(cookies))(c => Some((c ++: cookies).distinct))
          }
        }

        val responseF = pactWS.send(request.copy(cookies = cookiesOpt.map(_.mkString(";"))))
        val actual: WSResponse = Await.result(responseF, Duration(30, SECONDS))
        val expect: PactResponse = interaction.response
        val error = if (actual.status >= 500) Some(Error(actual.statusText, actual.body)) else None
        val failure = assert(expect, actual)
        val spend = (System.currentTimeMillis() - start) / 1000

        if (actual.status < 300) {
          responseOpt = Some(actual)
        } else {
          responseOpt = None
        }
        TestCase("assertions", interaction.description, interaction.description, "status", spend.toString, error, failure)
      }
    val errorsCount = result.count(_.error.isDefined)
    val failuresCount = result.count(_.failure.isDefined)
    val spendPact = (System.currentTimeMillis() - startPact) / 1000
    pactWS.close()
    TestSuite("disabled", errorsCount, failuresCount, "hostname", pact.name, pact.name, "pkg", "skipped", "tests",
      spendPact.toString, System.currentTimeMillis().toString, result)

  }

  private def rebuildRequest(request: PactRequest, responseOpt: Option[WSResponse]): PactRequest = {

    if (responseOpt.isDefined) {
      val response = responseOpt.get
      //parameters
      val body = request.body
      var requestBufOpt = body.map(_.toString())
      var url = request.path
      if (Try(Json.parse(response.body)).isSuccess) {
        if (request.body.isDefined) {
          PlaceHolderR.findAllMatchIn(request.body.get.toString()).map { m => m.group(1) }.foreach { placeId =>
            val placeJsValue = (Json.parse(response.body) \ placeId)
            if (!placeJsValue.isInstanceOf[JsUndefined]) {
              val placeValue = placeJsValue.toString().drop(1).dropRight(1)
              requestBufOpt = requestBufOpt.map(requestBuf => requestBuf.replaceAll("\\$" + placeId + "\\$", placeValue))
            }
          }
        }

        PlaceHolderR.findAllMatchIn(request.path).map { m => m.group(1) }.foreach { placeId =>
          val placeJsValue = (Json.parse(response.body) \ placeId)
          if (!placeJsValue.isInstanceOf[JsUndefined]) {
            val placeValue = placeJsValue.toString().drop(1).dropRight(1)
            url = url.replaceAll("\\$" + placeId + "\\$", placeValue)
          }
        }

      }

      request.copy(path = url, body = requestBufOpt.map(requestBuf => Json.parse(requestBuf)))
    } else request
  }

  private def assert(expect: PactResponse, actual: WSResponse): Option[Failure] = {
    actual match {
      case _ if expect.status != actual.status => Some(Failure(actual.statusText, s"Status: ${expect.status} != ${actual.status} \n${actual.body}"))
      case _ if expect.body.isDefined && !isEqual(expect.body.get, Json.parse(actual.body)) => Some(Failure(actual.statusText, s"期望:${expect.body.get}\n 实际返回:${actual.body}"))
      case _ => None
    }

  }

  private def isEqualObject(expect: JsObject, actual: JsObject): Boolean = {
    val asserts = expect.asInstanceOf[JsObject].fields.map { case (field, value) =>
      value == actual \ field
    }
    if (!asserts.isEmpty) {
      asserts.reduce(_ && _)
    } else false
  }

  private def isEqualArray(expect: JsArray, actual: JsArray): Boolean = {
    if (expect.value.size == actual.value.size) {
      val actualValues = actual.value
      val asserts = expect.value.zipWithIndex.map { case (v, i) => isEqual(v, actualValues(i)) }
      asserts.reduce(_ && _)
    } else false
  }

  private def isEqual(expect: JsValue, actual: JsValue): Boolean = {
    if (expect.isInstanceOf[JsObject] && actual.isInstanceOf[JsObject]) {
      isEqualObject(expect.asInstanceOf[JsObject], actual.asInstanceOf[JsObject])
    } else if (expect.isInstanceOf[JsArray] && actual.isInstanceOf[JsArray]) {
      isEqualArray(expect.asInstanceOf[JsArray], actual.asInstanceOf[JsArray])
    } else {
      expect == actual
    }
  }

  def testPacts(urlRoot: String, pacts: Pacts): TestSuites = {
    val start = System.currentTimeMillis()
    val testSuites: Seq[TestSuite] = pacts.pacts.map(testPact(urlRoot, _))
    val spend = (System.currentTimeMillis() - start) / 1000
    TestSuites("disabled", testSuites.map(_.errors).reduce(_ + _), testSuites.map(_.failures).reduce(_ + _), pacts.name, "", spend.toString, testSuites)
  }


}
