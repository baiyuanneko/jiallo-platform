package moe.byn.bynspring21.entity.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExternalLoginReturnValue {

    /**
     * 用户唯一标识
     */
    private String uid;

    /**
     * 用户名
     */
    private String username;

    /**
     * 显示名称
     */
    private String displayName;

    /**
     * 角色类型
     */
    private String roleType;

    /**
     * 头像，Base64 编码的图片数据
     */
    private String avatarBase64;

    /**
     * 邮箱地址
     */
    private String email;

    /**
     * 邮箱是否已验证
     */
    private Boolean emailVerified;
}
