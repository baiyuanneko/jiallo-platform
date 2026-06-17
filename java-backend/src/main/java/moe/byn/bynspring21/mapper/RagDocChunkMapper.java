package moe.byn.bynspring21.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import moe.byn.bynspring21.entity.RagDocChunk;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface RagDocChunkMapper extends BaseMapper<RagDocChunk> {

    @Delete("DELETE FROM rag_doc_chunk WHERE doc_id = #{docId}")
    int deleteByDocId(@Param("docId") String docId);

    @Select("<script>"
            + "SELECT c.id as chunk_id, c.doc_id, c.library_id, c.chunk_index,"
            + "       c.chunk_content, d.file_name,"
            + "       MATCH(c.chunk_content) AGAINST(#{query} IN NATURAL LANGUAGE MODE) as relevance"
            + " FROM rag_doc_chunk c"
            + " LEFT JOIN rag_library_doc d ON c.doc_id = d.id"
            + " WHERE c.library_id IN"
            + "   <foreach item='id' collection='libraryIds' open='(' separator=',' close=')'>#{id}</foreach>"
            + "   AND MATCH(c.chunk_content) AGAINST(#{query} IN NATURAL LANGUAGE MODE)"
            + " ORDER BY relevance DESC"
            + " LIMIT #{limit}"
            + "</script>")
    @Results({
            @Result(column = "chunk_id", property = "chunkId"),
            @Result(column = "doc_id", property = "docId"),
            @Result(column = "library_id", property = "libraryId"),
            @Result(column = "chunk_index", property = "chunkIndex"),
            @Result(column = "chunk_content", property = "chunkContent"),
            @Result(column = "file_name", property = "fileName"),
            @Result(column = "relevance", property = "relevance")
    })
    List<java.util.Map<String, Object>> searchChunks(
            @Param("libraryIds") List<String> libraryIds,
            @Param("query") String query,
            @Param("limit") int limit);
}
