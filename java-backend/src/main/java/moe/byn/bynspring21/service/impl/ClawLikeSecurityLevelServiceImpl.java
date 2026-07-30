package moe.byn.bynspring21.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import moe.byn.bynspring21.entity.ClawLikeSecurityLevel;
import moe.byn.bynspring21.entity.ClawLikeSecurityLevelGroup;
import moe.byn.bynspring21.entity.base.ClawSecurityLevel;
import moe.byn.bynspring21.entity.base.RoleType;
import moe.byn.bynspring21.exception.BynBaseException;
import moe.byn.bynspring21.mapper.ClawLikeSecurityLevelGroupMapper;
import moe.byn.bynspring21.mapper.ClawLikeSecurityLevelMapper;
import moe.byn.bynspring21.service.ClawLikeSecurityLevelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class ClawLikeSecurityLevelServiceImpl extends ServiceImpl<ClawLikeSecurityLevelMapper, ClawLikeSecurityLevel> implements ClawLikeSecurityLevelService {

    @Autowired
    private ClawLikeSecurityLevelGroupMapper clawLikeSecurityLevelGroupMapper;

    @Override
    public int resolveEffectiveLevel(RoleType roleType, List<String> groupIds) {
        int roleLevel = ClawSecurityLevel.NONE.getCode();
        if (roleType != null) {
            ClawLikeSecurityLevel roleRecord = this.getOne(new LambdaQueryWrapper<ClawLikeSecurityLevel>()
                    .eq(ClawLikeSecurityLevel::getRoleCode, roleType.getValue()), false);
            if (roleRecord != null && roleRecord.getSecurityLevel() != null) {
                roleLevel = roleRecord.getSecurityLevel();
            }
        }

        int groupLevel = ClawSecurityLevel.NONE.getCode();
        if (groupIds != null && !groupIds.isEmpty()) {
            groupLevel = clawLikeSecurityLevelGroupMapper.selectList(new LambdaQueryWrapper<ClawLikeSecurityLevelGroup>()
                            .in(ClawLikeSecurityLevelGroup::getGroupId, groupIds))
                    .stream()
                    .map(ClawLikeSecurityLevelGroup::getSecurityLevel)
                    .filter(Objects::nonNull)
                    .max(Integer::compareTo)
                    .orElse(ClawSecurityLevel.NONE.getCode());
        }

        return Math.max(roleLevel, groupLevel);
    }

    @Override
    public List<ClawLikeSecurityLevel> listRoleLevels() {
        return this.list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setRoleLevel(Integer roleCode, Integer securityLevel) {
        validateSecurityLevel(securityLevel);
        ClawLikeSecurityLevel existing = this.getOne(new LambdaQueryWrapper<ClawLikeSecurityLevel>()
                .eq(ClawLikeSecurityLevel::getRoleCode, roleCode), false);
        if (existing != null) {
            existing.setSecurityLevel(securityLevel);
            this.updateById(existing);
        } else {
            this.save(ClawLikeSecurityLevel.builder()
                    .roleCode(roleCode)
                    .securityLevel(securityLevel)
                    .build());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeRoleLevel(Integer roleCode) {
        this.remove(new LambdaQueryWrapper<ClawLikeSecurityLevel>()
                .eq(ClawLikeSecurityLevel::getRoleCode, roleCode));
    }

    @Override
    public List<ClawLikeSecurityLevelGroup> listGroupLevels() {
        return clawLikeSecurityLevelGroupMapper.selectList(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setGroupLevel(String groupId, Integer securityLevel) {
        validateSecurityLevel(securityLevel);
        ClawLikeSecurityLevelGroup existing = clawLikeSecurityLevelGroupMapper.selectOne(
                new LambdaQueryWrapper<ClawLikeSecurityLevelGroup>()
                        .eq(ClawLikeSecurityLevelGroup::getGroupId, groupId)
                        .last("LIMIT 1"));
        if (existing != null) {
            existing.setSecurityLevel(securityLevel);
            clawLikeSecurityLevelGroupMapper.updateById(existing);
        } else {
            clawLikeSecurityLevelGroupMapper.insert(ClawLikeSecurityLevelGroup.builder()
                    .groupId(groupId)
                    .securityLevel(securityLevel)
                    .build());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeGroupLevel(String groupId) {
        clawLikeSecurityLevelGroupMapper.delete(new LambdaQueryWrapper<ClawLikeSecurityLevelGroup>()
                .eq(ClawLikeSecurityLevelGroup::getGroupId, groupId));
    }

    private void validateSecurityLevel(Integer securityLevel) {
        if (securityLevel == null
                || (securityLevel != ClawSecurityLevel.WORKSPACE.getCode()
                && securityLevel != ClawSecurityLevel.FULL.getCode())) {
            throw new BynBaseException("安全级别只支持 1（工作区文件）或 2（完全访问）");
        }
    }
}
