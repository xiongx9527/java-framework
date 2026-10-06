package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.mapper;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.entity.KbSetting;
import com.xiongx9527.fw.plus.mp.base.MpBaseMapper;

/**
 * 知识库全局设置 Mapper（kb_setting 单行表，主键固定 {@link KbSetting#SINGLETON_ID}）。
 */
public interface KbSettingMapper extends MpBaseMapper<KbSetting> {
}
