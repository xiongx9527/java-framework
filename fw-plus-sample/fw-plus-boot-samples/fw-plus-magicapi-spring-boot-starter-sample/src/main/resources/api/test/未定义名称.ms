{
  "properties" : { },
  "id" : "378a1922553c4999bf222e858a371370",
  "script" : null,
  "groupId" : "6ba036bab29041888d8c2705d6fb77d3",
  "name" : "未定义名称",
  "createTime" : 1753078713722,
  "updateTime" : 1764509052100,
  "lock" : null,
  "createBy" : "admin",
  "updateBy" : null,
  "path" : "eeee",
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

import dynamicClassLoader ;

var test = dynamicClassLoader.newInstance("a.b.c.Test");

var test1 = dynamicClassLoader.getSpringBean("a.b.c.Test");

var test2 = dynamicClassLoader.newInstance("a.b.c.Test1");

//test2.test('ddddd');

dynamicClassLoader.setter(test2,'b','iiiiii');


return {

"test":test,
"test1":test1,
'test2':test2,
 'test2Result':dynamicClassLoader.getter(test2,'b')

};