package com.rain.chatroom.server.dao;

import com.rain.chatroom.common.db.DatabaseConnection;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class UserDao {

    public boolean createUser(String username, String password, String nickname, String email) {
        String sql = "INSERT INTO users (username, password, nickname, email) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password); // 注意：实际应用中应该加密存储
            pstmt.setString(3, nickname);
            pstmt.setString(4, email);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            log.error("创建用户失败: {}", e.getMessage());
            return false;
        }
    }

    public User findUserByUsername(String username) {
        String sql = "SELECT id, username, password, nickname, email, avatar, status, last_login_time, created_time " +
                "FROM users WHERE username = ? AND status = 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                User user = new User();
                user.setId(rs.getLong("id"));
                user.setUsername(rs.getString("username"));
                user.setPassword(rs.getString("password"));
                user.setNickname(rs.getString("nickname"));
                user.setEmail(rs.getString("email"));
                user.setAvatar(rs.getString("avatar"));
                user.setStatus(rs.getInt("status"));
                user.setLastLoginTime(rs.getTimestamp("last_login_time"));
                user.setCreatedTime(rs.getTimestamp("created_time"));
                return user;
            }

        } catch (SQLException e) {
            log.error("查询用户失败: {}", e.getMessage());
        }

        return null;
    }

    public boolean updateUserLoginTime(long userId) {
        String sql = "UPDATE users SET last_login_time = CURRENT_TIMESTAMP WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            log.error("更新用户登录时间失败: {}", e.getMessage());
            return false;
        }
    }

    public List<User> searchUsers(String keyword) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, username, nickname, avatar FROM users " +
                "WHERE (username LIKE ? OR nickname LIKE ?) AND status = 1 LIMIT 20";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String likeKeyword = "%" + keyword + "%";
            pstmt.setString(1, likeKeyword);
            pstmt.setString(2, likeKeyword);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                User user = new User();
                user.setId(rs.getLong("id"));
                user.setUsername(rs.getString("username"));
                user.setNickname(rs.getString("nickname"));
                user.setAvatar(rs.getString("avatar"));
                users.add(user);
            }

        } catch (SQLException e) {
            log.error("搜索用户失败: {}", e.getMessage());
        }

        return users;
    }

    public static class User {
        private Long id;
        private String username;
        private String password;
        private String nickname;
        private String email;
        private String avatar;
        private Integer status;
        private Timestamp lastLoginTime;
        private Timestamp createdTime;

        // Getter和Setter方法
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getAvatar() { return avatar; }
        public void setAvatar(String avatar) { this.avatar = avatar; }

        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }

        public Timestamp getLastLoginTime() { return lastLoginTime; }
        public void setLastLoginTime(Timestamp lastLoginTime) { this.lastLoginTime = lastLoginTime; }

        public Timestamp getCreatedTime() { return createdTime; }
        public void setCreatedTime(Timestamp createdTime) { this.createdTime = createdTime; }
    }
}