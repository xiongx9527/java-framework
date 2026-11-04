package com.xiongx9527.fw.plus.ai.base.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 模型目录与校验的公共实现，供各端口 adapter 复用，避免每个厂商各写一份匹配逻辑。
 *
 * <p>匹配规则：先按 {@code id} 精确匹配，再忽略大小写匹配一次（部分厂商返回的 id 大小写不稳定）。</p>
 */
public final class ModelCatalogSupport {

    private ModelCatalogSupport() {
    }

    /** 按能力类型过滤目录；{@code models} 为 {@code null} 时返回空列表。 */
    public static List<ModelInfo> ofType(List<ModelInfo> models, ModelType type) {
        List<ModelInfo> out = new ArrayList<ModelInfo>();
        if (models == null || type == null) {
            return out;
        }
        for (ModelInfo model : models) {
            if (model != null && model.getType() == type) {
                out.add(model);
            }
        }
        return out;
    }

    /** 在目录中查找模型；找不到返回 {@code null}。 */
    public static ModelInfo find(List<ModelInfo> models, String model) {
        if (models == null || model == null) {
            return null;
        }
        for (ModelInfo item : models) {
            if (item != null && model.equals(item.getId())) {
                return item;
            }
        }
        for (ModelInfo item : models) {
            if (item != null && item.getId() != null && model.equalsIgnoreCase(item.getId())) {
                return item;
            }
        }
        return null;
    }

    /**
     * 按目录判定模型是否可用。
     *
     * <p>目录为空表示厂商不支持枚举（或枚举失败），此时无法判定存在性，返回失败并说明原因，
     * 由调用方决定是否改用轻量探测。</p>
     */
    public static ModelValidation validate(ModelType type, List<ModelInfo> models, String model,
                                           long latencyMillis) {
        if (model == null || model.trim().isEmpty()) {
            return ModelValidation.invalid(model, "model id is required", latencyMillis);
        }
        if (find(models, model) != null) {
            return ModelValidation.valid(model, latencyMillis);
        }
        if (models == null || models.isEmpty()) {
            return ModelValidation.invalid(model,
                    "no " + (type == null ? "model" : type.name().toLowerCase())
                            + " model available from provider", latencyMillis);
        }
        return ModelValidation.invalid(model, "model not found in provider catalog", latencyMillis);
    }

    /** 空目录常量，避免 adapter 反复创建。 */
    public static List<ModelInfo> empty() {
        return Collections.emptyList();
    }
}
