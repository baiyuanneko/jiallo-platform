package moe.byn.bynspring21.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.experimental.Accessors;
import moe.byn.bynspring21.entity.base.CommonEntity;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("user_session")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserSession extends CommonEntity<UserSession> implements Serializable {


    @Serial
    private static final long serialVersionUID = -3956174124062138078L;

    private String userId;
    @JsonIgnore
    private String jwtAccessToken;
    @JsonIgnore
    private String jwtRefreshToken;
    private String ip;
    private String userAgent;

    @TableField(exist = false)
    private Boolean isCurrentSession = false;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",locale = "zh", timezone = "GMT+8")
    private Date refreshTokenExpireTime;
}
