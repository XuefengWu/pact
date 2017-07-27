package com.thoughtworks.pact.verify.pact

import com.thoughtworks.pact.verify.junit.Failure
import play.api.libs.json.{JsValue, Json}

import scala.util.Success

/**
  * Created by xfwu on 12/07/2017.
  */
case class Interaction(description: String,
                       request: PactRequest,
                       response: PactResponse) {

  def assert(request: PactRequest, actual: HttpResponse): Option[Failure] = {
    val expect = this.response
    actual match {
      case _ if expect.status != actual.status =>
        Some(Failure("status code failure",
          generateStatuesFailureMessage(request, actual, expect), Some(actual.body)))
      case _ if expect.getBody().isDefined  =>
        ResponseBodyJson.tryHardParseJsValue(actual.body) match {
          case Success(jsValue) => expect.isMatch(jsValue) match {
            case Some(err) => Some(Failure("context match failure",
              generateBodyMatchFailureMessage(err,request), Some(Json.stringify(jsValue))))
            case None => None
          }
          case scala.util.Failure(f) => Some(Failure("body parse failure",
            generateBodyParseFailureMessage(f.getStackTrace.mkString("/n"),request),Some(actual.body)))
        }
      case _ => None
    }
  }

  private def generateBodyParseFailureMessage(err:String, request: PactRequest) = {
    s"request url: ${request.path}\n Parse failure:$err \n"
  }

  private def generateBodyMatchFailureMessage(err:String, request: PactRequest) = {
    s"request url: ${request.path}\n Match failure:$err \n"
  }

  private def generateStatuesFailureMessage(request: PactRequest, actual: HttpResponse, expect: PactResponse) = {
    s"request url: ${request.path}\n Status Do not match: ${expect.status} != ${actual.status}"
  }



}


