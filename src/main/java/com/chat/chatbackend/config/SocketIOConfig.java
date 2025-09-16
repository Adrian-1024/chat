package com.chat.chatbackend.config;

import com.corundumstudio.socketio.SocketConfig;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.SpringAnnotationScanner;
import com.corundumstudio.socketio.store.RedissonStoreFactory;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SocketIOConfig {

    @Bean
    public SocketIOServer socketIOServer(RedissonClient redisson) {
        // 这里使用全限定名，避免和 Spring 的 @Configuration 重名
        com.corundumstudio.socketio.Configuration cfg =
                new com.corundumstudio.socketio.Configuration();

        cfg.setHostname("0.0.0.0");
        cfg.setPort(9092);           // 和 Nginx 反代到后端的端口保持一致
        cfg.setOrigin("*");

        // socket 细节参数（可选）
        SocketConfig sc = new SocketConfig();
        sc.setReuseAddress(true);
        sc.setTcpNoDelay(true);
        cfg.setSocketConfig(sc);

        // 心跳/超时（可按需调整）
        cfg.setPingInterval(25_000);
        cfg.setPingTimeout(60_000);
        cfg.setUpgradeTimeout(10_000);

        // 关键：启用 Redis 作为跨实例存储/广播总线
        cfg.setStoreFactory(new RedissonStoreFactory(redisson));

        return new SocketIOServer(cfg);
    }

    // 关键：把带 @OnEvent/@OnConnect 的 Spring Bean 扫描注册到 server
    @Bean
    public SpringAnnotationScanner springAnnotationScanner(SocketIOServer server) {
        return new SpringAnnotationScanner(server);
    }
}
