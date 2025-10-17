package com.rain.chatroom.common.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    public enum MessageType {
        TEXT, SYSTEM, COMMAND, PRIVATE
    }

    private MessageType type;
    private String fromUser;
    private String toUser;
    private String content;
    private long timestamp = System.currentTimeMillis();

    public ChatMessage(MessageType type, String fromUser, String content) {
        this.type = type;
        this.fromUser = fromUser;
        this.content = content;
    }
}