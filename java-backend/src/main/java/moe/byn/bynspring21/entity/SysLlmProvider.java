package moe.byn.bynspring21.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
@TableName("sys_llm_provider")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SysLlmProvider extends CommonEntity<SysLlmProvider> implements Serializable {

    @Serial
    private static final long serialVersionUID = -8612217839443756608L;

    private String providerName;
    private String baseUrl;
    @JsonIgnore
    private String apiKey;
    private String providerIconUrl;
}
