package moe.byn.bynspring21.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import moe.byn.bynspring21.entity.base.CommonEntity;

import java.io.Serial;
import java.io.Serializable;

@Data
@TableName("sys_llm_model")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SysLlmModel extends CommonEntity<SysLlmModel> implements Serializable {

    @Serial
    private static final long serialVersionUID = 3524893430559455761L;

    private String providerId;
    private String modelName;
    private String realModelName;
    private String modelDisplayName;
    private String modelIconUrl;
    private Boolean isVerifiedModel;

    @TableField(exist = false)
    private String providerName;
}
