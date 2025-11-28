package com.ynp.agent.config;

import com.ynp.agent.service.file.FileStorageService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置，处理静态资源映射。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final FileStorageService fileStorageService;

    public WebMvcConfig(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    /**
     * 1. 将上传目录暴露为静态资源。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadPath = fileStorageService.getUploadDir().toUri().toString();
        if (!uploadPath.endsWith("/")) {
            uploadPath = uploadPath + "/";
        }
        registry.addResourceHandler("/agent/uploads/**")
                .addResourceLocations(uploadPath);
    }
}
