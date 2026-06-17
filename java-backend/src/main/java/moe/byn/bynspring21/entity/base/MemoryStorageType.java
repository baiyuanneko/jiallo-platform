package moe.byn.bynspring21.entity.base;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MemoryStorageType {

    DEFAULT(0),
    BYN_DIGITAL_CLONE(1);

    @EnumValue
    private final int value;

    public static MemoryStorageType valueOf(int value) {
        for (MemoryStorageType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        return DEFAULT;
    }
}
