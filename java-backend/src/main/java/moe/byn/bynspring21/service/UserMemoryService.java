package moe.byn.bynspring21.service;

import com.baomidou.mybatisplus.extension.service.IService;
import moe.byn.bynspring21.entity.UserMemory;
import moe.byn.bynspring21.entity.base.MemoryStorageType;

public interface UserMemoryService extends IService<UserMemory> {

    UserMemory findOrCreate(String userId, MemoryStorageType storageType);
}
