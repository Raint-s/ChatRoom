package com.rain.chatroom.server.dao;

import com.rain.chatroom.common.db.DatabaseConnection;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class GroupDao {

    public Long createGroup(String groupName, String description, Long creatorId) {
        String sql = "INSERT INTO chat_groups (group_name, description, creator_id) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, groupName);
            pstmt.setString(2, description);
            pstmt.setLong(3, creatorId);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    Long groupId = rs.getLong(1);
                    // 创建者自动加入群组
                    addGroupMember(groupId, creatorId, 2); // 2表示群主
                    return groupId;
                }
            }

        } catch (SQLException e) {
            log.error("创建群组失败: {}", e.getMessage());
        }

        return null;
    }

    public boolean addGroupMember(Long groupId, Long userId, int role) {
        String sql = "INSERT INTO group_members (group_id, user_id, role) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, groupId);
            pstmt.setLong(2, userId);
            pstmt.setInt(3, role);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            log.error("添加群组成员失败: {}", e.getMessage());
            return false;
        }
    }

    public List<GroupInfo> getUserGroups(Long userId) {
        List<GroupInfo> groups = new ArrayList<>();
        String sql = "SELECT g.id, g.group_name, g.description, g.creator_id, gm.role, " +
                "u.nickname as creator_name, " +
                "(SELECT COUNT(*) FROM group_members WHERE group_id = g.id) as member_count " +
                "FROM chat_groups g " +
                "JOIN group_members gm ON g.id = gm.group_id " +
                "JOIN users u ON g.creator_id = u.id " +
                "WHERE gm.user_id = ? AND g.status = 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                GroupInfo group = new GroupInfo();
                group.setId(rs.getLong("id"));
                group.setGroupName(rs.getString("group_name"));
                group.setDescription(rs.getString("description"));
                group.setCreatorId(rs.getLong("creator_id"));
                group.setCreatorName(rs.getString("creator_name"));
                group.setRole(rs.getInt("role"));
                group.setMemberCount(rs.getInt("member_count"));
                groups.add(group);
            }

        } catch (SQLException e) {
            log.error("获取用户群组失败: {}", e.getMessage());
        }

        return groups;
    }

    public List<GroupMember> getGroupMembers(Long groupId) {
        List<GroupMember> members = new ArrayList<>();
        String sql = "SELECT gm.user_id, gm.role, gm.join_time, u.username, u.nickname, u.avatar " +
                "FROM group_members gm " +
                "JOIN users u ON gm.user_id = u.id " +
                "WHERE gm.group_id = ? " +
                "ORDER BY gm.role DESC, gm.join_time ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, groupId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                GroupMember member = new GroupMember();
                member.setUserId(rs.getLong("user_id"));
                member.setRole(rs.getInt("role"));
                member.setJoinTime(rs.getTimestamp("join_time"));
                member.setUsername(rs.getString("username"));
                member.setNickname(rs.getString("nickname"));
                member.setAvatar(rs.getString("avatar"));
                members.add(member);
            }

        } catch (SQLException e) {
            log.error("获取群组成员失败: {}", e.getMessage());
        }

        return members;
    }

    public static class GroupInfo {
        private Long id;
        private String groupName;
        private String description;
        private Long creatorId;
        private String creatorName;
        private Integer role;
        private Integer memberCount;

        // Getter和Setter方法
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getGroupName() { return groupName; }
        public void setGroupName(String groupName) { this.groupName = groupName; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public Long getCreatorId() { return creatorId; }
        public void setCreatorId(Long creatorId) { this.creatorId = creatorId; }

        public String getCreatorName() { return creatorName; }
        public void setCreatorName(String creatorName) { this.creatorName = creatorName; }

        public Integer getRole() { return role; }
        public void setRole(Integer role) { this.role = role; }

        public Integer getMemberCount() { return memberCount; }
        public void setMemberCount(Integer memberCount) { this.memberCount = memberCount; }
    }

    public static class GroupMember {
        private Long userId;
        private String username;
        private String nickname;
        private String avatar;
        private Integer role;
        private Timestamp joinTime;

        // Getter和Setter方法
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }

        public String getAvatar() { return avatar; }
        public void setAvatar(String avatar) { this.avatar = avatar; }

        public Integer getRole() { return role; }
        public void setRole(Integer role) { this.role = role; }

        public Timestamp getJoinTime() { return joinTime; }
        public void setJoinTime(Timestamp joinTime) { this.joinTime = joinTime; }
    }
}