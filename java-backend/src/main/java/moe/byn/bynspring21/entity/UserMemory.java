package moe.byn.bynspring21.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import moe.byn.bynspring21.entity.base.CommonEntity;
import moe.byn.bynspring21.entity.base.MemoryStorageType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("user_memory")
public class UserMemory extends CommonEntity<UserMemory> {

    private MemoryStorageType storageType;

    private String content;
}
