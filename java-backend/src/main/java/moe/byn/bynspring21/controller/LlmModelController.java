package moe.byn.bynspring21.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import moe.byn.bynspring21.entity.SysLlmModel;
import moe.byn.bynspring21.entity.User;
import moe.byn.bynspring21.entity.base.R;
import moe.byn.bynspring21.entity.vo.AgenticChatAppVo;
import moe.byn.bynspring21.security.SecurityUtil;
import moe.byn.bynspring21.service.LlmModelService;
import moe.byn.bynspring21.service.UserService;
import moe.byn.bynspring21.utils.RedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@RestController
@RequestMapping("llmModel")
@Tag(name = "LlmModel", description = "LLM 模型管理")
@Validated
public class LlmModelController {

    @Autowired
    private LlmModelService llmModelService;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private UserService userService;

    @Autowired
    private RedisUtils redisUtils;

    @Operation(summary = "分页查询当前用户可用的系统模型")
    @PostMapping("available/page")
    public R<Page<SysLlmModel>> pageAvailableSysModels(
            @Validated @RequestBody AgenticChatAppVo.PageSessionsVo vo) {
        return R.ok(llmModelService.pageAvailableSysModels(vo));
    }

    @Operation(
            summary = "查询当前用户可用的系统模型列表",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @GetMapping("available/list")
    public R<List<SysLlmModel>> listAvailableSysModels() {
        return R.ok(llmModelService.listAvailableSysModels());
    }

    @Operation(
            summary = "设置默认模型",
            description = "设置当前用户的偏好模型ID，传入空字符串表示清除默认",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("setDefaultModel")
    public R<Void> setDefaultModel(
            @Parameter(description = "模型ID，空字符串表示清除默认", required = true)
            @RequestParam("modelId") String modelId,
            @Parameter(description = "模型类型：0为sysModel、1为userModel", required = true)
            @RequestParam("modelType") Integer modelType) {
        if (!modelId.isEmpty() && Integer.valueOf(1).equals(modelType)) {
            throw new moe.byn.bynspring21.exception.BynBaseException("当前仅支持 modelType=0（sysModel）模式");
        }
        User currentUser = securityUtil.getUser();
        if (modelId.isEmpty()) {
            currentUser.setPreferredModelId(null);
            currentUser.setPreferredModelType(null);
        } else {
            currentUser.setPreferredModelId(modelId);
            currentUser.setPreferredModelType(modelType);
        }
        userService.updateById(currentUser);
        redisUtils.del("user:" + currentUser.getId());
        return R.ok();
    }

    @Operation(summary = "获取模型图标", description = "公开接口，无需认证。modelType=0 走 sys 表降级链，modelType=1 暂不支持")
    @GetMapping("icon/{modelId}")
    public ResponseEntity<byte[]> getModelIcon(
            @Parameter(description = "模型ID", required = true) @PathVariable String modelId,
            @Parameter(description = "模型类型：0为sysModel、1为userModel", required = true)
            @RequestParam("modelType") Integer modelType) {
        return llmModelService.getModelIcon(modelId, modelType);
    }
}
