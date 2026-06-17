package moe.byn.bynspring21.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import moe.byn.bynspring21.entity.SysLlmModel;
import moe.byn.bynspring21.entity.SysLlmProvider;
import moe.byn.bynspring21.entity.User;
import moe.byn.bynspring21.entity.vo.AgenticChatAppVo;
import moe.byn.bynspring21.exception.BynBaseException;
import moe.byn.bynspring21.security.SecurityUtil;
import moe.byn.bynspring21.service.LlmModelService;
import moe.byn.bynspring21.service.MinioService;
import moe.byn.bynspring21.service.SysConfigService;
import moe.byn.bynspring21.service.SysLlmModelService;
import moe.byn.bynspring21.service.SysLlmProviderService;
import moe.byn.bynspring21.service.UserGroupMemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LlmModelServiceImpl implements LlmModelService {

    @Autowired
    private SysLlmModelService sysLlmModelService;

    @Autowired
    private SysLlmProviderService sysLlmProviderService;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private MinioService minioService;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private UserGroupMemberService userGroupMemberService;

    @Override
    public Page<SysLlmModel> pageAvailableSysModels(AgenticChatAppVo.PageSessionsVo vo) {
        User currentUser = securityUtil.getUser();
        List<String> groupIds = userGroupMemberService.getUserGroupIds(currentUser.getId());
        Page<SysLlmModel> page = sysLlmModelService.pageAvailableModelsByRole(
                currentUser.getRoleType(), groupIds, vo.getPageNum(), vo.getPageSize());

        Set<String> providerIds = page.getRecords().stream()
                .map(SysLlmModel::getProviderId)
                .collect(Collectors.toSet());
        if (!providerIds.isEmpty()) {
            Map<String, String> providerNameMap = sysLlmProviderService.listByIds(providerIds).stream()
                    .collect(Collectors.toMap(SysLlmProvider::getId, SysLlmProvider::getProviderName));
            page.getRecords().forEach(model ->
                    model.setProviderName(providerNameMap.get(model.getProviderId())));
        }

        return page;
    }

    @Override
    public List<SysLlmModel> listAvailableSysModels() {
        User currentUser = securityUtil.getUser();
        List<String> groupIds = userGroupMemberService.getUserGroupIds(currentUser.getId());
        List<SysLlmModel> models = sysLlmModelService.listAvailableModelsByRole(currentUser.getRoleType(), groupIds);

        Set<String> providerIds = models.stream()
                .map(SysLlmModel::getProviderId)
                .collect(Collectors.toSet());
        if (!providerIds.isEmpty()) {
            Map<String, String> providerNameMap = sysLlmProviderService.listByIds(providerIds).stream()
                    .collect(Collectors.toMap(SysLlmProvider::getId, SysLlmProvider::getProviderName));
            models.forEach(model ->
                    model.setProviderName(providerNameMap.get(model.getProviderId())));
        }

        return models;
    }

    @Override
    public ResponseEntity<byte[]> getModelIcon(String modelId, Integer modelType) {
        if (Integer.valueOf(0).equals(modelType)) {
            return getSysModelIcon(modelId);
        } else {
            // modelType=1 暂不支持，后续实现 userLlmModel 时补全
            return ResponseEntity.notFound().build();
        }
    }

    private ResponseEntity<byte[]> getSysModelIcon(String modelId) {
        SysLlmModel model = sysLlmModelService.getById(modelId);
        if (model == null) {
            return ResponseEntity.notFound().build();
        }

        String iconUrl = null;

        // 降级链 1：model.iconUrl
        if (StringUtils.hasText(model.getModelIconUrl())) {
            iconUrl = model.getModelIconUrl();
        }

        // 降级链 2：provider.iconUrl
        if (iconUrl == null && StringUtils.hasText(model.getProviderId())) {
            SysLlmProvider provider = sysLlmProviderService.getById(model.getProviderId());
            if (provider != null && StringUtils.hasText(provider.getProviderIconUrl())) {
                iconUrl = provider.getProviderIconUrl();
            }
        }

        // 降级链 3：sysConfig defaultLlmIcon
        if (iconUrl == null) {
            String defaultIcon = sysConfigService.getConfigValue("defaultLlmIcon");
            if (StringUtils.hasText(defaultIcon)) {
                iconUrl = defaultIcon;
            }
        }

        // 降级链 4：全部为空 → 404
        if (iconUrl == null) {
            return ResponseEntity.notFound().build();
        }

        String contentType = minioService.getObjectContentType(iconUrl);
        byte[] bytes = minioService.getMediaBytes(iconUrl);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS))
                .body(bytes);
    }
}
