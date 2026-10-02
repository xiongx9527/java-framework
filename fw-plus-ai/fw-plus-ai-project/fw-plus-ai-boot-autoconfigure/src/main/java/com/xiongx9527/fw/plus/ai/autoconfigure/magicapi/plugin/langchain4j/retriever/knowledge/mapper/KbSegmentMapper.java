package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.mapper;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.entity.KbSegment;
import com.xiongx9527.fw.plus.mp.base.MpBaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
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

    /**
     * 混合检索关键词路：FULLTEXT ngram 全文召回（需要 ft_kb_segment_text 索引；
     * 索引缺失时调用方捕获异常回落 LIKE 方式）。relevance 写入实体非表字段。
     */
    @Select("SELECT s.*, MATCH(s.content, s.keywords) AGAINST(#{query} IN NATURAL LANGUAGE MODE) AS relevance "
            + "FROM kb_segment s "
            + "WHERE s.dataset_id = #{datasetId} AND s.enabled = '1' AND s.status = 'completed' "
            + "AND MATCH(s.content, s.keywords) AGAINST(#{query} IN NATURAL LANGUAGE MODE) "
            + "LIMIT #{limit}")
    java.util.List<KbSegment> fulltextRecall(@Param("datasetId") String datasetId,
            @Param("query") String query, @Param("limit") int limit);
}
