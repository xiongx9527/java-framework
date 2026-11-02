package com.xiongx9527.fw.plus.ai.model.chat.request;

/** 响应格式，镜像 langchain4j 1.23.0 的 {@code dev.langchain4j.model.chat.request.ResponseFormat}。 */
public class ResponseFormat {
    public enum ResponseFormatType { TEXT, JSON }
    public static final ResponseFormat TEXT = new ResponseFormat(ResponseFormatType.TEXT, null);
    public static final ResponseFormat JSON = new ResponseFormat(ResponseFormatType.JSON, null);
    private final ResponseFormatType type;
    private final String jsonSchema;
    public ResponseFormat(ResponseFormatType type) { this(type, null); }
    public ResponseFormat(ResponseFormatType type, String jsonSchema) { this.type = type; this.jsonSchema = jsonSchema; }
    public ResponseFormatType type() { return type; }
    public String jsonSchema() { return jsonSchema; }
}
