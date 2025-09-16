package com.chat.chatbackend.socket.dto;
import lombok.Builder;
import lombok.Data;

@Data
public class ChatSend {
    private String to;    // 单聊目标uid
    private String room = "";  // 群聊房间号
    private String msg;   // 消息
}
