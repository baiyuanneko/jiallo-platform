package moe.byn.bynspring21.service;

import com.baomidou.mybatisplus.extension.service.IService;
import moe.byn.bynspring21.entity.FeatureModuleAvailability;
import moe.byn.bynspring21.entity.base.RoleType;

import java.util.List;

public interface FeatureModuleAvailabilityService extends IService<FeatureModuleAvailability> {

    List<RoleType> getModuleAvailableRoles(String moduleCode);

    void authorizeModuleToRoles(String moduleCode, List<RoleType> roleTypes);

    void revokeModuleOfRoles(String moduleCode, List<RoleType> roleTypes);

    boolean isModuleAvailable(String moduleCode, RoleType roleType);

    boolean isModuleAvailable(String moduleCode, RoleType roleType, List<String> groupIds);
}
