package dev.langchain4j.store.embedding.infinispan;

import java.util.Objects;

/**
 * Holds configuration for the store.
 */
public class InfinispanStoreConfiguration {

   /**
    * Default Cache Config
    */
   public static final String DEFAULT_CACHE_CONFIG =
         "<distributed-cache name=\"CACHE_NAME\">\n"
               + "<indexing storage=\"local-heap\">\n"
               + "<indexed-entities>\n"
               + "<indexed-entity>LANGCHAINITEM</indexed-entity>\n"
               + "<indexed-entity>LANGCHAIN_METADATA</indexed-entity>\n"
               + "</indexed-entities>\n"
               + "</indexing>\n"
               + "</distributed-cache>";

   /**
    * Default package of the schema
    */
   public static final String DEFAULT_ITEM_PACKAGE = "dev.langchain4j";

   /**
    * Default name of the protobuf langchain item. Size will be added
    */
   public static final String DEFAULT_LANGCHAIN_ITEM = "LangChainItem";
   /**
    * Default name of the protobuf metadata item. Size will be added
    */
   public static final String DEFAULT_METADATA_ITEM = "LangChainMetadata";
   /**
    * The default distance to for the search
    */
   public static final int DEFAULT_DISTANCE = 3;
   /**
    * Default vector similarity
    */
   public static final String DEFAULT_SIMILARITY = "COSINE";

   private final String cacheName;
   private final Integer dimension;
   private final Integer distance;
   private final String similarity;
   private final String cacheConfig;
   private final String packageItem;
   private final String fileName;
   private final String langchainItemName;
   private final String metadataItemName;
   private final boolean createCache;
   private final boolean registerSchema;

   /**
    * Creates the configuration and sets default values.
    */
   public InfinispanStoreConfiguration(String cacheName, Integer dimension, Integer distance, String similarity, String cacheConfig,
                                       String packageItem, String fileName, String langchainItemName,
                                       String metadataItemName, boolean createCache, boolean registerSchema) {
      this.cacheName = cacheName;
      this.dimension = dimension;
      this.cacheConfig = cacheConfig;
      this.distance = distance != null ? distance : DEFAULT_DISTANCE;
      this.similarity = similarity != null ? similarity : DEFAULT_SIMILARITY;
      this.packageItem = packageItem != null ? packageItem : DEFAULT_ITEM_PACKAGE;
      this.fileName = fileName != null ? fileName : computeFileName(packageItem, dimension);
      this.langchainItemName = langchainItemName != null ? langchainItemName : DEFAULT_LANGCHAIN_ITEM + dimension;
      this.metadataItemName = metadataItemName != null ? metadataItemName : DEFAULT_METADATA_ITEM + dimension;
      this.createCache = createCache;
      this.registerSchema = registerSchema;
   }

   public String cacheName() {
      return cacheName;
   }

   public Integer dimension() {
      return dimension;
   }

   public Integer distance() {
      return distance;
   }

   public String similarity() {
      return similarity;
   }

   public String cacheConfig() {
      return cacheConfig;
   }

   public String packageItem() {
      return packageItem;
   }

   public String fileName() {
      return fileName;
   }

   public String langchainItemName() {
      return langchainItemName;
   }

   public String metadataItemName() {
      return metadataItemName;
   }

   public boolean createCache() {
      return createCache;
   }

   public boolean registerSchema() {
      return registerSchema;
   }

   /**
    * Get the full name of the langchainItem protobuf type.
    */
   public String langchainItemFullType() {
      return packageItem + "." + langchainItemName;
   }

   /**
    * Get the full name of the metadata protobuf type.
    */
   public String metadataFullType() {
      return packageItem + "." + metadataItemName;
   }

   private static String computeFileName(String itemPackage, int dimension) {
      return itemPackage + "." + "dimension." + dimension + ".proto";
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      }
      if (!(o instanceof InfinispanStoreConfiguration)) {
         return false;
      }
      InfinispanStoreConfiguration that = (InfinispanStoreConfiguration) o;
      return createCache == that.createCache
            && registerSchema == that.registerSchema
            && Objects.equals(cacheName, that.cacheName)
            && Objects.equals(dimension, that.dimension)
            && Objects.equals(distance, that.distance)
            && Objects.equals(similarity, that.similarity)
            && Objects.equals(cacheConfig, that.cacheConfig)
            && Objects.equals(packageItem, that.packageItem)
            && Objects.equals(fileName, that.fileName)
            && Objects.equals(langchainItemName, that.langchainItemName)
            && Objects.equals(metadataItemName, that.metadataItemName);
   }

   @Override
   public int hashCode() {
      return Objects.hash(cacheName, dimension, distance, similarity, cacheConfig, packageItem, fileName,
            langchainItemName, metadataItemName, createCache, registerSchema);
   }
}
