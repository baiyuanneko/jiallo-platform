package moe.byn.bynspring21.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateChatModelDto {
    private String baseUrl;
    private String apiKey;
    private String modelName;
    /** extra_body 参数，如 StepFun 推理模型需要 {"reasoning_format": "deepseek-style"} */
    private Map<String, Object> extraBody;
}
