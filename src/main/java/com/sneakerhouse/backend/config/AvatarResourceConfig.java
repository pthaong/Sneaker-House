package com.sneakerhouse.backend.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
import java.nio.file.Paths;
@Configuration public class AvatarResourceConfig implements WebMvcConfigurer {
private final String location;
public AvatarResourceConfig(@Value("${app.upload-dir:uploads/avatars}") String d){location=Paths.get(d).toAbsolutePath().normalize().toUri().toString();}
@Override public void addResourceHandlers(ResourceHandlerRegistry r){r.addResourceHandler("/uploads/avatars/**").addResourceLocations(location);}
}
