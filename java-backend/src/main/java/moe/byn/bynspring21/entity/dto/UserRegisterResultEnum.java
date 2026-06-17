package moe.byn.bynspring21.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserRegisterResultEnum {
    SUCCESS(0, "success"),
    REQUIRE_EMAIL_VERIFICATION(1, "require_email_verification");

    private final Integer code;
    private final String registerResult;

    public static UserRegisterResultEnum valueOf(Integer code) {
        for (UserRegisterResultEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
