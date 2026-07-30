package moe.byn.bynspring21.service;

import com.baomidou.mybatisplus.extension.service.IService;
import moe.byn.bynspring21.entity.ClawLikeSecurityLevel;
import moe.byn.bynspring21.entity.ClawLikeSecurityLevelGroup;
import moe.byn.bynspring21.entity.base.RoleType;

import java.util.List;

public interface ClawLikeSecurityLevelService extends IService<ClawLikeSecurityLevel> {

    /**
     * 解析生效安全级别：取角色授权与所有用户组授权中的最高值，无授权记录返回 0（NONE）
     */
    int resolveEffectiveLevel(RoleType roleType, List<String> groupIds);

    List<ClawLikeSecurityLevel> listRoleLevels();

    void setRoleLevel(Integer roleCode, Integer securityLevel);

    void removeRoleLevel(Integer roleCode);

    List<ClawLikeSecurityLevelGroup> listGroupLevels();

    void setGroupLevel(String groupId, Integer securityLevel);

    void removeGroupLevel(String groupId);
}
