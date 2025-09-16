package com.chat.chatbackend.rt;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class SocketIOGateway implements RealtimeGateway {

    private final SocketIOServer server;

    // uid <-> sid 映射
    private final Map<String, UUID> user2sid = new ConcurrentHashMap<>();
    private final Map<UUID, String> sid2user = new ConcurrentHashMap<>();

    // ---- 映射管理 ----
    @Override
    public void register(String uid, UUID sid) {
        user2sid.put(uid, sid);
        sid2user.put(sid, uid);
    }

    @Override
    public void unregister(UUID sid) {
        String uid = sid2user.remove(sid);
        if (uid != null) user2sid.remove(uid);
    }

    public String uidOf(UUID sid) { return sid2user.get(sid); }

    // ---- 发送 ----
    @Override
    public void sendToUser(String uid, String event, Object payload) {
        server.getRoomOperations(userRoom(uid)).sendEvent(event, payload);
    }
    @Override
    public String userRoom(String uid) { return "u:" + uid; }
    @Override
    public String groupRoom(String room) {
        return "g:" + room;
    }

    @Override
    public void sendToRoom(String room, String event, Object payload) {
        server.getRoomOperations(room).sendEvent(event, payload);
    }



    // ---- 包装 Conn/Ack，供上层使用 ----
    public RealtimeGateway.Conn connOf(SocketIOClient c) {
        UUID sid = c.getSessionId();
        return new RealtimeGateway.Conn() {
            @Override public String uid() { return uidOf(sid); }
            @Override public UUID sid() { return sid; }
            @Override public void join(String room) { c.joinRoom(room); }
            @Override public void leave(String room) { c.leaveRoom(room); }
        };
    }

    public RealtimeGateway.Ack ackOf(AckRequest req) {
        return new RealtimeGateway.Ack() {
            @Override public void ok(Object data) {
                if (req != null && req.isAckRequested()) req.sendAckData(data);
            }
            @Override public void fail(String code, String msg) {
                if (req != null && req.isAckRequested())
                    req.sendAckData(Map.of("ok", false, "code", code, "msg", msg));
            }
        };
    }

    public Ack noAck() {
        return new Ack() {
            @Override public void ok(Object data) { /* no-op */ }
            @Override public void fail(String code, String msg) { /* no-op */ }
        };
    }
}
