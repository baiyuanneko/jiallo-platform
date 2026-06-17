package moe.byn.bynspring21.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import moe.byn.bynspring21.entity.UserGroup;
import moe.byn.bynspring21.exception.BynBaseException;
import moe.byn.bynspring21.mapper.UserGroupMapper;
import moe.byn.bynspring21.service.AgentTypeGroupAvailabilityService;
import moe.byn.bynspring21.service.FeatureModuleGroupAvailabilityService;
import moe.byn.bynspring21.service.SysModelGroupAvailabilityService;
import moe.byn.bynspring21.service.UserGroupMemberService;
import moe.byn.bynspring21.service.UserGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserGroupServiceImpl extends ServiceImpl<UserGroupMapper, UserGroup> implements UserGroupService {

    @Autowired
    private UserGroupMemberService userGroupMemberService;

    @Autowired
    private SysModelGroupAvailabilityService sysModelGroupAvailabilityService;

    @Autowired
    private AgentTypeGroupAvailabilityService agentTypeGroupAvailabilityService;

    @Autowired
    private FeatureModuleGroupAvailabilityService featureModuleGroupAvailabilityService;

    @Override
    public Page<UserGroup> pageUserGroups(String keyword, Integer pageNum, Integer pageSize) {
        Page<UserGroup> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<UserGroup> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.like(UserGroup::getGroupName, keyword);
        }
        wrapper.orderByDesc(UserGroup::getCreateTime);
        return this.page(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserGroup createUserGroup(String groupName, String description) {
        UserGroup group = UserGroup.builder()
                .groupName(groupName)
                .description(description)
                .build();
        this.save(group);
        return group;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserGroup updateUserGroup(String groupId, String groupName, String description) {
        UserGroup group = this.getById(groupId);
        if (group == null) {
            throw new BynBaseException("用户组不存在");
        }
        if (StrUtil.isNotBlank(groupName)) {
            group.setGroupName(groupName);
        }
        if (description != null) {
            group.setDescription(description);
        }
        this.updateById(group);
        return group;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUserGroup(String groupId) {
        UserGroup group = this.getById(groupId);
        if (group == null) {
            throw new BynBaseException("用户组不存在");
        }

        // 级联删除成员
        userGroupMemberService.removeAllMembersFromGroup(groupId);

        // 级联删除三张 availability 表中该组的记录
        sysModelGroupAvailabilityService.remove(new LambdaQueryWrapper<moe.byn.bynspring21.entity.SysModelGroupAvailability>()
                .eq(moe.byn.bynspring21.entity.SysModelGroupAvailability::getGroupId, groupId));
        agentTypeGroupAvailabilityService.remove(new LambdaQueryWrapper<moe.byn.bynspring21.entity.AgentTypeGroupAvailability>()
                .eq(moe.byn.bynspring21.entity.AgentTypeGroupAvailability::getGroupId, groupId));
        featureModuleGroupAvailabilityService.remove(new LambdaQueryWrapper<moe.byn.bynspring21.entity.FeatureModuleGroupAvailability>()
                .eq(moe.byn.bynspring21.entity.FeatureModuleGroupAvailability::getGroupId, groupId));

        this.removeById(groupId);
    }
}
