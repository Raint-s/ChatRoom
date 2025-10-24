package com.rain.chatroom.server.dao;

import com.rain.chatroom.common.db.DatabaseConnection;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class MessageDao {

    public boolean saveMessage(int messageType, Long fromUserId, Long toUserId, Long groupId,
                               String content, int contentType, String fileUrl) {
        String sql = "INSERT INTO chat_messages (message_type, from_user_id, to_user_id, group_id, " +
                "content, content_type, file_url) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, messageType);
            pstmt.setObject(2, fromUserId);
            pstmt.setObject(3, toUserId);
            pstmt.setObject(4, groupId);
            pstmt.setString(5, content);
            pstmt.setInt(6, contentType);
            pstmt.setString(7, fileUrl);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            log.error("保存消息失败: {}", e.getMessage());
            return false;
        }
    }

    public List<ChatMessage> getPrivateMessageHistory(Long user1Id, Long user2Id, int limit) {
        List<ChatMessage> messages = new ArrayList<>();
        String sql = "SELECT cm.*, u.nickname as from_nickname " +
                "FROM chat_messages cm " +
                "LEFT JOIN users u ON cm.from_user_id = u.id " +
                "WHERE ((cm.from_user_id = ? AND cm.to_user_id = ?) OR " +
                "       (cm.from_user_id = ? AND cm.to_user_id = ?)) " +
                "AND cm.message_type = 1 " +
                "ORDER BY cm.created_time DESC LIMIT ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, user1Id);
            pstmt.setLong(2, user2Id);
            pstmt.setLong(3, user2Id);
            pstmt.setLong(4, user1Id);
            pstmt.setInt(5, limit);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                ChatMessage message = new ChatMessage();
                message.setId(rs.getLong("id"));
                message.setMessageType(rs.getInt("message_type"));
                message.setFromUserId(rs.getLong("from_user_id"));
                message.setToUserId(rs.getLong("to_user_id"));
                message.setFromNickname(rs.getString("from_nickname"));
                message.setContent(rs.getString("content"));
                message.setContentType(rs.getInt("content_type"));
                message.setFileUrl(rs.getString("file_url"));
                message.setCreatedTime(rs.getTimestamp("created_time"));
                messages.add(message);
            }

        } catch (SQLException e) {
            log.error("获取私聊历史失败: {}", e.getMessage());
        }

        return messages;
    }

    public static class ChatMessage {
        private Long id;
        private Integer messageType;
        private Long fromUserId;
        private Long toUserId;
        private Long groupId;
        private String fromNickname;
        private String content;
        private Integer contentType;
        private String fileUrl;
        private Timestamp createdTime;

        // Getter和Setter方法
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public Integer getMessageType() { return messageType; }
        public void setMessageType(Integer messageType) { this.messageType = messageType; }

        public Long getFromUserId() { return fromUserId; }
        public void setFromUserId(Long fromUserId) { this.fromUserId = fromUserId; }

        public Long getToUserId() { return toUserId; }
        public void setToUserId(Long toUserId) { this.toUserId = toUserId; }

        public Long getGroupId() { return groupId; }
        public void setGroupId(Long groupId) { this.groupId = groupId; }

        public String getFromNickname() { return fromNickname; }
        public void setFromNickname(String fromNickname) { this.fromNickname = fromNickname; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public Integer getContentType() { return contentType; }
        public void setContentType(Integer contentType) { this.contentType = contentType; }

        public String getFileUrl() { return fileUrl; }
        public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

        public Timestamp getCreatedTime() { return createdTime; }
        public void setCreatedTime(Timestamp createdTime) { this.createdTime = createdTime; }
    }
}