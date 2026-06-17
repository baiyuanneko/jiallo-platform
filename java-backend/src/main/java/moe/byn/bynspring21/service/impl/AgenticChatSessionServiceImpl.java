package moe.byn.bynspring21.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import moe.byn.bynspring21.entity.AgenticChatSession;
import moe.byn.bynspring21.mapper.AgenticChatSessionMapper;
import moe.byn.bynspring21.service.AgenticChatSessionService;
import org.springframework.stereotype.Service;

@Service
public class AgenticChatSessionServiceImpl extends ServiceImpl<AgenticChatSessionMapper, AgenticChatSession> implements AgenticChatSessionService {
}
