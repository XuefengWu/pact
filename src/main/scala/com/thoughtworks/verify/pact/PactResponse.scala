package com.thoughtworks.verify.pact

import play.api.libs.json.{JsArray, JsObject, JsValue}

/**
  * Created by xfwu on 12/07/2017.
  */
case class PactResponse(status: Int, body: Option[JsValue],matchingRules: Option[JsValue]) {

  def isMatch(actual: JsValue): Option[String] = {

    isEqual(body.get,actual) match {
      case true => None
      case false => matchFields(actual)
    }
  }

  def matchFields(actual: JsValue):Option[String] = {
    val expect: JsValue = body.get
    matchingRules match {
      case Some(r) =>
        val rules = MatchingRules(r)
        rules.foldLeft[Option[String]](None)((acc, matcher) => {
          val matched = matcher.isBodyMatch(actual, expect)
          acc.fold(matched)(err => matched.fold(Some(err))(err2 => Some(s"$err \n $err2"))
          )
        })
      case None => None
    }
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



}
