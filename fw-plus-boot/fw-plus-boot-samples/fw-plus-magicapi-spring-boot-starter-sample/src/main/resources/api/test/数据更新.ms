{
  "properties" : { },
  "id" : "a838bd96a94e4bd9a61be354970e534e",
  "script" : null,
  "groupId" : "6ba036bab29041888d8c2705d6fb77d3",
  "name" : "数据更新",
  "createTime" : 1757579742312,
  "updateTime" : 1761386556485,
  "lock" : null,
  "createBy" : "admin",
  "updateBy" : null,
  "path" : "/test",
  "method" : "GET",
  "parameters" : [ ],
  "options" : [ ],
  "requestBody" : "{\n    \"a\":\"ddd\"\n}",
  "headers" : [ ],
  "paths" : [ ],
  "responseBody" : null,
  "description" : null,
  "requestBodyDefinition" : null,
  "responseBodyDefinition" : null
}
================================
import ws;
import com.alibaba.fastjson2.JSON;

//

ws.broadcastMessageToClient("dddd","有数据更新啦");


return 'Hello magic-api'