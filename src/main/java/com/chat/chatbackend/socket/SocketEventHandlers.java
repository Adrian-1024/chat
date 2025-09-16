package com.chat.chatbackend.socket;

import com.chat.chatbackend.chat.ChatService;
import com.chat.chatbackend.rt.SocketIOGateway;
import com.chat.chatbackend.socket.dto.ChatSend;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import com.corundumstudio.socketio.annotation.OnEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class SocketEventHandlers {

    private final SocketIOGateway rt;
    private final ChatService chatService;

    @OnConnect
    public void onConnect(SocketIOClient c) {
        String token = c.getHandshakeData().getSingleUrlParam("token");
        if (token == null || token.isBlank()) {
            log.warn("reject connect, no token. sid={}", c.getSessionId());
            c.disconnect();
            return;
        }
        c.set("uid", token);
        c.joinRoom(rt.userRoom(token));
        log.info("connected uid={} sid={}", token, c.getSessionId());
        c.sendEvent("sys:welcome", Map.of("uid", token));
    }

    @OnDisconnect
    public void onDisconnect(SocketIOClient c) {
        log.info("disconnected sid={}", c.getSessionId());
        String uid = (String) c.get("uid");
        if (uid == null || uid.isBlank()) return;

        var ack = rt.noAck();
        var rooms = new HashSet<>(c.getAllRooms()); // 快照，避免并发修改
        for (String r : rooms) {
            if (!r.startsWith("g:")) continue; // 只广播群房间
            ChatSend payload = new ChatSend();
            payload.setRoom(r);
            payload.setMsg(uid + "离开房间");
            chatService.handleChatSend(r, uid, payload, ack);
        }
    }

    @OnEvent("room:join")
    public void onRoomJoin(SocketIOClient c, AckRequest ackReq, ChatSend payload) {
        log.info("onRoomJoin sid={}", c.getSessionId());
        var ack = rt.ackOf(ackReq);
        String room = payload.getRoom();
        if (room == null || room.isBlank()) {
            ack.fail("BAD_REQ", "room 必填");
            return;
        }
        if (checkRoom(c, payload)) { // 已在房间 -> 报错
            ack.fail("BAD_REQ", "你已经在房间内了");
            return;
        }
        String roomId = rt.groupRoom(room);
        rt.connOf(c).join(roomId);
        String uid = (String) c.get("uid");
        payload.setMsg(uid + payload.getMsg());
        chatService.handleChatSend(roomId, uid, payload, ack);
    }

    @OnEvent("room:leave")
    public void onRoomLeave(SocketIOClient c, AckRequest ackReq, ChatSend payload) {
        log.info("onRoomLeave sid={}", c.getSessionId());
        var ack = rt.ackOf(ackReq);
        String room = payload.getRoom();
        if (room == null || room.isBlank()) {
            ack.fail("BAD_REQ", "room 必填");
            return;
        }
        if (!checkRoom(c, payload)) {
            ack.fail("BAD_REQ", "你不在这个房间内");
            return;
        }
        String roomId = rt.groupRoom(room);
        String uid = (String) c.get("uid");
        payload.setMsg(uid + payload.getMsg());
        chatService.handleChatSend(roomId, uid, payload, ack);
        rt.connOf(c).leave(roomId);
    }

    @OnEvent("chat:send")
    public void onChatSend(SocketIOClient c, AckRequest ackReq, ChatSend payload) {
        log.info("onChatSend sid={}, data={}", c.getSessionId(), payload);
        var ack = rt.ackOf(ackReq);
        String uid = (String) c.get("uid");

        String roomId = getRoomId(ackReq, payload);
        if (roomId.isEmpty()) return; // 已在 getRoomId 内反馈错误

        // 仅当是群聊时校验是否在房间；私聊不需要
        if (roomId.startsWith("g:") && !c.getAllRooms().contains(roomId)) {
            if (ackReq.isAckRequested()) {
                ackReq.sendAckData(Map.of("ok", false, "err", "NOT_IN_ROOM"));
            }
            return;
        }

        chatService.handleChatSend(roomId, uid, payload, ack);
    }

    // payload.room 非空 -> 将逻辑房间名转为真实房间ID并检查是否在其中
    private boolean checkRoom(SocketIOClient c, ChatSend payload) {
        if (payload.getRoom() != null && !payload.getRoom().isBlank()) {
            String roomId = rt.groupRoom(payload.getRoom());
            return c.getAllRooms().contains(roomId);
        }
        return false;
    }

    // 统一返回发送目标：优先 to(私聊)，否则 room(群聊)；错误时用 ack.fail 并返回 ""
    private String getRoomId(AckRequest ackReq, ChatSend payload) {
        var ack = rt.ackOf(ackReq);
        boolean hasTo = payload.getTo() != null && !payload.getTo().isBlank();
        boolean hasRoom = payload.getRoom() != null && !payload.getRoom().isBlank();

        if (!hasTo && !hasRoom) {
            ack.fail("BAD_REQ", "to 或 room 必填其一");
            return "";
        }
        return hasTo ? rt.userRoom(payload.getTo()) : rt.groupRoom(payload.getRoom());
    }
}