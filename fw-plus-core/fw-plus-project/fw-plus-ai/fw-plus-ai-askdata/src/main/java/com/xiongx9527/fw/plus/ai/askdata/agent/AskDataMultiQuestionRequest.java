package com.xiongx9527.fw.plus.ai.askdata.agent;
import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext; import java.util.ArrayList; import java.util.Collections; import java.util.List;
/** 多问题执行请求，含协作取消与超时预算。 */
public final class AskDataMultiQuestionRequest {
	private final AskDataProjectContext context; private final String requestId; private final List<AskDataSubQuestion> questions; private final long timeoutMillis; private volatile boolean cancelled;
	public AskDataMultiQuestionRequest(AskDataProjectContext context,String requestId,List<AskDataSubQuestion> questions,long timeoutMillis){if(context==null||requestId==null||requestId.trim().length()==0||questions==null||questions.isEmpty()||timeoutMillis<=0)throw new IllegalArgumentException("多问题请求参数无效");this.context=context;this.requestId=requestId.trim();this.questions=Collections.unmodifiableList(new ArrayList<AskDataSubQuestion>(questions));this.timeoutMillis=timeoutMillis;}
	public AskDataProjectContext getContext(){return context;} public String getRequestId(){return requestId;} public List<AskDataSubQuestion> getQuestions(){return questions;} public long getTimeoutMillis(){return timeoutMillis;} public void cancel(){cancelled=true;} public boolean isCancelled(){return cancelled;}
}
