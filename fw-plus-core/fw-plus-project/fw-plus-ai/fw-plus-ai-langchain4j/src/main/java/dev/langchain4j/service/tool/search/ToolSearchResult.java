package dev.langchain4j.service.tool.search;
import dev.langchain4j.Experimental;
import java.util.List;
import java.util.Objects;
import static dev.langchain4j.internal.Utils.copy;
import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;
@Experimental public class ToolSearchResult {
 private final List<String> names; private final String text;
 public ToolSearchResult(List<String> n,String t){names=copy(n);text=ensureNotNull(t,"toolResultMessageText");}
 public List<String> foundToolNames(){return names;} public String toolResultMessageText(){return text;}
 public boolean equals(Object o){if(this==o)return true;if(!(o instanceof ToolSearchResult))return false;ToolSearchResult r=(ToolSearchResult)o;return Objects.equals(names,r.names)&&Objects.equals(text,r.text);} public int hashCode(){return Objects.hash(names,text);} public String toString(){return "ToolSearchResult{"+names+','+text+'}';}
}
