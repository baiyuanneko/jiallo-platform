package moe.byn.bynspring21.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import moe.byn.bynspring21.entity.SysModelGroupAvailability;
import moe.byn.bynspring21.exception.BynBaseException;
import moe.byn.bynspring21.mapper.SysModelGroupAvailabilityMapper;
import moe.byn.bynspring21.service.SysModelGroupAvailabilityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SysModelGroupAvailabilityServiceImpl extends ServiceImpl<SysModelGroupAvailabilityMapper, SysModelGroupAvailability> implements SysModelGroupAvailabilityService {

    @Override
    public List<String> getModelAvailableGroups(String modelId) {
        return this.list(new LambdaQueryWrapper<SysModelGroupAvailability>()
                        .eq(SysModelGroupAvailability::getModelId, modelId))
                .stream()
                .map(SysModelGroupAvailability::getGroupId)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void authorizeModelToGroups(String modelId, List<String> groupIds) {
        List<String> distinctGroupIds = groupIds.stream()
                .filter(groupId -> groupId != null && !groupId.isBlank())
                .distinct()
                .toList();
        if (distinctGroupIds.isEmpty()) {
            return;
        }

        Set<String> existingGroupIds = this.list(new LambdaQueryWrapper<SysModelGroupAvailability>()
                        .eq(SysModelGroupAvailability::getModelId, modelId)
                        .in(SysModelGroupAvailability::getGroupId, distinctGroupIds))
                .stream()
                .map(SysModelGroupAvailability::getGroupId)
                .collect(Collectors.toSet());

        List<String> duplicated = distinctGroupIds.stream()
                .filter(existingGroupIds::contains)
                .toList();
        if (!duplicated.isEmpty()) {
            throw new BynBaseException("以下用户组已授权该模型：" + duplicated);
        }

        this.saveBatch(distinctGroupIds.stream()
                .map(groupId -> SysModelGroupAvailability.builder()
                        .modelId(modelId)
                        .groupId(groupId)
                        .build())
                .toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeModelOfGroups(String modelId, List<String> groupIds) {
        List<String> distinctGroupIds = groupIds.stream()
                .filter(groupId -> groupId != null && !groupId.isBlank())
                .distinct()
                .toList();
        if (distinctGroupIds.isEmpty()) {
            return;
        }

        Set<String> existingGroupIds = this.list(new LambdaQueryWrapper<SysModelGroupAvailability>()
                        .eq(SysModelGroupAvailability::getModelId, modelId)
                        .in(SysModelGroupAvailability::getGroupId, distinctGroupIds))
                .stream()
                .map(SysModelGroupAvailability::getGroupId)
                .collect(Collectors.toSet());

        List<String> missing = distinctGroupIds.stream()
                .filter(groupId -> !existingGroupIds.contains(groupId))
                .toList();
        if (!missing.isEmpty()) {
            throw new BynBaseException("以下用户组尚未授权该模型：" + missing);
        }

        this.remove(new LambdaQueryWrapper<SysModelGroupAvailability>()
                .eq(SysModelGroupAvailability::getModelId, modelId)
                .in(SysModelGroupAvailability::getGroupId, distinctGroupIds));
    }

    @Override
    public List<String> getModelsAvailableToGroups(List<String> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return List.of();
        }
        return this.list(new LambdaQueryWrapper<SysModelGroupAvailability>()
                        .in(SysModelGroupAvailability::getGroupId, groupIds))
                .stream()
                .map(SysModelGroupAvailability::getModelId)
                .distinct()
                .toList();
    }
}
