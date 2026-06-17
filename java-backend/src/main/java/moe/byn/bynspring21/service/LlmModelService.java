package moe.byn.bynspring21.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import moe.byn.bynspring21.entity.SysLlmModel;
import moe.byn.bynspring21.entity.vo.AgenticChatAppVo;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface LlmModelService {

    Page<SysLlmModel> pageAvailableSysModels(AgenticChatAppVo.PageSessionsVo vo);

    List<SysLlmModel> listAvailableSysModels();

    ResponseEntity<byte[]> getModelIcon(String modelId, Integer modelType);
}
