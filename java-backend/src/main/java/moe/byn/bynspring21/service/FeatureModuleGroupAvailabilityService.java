package moe.byn.bynspring21.service;

import com.baomidou.mybatisplus.extension.service.IService;
import moe.byn.bynspring21.entity.FeatureModuleGroupAvailability;

import java.util.List;

public interface FeatureModuleGroupAvailabilityService extends IService<FeatureModuleGroupAvailability> {

    List<String> getModuleAvailableGroups(String moduleCode);

    void authorizeModuleToGroups(String moduleCode, List<String> groupIds);

    void revokeModuleOfGroups(String moduleCode, List<String> groupIds);

    boolean isModuleAvailableToGroups(String moduleCode, List<String> groupIds);
}
