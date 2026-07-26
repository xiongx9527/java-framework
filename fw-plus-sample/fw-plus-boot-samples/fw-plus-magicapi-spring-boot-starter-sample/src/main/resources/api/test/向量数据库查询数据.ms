{
  "properties" : { },
  "id" : "66d90703566c484ebd1c41021e550d1d",
  "script" : null,
  "groupId" : "6ba036bab29041888d8c2705d6fb77d3",
  "name" : "向量数据库查询数据",
  "createTime" : 1758710400512,
  "updateTime" : 1758770139865,
  "lock" : null,
  "createBy" : "admin",
  "updateBy" : "admin",
  "path" : "tststst",
  "method" : "GET",
  "parameters" : [ ],
  "options" : [ ],
  "requestBody" : "",
  "headers" : [ ],
  "paths" : [ ],
  "responseBody" : null,
  "description" : null,
  "requestBodyDefinition" : null,
  "responseBodyDefinition" : null
}
================================
import log;
import dev.langchain4j.model.embedding.EmbeddingModel ;
import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import qdrant;
import com.alibaba.fastjson2.JSON;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;

//嵌入模型
var embeddingModel = new AllMiniLmL6V2QuantizedEmbeddingModel();
//向量存储
var embeddingStore = qdrant['test'].buildEmbeddingStore("test",null);
// 查询相似的向量
Embedding queryEmbedding = embeddingModel.embed("为男性且年龄<18岁").content();
var relevant = embeddingStore.findRelevant(queryEmbedding,1);
for ( match in relevant) {
    log.info("==================相似度分数:{}",match.score());
    log.info("==================内容:{}",match.embeddingId());
}
return "ddddddddd" ;