package moe.byn.bynspring21.entity.base;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SsoClientPermissionType {

    UID_ONLY(0),
    ROLETYPE_ONLY(1),
    UID_AND_ROLETYPE(2),
    /**
     * 基本信息包括uid、头像base64、用户名、用户显示名称
     */
    BASIC_INFO(3),
    /**
     * 基本信息 + roleType
     */
    BASIC_INFO_WITH_ROLETYPE(4),
    /**
     * 基本信息 + 邮箱
     */
    BASIC_INFO_WITH_EMAIL(5),
    /**
     * 基本信息 + roleType + 邮箱
     */
    BASIC_INFO_WITH_ROLETYPE_AND_EMAIL(6);

    @EnumValue
    private final int value;

    public static SsoClientPermissionType valueOf(int value) {
        for (SsoClientPermissionType permissionType : values()) {
            if (permissionType.value == value) {
                return permissionType;
            }
        }
        return null;
    }

    @JsonValue
    public String getName() {
        return this.name();
    }
}
