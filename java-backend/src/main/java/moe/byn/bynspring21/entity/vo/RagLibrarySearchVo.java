package moe.byn.bynspring21.entity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class RagLibrarySearchVo {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "检索知识库请求参数")
    public static class SearchVo {
        @Schema(description = "知识库ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "请选择至少一个知识库")
        private List<String> libraryIds;

        @Schema(description = "查询文本", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "查询内容不能为空")
        private String query;

        @Schema(description = "返回条数", example = "10")
        @Min(value = 5, message = "返回条数不能少于 5")
        @Max(value = 20, message = "返回条数不能超过 20")
        @Builder.Default
        private Integer limit = 10;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "检索结果项")
    public static class SearchResultVo {
        @Schema(description = "分块ID")
        private String chunkId;

        @Schema(description = "文档ID")
        private String docId;

        @Schema(description = "知识库ID")
        private String libraryId;

        @Schema(description = "知识库名称")
        private String libraryName;

        @Schema(description = "文档名称")
        private String fileName;

        @Schema(description = "分块序号")
        private Integer chunkIndex;

        @Schema(description = "分块内容")
        private String chunkContent;

        @Schema(description = "相关度分数")
        private Double relevance;
    }
}
