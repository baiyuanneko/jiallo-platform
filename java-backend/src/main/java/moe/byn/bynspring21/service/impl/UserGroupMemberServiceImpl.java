package moe.byn.bynspring21.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import moe.byn.bynspring21.entity.UserGroupMember;
import moe.byn.bynspring21.exception.BynBaseException;
import moe.byn.bynspring21.mapper.UserGroupMemberMapper;
import moe.byn.bynspring21.service.UserGroupMemberService;
import moe.byn.bynspring21.utils.RedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserGroupMemberServiceImpl extends ServiceImpl<UserGroupMemberMapper, UserGroupMember> implements UserGroupMemberService {

    private static final String USER_GROUPS_CACHE_PREFIX = "userGroups:";

    @Autowired
    private RedisUtils redisUtils;

    @Override
    public List<String> getGroupMemberUserIds(String groupId) {
        return this.list(new LambdaQueryWrapper<UserGroupMember>()
                        .eq(UserGroupMember::getGroupId, groupId))
                .stream()
                .map(UserGroupMember::getUserId)
                .toList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> getUserGroupIds(String userId) {
        String cacheKey = USER_GROUPS_CACHE_PREFIX + userId;
        Object cached = redisUtils.get(cacheKey);
        if (cached != null) {
            return (List<String>) cached;
        }

        List<String> groupIds = this.list(new LambdaQueryWrapper<UserGroupMember>()
                        .eq(UserGroupMember::getUserId, userId))
                .stream()
                .map(UserGroupMember::getGroupId)
                .distinct()
                .toList();

        redisUtils.set(cacheKey, groupIds, 3600L);
        return groupIds;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addMembersToGroup(String groupId, List<String> userIds) {
        List<String> distinctUserIds = userIds.stream()
                .filter(userId -> userId != null && !userId.isBlank())
                .distinct()
                .toList();
        if (distinctUserIds.isEmpty()) {
            return;
        }

        // 检查已存在的有效成员（is_del=0）
        List<String> existingUserIds = this.list(new LambdaQueryWrapper<UserGroupMember>()
                        .eq(UserGroupMember::getGroupId, groupId)
                        .in(UserGroupMember::getUserId, distinctUserIds))
                .stream()
                .map(UserGroupMember::getUserId)
                .collect(Collectors.toSet())
                .stream().toList();

        List<String> duplicated = distinctUserIds.stream()
                .filter(existingUserIds::contains)
                .toList();
        if (!duplicated.isEmpty()) {
            throw new BynBaseException("以下用户已在该用户组中：" + duplicated);
        }

        this.saveBatch(distinctUserIds.stream()
                .map(userId -> UserGroupMember.builder()
                        .groupId(groupId)
                        .userId(userId)
                        .build())
                .toList());

        // 清除对应用户缓存
        distinctUserIds.forEach(userId -> redisUtils.del(USER_GROUPS_CACHE_PREFIX + userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeMembersFromGroup(String groupId, List<String> userIds) {
        List<String> distinctUserIds = userIds.stream()
                .filter(userId -> userId != null && !userId.isBlank())
                .distinct()
                .toList();
        if (distinctUserIds.isEmpty()) {
            return;
        }

        this.remove(new LambdaQueryWrapper<UserGroupMember>()
                .eq(UserGroupMember::getGroupId, groupId)
                .in(UserGroupMember::getUserId, distinctUserIds));

        // 清除对应用户缓存
        distinctUserIds.forEach(userId -> redisUtils.del(USER_GROUPS_CACHE_PREFIX + userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeAllMembersFromGroup(String groupId) {
        // 先获取该组所有成员，用于清除缓存
        List<String> memberUserIds = getGroupMemberUserIds(groupId);

        this.remove(new LambdaQueryWrapper<UserGroupMember>()
                .eq(UserGroupMember::getGroupId, groupId));

        // 清除所有成员的缓存
        memberUserIds.forEach(userId -> redisUtils.del(USER_GROUPS_CACHE_PREFIX + userId));
    }
}
