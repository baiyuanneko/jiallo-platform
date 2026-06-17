package moe.byn.bynspring21.config.interceptor;

import com.alibaba.fastjson2.JSON;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import moe.byn.bynspring21.entity.User;
import moe.byn.bynspring21.entity.base.R;
import moe.byn.bynspring21.security.SecurityUtil;
import moe.byn.bynspring21.service.FeatureModuleAvailabilityService;
import moe.byn.bynspring21.service.UserGroupMemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

@Component
public class FeatureModuleInterceptor implements HandlerInterceptor {

    @Autowired
    private FeatureModuleAvailabilityService featureModuleAvailabilityService;

    @Autowired
    private UserGroupMemberService userGroupMemberService;

    @Autowired
    private SecurityUtil securityUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        User currentUser = securityUtil.getUser();
        if (currentUser == null) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(JSON.toJSONString(R.error("未登录")));
            return false;
        }

        List<String> groupIds = userGroupMemberService.getUserGroupIds(currentUser.getId());
        boolean available = featureModuleAvailabilityService.isModuleAvailable("rag_knowledge_base", currentUser.getRoleType(), groupIds);
        if (!available) {
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(JSON.toJSONString(R.error("当前角色无权访问 RAG 知识库功能")));
            return false;
        }

        return true;
    }
}
