package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.resource;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.resource.Resource;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;
import org.ssssssss.magicapi.utils.JsonUtils;

import java.util.Objects;
import java.util.Set;

/**
 * 七类模型资源存储的公共骨架：folder 与 datasource type 同源（Langchain4jModelTypes），
 * JSON 序列化、公共字段校验、key 唯一性、provider 短名表校验。
 *
 * @param <T> 具体资源实体
 */
public abstract class AbstractLangchain4jModelResourceStorage<T extends Langchain4jModelInfo>
        implements MagicResourceStorage<T> {

    private final Langchain4jModelTypes type;
    private final Class<T> magicClass;
    private final Set<String> providers;

    protected MagicResourceService magicResourceService;

    protected AbstractLangchain4jModelResourceStorage(Langchain4jModelTypes type, Class<T> magicClass,
            Set<String> providers) {
        this.type = type;
        this.magicClass = magicClass;
        this.providers = providers;
    }

    @Override
    public String folder() {
        return type.type();
    }

    @Override
    public String suffix() {
        return ".json";
    }

    @Override
    public Class<T> magicClass() {
        return magicClass;
    }

    @Override
    public boolean requirePath() {
        return false;
    }

    @Override
    public boolean requiredScript() {
        return false;
    }

    @Override
    public boolean allowRoot() {
        return true;
    }

    @Override
    public String buildMappingKey(T entity) {
        return String.format("%s-%s", entity.getKey(), entity.getUpdateTime());
    }

    @Override
    public void validate(T entity) {
        validateRequired(entity.getKey(), "key");
        if (!entity.getKey().matches("^[\\w.-]+$")) {
            throw new IllegalArgumentException("key只能包含字母、数字、下划线、点与中划线");
        }
        validateRequired(entity.getProvider(), "provider");
        if (!providers.contains(entity.getProvider())) {
            throw new IllegalArgumentException(String.format(
                "provider '%s' 不受支持，可用 provider: %s", entity.getProvider(), providers));
        }
        validateKeyUnique(entity);
    }

    protected void validateRequired(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + "不能为空");
        }
    }

    private void validateKeyUnique(T entity) {
        if (magicResourceService == null) {
            return;
        }
        boolean noneMatchKey = magicResourceService.listFiles(folder() + ":0").stream()
                .filter(it -> it instanceof Langchain4jModelInfo)
                .map(it -> (Langchain4jModelInfo) it)
                .filter(it -> !Objects.equals(it.getId(), entity.getId()))
                .noneMatch(it -> Objects.equals(it.getKey(), entity.getKey()));
        if (!noneMatchKey) {
            throw new IllegalArgumentException(String.format("key '%s' 已存在，请使用其他 key", entity.getKey()));
        }
    }

    @Override
    public void setMagicResourceService(MagicResourceService magicResourceService) {
        this.magicResourceService = magicResourceService;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T read(byte[] bytes) {
        return JsonUtils.readValue(bytes, magicClass);
    }

    @Override
    public byte[] write(MagicEntity entity) {
        return JsonUtils.toJsonBytes(entity);
    }

    @Override
    public T readResource(Resource resource) {
        return read(resource.read());
    }
}
