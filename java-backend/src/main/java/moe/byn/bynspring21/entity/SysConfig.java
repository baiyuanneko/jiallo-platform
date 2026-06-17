package moe.byn.bynspring21.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.Accessors;
import moe.byn.bynspring21.entity.base.CommonEntity;

import java.io.Serial;
import java.io.Serializable;

@Data
@TableName("sys_config")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SysConfig extends CommonEntity<SysConfig> implements Serializable {

    @Serial
    private static final long serialVersionUID = -4131122767522932539L;

    private String configKey;
    private String configValue;
    private String configInstruction;
}
