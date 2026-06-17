package moe.byn.bynspring21.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import moe.byn.bynspring21.entity.AgentTypeGroupAvailability;
import moe.byn.bynspring21.exception.BynBaseException;
import moe.byn.bynspring21.mapper.AgentTypeGroupAvailabilityMapper;
import moe.byn.bynspring21.service.AgentTypeGroupAvailabilityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AgentTypeGroupAvailabilityServiceImpl extends ServiceImpl<AgentTypeGroupAvailabilityMapper, AgentTypeGroupAvailability> implements AgentTypeGroupAvailabilityService {

    @Override
    public List<String> getAgentTypeAvailableGroups(Integer agentType) {
        return this.list(new LambdaQueryWrapper<AgentTypeGroupAvailability>()
                        .eq(AgentTypeGroupAvailability::getAgentType, agentType))
                .stream()
                .map(AgentTypeGroupAvailability::getGroupId)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void authorizeAgentTypeToGroups(Integer agentType, List<String> groupIds) {
        List<String> distinctGroupIds = groupIds.stream()
                .filter(groupId -> groupId != null && !groupId.isBlank())
                .distinct()
                .toList();
        if (distinctGroupIds.isEmpty()) {
            return;
        }

        Set<String> existingGroupIds = this.list(new LambdaQueryWrapper<AgentTypeGroupAvailability>()
                        .eq(AgentTypeGroupAvailability::getAgentType, agentType)
                        .in(AgentTypeGroupAvailability::getGroupId, distinctGroupIds))
                .stream()
                .map(AgentTypeGroupAvailability::getGroupId)
                .collect(Collectors.toSet());

        List<String> duplicated = distinctGroupIds.stream()
                .filter(existingGroupIds::contains)
                .toList();
        if (!duplicated.isEmpty()) {
            throw new BynBaseException("以下用户组已授权该 agentType：" + duplicated);
        }

        this.saveBatch(distinctGroupIds.stream()
                .map(groupId -> AgentTypeGroupAvailability.builder()
                        .agentType(agentType)
                        .groupId(groupId)
                        .build())
                .toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeAgentTypeOfGroups(Integer agentType, List<String> groupIds) {
        List<String> distinctGroupIds = groupIds.stream()
                .filter(groupId -> groupId != null && !groupId.isBlank())
                .distinct()
                .toList();
        if (distinctGroupIds.isEmpty()) {
            return;
        }

        Set<String> existingGroupIds = this.list(new LambdaQueryWrapper<AgentTypeGroupAvailability>()
                        .eq(AgentTypeGroupAvailability::getAgentType, agentType)
                        .in(AgentTypeGroupAvailability::getGroupId, distinctGroupIds))
                .stream()
                .map(AgentTypeGroupAvailability::getGroupId)
                .collect(Collectors.toSet());

        List<String> missing = distinctGroupIds.stream()
                .filter(groupId -> !existingGroupIds.contains(groupId))
                .toList();
        if (!missing.isEmpty()) {
            throw new BynBaseException("以下用户组尚未授权该 agentType：" + missing);
        }

        this.remove(new LambdaQueryWrapper<AgentTypeGroupAvailability>()
                .eq(AgentTypeGroupAvailability::getAgentType, agentType)
                .in(AgentTypeGroupAvailability::getGroupId, distinctGroupIds));
    }

    @Override
    public List<Integer> getAvailableAgentTypesForGroups(List<String> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return List.of();
        }
        return this.list(new LambdaQueryWrapper<AgentTypeGroupAvailability>()
                        .in(AgentTypeGroupAvailability::getGroupId, groupIds))
                .stream()
                .map(AgentTypeGroupAvailability::getAgentType)
                .distinct()
                .toList();
    }
}
