package com.xiongx9527.fw.plus.ai.askdata.agent;
import java.util.ArrayList; import java.util.Collections; import java.util.List;
/** 具有稳定标识和前置依赖的子问题。 */
public final class AskDataSubQuestion {
	private final String id; private final String question; private final List<String> dependsOn;
	public AskDataSubQuestion(String id, String question, List<String> dependsOn) { if (blank(id) || blank(question)) throw new IllegalArgumentException("id 和 question 不能为空"); this.id=id.trim(); this.question=question.trim(); this.dependsOn=Collections.unmodifiableList(new ArrayList<String>(dependsOn == null ? Collections.<String>emptyList() : dependsOn)); }
	public String getId(){return id;} public String getQuestion(){return question;} public List<String> getDependsOn(){return dependsOn;}
	private static boolean blank(String value){return value==null||value.trim().length()==0;}
}
