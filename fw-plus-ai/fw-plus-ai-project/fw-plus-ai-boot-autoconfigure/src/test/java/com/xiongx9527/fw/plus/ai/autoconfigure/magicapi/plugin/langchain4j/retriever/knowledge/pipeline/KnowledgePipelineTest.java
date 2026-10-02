package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.pipeline;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 切分管道测试（验收 A10/A11）：KbTokenCounter 近似、通用分段分隔符预切、父子分段结构。
 */
class KnowledgePipelineTest {

    @Test
    void token计数中文与英文混合近似() {
        assertThat(KbTokenCounter.estimate(null)).isZero();
        assertThat(KbTokenCounter.estimate("")).isZero();
        // 10 个汉字 ≈ 10 token
        assertThat(KbTokenCounter.estimate("一二三四五六七八九十")).isEqualTo(10);
        // 8 个英文字符 ≈ 2 token
        assertThat(KbTokenCounter.estimate("abcdefgh")).isEqualTo(2);
    }

    @Test
    void 通用分段按自定义分隔符预切() {
        DocumentPipeline pipeline = new DocumentPipeline();
        String text = "第一段落内容##第二段落内容##第三段落内容";
        List<String> chunks = pipeline.splitGeneral(text, 1000, 200, "##");
        assertThat(chunks).hasSize(3);
        assertThat(chunks.get(0)).isEqualTo("第一段落内容");
        assertThat(chunks.get(2)).isEqualTo("第三段落内容");
    }

    @Test
    void 通用分段超长文本递归切分() {
        DocumentPipeline pipeline = new DocumentPipeline();
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            text.append("这是第").append(i).append("段测试内容，用于验证递归切分行为。");
        }
        List<String> chunks = pipeline.splitGeneral(text.toString(), 100, 20, null);
        assertThat(chunks.size()).isGreaterThan(1);
        for (String chunk : chunks) {
            assertThat(chunk).isNotEmpty();
        }
    }

    @Test
    void 父子分段父段落表子段嵌入() {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            text.append("父段").append(i).append("：这是一段较长的父级内容，用来验证父子分段的结构划分是否正确。");
        }
        ParentChildSplitter splitter = new ParentChildSplitter(200, 40);
        List<ParentChildSplitter.Piece> pieces = splitter.split(text.toString(), null);
        assertThat(pieces).isNotEmpty();
        // 子段内容短于父段，且同一父段可对应多个子段
        int parentCount = pieces.stream()
                .map(ParentChildSplitter.Piece::getParentContent)
                .distinct().mapToInt(it -> 1).sum();
        assertThat(parentCount).isGreaterThanOrEqualTo(1);
        for (ParentChildSplitter.Piece piece : pieces) {
            assertThat(piece.getChildContent().length()).isLessThanOrEqualTo(piece.getParentContent().length());
            assertThat(piece.getChildPosition()).isGreaterThanOrEqualTo(0);
        }
        // 子段粒度小于父段：父段 0 至少切出 2 个子段（或整体子段数多于父段数）
        long firstParentChildren = pieces.stream()
                .filter(p -> p.getParentPosition() == 0).count();
        long distinctParents = pieces.stream()
                .map(ParentChildSplitter.Piece::getParentContent).distinct().count();
        assertThat(firstParentChildren >= 2 || pieces.size() > distinctParents).isTrue();
    }
}
