package moe.byn.bynspring21.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import moe.byn.bynspring21.entity.SysLlmModel;
import moe.byn.bynspring21.entity.SysLlmProvider;
import moe.byn.bynspring21.entity.SysModelAvailability;
import moe.byn.bynspring21.entity.base.RoleType;
import moe.byn.bynspring21.entity.dto.CreateChatModelDto;
import moe.byn.bynspring21.exception.BynBaseException;
import moe.byn.bynspring21.mapper.SysLlmModelMapper;
import moe.byn.bynspring21.service.SysLlmModelService;
import moe.byn.bynspring21.service.SysLlmProviderService;
import moe.byn.bynspring21.service.SysModelAvailabilityService;
import moe.byn.bynspring21.service.SysModelGroupAvailabilityService;
import moe.byn.bynspring21.utils.AesUtils;
import moe.byn.bynspring21.utils.SpringAiUtils;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SysLlmModelServiceImpl extends ServiceImpl<SysLlmModelMapper, SysLlmModel> implements SysLlmModelService {

    @Autowired
    private SysModelAvailabilityService sysModelAvailabilityService;

    @Autowired
    private SysModelGroupAvailabilityService sysModelGroupAvailabilityService;

    @Autowired
    private SysLlmProviderService sysLlmProviderService;

    @Autowired
    private AesUtils aesUtils;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeModelWithAvailability(String modelId) {
        SysLlmModel model = this.getById(modelId);
        if (model == null) {
            throw new BynBaseException("模型不存在");
        }

        sysModelAvailabilityService.remove(new LambdaQueryWrapper<SysModelAvailability>()
                .eq(SysModelAvailability::getModelId, modelId));

        this.removeById(modelId);
    }

    @Override
    public ChatModel createChatModelByModelId(String modelId) {
        SysLlmModel model = this.getById(modelId);
        if (model == null) {
            throw new BynBaseException("模型不存在");
        }

        SysLlmProvider provider = sysLlmProviderService.getById(model.getProviderId());
        if (provider == null) {
            throw new BynBaseException("供应商不存在");
        }

        String effectiveModelName = StrUtil.blankToDefault(model.getRealModelName(), model.getModelName());

        CreateChatModelDto.CreateChatModelDtoBuilder dtoBuilder = CreateChatModelDto.builder()
                .baseUrl(provider.getBaseUrl())
                .apiKey(aesUtils.decrypt(provider.getApiKey()))
                .modelName(effectiveModelName);

        // StepFun 推理模型流式响应用 reasoning 字段，但 Spring AI 只认 reasoning_content。
        // 传入 reasoning_format=deepseek-style 让 API 返回后者以兼容。
        if (effectiveModelName.startsWith("step-")) {
            dtoBuilder.extraBody(Map.of("reasoning_format", "deepseek-style"));
        }

        return SpringAiUtils.createChatModel(dtoBuilder.build());
    }

    @Override
    public Page<SysLlmModel> pageAvailableModelsByRole(RoleType roleType, Integer pageNum, Integer pageSize) {
        return pageAvailableModelsByRole(roleType, List.of(), pageNum, pageSize);
    }

    @Override
    public List<SysLlmModel> listAvailableModelsByRole(RoleType roleType) {
        return listAvailableModelsByRole(roleType, List.of());
    }

    @Override
    public Page<SysLlmModel> pageAvailableModelsByRole(RoleType roleType, List<String> groupIds, Integer pageNum, Integer pageSize) {
        Page<SysLlmModel> page = new Page<>(pageNum, pageSize);
        Set<String> modelIdSet = new HashSet<>(sysModelAvailabilityService.list(new LambdaQueryWrapper<SysModelAvailability>()
                        .eq(SysModelAvailability::getRoleCode, roleType.getValue()))
                .stream()
                .map(SysModelAvailability::getModelId)
                .toList());

        if (groupIds != null && !groupIds.isEmpty()) {
            modelIdSet.addAll(sysModelGroupAvailabilityService.getModelsAvailableToGroups(groupIds));
        }

        if (modelIdSet.isEmpty()) {
            return page;
        }

        return this.page(page, new LambdaQueryWrapper<SysLlmModel>()
                .in(SysLlmModel::getId, modelIdSet)
                .orderByDesc(SysLlmModel::getCreateTime));
    }

    @Override
    public List<SysLlmModel> listAvailableModelsByRole(RoleType roleType, List<String> groupIds) {
        Set<String> modelIdSet = new HashSet<>(sysModelAvailabilityService.list(new LambdaQueryWrapper<SysModelAvailability>()
                        .eq(SysModelAvailability::getRoleCode, roleType.getValue()))
                .stream()
                .map(SysModelAvailability::getModelId)
                .toList());

        if (groupIds != null && !groupIds.isEmpty()) {
            modelIdSet.addAll(sysModelGroupAvailabilityService.getModelsAvailableToGroups(groupIds));
        }

        if (modelIdSet.isEmpty()) {
            return List.of();
        }

        return this.list(new LambdaQueryWrapper<SysLlmModel>()
                .in(SysLlmModel::getId, modelIdSet)
                .orderByDesc(SysLlmModel::getCreateTime));
    }
}
