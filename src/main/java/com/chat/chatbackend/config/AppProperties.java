package com.chat.chatbackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private Socket socket = new Socket();
    private Redis redis = new Redis();

    @Data public static class Socket { private String host; private int port; private String allowedOrigins; }
    @Data public static class Redis  { private String url; }
}
