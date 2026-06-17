package moe.byn.bynspring21.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import moe.byn.bynspring21.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
