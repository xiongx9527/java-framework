{
  "properties" : { },
  "id" : "5946b19931ba4098acb700d46ba6aaa0",
  "script" : null,
  "groupId" : "6ba036bab29041888d8c2705d6fb77d3",
  "name" : "未定义名称1",
  "createTime" : 1756280563945,
  "updateTime" : 1764509043092,
  "lock" : null,
  "createBy" : "admin",
  "updateBy" : null,
  "path" : "tse",
  "method" : "GET",
  "parameters" : [ ],
  "options" : [ ],
  "requestBody" : "",
  "headers" : [ ],
  "paths" : [ ],
  "responseBody" : null,
  "description" : null,
  "requestBodyDefinition" : null,
  "responseBodyDefinition" : null
}
================================
import kafka ;

var test = "123";
kafka['test'].send("test",test);



return 'Hello magic-api'