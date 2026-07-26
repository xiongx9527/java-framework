package dev.langchain4j.store.embedding.infinispan;

import java.util.Objects;

/**
 * Langchain Metadata item that is serialized for the langchain integration use case.
 */
public class LangChainMetadata {

   private final String name;
   private final String value;

   public LangChainMetadata(String name, String value) {
      this.name = name;
      this.value = value;
   }

   public String name() {
      return name;
   }

   public String value() {
      return value;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      }
      if (!(o instanceof LangChainMetadata)) {
         return false;
      }
      LangChainMetadata that = (LangChainMetadata) o;
      return Objects.equals(name, that.name) && Objects.equals(value, that.value);
   }

   @Override
   public int hashCode() {
      return Objects.hash(name, value);
   }
}
