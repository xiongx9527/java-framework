package dev.langchain4j.service.tool;
import dev.langchain4j.agent.tool.ReturnBehavior;
import dev.langchain4j.agent.tool.ToolSpecification;
import java.util.Objects;
import static dev.langchain4j.internal.Utils.getOrDefault;
import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;
public class AiServiceTool {
 private final ToolSpecification specification; private final ToolExecutor executor; private final ReturnBehavior behavior;
 private AiServiceTool(Builder b){specification=ensureNotNull(b.specification,"toolSpecification");executor=ensureNotNull(b.executor,"toolExecutor");behavior=getOrDefault(b.behavior,ReturnBehavior.TO_LLM);}
 public String name(){return specification.name();} public ToolSpecification toolSpecification(){return specification;} public ToolExecutor toolExecutor(){return executor;} public ReturnBehavior returnBehavior(){return behavior;} @Deprecated public boolean immediateReturn(){return behavior==ReturnBehavior.IMMEDIATE;}
 public Builder toBuilder(){return builder().toolSpecification(specification).toolExecutor(executor).returnBehavior(behavior);} public static Builder builder(){return new Builder();}
 public boolean equals(Object o){if(this==o)return true;if(!(o instanceof AiServiceTool))return false;AiServiceTool t=(AiServiceTool)o;return Objects.equals(specification,t.specification)&&Objects.equals(executor,t.executor)&&behavior==t.behavior;} public int hashCode(){return Objects.hash(specification,executor,behavior);} public String toString(){return "AiServiceTool{"+specification+','+executor+','+behavior+'}';}
 public static class Builder { private ToolSpecification specification; private ToolExecutor executor; private ReturnBehavior behavior; public Builder toolSpecification(ToolSpecification v){specification=v;return this;} public Builder toolExecutor(ToolExecutor v){executor=v;return this;} public Builder returnBehavior(ReturnBehavior v){behavior=v;return this;} @Deprecated public Builder immediateReturn(boolean v){if(v)behavior=ReturnBehavior.IMMEDIATE;return this;} public AiServiceTool build(){return new AiServiceTool(this);} }
}
