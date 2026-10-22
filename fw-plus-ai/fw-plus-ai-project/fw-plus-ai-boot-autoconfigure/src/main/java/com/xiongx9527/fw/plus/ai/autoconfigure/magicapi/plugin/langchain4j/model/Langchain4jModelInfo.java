package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model;

import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ssssssss.magicapi.core.model.MagicEntity;

import java.util.Map;

/**
 * LangChain4j 模型资源公共字段：七类 datasource 共享的登记信息。
 * 连接/生成参数不在此列，统一放 options（各 provider 内容不一致）。
 *
 * <p>展示名直接复用父类 {@link MagicEntity#name}，<b>切勿在此重新声明 {@code name} 字段</b>：
 * Lombok {@code @Data} 会生成覆盖父类的 getName/setName，导致 JSON 反序列化写入子类字段
 * 而父类字段恒为 null，{@link MagicEntity#simple()} 拷贝父类字段后节点 name 为 null，
 * 控制台资源树的搜索过滤（{@code S.name.indexOf(...)}）会因此抛错、面板不再渲染。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class Langchain4jModelInfo extends MagicEntity {
    /** 唯一 key，脚本按 key 取实例；仅允许 [\w.-] */
    private String key;
    /** provider 短名，须命中该类资源短名表 */
    private String provider;
    /** false：销毁实例且不注册（下线而非删除） */
    private boolean enabled = true;
    /** 差异参数对象：不同 provider 的键集合不同 */
    private Map<String, Object> options;

    /**
     * 资源树列表节点：在父类公共字段（id/name/groupId/时间/lock）之上补齐
     * 控制台列表与搜索过滤所需的 key/provider/enabled/options。
     *
     * <p>本类为抽象类，借助具体子类的无参构造创建实例（各子类均为 {@code @Data} POJO）。</p>
     */
    @Override
    public MagicEntity simple() {
        Langchain4jModelInfo info;
        try {
            info = getClass().getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(I18nMessageResolver.resolveKey(
                    "fw.ai.Langchain4jModelInfo.simpleInstanceFailed", getClass().getName()), e);
        }
        super.simple(info);
        info.setKey(this.key);
        info.setProvider(this.provider);
        info.setEnabled(this.enabled);
        info.setOptions(this.options);
        return info;
    }
}
