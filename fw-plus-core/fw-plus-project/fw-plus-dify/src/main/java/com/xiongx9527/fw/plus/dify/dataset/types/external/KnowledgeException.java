package com.xiongx9527.fw.plus.dify.dataset.types.external;

import com.alibaba.fastjson.annotation.JSONField;
import com.xiongx9527.fw.plus.common.exception.BizException;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class KnowledgeException extends BizException {

    /** 状态码 50008003（SERVER/THIRD_PARTY/003 Dify 知识库接口错误） */
    public static final int CODE = 50_008_003;

    /**
     * 错误代码
     */
    private Integer error_code;

    /**
     * 错误信息
     */
    private String error_msg;

    public KnowledgeException() {
        super(CODE, (String) null);
    }

    public KnowledgeException(Integer error_code, String error_msg) {
        super(CODE, (String) null);
        this.error_code = error_code;
        this.error_msg = error_msg;
    }

    /**
     * toObject
     */
    public Object toObject() {
        Map<String, Object> obj = new HashMap<>();
        obj.put("error_code", error_code);
        obj.put("error_msg", error_msg);

        return obj;
    }

    @Override
    public String toString() {
        return "{" +
                "error_code: " + error_code +
                ", error_msg: '" + error_msg + '\'' +
                '}';
    }

    /**
     * 兼容旧客户端的错误信息访问器。
     */
    @Deprecated
    @JSONField(serialize = false, deserialize = false)
    public String getError_message() {
        return error_msg;
    }

    /**
     * 兼容旧客户端的错误信息访问器。
     */
    @Deprecated
    @JSONField(serialize = false, deserialize = false)
    public void setError_message(String error_message) {
        this.error_msg = error_message;
    }
}
