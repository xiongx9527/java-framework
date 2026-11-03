package com.xiongx9527.fw.plus.ai.base.tool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 按技能白名单收窄工具可见性的包装器：只允许白名单内的工具名通过。
 *
 * <p>技能装载后激活的那组工具 = 白名单 ∩ 候选工具；白名单外的工具对模型不可见、也调不到，
 * 这是 skill 最小权限的落地点。</p>
 */
public final class SkillGatedToolProvider implements ToolProvider {

    private final ToolProvider delegate;

    private final Set<String> allowed;

    public SkillGatedToolProvider(ToolProvider delegate, Set<String> allowed) {
        this.delegate = delegate;
        this.allowed = allowed == null ? Collections.<String>emptySet()
                : Collections.unmodifiableSet(new HashSet<String>(allowed));
    }

    @Override
    public List<ToolSpecification> specifications() {
        List<ToolSpecification> all = delegate.specifications();
        if (all == null || allowed.isEmpty()) {
            return all;
        }
        List<ToolSpecification> filtered = new ArrayList<ToolSpecification>();
        for (ToolSpecification spec : all) {
            if (allowed.contains(spec.getName())) {
                filtered.add(spec);
            }
        }
        return filtered;
    }

    @Override
    public String execute(ToolExecutionRequest request) {
        if (request != null && !allowed.isEmpty() && !allowed.contains(request.getName())) {
            throw new ToolExecutionException("tool not allowed by skill whitelist: " + request.getName());
        }
        return delegate.execute(request);
    }
}
