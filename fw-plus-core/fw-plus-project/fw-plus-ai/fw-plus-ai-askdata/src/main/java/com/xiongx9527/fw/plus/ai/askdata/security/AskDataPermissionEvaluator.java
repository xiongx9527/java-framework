package com.xiongx9527.fw.plus.ai.askdata.security;
import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
/** 权限判定 SPI。 */
public interface AskDataPermissionEvaluator { boolean isAllowed(AskDataProjectContext context, String capability, String resource); }
