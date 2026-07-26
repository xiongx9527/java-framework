package dev.langchain4j.store.embedding.infinispan;

/**
 * LangchainSchemaCreator for Infinispan.
 */
public final class LangchainSchemaCreator {

   private LangchainSchemaCreator() {
   }

   /**
    * Build the Infinispan ProtoStream schema to marshall embeddings.
    */
   public static String buildSchema(InfinispanStoreConfiguration storeConfiguration) {
      StringBuilder schema = new StringBuilder();
      schema.append("package ").append(storeConfiguration.packageItem()).append(";\n\n");
      schema.append("/* @Indexed */\n");
      schema.append("message ").append(storeConfiguration.metadataItemName()).append(" {\n");
      schema.append("  /* @Text */\n");
      schema.append("  optional string name = 1;\n");
      schema.append("  /* @Text */\n");
      schema.append("  optional string value = 2;\n");
      schema.append("}\n\n");
      schema.append("/* @Indexed */\n");
      schema.append("message ").append(storeConfiguration.langchainItemName()).append(" {\n");
      schema.append("  /* @Text */\n");
      schema.append("  optional string id = 1;\n");
      schema.append("  /* @Keyword */\n");
      schema.append("  optional string text = 2;\n");
      schema.append("  /* @Vector(dimension=").append(storeConfiguration.dimension())
            .append(", similarity=").append(storeConfiguration.similarity()).append(") */\n");
      schema.append("  repeated float embedding = 3;\n");
      schema.append("  repeated ").append(storeConfiguration.metadataItemName()).append(" metadata = 4;\n");
      schema.append("}\n");
      return schema.toString();
   }
}
