package com.xiongx9527.fw.plus.ai.support.retrieval;

import com.xiongx9527.fw.plus.ai.base.model.RerankPort;
import com.xiongx9527.fw.plus.ai.base.retrieval.DefaultReranker;

/**
 * 基于 RerankPort 的重排器（保留 support 侧既有入口）。
 *
 * <p>实现已下沉到 base 的 {@link DefaultReranker}，这里只保留类名与构造，
 * 避免两处重复维护同一套打分回填逻辑。</p>
 */
public class ModelReranker extends DefaultReranker {

    public ModelReranker(RerankPort port) {
        super(port);
    }
}
