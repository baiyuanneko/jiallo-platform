package moe.byn.bynspring21.service;

import com.baomidou.mybatisplus.extension.service.IService;
import moe.byn.bynspring21.entity.RagDocChunk;
import moe.byn.bynspring21.entity.vo.RagLibrarySearchVo.SearchResultVo;

import java.util.List;

public interface RagDocChunkService extends IService<RagDocChunk> {

    void parseDocContent(String docId, String libraryId, String content);

    void deleteByDocId(String docId);

    List<SearchResultVo> searchChunks(List<String> libraryIds, String query, int limit);
}
