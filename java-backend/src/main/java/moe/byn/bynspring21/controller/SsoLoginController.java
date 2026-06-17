package moe.byn.bynspring21.controller;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import moe.byn.bynspring21.entity.SsoClient;
import moe.byn.bynspring21.entity.User;
import moe.byn.bynspring21.entity.base.R;
import moe.byn.bynspring21.entity.vo.SsoLoginVo;
import moe.byn.bynspring21.exception.BynBaseException;
import moe.byn.bynspring21.security.SecurityUtil;
import moe.byn.bynspring21.service.SsoClientService;
import moe.byn.bynspring21.service.SsoLoginService;
import moe.byn.bynspring21.utils.RedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;

@RestController
@RequestMapping("/sso")
public class SsoLoginController {

    @Autowired
    private SsoClientService ssoClientService;
    @Autowired
    private SecurityUtil securityUtil;
    @Autowired
    private SsoLoginService ssoLoginService;
    @Autowired
    private RedisUtils redisUtils;
    @Value("${myapp.clientMode.isSsoClient:false}")
    private Boolean isSsoClient;

    @PostMapping("/getSsoClientInfo")
    public R<SsoClient> getSsoClientInfo(@Validated @RequestBody SsoLoginVo.GetSsoClientInfoVo vo) {
        if (isSsoClient) {
            return R.error("SSO 客户端不允许执行此操作");
        }
        SsoClient ssoClient = ssoClientService.getOne(new LambdaQueryWrapper<SsoClient>()
                .eq(SsoClient::getClientUniqueName, vo.getClientUniqueName()));

        if (ssoClient == null) {
            return R.error("应用不存在");
        }

        ssoClient.setClientSecret("<filtered>");

        return R.ok(ssoClient);
    }

    @PostMapping("/confirmLogin")
    public R<String> confirmLogin(@Validated @RequestBody SsoLoginVo.ConfirmLoginVo vo) {
        if (isSsoClient) {
            return R.error("SSO 客户端不允许执行此操作");
        }
        return R.ok(ssoLoginService.confirmLogin(vo));
    }

    @PostMapping("/clientGetUser")
    public R<JSONObject> clientGetUser(@Validated @RequestBody SsoLoginVo.ClientGetUserVo vo) {
        if (isSsoClient) {
            return R.error("SSO 客户端不允许执行此操作");
        }
        SsoClient ssoClient = ssoClientService.getOne(new LambdaQueryWrapper<SsoClient>()
                .eq(SsoClient::getClientUniqueName, vo.getClientUniqueName()));

        if (ssoClient == null) {
            return R.error("应用不存在");
        }

        if (!ssoClient.getClientSecret().equals(vo.getClientSecret())) {
            return R.error("Client Secret 不正确！");
        }

       String userInfoJsonStr = (String) redisUtils.get(vo.getClientUniqueName() + ":" + vo.getToken());

        if (userInfoJsonStr == null) {
            return R.error("Token 已过期！");
        }

       JSONObject userInfoJson = JSONObject.parseObject(userInfoJsonStr);
       return R.ok(userInfoJson);
    }

    @Operation(summary = "获取SSO客户端图标", description = "根据客户端唯一名称获取图标图片（公开访问）")
    @GetMapping("/icon/{clientUniqueName}")
    public ResponseEntity<byte[]> getSsoClientIcon(
            @Parameter(description = "客户端唯一名称", required = true)
            @PathVariable String clientUniqueName) {
        try {
            // 从Service获取图标流
            InputStream inputStream = ssoClientService.getClientIconStream(clientUniqueName);
            byte[] imageBytes = StreamUtils.copyToByteArray(inputStream);
            inputStream.close();

            // 获取Content-Type
            String contentType = ssoClientService.getClientIconContentType(clientUniqueName);

            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentLength(imageBytes.length);
            headers.setCacheControl("max-age=604800"); // 缓存7天

            return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (BynBaseException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
