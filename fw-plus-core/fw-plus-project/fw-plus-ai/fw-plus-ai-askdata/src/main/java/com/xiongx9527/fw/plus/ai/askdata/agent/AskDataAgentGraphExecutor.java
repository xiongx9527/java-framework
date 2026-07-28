package com.xiongx9527.fw.plus.ai.askdata.agent;
import java.util.ArrayList; import java.util.HashSet; import java.util.List; import java.util.Set;
/** 依赖满足后执行节点；没有依赖时严格保持图声明顺序。 */
public final class AskDataAgentGraphExecutor {
	public List<String> execute(AskDataAgentGraph graph,AskDataAgentNodeExecutor executor){if(graph==null||executor==null)throw new IllegalArgumentException("graph 和 executor 不能为空");List<AskDataAgentNode> pending=new ArrayList<AskDataAgentNode>(graph.getNodes());List<String> results=new ArrayList<String>();Set<String> completed=new HashSet<String>();while(!pending.isEmpty()){boolean progressed=false;for(int i=0;i<pending.size();i++){AskDataAgentNode node=pending.get(i);if(completed.containsAll(node.getDependsOn())){results.add(executor.execute(node));completed.add(node.getId());pending.remove(i--);progressed=true;}}if(!progressed)throw new IllegalStateException("Agent 图存在未满足依赖或循环");}return results;}
}
