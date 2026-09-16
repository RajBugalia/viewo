package com.example.viewobackend.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.io.File

@Configuration
class WebConfig : WebMvcConfigurer {
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        // Ensure the directory exists using absolute path to match the controller
        val uploadPath = java.nio.file.Paths.get(System.getProperty("user.dir"), "uploads").toAbsolutePath()
        val uploadDir = uploadPath.toFile()
        if (!uploadDir.exists()) {
            uploadDir.mkdirs()
        }

        // Convert Windows backslashes to forward slashes for Spring Resource Handler
        val normalizedPath = uploadDir.absolutePath.replace("\\", "/")
        registry.addResourceHandler("/uploads/**")
            .addResourceLocations("file:///$normalizedPath/")
    }
}
