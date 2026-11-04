package com.xiongx9527.fw.plus.ai.interactive.confirm;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 确认级别判定（对应 §6.33.2 四级确认）。
 *
 * <p>级别由"动作类型 × 影响行数 × 是否可逆"规则判定，不由模型判断。默认规则：</p>
 * <ul>
 *   <li>{@code DUAL}：不可逆且高危（删数据源 / 改权限 / 批量删除）；</li>
 *   <li>{@code REPEAT}：写操作（提交预约 / 删单条 / 批量修改 / 提交）；</li>
 *   <li>{@code LIGHT}：高成本只读（导出 / 跨空间只读）；</li>
 *   <li>{@code NONE}：其余只读、小结果集。</li>
 * </ul>
 */
public final class ConfirmGate {

    private static final Set<String> DUAL_ACTIONS = new HashSet<String>(Arrays.asList(
            "DELETE_DATASOURCE", "MODIFY_PERMISSION", "BATCH_DELETE"));

    private static final Set<String> REPEAT_ACTIONS = new HashSet<String>(Arrays.asList(
            "CREATE_APPOINTMENT", "DELETE_RECORD", "BATCH_MODIFY", "COMMIT"));

    private static final Set<String> LIGHT_ACTIONS = new HashSet<String>(Arrays.asList(
            "EXPORT", "CROSS_SPACE_READ"));

    private ConfirmGate() {
    }

    public static ConfirmLevel decide(String actionType, long impactRows, boolean reversible) {
        if (actionType == null) {
            return reversible ? ConfirmLevel.NONE : ConfirmLevel.REPEAT;
        }
        if (DUAL_ACTIONS.contains(actionType)) {
            return ConfirmLevel.DUAL;
        }
        if (REPEAT_ACTIONS.contains(actionType)) {
            return ConfirmLevel.REPEAT;
        }
        if (LIGHT_ACTIONS.contains(actionType)) {
            return ConfirmLevel.LIGHT;
        }
        // 未枚举：写且不可逆按复述确认，其余按无确认
        return reversible ? ConfirmLevel.NONE : ConfirmLevel.REPEAT;
    }
}
