package dev.langchain4j.service.tool.search;
import dev.langchain4j.Experimental;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.invocation.InvocationContext;
import java.util.List;
import java.util.Objects;
import static dev.langchain4j.internal.Utils.copy;
import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;
@Experimental public class ToolSearchRequest {
 private final ToolExecutionRequest request; private final List<ToolSpecification> tools; private final InvocationContext context;
 public ToolSearchRequest(Builder b){request=ensureNotNull(b.request,"toolExecutionRequest");tools=copy(b.tools);context=ensureNotNull(b.context,"invocationContext");}
 public ToolExecutionRequest toolExecutionRequest(){return request;} public List<ToolSpecification> searchableTools(){return tools;} public InvocationContext invocationContext(){return context;}
 public static Builder builder(){return new Builder();}
 public boolean equals(Object o){if(this==o)return true;if(!(o instanceof ToolSearchRequest))return false;ToolSearchRequest r=(ToolSearchRequest)o;return Objects.equals(request,r.request)&&Objects.equals(tools,r.tools)&&Objects.equals(context,r.context);} public int hashCode(){return Objects.hash(request,tools,context);} public String toString(){return "ToolSearchRequest{"+request+','+tools+','+context+'}';}
 public static class Builder {private ToolExecutionRequest request;private List<ToolSpecification> tools;private InvocationContext context;public Builder toolExecutionRequest(ToolExecutionRequest v){request=v;return this;}public Builder searchableTools(List<ToolSpecification> v){tools=v;return this;}public Builder invocationContext(InvocationContext v){context=v;return this;}public ToolSearchRequest build(){return new ToolSearchRequest(this);}}
}
