package com.xiongx9527.fw.plus.ai.askdata.memory;
/** 可排序且可审计的推荐候选。 */
public final class AskDataRecommendationCandidate { private final String content; private final double score; private final boolean generic;
	public AskDataRecommendationCandidate(String content,double score,boolean generic){if(content==null||content.trim().length()==0)throw new IllegalArgumentException("content 不能为空");this.content=content.trim();this.score=score;this.generic=generic;} public String getContent(){return content;} public double getScore(){return score;} public boolean isGeneric(){return generic;} }
