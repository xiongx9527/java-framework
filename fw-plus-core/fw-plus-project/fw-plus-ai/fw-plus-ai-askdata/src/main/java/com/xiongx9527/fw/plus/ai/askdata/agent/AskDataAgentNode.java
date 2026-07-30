package com.xiongx9527.fw.plus.ai.askdata.agent;
import java.util.ArrayList; import java.util.Collections; import java.util.List;
/** Agent 图节点定义。 */
public final class AskDataAgentNode { private final String id; private final List<String> dependsOn;
	public AskDataAgentNode(String id,List<String> dependsOn){if(id==null||id.trim().length()==0)throw new IllegalArgumentException("id 不能为空");this.id=id.trim();this.dependsOn=Collections.unmodifiableList(new ArrayList<String>(dependsOn==null?Collections.<String>emptyList():dependsOn));}
	public String getId(){return id;} public List<String> getDependsOn(){return dependsOn;} }
