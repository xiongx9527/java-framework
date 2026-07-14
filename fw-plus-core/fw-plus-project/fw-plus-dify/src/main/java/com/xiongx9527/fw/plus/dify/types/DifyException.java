package com.xiongx9527.fw.plus.dify.types;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Dify exception type
 */
public class DifyException extends RuntimeException {
    @Getter
    private String original;

    @Getter
    private Integer status;

    @Getter
    private Integer httpStatus;

    @Getter
    private Integer difyStatus;

    @Getter
    private String code;

    private String message;

    @Getter
    private String params;

    public DifyException() {
        super();
    }

    public DifyException(String response, Integer status) {
        this.httpStatus = status;
        this.status = status;
        this.original = response;

        if (response == null || response.trim().length() == 0) {
            this.message = "Dify request failed with HTTP status " + status;
            return;
        }

        try {
            JSONObject object = JSON.parseObject(response);
            this.difyStatus = object.getInteger("status");
            this.status = this.difyStatus != null ? this.difyStatus : status;
            this.code = object.getString("code");
            this.message = object.getString("message");
            this.params = object.getString("params");
            if (this.message == null || this.message.trim().length() == 0) {
                this.message = response;
            }
        } catch (Exception e) {
            this.message = response;
        }
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        Map<String, Object> map = new HashMap<>();
        map.put("status", status);
        map.put("httpStatus", httpStatus);
        map.put("difyStatus", difyStatus);
        map.put("code", code);
        map.put("message", message);
        map.put("params", params);
        return JSON.toJSONString(map);
    }
}
