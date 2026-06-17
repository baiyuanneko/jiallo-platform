package moe.byn.bynspring21.service;

import com.baomidou.mybatisplus.extension.service.IService;
import moe.byn.bynspring21.entity.SysConfig;

public interface SysConfigService extends IService<SysConfig> {
    String getConfigValue(String key);

    void setConfigValue(String key, String value);
}
