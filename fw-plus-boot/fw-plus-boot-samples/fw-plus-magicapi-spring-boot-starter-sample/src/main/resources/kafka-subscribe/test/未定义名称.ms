{
  "properties" : { },
  "id" : "2806eda373f5480cb85a79fff8207099",
  "script" : null,
  "groupId" : "e69696de42344850902d6efeb10711f8",
  "name" : "未定义名称",
  "createTime" : 1764494030910,
  "updateTime" : 1765438345782,
  "lock" : null,
  "createBy" : null,
  "updateBy" : null,
  "path" : null,
  "key" : "test",
  "topic" : "1234567",
  "broadcast" : false,
  "group" : "magic-api",
  "partition" : null,
  "enabled" : true,
  "ackMode" : "BATCH",
  "ackCount" : 100,
  "ackTime" : 5000,
  "concurrency" : null,
  "other" : "{\n  \"enable.auto.commit\":true\n}",
  "params" : null
}
================================
import log ;

log.info("===================={}",msg.value);
return null; // 正常处理时返回null