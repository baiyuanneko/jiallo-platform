package moe.byn.bynspring21.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.Accessors;
import moe.byn.bynspring21.entity.base.CommonEntity;
import moe.byn.bynspring21.entity.base.RoleType;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Schema(description = "用户实体")
@Data
@TableName("user")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User extends CommonEntity<User> implements Serializable {
    @Serial
    private static final long serialVersionUID = -6370176613681860908L;

    @Schema(description = "用户名", example = "john_doe")
    private String username;

    @Schema(description = "用户显示名称", example = "John Doe")
    private String displayName;

    @Schema(description = "密码（已加密，不会在API响应中返回）", hidden = true)
    @JsonIgnore
    private String password;

    @Schema(description = "密码最后更新时间", example = "2025-01-15 10:30:00")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",locale = "zh", timezone = "GMT+8")
    private Date passwordUpdatedAt;

    @Schema(description = "邮箱地址", example = "john@example.com")
    private String email;

    @Schema(description = "邮箱是否已验证", example = "true")
    private Boolean emailVerified;

    @Schema(description = "手机号码", example = "13800138000")
    private String mobile;

    @Schema(description = "手机号码是否已验证", example = "false")
    private Boolean mobileVerified;

    @Schema(description = "用户角色类型", example = "USER")
    private RoleType roleType;

    @Schema(description = "账号是否被封禁", example = "false")
    private Boolean banned;

    @Schema(description = "用户头像对象路径（MinIO object key）", example = "avatars/123456/uuid.jpg")
    private String avatarUrl;

    @Schema(description = "用户偏好模型ID", example = "1234567890")
    private String preferredModelId;

    @Schema(description = "用户偏好模型类型：0为sysModel、1为userModel")
    private Integer preferredModelType;

    @Schema(description = "如果是使用外部SSO登入本系统，在外部SSO中的用户ID", example = "1234567890")
    private String externalUserId;
}
