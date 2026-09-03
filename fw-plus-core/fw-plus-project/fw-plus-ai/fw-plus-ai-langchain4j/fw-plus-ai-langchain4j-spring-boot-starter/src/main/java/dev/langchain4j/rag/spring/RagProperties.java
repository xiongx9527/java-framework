//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.langchain4j.rag.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(
    prefix = "langchain4j.rag"
)
public class RagProperties {
    static final String PREFIX = "langchain4j.rag";
    @NestedConfigurationProperty
    RetrievalProperties retrieval;

    public RetrievalProperties getRetrieval() {
        return this.retrieval;
    }

    public void setRetrieval(RetrievalProperties retrieval) {
        this.retrieval = retrieval;
    }
}
