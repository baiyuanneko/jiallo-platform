package moe.byn.bynspring21.entity.base;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Claw-like 应用（agentType=3）工具调用审批模式
 */
@Getter
@AllArgsConstructor
public enum ClawApprovalMode {
    AUTO(0, "自动审批"),
    BYPASS(1, "绕过审批"),
    MANUAL(2, "人工审批");

    private final int code;
    private final String desc;

    public static ClawApprovalMode fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ClawApprovalMode mode : values()) {
            if (mode.code == code) {
                return mode;
            }
        }
        return null;
    }
}
