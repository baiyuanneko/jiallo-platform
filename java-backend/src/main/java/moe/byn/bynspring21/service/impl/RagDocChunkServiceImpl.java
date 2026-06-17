package moe.byn.bynspring21.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import moe.byn.bynspring21.entity.RagDocChunk;
import moe.byn.bynspring21.entity.RagLibrary;
import moe.byn.bynspring21.exception.BynBaseException;
import moe.byn.bynspring21.mapper.RagDocChunkMapper;
import moe.byn.bynspring21.security.SecurityUtil;
import moe.byn.bynspring21.service.RagDocChunkService;
import moe.byn.bynspring21.service.RagLibraryDocService;
import moe.byn.bynspring21.service.RagLibraryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import moe.byn.bynspring21.entity.vo.RagLibrarySearchVo.SearchResultVo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RagDocChunkServiceImpl extends ServiceImpl<RagDocChunkMapper, RagDocChunk> implements RagDocChunkService {

    private static final int CHUNK_SIZE = 1280;

    @Autowired
    @Lazy
    private RagLibraryDocService ragLibraryDocService;

    @Autowired
    @Lazy
    private RagLibraryService ragLibraryService;

    @Autowired
    private SecurityUtil securityUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void parseDocContent(String docId, String libraryId, String content) {
        deleteByDocId(docId);

        if (content == null || content.isBlank()) {
            throw new BynBaseException("文档内容为空，无法解析");
        }

        List<RagDocChunk> chunks = new ArrayList<>();
        int length = content.length();
        int index = 0;

        for (int start = 0; start < length; start += CHUNK_SIZE) {
            int end = Math.min(start + CHUNK_SIZE, length);
            RagDocChunk chunk = RagDocChunk.builder()
                    .docId(docId)
                    .libraryId(libraryId)
                    .chunkIndex(index++)
                    .chunkContent(content.substring(start, end))
                    .build();
            chunk.setCreateUser(securityUtil.getUserId());
            chunks.add(chunk);
        }

        this.saveBatch(chunks);

        ragLibraryDocService.update()
                .set("parsed", true)
                .set("chunk_num", chunks.size())
                .eq("id", docId)
                .update();
    }

    @Override
    public List<SearchResultVo> searchChunks(List<String> libraryIds, String query, int limit) {
        if (libraryIds == null || libraryIds.isEmpty()) {
            return List.of();
        }
        // Verify all libraries belong to current user
        String userId = securityUtil.getUserId();
        for (String libId : libraryIds) {
            RagLibrary lib = ragLibraryService.getById(libId);
            if (lib == null || !lib.getCreateUser().equals(userId)) {
                throw new BynBaseException("知识库不存在或无权访问");
            }
        }
        // Build library name map
        Map<String, String> libNameMap = ragLibraryService.listByIds(libraryIds).stream()
                .collect(Collectors.toMap(RagLibrary::getId, RagLibrary::getName));

        List<Map<String, Object>> rawResults = baseMapper.searchChunks(libraryIds, query, limit);

        return rawResults.stream().map(row -> {
            SearchResultVo vo = new SearchResultVo();
            vo.setChunkId((String) row.get("chunkId"));
            vo.setDocId((String) row.get("docId"));
            vo.setLibraryId((String) row.get("libraryId"));
            String libId = (String) row.get("libraryId");
            vo.setLibraryName(libNameMap.getOrDefault(libId, ""));
            vo.setFileName((String) row.get("fileName"));
            vo.setChunkIndex(row.get("chunkIndex") != null ? ((Number) row.get("chunkIndex")).intValue() : null);
            vo.setChunkContent((String) row.get("chunkContent"));
            vo.setRelevance(row.get("relevance") != null ? ((Number) row.get("relevance")).doubleValue() : 0.0);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public void deleteByDocId(String docId) {
        baseMapper.deleteByDocId(docId);
    }
}
