package moe.byn.bynspring21.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import moe.byn.bynspring21.entity.FeatureModuleAvailability;
import moe.byn.bynspring21.entity.base.RoleType;
import moe.byn.bynspring21.exception.BynBaseException;
import moe.byn.bynspring21.mapper.FeatureModuleAvailabilityMapper;
import moe.byn.bynspring21.service.FeatureModuleAvailabilityService;
import moe.byn.bynspring21.service.FeatureModuleGroupAvailabilityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FeatureModuleAvailabilityServiceImpl extends ServiceImpl<FeatureModuleAvailabilityMapper, FeatureModuleAvailability> implements FeatureModuleAvailabilityService {

    @Autowired
    private FeatureModuleGroupAvailabilityService featureModuleGroupAvailabilityService;

    @Override
    public List<RoleType> getModuleAvailableRoles(String moduleCode) {
        return this.list(new LambdaQueryWrapper<FeatureModuleAvailability>()
                        .eq(FeatureModuleAvailability::getModuleCode, moduleCode))
                .stream()
                .map(FeatureModuleAvailability::getRoleCode)
                .map(RoleType::valueOf)
                .filter(roleType -> roleType != null)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void authorizeModuleToRoles(String moduleCode, List<RoleType> roleTypes) {
        List<RoleType> distinctRoleTypes = roleTypes.stream()
                .filter(roleType -> roleType != null)
                .distinct()
                .toList();
        if (distinctRoleTypes.isEmpty()) {
            return;
        }

        List<Integer> roleCodes = distinctRoleTypes.stream()
                .map(RoleType::getValue)
                .toList();
        Set<Integer> existingRoleCodes = this.list(new LambdaQueryWrapper<FeatureModuleAvailability>()
                        .eq(FeatureModuleAvailability::getModuleCode, moduleCode)
                        .in(FeatureModuleAvailability::getRoleCode, roleCodes))
                .stream()
                .map(FeatureModuleAvailability::getRoleCode)
                .collect(Collectors.toSet());

        List<RoleType> duplicatedRoles = distinctRoleTypes.stream()
                .filter(roleType -> existingRoleCodes.contains(roleType.getValue()))
                .toList();
        if (!duplicatedRoles.isEmpty()) {
            throw new BynBaseException("以下角色已经授权该功能模块：" + duplicatedRoles.stream().map(RoleType::getName).toList());
        }

        this.saveBatch(distinctRoleTypes.stream()
                .map(roleType -> FeatureModuleAvailability.builder()
                        .moduleCode(moduleCode)
                        .roleCode(roleType.getValue())
                        .build())
                .toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeModuleOfRoles(String moduleCode, List<RoleType> roleTypes) {
        List<RoleType> distinctRoleTypes = roleTypes.stream()
                .filter(roleType -> roleType != null)
                .distinct()
                .toList();
        if (distinctRoleTypes.isEmpty()) {
            return;
        }

        List<Integer> roleCodes = distinctRoleTypes.stream()
                .map(RoleType::getValue)
                .toList();
        Set<Integer> existingRoleCodes = this.list(new LambdaQueryWrapper<FeatureModuleAvailability>()
                        .eq(FeatureModuleAvailability::getModuleCode, moduleCode)
                        .in(FeatureModuleAvailability::getRoleCode, roleCodes))
                .stream()
                .map(FeatureModuleAvailability::getRoleCode)
                .collect(Collectors.toSet());

        List<RoleType> missingRoles = distinctRoleTypes.stream()
                .filter(roleType -> !existingRoleCodes.contains(roleType.getValue()))
                .toList();
        if (!missingRoles.isEmpty()) {
            throw new BynBaseException("以下角色尚未授权该功能模块：" + missingRoles.stream().map(RoleType::getName).toList());
        }

        this.remove(new LambdaQueryWrapper<FeatureModuleAvailability>()
                .eq(FeatureModuleAvailability::getModuleCode, moduleCode)
                .in(FeatureModuleAvailability::getRoleCode, roleCodes));
    }

    @Override
    public boolean isModuleAvailable(String moduleCode, RoleType roleType) {
        return this.count(new LambdaQueryWrapper<FeatureModuleAvailability>()
                .eq(FeatureModuleAvailability::getModuleCode, moduleCode)
                .eq(FeatureModuleAvailability::getRoleCode, roleType.getValue())) > 0;
    }

    @Override
    public boolean isModuleAvailable(String moduleCode, RoleType roleType, List<String> groupIds) {
        // 先检查 role-based，若通过直接返回 true
        if (isModuleAvailable(moduleCode, roleType)) {
            return true;
        }
        // 再检查 group-based
        if (groupIds != null && !groupIds.isEmpty()) {
            return featureModuleGroupAvailabilityService.isModuleAvailableToGroups(moduleCode, groupIds);
        }
        return false;
    }
}
