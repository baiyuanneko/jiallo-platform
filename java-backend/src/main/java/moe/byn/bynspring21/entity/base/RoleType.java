package moe.byn.bynspring21.entity.base;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RoleType {
    USER(0),
    ADMIN(1),
    VIP_USER(2),
    VANILLA(3),
    AWAIT_EMAIL_VERIFICATION(4);

    @EnumValue
    private final int value;
    public static RoleType valueOf(int value) {
        for (RoleType roleType : values()) {
            if (roleType.value == value) {
                return roleType;
            }
        }
        return null;
    }

    @JsonValue
    public String getName() {
        return this.name();
    }
}
