package com.xiongx9527.fw.plus.ai.askdata.memory;
import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
/** 推荐请求，显式表达是否可读取通用画像。 */
public final class AskDataRecommendationRequest { private final AskDataProjectContext context; private final boolean allowGeneric; private final int limit;
	public AskDataRecommendationRequest(AskDataProjectContext context,boolean allowGeneric,int limit){if(context==null||limit<=0)throw new IllegalArgumentException("context 和 limit 必须有效");this.context=context;this.allowGeneric=allowGeneric;this.limit=limit;} public AskDataProjectContext getContext(){return context;} public boolean isAllowGeneric(){return allowGeneric;} public int getLimit(){return limit;} }
