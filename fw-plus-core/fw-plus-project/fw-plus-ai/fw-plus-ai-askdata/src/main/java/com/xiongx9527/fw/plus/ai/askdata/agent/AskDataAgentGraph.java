package com.xiongx9527.fw.plus.ai.askdata.agent;
import java.util.ArrayList; import java.util.Collections; import java.util.List;
/** 版本化 Agent 图。 */
public final class AskDataAgentGraph { private final String id; private final String version; private final List<AskDataAgentNode> nodes;
	public AskDataAgentGraph(String id,String version,List<AskDataAgentNode> nodes){if(id==null||id.trim().length()==0||version==null||version.trim().length()==0||nodes==null||nodes.isEmpty())throw new IllegalArgumentException("图定义不能为空");this.id=id.trim();this.version=version.trim();this.nodes=Collections.unmodifiableList(new ArrayList<AskDataAgentNode>(nodes));}
	public String getId(){return id;} public String getVersion(){return version;} public List<AskDataAgentNode> getNodes(){return nodes;} }
