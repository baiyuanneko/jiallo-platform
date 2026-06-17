package moe.byn.bynspring21.entity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class RagLibraryVo {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "分页查询知识库请求参数")
    public static class PageRagLibraryVo {
        @Schema(description = "页码", example = "1")
        @Builder.Default
        private Integer pageNum = 1;

        @Schema(description = "每页大小", example = "10")
        @Builder.Default
        private Integer pageSize = 10;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "新增知识库请求参数")
    public static class AddRagLibraryVo {
        @Schema(description = "知识库名称", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "知识库名称不能为空")
        private String name;

        @Schema(description = "知识库描述")
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "修改知识库请求参数")
    public static class UpdateRagLibraryVo {
        @Schema(description = "知识库ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "知识库ID不能为空")
        private String id;

        @Schema(description = "知识库名称")
        private String name;

        @Schema(description = "知识库描述")
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "删除知识库请求参数")
    public static class DeleteVo {
        @Schema(description = "知识库ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "知识库ID不能为空")
        private String id;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "分页查询知识库文档请求参数")
    public static class PageRagLibraryDocVo {
        @Schema(description = "知识库ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "知识库ID不能为空")
        private String libraryId;

        @Schema(description = "文件名关键词搜索")
        private String keyword;

        @Schema(description = "页码", example = "1")
        @Builder.Default
        private Integer pageNum = 1;

        @Schema(description = "每页大小", example = "10")
        @Builder.Default
        private Integer pageSize = 10;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "新增文档请求参数")
    public static class AddRagLibraryDocVo {
        @Schema(description = "知识库ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "知识库ID不能为空")
        private String libraryId;

        @Schema(description = "文件名称", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "文件名称不能为空")
        private String fileName;

        @Schema(description = "文件内容", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "文件内容不能为空")
        private String fileContent;

    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "删除文档请求参数")
    public static class DeleteDocVo {
        @Schema(description = "文档ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "文档ID不能为空")
        private String id;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "重命名文档请求参数")
    public static class RenameDocVo {
        @Schema(description = "文档ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "文档ID不能为空")
        private String id;

        @Schema(description = "新文件名", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "文件名不能为空")
        private String fileName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "文档详情请求参数")
    public static class DocDetailVo {
        @Schema(description = "文档ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "文档ID不能为空")
        private String id;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "文档预览结果")
    public static class DocPreviewVo {
        @Schema(description = "文档ID")
        private String id;

        @Schema(description = "文件名")
        private String fileName;

        @Schema(description = "文件大小（字节）")
        private Long fileSize;

        @Schema(description = "文档完整内容（从分块拼接）")
        private String content;
    }
}
