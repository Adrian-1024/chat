package com.chat.chatbackend.chat;

import com.chat.chatbackend.rt.RealtimeGateway;
import com.chat.chatbackend.socket.dto.ChatSend;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final RealtimeGateway rt; // 实际注入的是 SocketIOGateway

    public void handleChatSend(String roomId, String senderUid, ChatSend req, RealtimeGateway.Ack ack) {
        // TODO: 这里接入 MySQL/幂等/风控/群成员校验等
        // 只放非空字段，避免 Map.of 的空值 NPE
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("from", senderUid);
        if (req.getTo() != null && !req.getTo().isBlank()) {
            payload.put("to", req.getTo());
        }
        if (req.getRoom() != null && !req.getRoom().isBlank()) {
            payload.put("room", req.getRoom());
        }
        if (req.getMsg() != null) {
            payload.put("msg", req.getMsg());
        }
        payload.put("ts", System.currentTimeMillis());

        rt.sendToRoom(roomId, "chat:new", payload);
        ack.ok(Map.of("ok", true));
    }
}
