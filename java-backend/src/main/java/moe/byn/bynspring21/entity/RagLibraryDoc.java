package moe.byn.bynspring21.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.Accessors;
import moe.byn.bynspring21.entity.base.CommonEntity;

import java.io.Serial;
import java.io.Serializable;

@Data
@TableName("rag_library_doc")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RagLibraryDoc extends CommonEntity<RagLibraryDoc> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String ragLibraryId;

    private String fileName;

    private Long fileSize;

    @Builder.Default
    private Long chunkNum = 0L;

    private Boolean parsed;

}
