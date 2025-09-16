package com.chat.chatbackend.config;

import com.corundumstudio.socketio.SocketIOServer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SocketIOLifecycle {

    private final SocketIOServer server;

    @PostConstruct
    public void start() {
        server.start();
        log.info("Socket.IO started on 0.0.0.0:{}", server.getConfiguration().getPort());
    }

    @PreDestroy
    public void stop() {
        log.info("Stopping Socket.IO...");
        server.stop();
    }
}
