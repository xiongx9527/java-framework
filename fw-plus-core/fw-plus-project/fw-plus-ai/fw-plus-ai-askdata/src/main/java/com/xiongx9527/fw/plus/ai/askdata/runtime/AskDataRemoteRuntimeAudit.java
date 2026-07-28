package com.xiongx9527.fw.plus.ai.askdata.runtime;
import java.util.Collections; import java.util.List;
/** 远程调用的脱敏审计关联记录。 */
public final class AskDataRemoteRuntimeAudit { private final List<String> correlationIds; private final String errorCode;
	private AskDataRemoteRuntimeAudit(List<String> correlationIds,String errorCode){this.correlationIds=correlationIds;this.errorCode=errorCode;} public static AskDataRemoteRuntimeAudit from(AskDataRemoteRuntimeRequest request,AskDataRemoteRuntimeResult result){if(request==null)throw new IllegalArgumentException("request 不能为空");return new AskDataRemoteRuntimeAudit(Collections.singletonList(request.getAuditCorrelationId()),result==null?null:result.getErrorCode());} public List<String> getCorrelationIds(){return correlationIds;} public String getErrorCode(){return errorCode;} }
