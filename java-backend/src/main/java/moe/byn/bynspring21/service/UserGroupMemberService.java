package moe.byn.bynspring21.service;

import com.baomidou.mybatisplus.extension.service.IService;
import moe.byn.bynspring21.entity.UserGroupMember;

import java.util.List;

public interface UserGroupMemberService extends IService<UserGroupMember> {

    List<String> getGroupMemberUserIds(String groupId);

    List<String> getUserGroupIds(String userId);

    void addMembersToGroup(String groupId, List<String> userIds);

    void removeMembersFromGroup(String groupId, List<String> userIds);

    void removeAllMembersFromGroup(String groupId);
}
