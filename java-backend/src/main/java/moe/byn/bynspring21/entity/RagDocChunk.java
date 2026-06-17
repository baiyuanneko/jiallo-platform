package moe.byn.bynspring21.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.Accessors;
import moe.byn.bynspring21.entity.base.CommonEntity;

import java.io.Serial;
import java.io.Serializable;

@Data
@TableName("rag_doc_chunk")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RagDocChunk extends CommonEntity<RagDocChunk> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String docId;

    private String libraryId;

    private Integer chunkIndex;

    private String chunkContent;
}
