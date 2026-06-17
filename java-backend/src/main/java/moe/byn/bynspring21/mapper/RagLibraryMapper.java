package moe.byn.bynspring21.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import moe.byn.bynspring21.entity.RagLibrary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RagLibraryMapper extends BaseMapper<RagLibrary> {

    @Select("SELECT COALESCE(SUM(total_file_size), 0) FROM rag_library WHERE create_user = #{userId} AND is_del = 0")
    long sumFileSizeByUser(@Param("userId") String userId);
}
