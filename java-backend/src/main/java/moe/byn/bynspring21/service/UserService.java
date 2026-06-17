package moe.byn.bynspring21.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import moe.byn.bynspring21.entity.User;
import moe.byn.bynspring21.entity.UserLog;
import moe.byn.bynspring21.entity.dto.CaptchaDto;
import moe.byn.bynspring21.entity.dto.UserRegisterResult;
import moe.byn.bynspring21.entity.vo.UserVo;
import moe.byn.bynspring21.security.entity.TokenResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface UserService extends IService<User> {

    UserRegisterResult registerUser(UserVo.RegisterUserVo vo);

    CaptchaDto getRegisterCaptcha();

    CaptchaDto getLoginCaptcha();

    TokenResponse verifyEmailCode(UserVo.VerifyEmailCodeVo vo);

    void resendEmailCode();

    void changePassword(String newPassword);

    Page<UserLog> viewUserLog(UserVo.ViewUserLogVo vo);

    User editUserInfo(String displayName);

    void updateAvatar(String userId, String avatarUrl);

    UserVo.UpdateAvatarResponse uploadAvatar(MultipartFile file);

    InputStream getMyAvatarStream();

    String getMyAvatarContentType();

    String getMyAvatarBase64();

    void sendEmailCodeAndNotify(String userId, String userEmail);

    void resetPassword(UserVo.ResetPasswordVo vo);

    void bindEmail(UserVo.BindEmailVo vo);

    void bindEmailVerify(UserVo.BindEmailVerifyVo vo);

    void unbindEmail();
}
