package moe.byn.bynspring21.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.Accessors;
import moe.byn.bynspring21.entity.base.CommonEntity;

import java.io.Serial;
import java.io.Serializable;

@Data
@TableName("sys_model_group_availability")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SysModelGroupAvailability extends CommonEntity<SysModelGroupAvailability> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String modelId;
    private String groupId;
}
