{
  "properties" : { },
  "id" : "a0f379e4e7f447cd9e4d447da7432067",
  "script" : null,
  "groupId" : "34307223d67e4587b7b8341940341b9a",
  "name" : "未定义名称",
  "createTime" : 1761282725041,
  "updateTime" : 1777024289395,
  "lock" : null,
  "createBy" : null,
  "updateBy" : null,
  "path" : "等等",
  "method" : "GET",
  "parameters" : [ ],
  "options" : [ ],
  "requestBody" : "{\n    \"a\":\"1234\"\n}",
  "headers" : [ ],
  "paths" : [ ],
  "responseBody" : null,
  "description" : null,
  "requestBodyDefinition" : {
    "name" : "",
    "value" : "",
    "description" : "",
    "required" : false,
    "dataType" : "Object",
    "type" : null,
    "defaultValue" : null,
    "validateType" : "",
    "error" : "",
    "expression" : "",
    "children" : [ {
      "name" : "a",
      "value" : "1234",
      "description" : "",
      "required" : false,
      "dataType" : "String",
      "type" : null,
      "defaultValue" : null,
      "validateType" : "",
      "error" : "",
      "expression" : "",
      "children" : [ ]
    } ]
  },
  "responseBodyDefinition" : null
}
================================
//import vue3 ;
import s3 ;
import log ;
var params = {} ;
var url = s3['jbptest'].getFileStorageService().uploadByUrl("http://jbptest.oss-cn-beijing.aliyuncs.com/dev/6960a7cb3b8b36a3383b139d.docx?Expires=1767945691&OSSAccessKeyId=LTAI5tDQFDkdcZQfmzy5CwDT&Signature=aNEM%2B6tkMpDc%2Fi2xIx8Txopn754%3D");
log.info("===========>url:{}",url);
return url;//vue3.view('/test/login',params);