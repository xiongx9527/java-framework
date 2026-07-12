{
  "properties" : { },
  "id" : "db2445346788488b8db6a0dd63daf8de",
  "script" : null,
  "groupId" : "f81539ec5f89465181a74b22fb9e5272",
  "name" : "上线通知",
  "createTime" : 1757575615888,
  "updateTime" : 1757580020875,
  "lock" : null,
  "createBy" : "admin",
  "updateBy" : "admin",
  "path" : "",
  "key" : "default",
  "businessId" : "init",
  "enabled" : true,
  "description" : null
}
================================
import log;
import ws;

log.info("============================================clientId:{} 上线了",clientId);
//log.info("============================================sessionId:{}",sessionId);

ws.broadcastMessageToClient(clientId,"hello，暂时无数据更新")


return null; // 正常处理时返回null