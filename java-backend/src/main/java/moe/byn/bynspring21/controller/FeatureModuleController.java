package moe.byn.bynspring21.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import moe.byn.bynspring21.entity.User;
import moe.byn.bynspring21.entity.base.R;
import moe.byn.bynspring21.entity.vo.FeatureModuleVo;
import moe.byn.bynspring21.security.SecurityUtil;
import moe.byn.bynspring21.service.FeatureModuleAvailabilityService;
import moe.byn.bynspring21.service.UserGroupMemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("featureModule")
@Tag(name = "FeatureModule", description = "功能模块授权")
@Validated
public class FeatureModuleController {

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private FeatureModuleAvailabilityService featureModuleAvailabilityService;

    @Autowired
    private UserGroupMemberService userGroupMemberService;

    @Operation(summary = "检查功能模块授权", description = "当前用户是否有指定功能模块的授权", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("checkAuth")
    public R<Boolean> checkAuth(@Validated @RequestBody FeatureModuleVo.CheckAuthVo vo) {
        User currentUser = securityUtil.getUser();
        List<String> groupIds = userGroupMemberService.getUserGroupIds(currentUser.getId());
        boolean authorized = featureModuleAvailabilityService.isModuleAvailable(vo.getModuleCode(), currentUser.getRoleType(), groupIds);
        return R.ok(authorized);
    }
}
