package moe.byn.bynspring21.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import moe.byn.bynspring21.entity.FeatureModuleGroupAvailability;
import moe.byn.bynspring21.exception.BynBaseException;
import moe.byn.bynspring21.mapper.FeatureModuleGroupAvailabilityMapper;
import moe.byn.bynspring21.service.FeatureModuleGroupAvailabilityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FeatureModuleGroupAvailabilityServiceImpl extends ServiceImpl<FeatureModuleGroupAvailabilityMapper, FeatureModuleGroupAvailability> implements FeatureModuleGroupAvailabilityService {

    @Override
    public List<String> getModuleAvailableGroups(String moduleCode) {
        return this.list(new LambdaQueryWrapper<FeatureModuleGroupAvailability>()
                        .eq(FeatureModuleGroupAvailability::getModuleCode, moduleCode))
                .stream()
                .map(FeatureModuleGroupAvailability::getGroupId)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void authorizeModuleToGroups(String moduleCode, List<String> groupIds) {
        List<String> distinctGroupIds = groupIds.stream()
                .filter(groupId -> groupId != null && !groupId.isBlank())
                .distinct()
                .toList();
        if (distinctGroupIds.isEmpty()) {
            return;
        }

        Set<String> existingGroupIds = this.list(new LambdaQueryWrapper<FeatureModuleGroupAvailability>()
                        .eq(FeatureModuleGroupAvailability::getModuleCode, moduleCode)
                        .in(FeatureModuleGroupAvailability::getGroupId, distinctGroupIds))
                .stream()
                .map(FeatureModuleGroupAvailability::getGroupId)
                .collect(Collectors.toSet());

        List<String> duplicated = distinctGroupIds.stream()
                .filter(existingGroupIds::contains)
                .toList();
        if (!duplicated.isEmpty()) {
            throw new BynBaseException("以下用户组已授权该功能模块：" + duplicated);
        }

        this.saveBatch(distinctGroupIds.stream()
                .map(groupId -> FeatureModuleGroupAvailability.builder()
                        .moduleCode(moduleCode)
                        .groupId(groupId)
                        .build())
                .toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeModuleOfGroups(String moduleCode, List<String> groupIds) {
        List<String> distinctGroupIds = groupIds.stream()
                .filter(groupId -> groupId != null && !groupId.isBlank())
                .distinct()
                .toList();
        if (distinctGroupIds.isEmpty()) {
            return;
        }

        Set<String> existingGroupIds = this.list(new LambdaQueryWrapper<FeatureModuleGroupAvailability>()
                        .eq(FeatureModuleGroupAvailability::getModuleCode, moduleCode)
                        .in(FeatureModuleGroupAvailability::getGroupId, distinctGroupIds))
                .stream()
                .map(FeatureModuleGroupAvailability::getGroupId)
                .collect(Collectors.toSet());

        List<String> missing = distinctGroupIds.stream()
                .filter(groupId -> !existingGroupIds.contains(groupId))
                .toList();
        if (!missing.isEmpty()) {
            throw new BynBaseException("以下用户组尚未授权该功能模块：" + missing);
        }

        this.remove(new LambdaQueryWrapper<FeatureModuleGroupAvailability>()
                .eq(FeatureModuleGroupAvailability::getModuleCode, moduleCode)
                .in(FeatureModuleGroupAvailability::getGroupId, distinctGroupIds));
    }

    @Override
    public boolean isModuleAvailableToGroups(String moduleCode, List<String> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return false;
        }
        return this.count(new LambdaQueryWrapper<FeatureModuleGroupAvailability>()
                .eq(FeatureModuleGroupAvailability::getModuleCode, moduleCode)
                .in(FeatureModuleGroupAvailability::getGroupId, groupIds)) > 0;
    }
}
