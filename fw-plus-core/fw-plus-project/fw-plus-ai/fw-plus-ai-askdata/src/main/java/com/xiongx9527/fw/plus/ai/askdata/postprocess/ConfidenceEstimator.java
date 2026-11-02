package com.xiongx9527.fw.plus.ai.askdata.postprocess;

/**
 * 置信度估计（§6.36 质量基线）。高置信 = 验证通过且未修复；
 * 修复后通过降级；验证失败走 §6.32 反问。
 */
public class ConfidenceEstimator {

    public double estimate(ValidationResult vr, boolean repaired) {
        if (vr == null || !vr.isOk()) {
            return 0.2d;
        }
        return repaired ? 0.7d : 0.9d;
    }
}
