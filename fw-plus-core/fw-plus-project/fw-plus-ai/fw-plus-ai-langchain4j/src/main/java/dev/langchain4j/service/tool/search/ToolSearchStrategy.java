package dev.langchain4j.service.tool.search;
import dev.langchain4j.Experimental;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.invocation.InvocationContext;
import java.util.List;
@Experimental public interface ToolSearchStrategy { List<ToolSpecification> getToolSearchTools(InvocationContext context); ToolSearchResult search(ToolSearchRequest request); }
