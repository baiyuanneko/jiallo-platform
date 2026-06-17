package moe.byn.bynspring21.service;

import com.baomidou.mybatisplus.extension.service.IService;
import moe.byn.bynspring21.entity.SysModelAvailability;
import moe.byn.bynspring21.entity.base.RoleType;

import java.util.List;

public interface SysModelAvailabilityService extends IService<SysModelAvailability> {

    List<RoleType> getModelAvailableRoles(String modelId);

    void authorizeModelToRoles(String modelId, List<RoleType> roleTypes);

    void revokeModelOfRoles(String modelId, List<RoleType> roleTypes);
}
