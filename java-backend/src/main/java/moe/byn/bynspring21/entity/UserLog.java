package moe.byn.bynspring21.entity;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.Accessors;
import moe.byn.bynspring21.entity.base.CommonEntity;
import moe.byn.bynspring21.entity.base.UserLogBehaviour;

import java.io.Serial;
import java.io.Serializable;

@Data
@TableName("user_log")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserLog extends CommonEntity<UserLog> implements Serializable {

    @Serial
    private static final long serialVersionUID = 5856449830677073929L;

    private String userId;
    private String ip;
    private String userAgent;
    private UserLogBehaviour behaviour;
    private String message;
}
