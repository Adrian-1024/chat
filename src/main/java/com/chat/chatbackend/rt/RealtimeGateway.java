package com.chat.chatbackend.rt;

import java.util.UUID;

public interface RealtimeGateway {
    interface Conn {
        String uid();
        UUID sid();
        void join(String room);
        void leave(String room);
    }

    interface Ack {
        void ok(Object data);
        void fail(String code, String msg);
    }

    // 连接注册/注销（在 onConnect/onDisconnect 调用）
    void register(String uid, UUID sid);
    void unregister(UUID sid);

    // 派发
    void sendToUser(String uid, String event, Object payload);

    String userRoom(String uid);

    String groupRoom(String room);

    void sendToRoom(String room, String event, Object payload);
}
