package com.thoughtworks.verify.pact

import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json.Json

/**
  * Created by xfwu on 12/07/2017.
  */
class MatchingRulesSpec extends FlatSpec with Matchers {

  "Matching Rule Select" should "find field" in {
      val rule = MatchingRule("$.body.data.array1","type","array",None)
      val result = rule.select(body)
      val expected = body.\("data").\("array1")
      result should be(expected)
  }

  it should "select element in array " in {
    val rule = MatchingRule("$.body.data.array1[0].id","match","integer",None)
    val expected = body \ "data" \ "array1" \ 0 \ "id"
    val result = rule.select(body)
    result should be(expected)
  }

  it should "select element in array of array" in {
    val rule = MatchingRule("$.body.data.array3[0][0].itemCount","match","integer",None)
    val expected = body \ "data" \ "array3" \ 0 \ 0 \ "itemCount"
    val result = rule.select(body)
    result should be(expected)
  }

  it should "select element in array from root array" in {
    val body = Json.parse("""[{"id":123}]""")
    val rule = MatchingRule("$.body[0].id","match","integer",None)
    val expected = body \ 0 \ "id"
    val result = rule.select(body)
    result should be(expected)
  }

  "Matching Rule Match Raw Type" should " with type array" in {
    val rule = MatchingRule("$.body.data.array1[0].id","type","array",None)
    val value = """[{"dob":"2017-07-12","id":613313905,"name":"ehGKdDIADDeeWpnNiZru"}]"""
    val jsValue = Json.parse(value)
    rule.isMatchExpress(jsValue) should be(None)
  }

  "Matching Rule Match Customer type" should "with  number" in {
    val value = """{"dob":"2017-07-12","id":613313905,"name":"ehGKdDIADDeeWpnNiZru","school":{"name":"TWU","address":"SH"}} """
    val jsValue = Json.parse(value)
    val rule1 = MatchingRule("$.body.id","match","type",None)
    rule1.isMatchExpress(rule1.select(jsValue).get,jsValue) should be(None)
    val rule2 = MatchingRule("$.body.name","match","type",None)
    rule2.isMatchExpress(rule2.select(jsValue).get,jsValue) should be(None)
    val rule3 = MatchingRule("$.body.dob","match","type",None)
    rule3.isMatchExpress(rule3.select(jsValue).get,jsValue) should be(None)
    val rule4 = MatchingRule("$.body.school","match","type",None)
    rule4.isMatchExpress(rule4.select(jsValue).get,jsValue) should be(None)
  }

  it should "match customer type in array" in {
    val expected = """{"numbers":[{"a":1,"b":2,"c":3},{"a":4,"b":5,"c":6}]} """
    val expectedJsValue = Json.parse(expected)
    val rule1 = MatchingRule("$.body.numbers","match","type",None)
    rule1.isMatchExpress(rule1.select(expectedJsValue).get,expectedJsValue) should be(None)

    val actual1 = Json.parse("""{"numbers":[{"a":1,"b":2,"c":3}]} """)
    rule1.isMatchExpress(rule1.select(actual1).get,expectedJsValue) should be(None)

    val actual2 = Json.parse("""{"numbers":[{"a":1,"b":2,"c":3,"d":4}]} """)
    rule1.isMatchExpress(rule1.select(actual2).get,expectedJsValue) should be(None)

  }

  it should "not match customer type when less field in array" in {
    val expected = """{"numbers":[{"a":1,"b":2,"c":3},{"a":4,"b":5,"c":6}]} """
    val expectedJsValue = Json.parse(expected)
    val rule1 = MatchingRule("$.body.numbers","match","type",None)
    val actual3 = Json.parse("""{"numbers":[{"a":9,"b":2,"d":4}]} """)
    rule1.isMatchExpress(rule1.select(actual3).get,expectedJsValue) should be(Some("expected field:[c] is not exists"))
  }

  it should "match value with type number" in {
    val rule = MatchingRule("xxx","type","number",None)
    val value = """613313905"""
    val jsValue = Json.parse(value)
    rule.isMatchExpress(jsValue) should be(None)
  }


  "Matching Rule Match date format" should "success with  express" in {
    val value = """{"dob":"2017-07-12","id":613313905} """
    val jsValue = Json.parse(value)
    val rule1 = MatchingRule("$.body.dob","date","yyyy-MM-dd",None)
    rule1.isMatchExpress(rule1.select(jsValue).get,jsValue) should be(None)
  }

  "Matching Rule Match date timestamp" should "success with  express" in {
    val value = """{"time":"2017-07-12T19:51:56","id":613313905} """
    val jsValue = Json.parse(value)
    val rule1 = MatchingRule("$.body.time","timestamp","yyyy-MM-dd'T'HH:mm:ss",None)
    rule1.isMatchExpress(rule1.select(jsValue).get,jsValue) should be(None)
  }


  "Matching Rule Match regex" should "success with  express" in {
    val value = """{"dob":"2017-07-12","id":613313905,"ip":"127.0.0.1","school":{"name":"TWU","address":"SH"}} """
    val jsValue = Json.parse(value)
    val rule1 = MatchingRule("$.body.ip","regex","(\\d{1,3}\\.)+\\d{1,3}",None)
    rule1.isMatchExpress(rule1.select(jsValue).get,jsValue) should be(None)
  }

  private val body = {
    val bodyStr =
      """
          {
             "data": {
                 "array1": [
                     {
                         "dob": "2017-07-12",
                         "id": 613313905,
                         "name": "ehGKdDIADDeeWpnNiZru"
                     }
                 ],
                 "array2": [
                     {
                         "address": "127.0.0.1",
                         "name": "AwpSKbcrQCSxKFKBcieW"
                     }
                 ],
                 "array3": [
                     [
                         {
                             "itemCount": 342721542
                         }
                     ]
                 ]
             },
             "id": 5177628645
         }
        """
    Json.parse(bodyStr)
  }
}
