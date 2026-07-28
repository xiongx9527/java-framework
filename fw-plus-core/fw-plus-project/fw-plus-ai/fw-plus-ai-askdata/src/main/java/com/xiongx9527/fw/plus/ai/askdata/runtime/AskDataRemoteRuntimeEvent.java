package com.xiongx9527.fw.plus.ai.askdata.runtime;
/** 远程运行时的有序流式事件。 */
public final class AskDataRemoteRuntimeEvent { private final String requestId; private final String content; private final boolean completed;
	private AskDataRemoteRuntimeEvent(String requestId,String content,boolean completed){if(requestId==null||requestId.trim().length()==0)throw new IllegalArgumentException("requestId 不能为空");this.requestId=requestId.trim();this.content=content;this.completed=completed;}
	public static AskDataRemoteRuntimeEvent delta(String requestId,String content){return new AskDataRemoteRuntimeEvent(requestId,content,false);} public static AskDataRemoteRuntimeEvent completed(String requestId){return new AskDataRemoteRuntimeEvent(requestId,null,true);} public String getRequestId(){return requestId;} public String getContent(){return content;} public boolean isCompleted(){return completed;} }
