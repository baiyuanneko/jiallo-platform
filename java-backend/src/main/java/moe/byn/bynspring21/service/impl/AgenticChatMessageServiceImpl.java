package moe.byn.bynspring21.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import moe.byn.bynspring21.entity.AgenticChatMessage;
import moe.byn.bynspring21.mapper.AgenticChatMessageMapper;
import moe.byn.bynspring21.service.AgenticChatMessageService;
import org.springframework.stereotype.Service;

@Service
public class AgenticChatMessageServiceImpl extends ServiceImpl<AgenticChatMessageMapper, AgenticChatMessage> implements AgenticChatMessageService {
}
