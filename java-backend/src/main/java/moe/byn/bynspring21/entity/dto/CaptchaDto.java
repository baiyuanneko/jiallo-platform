package moe.byn.bynspring21.entity.dto;

import cn.hutool.captcha.ICaptcha;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CaptchaDto {
    private String captchaId;
    private String captchaBase64;
    @JsonIgnore
    private String correctCode;
}
