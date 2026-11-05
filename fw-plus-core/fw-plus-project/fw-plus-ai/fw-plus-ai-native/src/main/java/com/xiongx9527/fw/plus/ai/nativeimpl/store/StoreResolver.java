package com.xiongx9527.fw.plus.ai.nativeimpl.store;

import com.xiongx9527.fw.plus.ai.base.store.Store;
import com.xiongx9527.fw.plus.ai.base.store.StoreType;
import com.xiongx9527.fw.plus.ai.nativeimpl.store.graph.GraphStoreConfig;
import com.xiongx9527.fw.plus.ai.nativeimpl.store.graph.GraphStoreResolver;
import com.xiongx9527.fw.plus.ai.nativeimpl.store.graph.GraphStoreVendor;
import com.xiongx9527.fw.plus.ai.nativeimpl.store.graph.GraphStoreProtocol;
import com.xiongx9527.fw.plus.ai.nativeimpl.store.vector.VectorStoreConfig;
import com.xiongx9527.fw.plus.ai.nativeimpl.store.vector.VectorStoreResolver;
import com.xiongx9527.fw.plus.ai.nativeimpl.store.vector.VectorStoreVendor;
import com.xiongx9527.fw.plus.ai.nativeimpl.store.vector.VectorStoreProtocol;

/**
 * 统一存储门面：在向量库 / 图库各自的 resolver 之外再包一层，调用方只面对一个 {@link StoreConfig} 入口。
 *
 * <p>按 {@link StoreConfig#getStoreType()} 派发：VECTOR 转 {@link VectorStoreConfig} 交给 {@link VectorStoreResolver}，
 * GRAPH 转 {@link GraphStoreConfig} 交给 {@link GraphStoreResolver}。厂商 / 协议字符串按对应枚举名匹配，
 * 无法识别时传 null，由下层 resolver 按既有规则（协议优先、厂商回退、二者皆无报错）处理。</p>
 */
public final class StoreResolver {
    private StoreResolver() { }

    public static Store create(StoreConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("store config is required");
        }
        if (config.getStoreType() == null) {
            throw new IllegalArgumentException("store type is required");
        }
        switch (config.getStoreType()) {
            case VECTOR:
                return VectorStoreResolver.create(toVectorConfig(config));
            case GRAPH:
                return GraphStoreResolver.create(toGraphConfig(config));
            default:
                throw new IllegalArgumentException("unsupported store type: " + config.getStoreType());
        }
    }

    private static VectorStoreConfig toVectorConfig(StoreConfig config) {
        return VectorStoreConfig.builder()
                .vendor(parse(VectorStoreVendor.class, config.getVendor()))
                .protocol(parse(VectorStoreProtocol.class, config.getProtocol()))
                .common(config)
                .build();
    }

    private static GraphStoreConfig toGraphConfig(StoreConfig config) {
        return GraphStoreConfig.builder()
                .vendor(parse(GraphStoreVendor.class, config.getVendor()))
                .protocol(parse(GraphStoreProtocol.class, config.getProtocol()))
                .common(config)
                .build();
    }

    private static <E extends Enum<E>> E parse(Class<E> enumClass, String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Enum.valueOf(enumClass, value.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
