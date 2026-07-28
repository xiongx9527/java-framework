package com.xiongx9527.fw.plus.ai.askdata.runtime;
/** 可补偿的远程执行回滚 SPI。 */
public interface AskDataRemoteRuntimeRollback { void rollback(AskDataRemoteRuntimeRequest request, AskDataRemoteRuntimeAudit audit); }
