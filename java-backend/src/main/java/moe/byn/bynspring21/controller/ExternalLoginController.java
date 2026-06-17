package moe.byn.bynspring21.controller;

import lombok.extern.slf4j.Slf4j;
import moe.byn.bynspring21.entity.base.R;
import moe.byn.bynspring21.entity.vo.ExternalLoginVo;
import moe.byn.bynspring21.security.entity.TokenResponse;
import moe.byn.bynspring21.service.ExternalLoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("externalLogin")
public class ExternalLoginController {

    @Autowired
    private ExternalLoginService externalLoginService;

    @PostMapping("hajimaruyo")
    public R<TokenResponse> login(@Validated @RequestBody ExternalLoginVo.LoginVo vo){
        return R.ok(externalLoginService.login(vo));
    }
}
