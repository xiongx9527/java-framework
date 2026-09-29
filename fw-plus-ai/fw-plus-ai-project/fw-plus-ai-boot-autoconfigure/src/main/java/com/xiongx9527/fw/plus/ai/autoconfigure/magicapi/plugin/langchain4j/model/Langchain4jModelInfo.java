package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ssssssss.magicapi.core.model.MagicEntity;

import java.util.Map;

/**
 * LangChain4j 模型资源公共字段：七类 datasource 共享的登记信息。
 * 连接/生成参数不在此列，统一放 options（各 provider 内容不一致）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class Langchain4jModelInfo extends MagicEntity {
    /** 唯一 key，脚本按 key 取实例；仅允许 [\w.-] */
    private String key;
    /** 展示名，缺省取 key */
    private String name;
    /** provider 短名，须命中该类资源短名表 */
    private String provider;
    /** false：销毁实例且不注册（下线而非删除） */
    private boolean enabled = true;
    /** 差异参数对象：不同 provider 的键集合不同 */
    private Map<String, Object> options;
}
