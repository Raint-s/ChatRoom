package com.rain.chatroom.server.dao;

import com.rain.chatroom.common.db.DatabaseConnection;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class FriendDao {

    public boolean addFriend(Long userId, Long friendId, String friendNickname) {
        String sql = "INSERT INTO user_friends (user_id, friend_id, friend_nickname) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            pstmt.setLong(2, friendId);
            pstmt.setString(3, friendNickname);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            log.error("添加好友失败: {}", e.getMessage());
            return false;
        }
    }

    public boolean removeFriend(Long userId, Long friendId) {
        String sql = "DELETE FROM user_friends WHERE user_id = ? AND friend_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            pstmt.setLong(2, friendId);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            log.error("删除好友失败: {}", e.getMessage());
            return false;
        }
    }

    public List<FriendInfo> getFriends(Long userId) {
        List<FriendInfo> friends = new ArrayList<>();
        String sql = "SELECT uf.friend_id, uf.friend_nickname, u.username, u.nickname, u.avatar, " +
                "CASE WHEN s.client_id IS NOT NULL THEN 1 ELSE 0 END as is_online " +
                "FROM user_friends uf " +
                "JOIN users u ON uf.friend_id = u.id " +
                "LEFT JOIN (SELECT DISTINCT user_id, client_id FROM session_info) s ON u.id = s.user_id " +
                "WHERE uf.user_id = ? AND uf.status = 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                FriendInfo friend = new FriendInfo();
                friend.setFriendId(rs.getLong("friend_id"));
                friend.setFriendNickname(rs.getString("friend_nickname"));
                friend.setUsername(rs.getString("username"));
                friend.setNickname(rs.getString("nickname"));
                friend.setAvatar(rs.getString("avatar"));
                friend.setOnline(rs.getInt("is_online") == 1);
                friends.add(friend);
            }

        } catch (SQLException e) {
            log.error("获取好友列表失败: {}", e.getMessage());
        }

        return friends;
    }

    public boolean isFriend(Long userId, Long friendId) {
        String sql = "SELECT COUNT(*) FROM user_friends WHERE user_id = ? AND friend_id = ? AND status = 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            pstmt.setLong(2, friendId);

            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;

        } catch (SQLException e) {
            log.error("检查好友关系失败: {}", e.getMessage());
            return false;
        }
    }

    public static class FriendInfo {
        private Long friendId;
        private String friendNickname;
        private String username;
        private String nickname;
        private String avatar;
        private boolean online;

        // Getter和Setter方法
        public Long getFriendId() { return friendId; }
        public void setFriendId(Long friendId) { this.friendId = friendId; }

        public String getFriendNickname() { return friendNickname; }
        public void setFriendNickname(String friendNickname) { this.friendNickname = friendNickname; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }

        public String getAvatar() { return avatar; }
        public void setAvatar(String avatar) { this.avatar = avatar; }

        public boolean isOnline() { return online; }
        public void setOnline(boolean online) { this.online = online; }
    }
}