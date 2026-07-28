package com.xiongx9527.fw.plus.ai.askdata.runtime;
/** 稳定远程错误码语义。 */
public interface AskDataRemoteRuntimeErrorMapper { boolean isRollbackRequired(AskDataRemoteRuntimeResult result);
	static AskDataRemoteRuntimeErrorMapper defaultMapper(){return new AskDataRemoteRuntimeErrorMapper(){public boolean isRollbackRequired(AskDataRemoteRuntimeResult result){return result!=null&&!result.isSuccess()&&result.isRetryable();}};}}
