package org.ssssssss.magicapi.core.config;

import lombok.Data;

@Data
public class ResponseCode extends ResponseBase {
    private int success = 1;
    private int invalid = 0;
    private int exception = -1;
}