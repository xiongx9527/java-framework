package com.xiongx9527.fw.plus.ai.autoconfigure.askdata;

public final class AskDataErrorCode {

    public static final String MODEL_NOT_FOUND = "MODEL_NOT_FOUND";
    public static final String DEFAULT_MODEL_NOT_CONFIGURED = "DEFAULT_MODEL_NOT_CONFIGURED";
    public static final String METADATA_NOT_FOUND = "METADATA_NOT_FOUND";
    public static final String VECTOR_RETRIEVAL_FAILED = "VECTOR_RETRIEVAL_FAILED";
    public static final String MODEL_RESPONSE_INVALID = "MODEL_RESPONSE_INVALID";
    public static final String DIALECT_RESOLUTION_FAILED = "DIALECT_RESOLUTION_FAILED";
    public static final String SQL_REJECTED = "SQL_REJECTED";
    public static final String SQL_EXECUTION_FAILED = "SQL_EXECUTION_FAILED";
    public static final String RESUME_FAILED = "RESUME_FAILED";
    public static final String CHECKPOINT_NOT_FOUND = "CHECKPOINT_NOT_FOUND";
    public static final String CONTEXT_COMPRESSION_FAILED = "CONTEXT_COMPRESSION_FAILED";
    public static final String SUBQUESTION_FAILED = "SUBQUESTION_FAILED";
    public static final String SUBQUESTION_TIMED_OUT = "SUBQUESTION_TIMED_OUT";
    public static final String SUBQUESTION_CANCELLED = "SUBQUESTION_CANCELLED";
    public static final String SUBQUESTION_SKIPPED = "SUBQUESTION_SKIPPED";
    public static final String INTERFACE_PUBLISH_FAILED = "INTERFACE_PUBLISH_FAILED";

    private AskDataErrorCode() {
    }
}
