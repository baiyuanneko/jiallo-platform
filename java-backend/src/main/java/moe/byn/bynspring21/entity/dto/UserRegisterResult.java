package moe.byn.bynspring21.entity.dto;

import lombok.Builder;
import lombok.Data;
import moe.byn.bynspring21.entity.User;
import moe.byn.bynspring21.security.entity.TokenResponse;

@Data
@Builder
public class UserRegisterResult {

    private String registerResult;

    private User user;

    private TokenResponse tokenResponse;
}
