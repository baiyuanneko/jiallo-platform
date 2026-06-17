package moe.byn.bynspring21.service;

import moe.byn.bynspring21.entity.vo.SsoLoginVo;

public interface SsoLoginService {
    String confirmLogin(SsoLoginVo.ConfirmLoginVo vo);
}
