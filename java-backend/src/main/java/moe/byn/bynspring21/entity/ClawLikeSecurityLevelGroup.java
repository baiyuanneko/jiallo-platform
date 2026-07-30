package moe.byn.bynspring21.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.Accessors;
import moe.byn.bynspring21.entity.base.CommonEntity;

import java.io.Serial;
import java.io.Serializable;

@Data
@TableName("claw_like_security_level_group")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClawLikeSecurityLevelGroup extends CommonEntity<ClawLikeSecurityLevelGroup> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String groupId;
    private Integer securityLevel;
}
