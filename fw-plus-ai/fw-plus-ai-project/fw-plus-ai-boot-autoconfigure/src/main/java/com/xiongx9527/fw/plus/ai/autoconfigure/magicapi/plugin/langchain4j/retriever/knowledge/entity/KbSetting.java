package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiongx9527.fw.plus.mp.base.MpBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 知识库全局设置（kb_setting）：单行表，{@link #SINGLETON_ID} 为固定主键。
 *
 * <p>取代原先绑定 yaml 的 {@code magic-api.plugin.knowledge.*}（{@code KnowledgeProperties}）：
 * 全部字段改由控制台知识库「设置」页读写，落库后由 {@code KbSettingService} 热替换，
 * 各消费方每次读取当前快照，因此保存即生效、无需重启。
 *
 * <p>{@link #enabled} 是**运行期**开关（原先是 {@code @ConditionalOnProperty} 装配条件，
 * 装配期决策、页面上配不到）。改为落库后，停用只是让业务入口前置检查失败，
 * 不再阻止 Bean 装配——这正是「全部到页面上配置」必须付出的代价。
 *
 * <p>{@link #embeddingModelKey} / {@link #embeddingStoreKey} / {@link #scoringModelKey} /
 * {@link #chatModelKey} 是<b>新建数据集的模型默认值</b>；{@code default*} 六个字段是<b>切分/召回参数
 * 默认值</b>。它们都只在数据集行对应字段为空时生效，不回头改已有数据集。
 * 不提供 {@code embeddingModelActive} 的默认值——那个字段记的是「上次成功索引实际用的模型」，
 * 是索引的产出而非配置，只能由首次成功索引写入。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_setting")
public class KbSetting extends MpBaseEntity<String> {

    /** 单行表固定主键 */
    public static final String SINGLETON_ID = "singleton";

    /** 运行期总开关：false 时业务入口前置检查失败（控制台仍可进入以便重新启用） */
    private Boolean enabled;

    /** 原始文件存储介质：local / s3 */
    private String storageType;

    /** 原始文件目录（storageType=local 时生效），默认 ${user.home}/.fw-plus/kb */
    private String storagePath;

    /** s3 数据源资源 key（storageType=s3 时生效；空则取 Spring 默认 s3 数据源） */
    private String s3Key;

    /** 混合检索开关：向量路 + 关键词路融合（默认开启；关闭则仅向量路） */
    private Boolean hybridSearch;

    /** ES 全文检索开关：开启后关键词路优先走 ES（需启用 magicapi elasticsearch 插件并配置 ES 数据源） */
    private Boolean esEnabled;

    /** ES 数据源资源 key（空则取 Spring 默认 ES 数据源） */
    private String esKey;

    /** ES 索引名前缀（分段全文索引名 = 前缀 + "-segments"），默认 fw-kb */
    private String esIndexPrefix;

    /**
     * 对外 Open API（{@code /v1/*} 与 {@code /settings}）的认证 provider。
     *
     * <p>取值与 {@code fw-plus-auth-core} 的内置 provider 清单一致（{@code permit-all}、
     * {@code sa-token-login}、{@code sa-token-oauth2}、{@code sa-token-jwt}、
     * {@code sa-token-apikey}、{@code sa-token-sso}），由控制台「设置」页选择。</p>
     *
     * <p>留空按 {@code sa-token-login} 处理（保持改造前的默认行为）；provider 模块不在 classpath
     * 或取值未知时 fail-closed（对外 API 返回 error_code 1002），不回退。</p>
     */
    private String openApiAuthProvider;

    // ---------------- 新建数据集的模型默认值 ----------------
    // 这四项是「建数据集时不必每次重填」的全局默认：页面上配一次，之后 create 数据集时
    // 对应字段留空即回落这里。数据集自身的值优先（可逐个覆盖），只有为空时才用默认值。

    /** 新建数据集默认使用的嵌入模型资源 key（langchain4j embedding-model） */
    private String embeddingModelKey;

    /** 新建数据集默认使用的向量库资源 key（langchain4j embedding-store） */
    private String embeddingStoreKey;

    /** 新建数据集默认使用的 rerank 模型资源 key（scoring-model），空则不重排 */
    private String scoringModelKey;

    /** 新建数据集默认使用的对话模型资源 key（chat-model），segment_mode=qa 时需要 */
    private String chatModelKey;

    // ---------------- 新建数据集的切分/召回参数默认值 ----------------
    // 与模型默认值同一套回落规则：数据集行自身的值优先，为空才用这里的默认。

    /** 新建数据集默认召回数量，默认 4 */
    private Integer defaultRetrievalTopK;

    /** 新建数据集默认分数阈值，默认 0.0000（即不按分数过滤） */
    private java.math.BigDecimal defaultRetrievalScoreThreshold;

    /** 新建数据集默认分段最大长度（字符），默认 1000 */
    private Integer defaultSegmentMaxLength;

    /** 新建数据集默认分段重叠长度（字符），默认 200 */
    private Integer defaultSegmentOverlap;

    /** 新建数据集默认自定义分段分隔符，默认空（仅按递归规则切分） */
    private String defaultSegmentDelimiter;

    /** 新建数据集默认分段模式：general / parent-child / qa，默认 general */
    private String defaultSegmentMode;

    /** 新建数据集默认索引技术：high_quality（入向量库）/ economy（仅关键词单路），默认 high_quality */
    private String defaultIndexingTechnique;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;
}
