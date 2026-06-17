package moe.byn.bynspring21.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import moe.byn.bynspring21.entity.SysConfig;
import moe.byn.bynspring21.mapper.SysConfigMapper;
import moe.byn.bynspring21.service.SysConfigService;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements SysConfigService {

    @Override
    public String getConfigValue(String key) {
        SysConfig entry = this.getOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, key));

        if (entry == null) {
            return null;
        }

        return entry.getConfigValue();
    }

    @Override
    public void setConfigValue(String key, String value) {
        SysConfig entry = this.getOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, key));

        if (entry == null) {
            entry = new SysConfig(key, value, null);
        } else {
            entry.setConfigValue(value);
        }

        this.saveOrUpdate(entry);
    }
}
