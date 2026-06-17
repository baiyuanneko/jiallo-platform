package moe.byn.bynspring21.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import moe.byn.bynspring21.entity.UserGroup;

public interface UserGroupService extends IService<UserGroup> {

    Page<UserGroup> pageUserGroups(String keyword, Integer pageNum, Integer pageSize);

    UserGroup createUserGroup(String groupName, String description);

    UserGroup updateUserGroup(String groupId, String groupName, String description);

    void deleteUserGroup(String groupId);
}
