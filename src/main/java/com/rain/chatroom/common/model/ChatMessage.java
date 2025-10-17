package com.rain.chatroom.common.model;

public class ChatMessage {
    private MessageType type;
    private String fromUser;
    private String toUser;
    private String content;
    private long timestamp;

    public enum MessageType {
        TEXT, SYSTEM, COMMAND, PRIVATE
    }

    // 构造器
    public ChatMessage() {
        this.timestamp = System.currentTimeMillis();
    }

    public ChatMessage(MessageType type, String fromUser, String content) {
        this();
        this.type = type;
        this.fromUser = fromUser;
        this.content = content;
    }

    // Getter和Setter
    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }

    public String getFromUser() { return fromUser; }
    public void setFromUser(String fromUser) { this.fromUser = fromUser; }

    public String getToUser() { return toUser; }
    public void setToUser(String toUser) { this.toUser = toUser; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "type=" + type +
                ", fromUser='" + fromUser + '\'' +
                ", toUser='" + toUser + '\'' +
                ", content='" + content + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}