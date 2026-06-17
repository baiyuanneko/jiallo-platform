package moe.byn.bynspring21.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import moe.byn.bynspring21.entity.ChatsessEnabledTool;
import moe.byn.bynspring21.entity.base.AgenticToolEnum;
import moe.byn.bynspring21.mapper.ChatsessEnabledToolMapper;
import moe.byn.bynspring21.service.ChatsessEnabledToolService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class ChatsessEnabledToolServiceImpl extends ServiceImpl<ChatsessEnabledToolMapper, ChatsessEnabledTool>
        implements ChatsessEnabledToolService {

    @Override
    public void saveEnabledTools(String sessionId, List<AgenticToolEnum> tools, String userId) {
        if (tools == null || tools.isEmpty()) return;

        List<ChatsessEnabledTool> records = tools.stream()
                .map(tool -> ChatsessEnabledTool.builder()
                        .sessionId(sessionId)
                        .toolCode(tool)
                        .build())
                .toList();

        for (ChatsessEnabledTool record : records) {
            record.setCreateUser(userId);
            record.setUpdateUser(userId);
        }

        saveBatch(records);
    }

    @Override
    public void updateEnabledTools(String sessionId, List<AgenticToolEnum> tools, String userId) {
        // 删除旧的工具记录
        remove(new LambdaQueryWrapper<ChatsessEnabledTool>()
                .eq(ChatsessEnabledTool::getSessionId, sessionId));
        // 保存新的工具记录
        saveEnabledTools(sessionId, tools, userId);
    }

    @Override
    public List<AgenticToolEnum> getEnabledTools(String sessionId) {
        List<ChatsessEnabledTool> records = list(
                new LambdaQueryWrapper<ChatsessEnabledTool>()
                        .eq(ChatsessEnabledTool::getSessionId, sessionId)
        );
        if (records.isEmpty()) return Collections.emptyList();

        return records.stream()
                .map(ChatsessEnabledTool::getToolCode)
                .toList();
    }
}
