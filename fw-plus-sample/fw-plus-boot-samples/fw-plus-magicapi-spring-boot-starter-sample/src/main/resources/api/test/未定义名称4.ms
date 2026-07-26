{
  "properties" : { },
  "id" : "3245dcde2b7b4848998938d80f309096",
  "script" : null,
  "groupId" : "6ba036bab29041888d8c2705d6fb77d3",
  "name" : "未定义名称4",
  "createTime" : 1757572592331,
  "updateTime" : 1766720417557,
  "lock" : null,
  "createBy" : "admin",
  "updateBy" : null,
  "path" : "eee",
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
import ws;
import log;
import trace;


trace.traceLog("a",()->{
    // 测试代码（如果可能）
    log.info("------------------- 纯分隔符 -------------------");
    log.info("key=value, position=开始, positionNum=1");  // 结构化
    log.info("{\"action\":\"test\",\"data\":\"value\"}");  // JSON
    log.info("普通文本日志");
    trace.traceLog("b",()->{
       // 测试代码（如果可能）
       log.info("/test/eee2");
    });
}) ;

return ws.generateToken("123");