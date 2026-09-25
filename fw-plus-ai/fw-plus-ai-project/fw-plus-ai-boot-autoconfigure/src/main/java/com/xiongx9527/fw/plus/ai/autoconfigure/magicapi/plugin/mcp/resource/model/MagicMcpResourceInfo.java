package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.model;

import lombok.Data;
import org.ssssssss.magicapi.core.model.MagicEntity;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class MagicMcpResourceInfo extends MagicEntity {

    private String name;

    private String uri;

    private String mimeType = "text/plain";

    private String description;

    /**
     * 资源内容：{@code resources/read} 直接返回的文本，缺省为空串。
     */
    private String content;

    private boolean enabled = true;

    private Map<String, Object> metadata = new LinkedHashMap<String, Object>();
}
