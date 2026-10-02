package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.mapper;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.entity.KbDocument;
import com.xiongx9527.fw.plus.mp.base.MpBaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 知识库文档 Mapper：状态机 CAS 认领等专用更新。
 */
public interface KbDocumentMapper extends MpBaseMapper<KbDocument> {

    /**
     * CAS 认领：仅当当前状态等于 expected 时流转到 target（多实例部署下保证同一文档不被重复索引）。
     *
     * @return 命中行数：1 表示本实例抢到，0 表示已被其它实例处理
     */
    @Update("UPDATE kb_document SET indexing_status = #{target} "
            + "WHERE id = #{id} AND indexing_status = #{expected}")
    int casTransit(@Param("id") String id, @Param("expected") String expected, @Param("target") String target);
}
