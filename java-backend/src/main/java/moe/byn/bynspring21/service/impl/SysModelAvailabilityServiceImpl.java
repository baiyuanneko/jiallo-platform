package moe.byn.bynspring21.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import moe.byn.bynspring21.entity.SysModelAvailability;
import moe.byn.bynspring21.entity.base.RoleType;
import moe.byn.bynspring21.exception.BynBaseException;
import moe.byn.bynspring21.mapper.SysModelAvailabilityMapper;
import moe.byn.bynspring21.service.SysModelAvailabilityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class SysModelAvailabilityServiceImpl extends ServiceImpl<SysModelAvailabilityMapper, SysModelAvailability> implements SysModelAvailabilityService {

    @Override
    public List<RoleType> getModelAvailableRoles(String modelId) {
        return this.list(new LambdaQueryWrapper<SysModelAvailability>()
                        .eq(SysModelAvailability::getModelId, modelId))
                .stream()
                .map(SysModelAvailability::getRoleCode)
                .map(RoleType::valueOf)
                .filter(roleType -> roleType != null)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void authorizeModelToRoles(String modelId, List<RoleType> roleTypes) {
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
        Set<Integer> existingRoleCodes = this.list(new LambdaQueryWrapper<SysModelAvailability>()
                        .eq(SysModelAvailability::getModelId, modelId)
                        .in(SysModelAvailability::getRoleCode, roleCodes))
                .stream()
                .map(SysModelAvailability::getRoleCode)
                .collect(java.util.stream.Collectors.toSet());

        List<RoleType> duplicatedRoles = distinctRoleTypes.stream()
                .filter(roleType -> existingRoleCodes.contains(roleType.getValue()))
                .toList();
        if (!duplicatedRoles.isEmpty()) {
            throw new BynBaseException("以下角色已经授权该模型：" + duplicatedRoles.stream().map(RoleType::getName).toList());
        }

        this.saveBatch(distinctRoleTypes.stream()
                .map(roleType -> SysModelAvailability.builder()
                        .modelId(modelId)
                        .roleCode(roleType.getValue())
                        .build())
                .toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeModelOfRoles(String modelId, List<RoleType> roleTypes) {
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
        Set<Integer> existingRoleCodes = this.list(new LambdaQueryWrapper<SysModelAvailability>()
                        .eq(SysModelAvailability::getModelId, modelId)
                        .in(SysModelAvailability::getRoleCode, roleCodes))
                .stream()
                .map(SysModelAvailability::getRoleCode)
                .collect(java.util.stream.Collectors.toSet());

        List<RoleType> missingRoles = distinctRoleTypes.stream()
                .filter(roleType -> !existingRoleCodes.contains(roleType.getValue()))
                .toList();
        if (!missingRoles.isEmpty()) {
            throw new BynBaseException("以下角色尚未授权该模型：" + missingRoles.stream().map(RoleType::getName).toList());
        }

        this.remove(new LambdaQueryWrapper<SysModelAvailability>()
                .eq(SysModelAvailability::getModelId, modelId)
                .in(SysModelAvailability::getRoleCode, roleCodes));
    }
}
