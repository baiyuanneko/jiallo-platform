package moe.byn.bynspring21.utils;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.ICaptcha;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import moe.byn.bynspring21.entity.dto.CaptchaDto;
import moe.byn.bynspring21.exception.BynBaseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

@Slf4j
@Component
public class CaptchaUtils {

    @Autowired
    private RedisUtils redisUtils;

    private final LineCaptcha lineCaptcha = CaptchaUtil.createLineCaptcha(200, 100);

    public CaptchaDto getCaptcha(String usage) {
        CaptchaDto captchaDto = new CaptchaDto();
        captchaDto.setCaptchaId(SecureRandomUtils.randomString(16));

        lineCaptcha.createCode();

        captchaDto.setCaptchaBase64(lineCaptcha.getImageBase64Data());
        captchaDto.setCorrectCode(lineCaptcha.getCode());

        CaptchaDto captchaDto2 = BeanUtil.copyProperties(captchaDto, CaptchaDto.class);
        captchaDto2.setCaptchaBase64(null);

        String redisData = JSON.toJSONString(captchaDto2);

        redisUtils.set("captcha:" + usage + ":" +  captchaDto.getCaptchaId(), redisData, 300);

        return captchaDto;
    }

    public void verifyCaptcha(String captchaId, String usage, String code) {
        String captchaDtoStr = (String) redisUtils.get("captcha:" + usage + ":" + captchaId);
        CaptchaDto captchaDto = JSON.parseObject(captchaDtoStr, CaptchaDto.class);

        if (!captchaDto.getCorrectCode().equalsIgnoreCase(code)) {
            throw new BynBaseException("INCORRECT_CAPTCHA");
        } else {
            redisUtils.del("captcha:" + usage + ":" + captchaDto.getCaptchaId());
        }
    }

}
