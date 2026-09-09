package com.rnpc.inventory.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.HiddenHttpMethodFilter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // other configuration...

    @Bean(name = "customHiddenHttpMethodFilter")
    public HiddenHttpMethodFilter hiddenHttpMethodFilter() {
        return new HiddenHttpMethodFilter();
    }

    // Every product/repair/order/appointment/client upload handler (see e.g. OrderService,
    // RepairRecordService) writes to public/images/ relative to the process working directory,
    // and every template references the result as /images/<filename> - but nothing was actually
    // registering that directory as a static-resource location, so every uploaded image 404'd.
    // "public/images/" (no leading slash) is resolved relative to the working directory the JVM
    // was started from, matching Paths.get("public/images/") in the upload handlers exactly.
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:public/images/");
    }
}
