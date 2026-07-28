package com.xiongx9527.fw.plus.ai.askdata.publishing;
/** 已发布接口的不可变描述。 */
public final class AskDataPublishedInterface { private final String id; private final String name; private final String definitionId;
	public AskDataPublishedInterface(String id,String name,String definitionId){if(id==null||id.trim().length()==0)throw new IllegalArgumentException("id 不能为空");this.id=id.trim();this.name=name;this.definitionId=definitionId;} public String getId(){return id;} public String getName(){return name;} public String getDefinitionId(){return definitionId;} }
