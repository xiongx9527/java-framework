package org.ssssssss.magicapi.core.config;

import lombok.Data;

@Data
public class ResponseBase {

    private String code;

    private String message;

    private String data;

    private String traceId;

    private String spanId;

    private String timestamp;

    private String executeTime;
}
