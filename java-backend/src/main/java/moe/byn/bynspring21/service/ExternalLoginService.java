package moe.byn.bynspring21.service;

import moe.byn.bynspring21.entity.vo.ExternalLoginVo;
import moe.byn.bynspring21.security.entity.TokenResponse;

public interface ExternalLoginService {
    TokenResponse login(ExternalLoginVo.LoginVo vo);
}
