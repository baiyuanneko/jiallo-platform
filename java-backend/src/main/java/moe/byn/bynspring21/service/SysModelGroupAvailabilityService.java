package moe.byn.bynspring21.service;

import com.baomidou.mybatisplus.extension.service.IService;
import moe.byn.bynspring21.entity.SysModelGroupAvailability;

import java.util.List;

public interface SysModelGroupAvailabilityService extends IService<SysModelGroupAvailability> {

    List<String> getModelAvailableGroups(String modelId);

    void authorizeModelToGroups(String modelId, List<String> groupIds);

    void revokeModelOfGroups(String modelId, List<String> groupIds);

    List<String> getModelsAvailableToGroups(List<String> groupIds);
}
