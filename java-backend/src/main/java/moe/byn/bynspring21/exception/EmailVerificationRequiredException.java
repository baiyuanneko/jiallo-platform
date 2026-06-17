package moe.byn.bynspring21.exception;

import lombok.Getter;

@Getter
public class EmailVerificationRequiredException extends RuntimeException {
    private final String userId;
    private final String email;

    public EmailVerificationRequiredException(String userId, String email) {
        super("Email verification required");
        this.userId = userId;
        this.email = email;
    }
}
