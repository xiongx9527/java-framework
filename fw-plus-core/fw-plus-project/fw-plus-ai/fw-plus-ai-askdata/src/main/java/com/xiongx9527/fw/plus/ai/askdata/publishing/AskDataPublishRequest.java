package com.xiongx9527.fw.plus.ai.askdata.publishing;
/** 只含脱敏定义引用的发布请求。 */
public final class AskDataPublishRequest { private final String name; private final String definitionId;
	public AskDataPublishRequest(String name,String definitionId){if(blank(name)||blank(definitionId))throw new IllegalArgumentException("name 和 definitionId 不能为空");this.name=name.trim();this.definitionId=definitionId.trim();} public String getName(){return name;} public String getDefinitionId(){return definitionId;} private static boolean blank(String s){return s==null||s.trim().length()==0;} }
