{
  "properties" : { },
  "id" : "0687c768881044b8a1f9ff90a102c74a",
  "script" : null,
  "groupId" : "6ba036bab29041888d8c2705d6fb77d3",
  "name" : "向量数据库添加数据",
  "createTime" : 1758684961204,
  "updateTime" : 1777039249126,
  "lock" : null,
  "createBy" : "admin",
  "updateBy" : null,
  "path" : "test123",
  "method" : "GET",
  "parameters" : [ ],
  "options" : [ ],
  "requestBody" : "{\n    \"a\":\"b44\"\n}",
  "headers" : [ ],
  "paths" : [ ],
  "responseBody" : null,
  "description" : null,
  "requestBodyDefinition" : {
    "name" : "",
    "value" : "",
    "description" : "",
    "required" : false,
    "dataType" : "Object",
    "type" : null,
    "defaultValue" : null,
    "validateType" : "",
    "error" : "",
    "expression" : "",
    "children" : [ {
      "name" : "a",
      "value" : "b44",
      "description" : "",
      "required" : false,
      "dataType" : "String",
      "type" : null,
      "defaultValue" : null,
      "validateType" : "",
      "error" : "",
      "expression" : "",
      "children" : [ ]
    } ]
  },
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
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
//嵌入模型
var embeddingModel = new AllMiniLmL6V2QuantizedEmbeddingModel();
//向量存储
var embeddingStore = qdrant['test'].buildEmbeddingStore("test",null);


var enhance = (text) -> {
   return text
    // 复合比较操作符（优先处理，避免被单个词替换）
    .replaceAll("大于等于", "大于等于【GE】")
    .replaceAll("小于等于", "小于等于【LE】")
    .replaceAll("不等于", "不等于【NEQ】")
    .replaceAll("不小于", "不小于【NOT_LT】")
    .replaceAll("不大于", "不大于【NOT_GT】")
    .replaceAll("至少", "至少【GE】")
    .replaceAll("至多", "至多【LE】")
    .replaceAll("不超过", "不超过【LE】")
    .replaceAll("不低于", "不低于【GE】")
    // 基础比较操作符
    .replaceAll("大于", "大于【GT】")
    .replaceAll("小于", "小于【LT】")
    .replaceAll("等于", "等于【EQ】")
    .replaceAll("超过", "超过【GT】")
    .replaceAll("低于", "低于【LT】")
    .replaceAll("高于", "高于【GT】")
    .replaceAll("少于", "少于【LT】")
    .replaceAll("多于", "多于【GT】")
    .replaceAll("不少于", "不少于【GE】")
    .replaceAll("不多于", "不多于【LE】")
    // 数学符号
    .replaceAll(">", "大于【GT】")
    .replaceAll("<", "小于【LT】")
    .replaceAll(">=", "大于等于【GE】")
    .replaceAll("<=", "小于等于【LE】")
    .replaceAll("==", "等于【EQ】")
    .replaceAll("!=", "不等于【NEQ】")
    .replaceAll("≠", "不等于【NEQ】")
    .replaceAll("≥", "大于等于【GE】")
    .replaceAll("≤", "小于等于【LE】")
    // 逻辑比较
    .replaceAll("高于等于", "大于等于【GE】")
    .replaceAll("低于等于", "小于等于【LE】")
    .replaceAll("超过等于", "大于等于【GE】")
    .replaceAll("少于等于", "小于等于【LE】")
    .replaceAll("多于等于", "大于等于【GE】")
    .replaceAll("低于等于", "小于等于【LE】")
    // 其他表达方式
    .replaceAll("上限", "上限【LE】")
    .replaceAll("下限", "下限【GE】")
    .replaceAll("最大不超过", "最大不超过【LE】")
    .replaceAll("最小不低于", "最小不低于【GE】");

} 


//业务数据
var arrayStr = [
    {id:"1",name:"如果年龄大于18岁且性别为男性"},
    {id:"2",name:"如果年龄小于18岁或来自北京"},
    {id:"3",name:"如果学历为本科且工作经验超过5年"}
];

//"e1135c34-795e-45f6-8b4c-cf70c6f0529c",
//"39554f9d-bf06-4134-9a8b-67e611911b3a",
//"ce91f601-cc1a-44e2-8029-a67ed004bee6"

//便利入库
var ids = [];
for (str in arrayStr) {
    var text = enhance(str.name);
    var egment = embeddingModel.embed(TextSegment.from(text,Metadata.from("condition", text))).content() ;
    ids.push(embeddingStore.add(egment));
}
return ids ;