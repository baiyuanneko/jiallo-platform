package moe.byn.bynspring21.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Claw-like 文件列表条目")
public class ClawFileItemDto {

    @Schema(description = "文件或目录名")
    private String name;

    @Schema(description = "是否为目录")
    private Boolean isDir;

    @Schema(description = "文件大小（字节），目录为 0")
    private Long size;

    @Schema(description = "最后修改时间（ epoch 毫秒）")
    private Long updateTime;
}
