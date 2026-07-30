package moe.byn.bynspring21.entity.base;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Claw-like 应用（agentType=3）安全级别，数值越大权限越高
 */
@Getter
@AllArgsConstructor
public enum ClawSecurityLevel {
    NONE(0, "无权限"),
    WORKSPACE(1, "工作区文件"),
    FULL(2, "完全访问");

    private final int code;
    private final String desc;

    public static ClawSecurityLevel fromCode(int code) {
        for (ClawSecurityLevel level : values()) {
            if (level.code == code) {
                return level;
            }
        }
        return null;
    }
}
