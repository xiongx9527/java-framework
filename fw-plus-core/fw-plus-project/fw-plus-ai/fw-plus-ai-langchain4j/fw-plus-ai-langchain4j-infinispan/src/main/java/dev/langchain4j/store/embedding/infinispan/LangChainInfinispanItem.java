package dev.langchain4j.store.embedding.infinispan;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

/**
 * Langchain item that is serialized for the langchain integration use case.
 */
public class LangChainInfinispanItem {

   private final String id;
   private final float[] embedding;
   private final String text;
   private final Set<LangChainMetadata> metadata;

   public LangChainInfinispanItem(String id, float[] embedding, String text, Set<LangChainMetadata> metadata) {
      this.id = id;
      this.embedding = embedding;
      this.text = text;
      this.metadata = metadata;
   }

   public String id() {
      return id;
   }

   public float[] embedding() {
      return embedding;
   }

   public String text() {
      return text;
   }

   public Set<LangChainMetadata> metadata() {
      return metadata;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      }
      if (!(o instanceof LangChainInfinispanItem)) {
         return false;
      }
      LangChainInfinispanItem that = (LangChainInfinispanItem) o;
      return Objects.equals(id, that.id)
            && Arrays.equals(embedding, that.embedding)
            && Objects.equals(text, that.text)
            && Objects.equals(metadata, that.metadata);
   }

   @Override
   public int hashCode() {
      int result = Objects.hash(id, text, metadata);
      result = 31 * result + Arrays.hashCode(embedding);
      return result;
   }
}
