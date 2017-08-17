package com.thoughtworks.pact.verify.json

import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json.Json

/**
  * Created by xfwu on 14/07/2017.
  */
class ResponseBodyJsonSpec  extends FlatSpec with Matchers {
  "Pact Response Json" should "parse string as number" in {
    val s = """ {"message": "{\"score\": 123}"} """
    val js = (Json.parse(s) \ "message").get
    ResponseBodyJson.hardParseJsValue(js).get should be(Json.parse("""{"score":123}"""))
  }

  it should "parse string as json in json" in {
    val s1 = """ {"message": "{\"score\": 123}"} """
    val s2 = """ {"message": {"score": 123} } """
    val js1 = Json.parse(s1)
    val js2 = Json.parse(s2)
    js1 shouldNot be(js2)
    ResponseBodyJson.hardParseJsValue(js1).get should be(js2)
  }


  it should "parse string as json in json in json" in {
    val s1 = """ {"message": "{\"params\": \"{\\\"score\\\": 123}\" }" }"""
    val s2 = """ {"message": {"params": {"score": 123} } }"""
    val js1 = Json.parse(s1)
    val js2 = Json.parse(s2)
    js1 shouldNot be(js2)
    ResponseBodyJson.hardParseJsValue(js1).get should be(js2)
  }


}
