package moe.byn.bynspring21.utils;

import cn.hutool.extra.servlet.JakartaServletUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class RequestUtils {
    public static HttpServletRequest getRequest() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        return attrs.getRequest();
    }

    public static String getClientIp() {
        return JakartaServletUtil.getClientIP(RequestUtils.getRequest());
    }

    public static String getUserAgent() {
        return RequestUtils.getRequest().getHeader("User-Agent");
    }
}
