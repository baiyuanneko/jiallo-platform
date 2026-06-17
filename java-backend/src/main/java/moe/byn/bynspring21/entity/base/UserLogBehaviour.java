package moe.byn.bynspring21.entity.base;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserLogBehaviour {
    SUCCESSFUL_LOGIN(0),
    FAILED_LOGIN(1),
    PASSWORD_CHANGE(2),
    SUCCESSFUL_REGISTER(3),
    EMAIL_CODE_INVALID(4),
    SEND_EMAIL_CODE(5),
    SSO_CLIENT_LOGIN(6),;

    @EnumValue
    private final int value;

    public static UserLogBehaviour valueOf(int value) {
        for (UserLogBehaviour behaviour : UserLogBehaviour.values()) {
            if (behaviour.value == value) {
                return behaviour;
            }
        }
        return null;
    }
}
