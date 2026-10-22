{"id":"e2e-chat-0001","name":"chat-e2e","groupId":"e2e-group-0001","path":"chat-e2e","method":"GET","description":"mem0 end-to-end verification: multi-turn chat memory"}
================================
import langchain4j;
var user = header['X-User'];
if (user == null) { user = 'e2e-user'; }
var session = header['X-Session'];
if (session == null) { session = 'e2e-session'; }
// 中文消息走 GET 查询参数（?msg=...，URL 编码安全）；
// HTTP 头经 Tomcat ISO-8859-1 解码中文会变乱码，不可用于传消息
var userMsg = msg;
if (userMsg == null) { userMsg = '你好，我叫小明，记住我的名字'; }
var assistant = langchain4j.simpleAssistant()
	.dimensions('sample-app', 'e2e-agent', user)
	.session(session);
return assistant.chat(userMsg);
