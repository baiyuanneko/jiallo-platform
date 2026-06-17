package moe.byn.bynspring21.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import moe.byn.bynspring21.entity.UserMemory;
import moe.byn.bynspring21.entity.base.MemoryStorageType;
import moe.byn.bynspring21.mapper.UserMemoryMapper;
import moe.byn.bynspring21.service.UserMemoryService;
import org.springframework.stereotype.Service;

@Service
public class UserMemoryServiceImpl extends ServiceImpl<UserMemoryMapper, UserMemory>
        implements UserMemoryService {

    @Override
    public UserMemory findOrCreate(String userId, MemoryStorageType storageType) {
        UserMemory memory = getOne(
                new LambdaQueryWrapper<UserMemory>()
                        .eq(UserMemory::getCreateUser, userId)
                        .eq(UserMemory::getStorageType, storageType)
        );
        if (memory == null) {
            memory = UserMemory.builder()
                    .storageType(storageType)
                    .build();
            memory.setCreateUser(userId);
            memory.setUpdateUser(userId);
            save(memory);
        }
        return memory;
    }
}
