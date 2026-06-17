package moe.byn.bynspring21.config;

import moe.byn.bynspring21.config.interceptor.FeatureModuleInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private FeatureModuleInterceptor featureModuleInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(featureModuleInterceptor)
                .addPathPatterns("/ragLibrary/**")
                .excludePathPatterns("/ragLibrary/checkAuth");
    }
}
