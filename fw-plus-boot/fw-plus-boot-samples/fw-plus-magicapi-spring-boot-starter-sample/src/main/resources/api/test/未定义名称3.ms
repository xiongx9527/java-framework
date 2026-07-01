{
  "properties" : { },
  "id" : "aa5de43a3e9843c58dc6cdd6e0200a32",
  "script" : null,
  "groupId" : "6ba036bab29041888d8c2705d6fb77d3",
  "name" : "未定义名称3",
  "createTime" : 1756638231875,
  "updateTime" : 1765855612964,
  "lock" : null,
  "createBy" : "admin",
  "updateBy" : null,
  "path" : "34534",
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
import redis ;
import log ;

//rrrsss收拾收拾345345345
redis.test.set('1','3');
log.info('dddddd:{}',redis.getRedisTemplate());


return 'Hello magic-api'