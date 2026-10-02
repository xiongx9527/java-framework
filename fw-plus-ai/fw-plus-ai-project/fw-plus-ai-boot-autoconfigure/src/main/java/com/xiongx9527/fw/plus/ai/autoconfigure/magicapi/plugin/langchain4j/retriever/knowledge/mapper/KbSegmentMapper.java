package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.mapper;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.entity.KbSegment;
import com.xiongx9527.fw.plus.mp.base.MpBaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 知识库分段 Mapper。
 */
public interface KbSegmentMapper extends MpBaseMapper<KbSegment> {

    /**
     * 召回命中计数异步累加（设计 §3.1）。
     */
    @Update("UPDATE kb_segment SET hit_count = hit_count + 1 WHERE id = #{id}")
    int incrHitCount(@Param("id") String id);
}
