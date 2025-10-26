package com.rain.chatroom.server.service;

import com.rain.chatroom.server.dao.FriendDao;
import com.rain.chatroom.server.dao.GroupDao;
import com.rain.chatroom.server.dao.MessageDao;
import com.rain.chatroom.server.dao.UserDao;
import com.rain.chatroom.server.handler.ClientSession;
import com.rain.chatroom.server.manager.SessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.List;

/**
 * 广播服务 - 负责消息的广播和定向发送
 */
//BroadcastService：负责消息的广播和定向发送，它依赖于SessionManager来获取所有会话。
@Slf4j
@RequiredArgsConstructor
public class BroadcastService {
    //广播的本质还是靠sessionManageer去遍历所有的clientSession，这样才能给所有客户端发消息
    private final SessionManager sessionManager;

    // 在BroadcastService中添加
    private final FriendDao friendDao = new FriendDao();
    private final GroupDao groupDao = new GroupDao();

    // 初始化 UserDao 实例
    private final UserDao userDao = new UserDao();

    private final MessageDao messageDao = new MessageDao();

    public void broadcastToAll(String message) {
        broadcastToAll(message, null);
    }

    //遍历所有在线的session，除了自己都发消息
    public void broadcastToAll(String message, ClientSession excludeSession) {
        Collection<ClientSession> sessions = sessionManager.getAllSessions();

        int sentCount = 0;
        for (ClientSession session : sessions) {
            if (session != excludeSession && session.isActive()) {
                session.sendMessage(message);
                sentCount++;
            }
        }

        log.debug("广播消息: {}, 接收者: {}", message, sentCount);
    }

    //私聊：发消息给指定用户
    public void sendToUser(String username, String message) {
        //流遍历，过滤实现
        sessionManager.getAllSessions().stream()
                .filter(session -> username.equals(session.getUsername()) && session.isActive())
                .forEach(session -> session.sendMessage(message));
    }

    // 修改sendPrivateMessage方法
    public boolean sendPrivateMessage(String fromUser, String toUser, String message) {
        // 查找发送者用户信息
        UserDao.User fromUserObj = null;
        UserDao.User toUserObj = null;

        // 从在线用户中查找
        for (ClientSession session : sessionManager.getAllSessions()) {
            if (fromUser.equals(session.getUsername())) {
                fromUserObj = session.getUser();
            }
            if (toUser.equals(session.getUsername())) {
                toUserObj = session.getUser();
            }
        }

        // 如果在线用户中没找到，从数据库查找
        if (fromUserObj == null) {
            fromUserObj = userDao.findUserByUsername(fromUser);
        }
        if (toUserObj == null) {
            toUserObj = userDao.findUserByUsername(toUser);
        }

        if (fromUserObj == null || toUserObj == null) {
            return false;
        }

        // 保存私聊消息到数据库
        messageDao.savePrivateMessage(fromUserObj.getId(), toUserObj.getId(), message);

        // 发送给接收者（如果在线）
        boolean targetOnline = false;
        for (ClientSession session : sessionManager.getAllSessions()) {
            if (toUser.equals(session.getUsername()) && session.isActive()) {
                session.sendMessage("[私聊][" + fromUser + "->你]: " + message);
                targetOnline = true;
            }
        }

        // 给发送者反馈
        for (ClientSession session : sessionManager.getAllSessions()) {
            if (fromUser.equals(session.getUsername()) && session.isActive()) {
                String status = targetOnline ? "✓" : "✗";
                session.sendMessage("[私聊][你->" + toUser + "]" + status + ": " + message);
                break;
            }
        }

        log.info("私聊消息: {} -> {}: {}", fromUser, toUser, message);
        return targetOnline;
    }

    public void sendSystemMessage(String message) {
        String formattedMessage = "[系统] " + message;
        broadcastToAll(formattedMessage);
        log.info("系统消息: {}", message);
    }

    // 扩展handleCommand方法
    public void handleCommand(ClientSession session, String command) {
        String[] parts = command.split(" ", 3);
        String cmd = parts[0].toLowerCase();
        String param1 = parts.length > 1 ? parts[1] : "";
        String param2 = parts.length > 2 ? parts[2] : "";

        switch (cmd) {
            case "/users":
                listOnlineUsers(session);
                break;
            case "/stats":
                showStats(session);
                break;
            case "/help":
                showHelp(session);
                break;
            case "/msg":
            case "/whisper":
                handlePrivateMessage(session, param1, param2);
                break;
            case "/addfriend":
                handleAddFriend(session, param1);
                break;
            case "/delfriend":
                handleDeleteFriend(session, param1);
                break;
            case "/friends":
                handleListFriends(session);
                break;
            case "/creategroup":
                handleCreateGroup(session, param1, param2);
                break;
            case "/joingroup":
                handleJoinGroup(session, param1);
                break;
            case "/groups":
                handleListGroups(session);
                break;
            case "/groupmsg":
                handleGroupMessage(session, param1, param2);
                break;
            default:
                session.sendMessage("[系统] 未知命令: " + cmd + "，输入 /help 查看帮助");
        }
    }

    private void handlePrivateMessage(ClientSession session, String targetUser, String message) {
        if (targetUser.isEmpty() || message.isEmpty()) {
            session.sendMessage("[系统] 用法: /msg 用户名 消息内容");
            return;
        }

        if (targetUser.equals(session.getUsername())) {
            session.sendMessage("[系统] 不能给自己发送私聊消息");
            return;
        }

        if (!sendPrivateMessage(session.getUsername(), targetUser, message)) {
            session.sendMessage("[系统] 用户 " + targetUser + " 不在线，消息已保存");
        }
    }

    private void handleAddFriend(ClientSession session, String friendUsername) {
        if (friendUsername.isEmpty()) {
            session.sendMessage("[系统] 用法: /addfriend 用户名");
            return;
        }

        if (friendUsername.equals(session.getUsername())) {
            session.sendMessage("[系统] 不能添加自己为好友");
            return;
        }

        UserDao.User currentUser = session.getUser();
        UserDao.User friendUser = userDao.findUserByUsername(friendUsername);

        if (friendUser == null) {
            session.sendMessage("[系统] 用户 " + friendUsername + " 不存在");
            return;
        }

        if (friendDao.isFriend(currentUser.getId(), friendUser.getId())) {
            session.sendMessage("[系统] " + friendUsername + " 已经是你的好友");
            return;
        }

        if (friendDao.addFriend(currentUser.getId(), friendUser.getId(), friendUser.getNickname())) {
            session.sendMessage("[系统] 成功添加 " + friendUsername + " 为好友");
        } else {
            session.sendMessage("[系统] 添加好友失败");
        }
    }

    private void handleDeleteFriend(ClientSession session, String friendUsername) {
        if (friendUsername.isEmpty()) {
            session.sendMessage("[系统] 用法: /delfriend 用户名");
            return;
        }

        UserDao.User currentUser = session.getUser();
        UserDao.User friendUser = userDao.findUserByUsername(friendUsername);

        if (friendUser == null) {
            session.sendMessage("[系统] 用户 " + friendUsername + " 不存在");
            return;
        }

        if (!friendDao.isFriend(currentUser.getId(), friendUser.getId())) {
            session.sendMessage("[系统] " + friendUsername + " 不是你的好友");
            return;
        }

        if (friendDao.removeFriend(currentUser.getId(), friendUser.getId())) {
            session.sendMessage("[系统] 成功删除好友 " + friendUsername);
        } else {
            session.sendMessage("[系统] 删除好友失败");
        }
    }

    private void handleListFriends(ClientSession session) {
        UserDao.User currentUser = session.getUser();
        List<FriendDao.FriendInfo> friends = friendDao.getFriends(currentUser.getId());

        if (friends.isEmpty()) {
            session.sendMessage("[系统] 你还没有好友，使用 /addfriend 用户名 添加好友");
            return;
        }

        StringBuilder friendList = new StringBuilder("[系统] 好友列表:\n");
        for (FriendDao.FriendInfo friend : friends) {
            String status = friend.isOnline() ? "🟢" : "⚫";
            friendList.append(String.format("%s %s (%s)\n", status,
                    friend.getFriendNickname(), friend.getUsername()));
        }

        session.sendMessage(friendList.toString());
    }

    private void handleCreateGroup(ClientSession session, String groupName, String description) {
        if (groupName.isEmpty()) {
            session.sendMessage("[系统] 用法: /creategroup 群组名称 [描述]");
            return;
        }

        UserDao.User currentUser = session.getUser();
        Long groupId = groupDao.createGroup(groupName, description, currentUser.getId());

        if (groupId != null) {
            session.sendMessage("[系统] 群组 '" + groupName + "' 创建成功，ID: " + groupId);
        } else {
            session.sendMessage("[系统] 创建群组失败");
        }
    }

    private void handleJoinGroup(ClientSession session, String groupIdStr) {
        if (groupIdStr.isEmpty()) {
            session.sendMessage("[系统] 用法: /joingroup 群组ID");
            return;
        }

        try {
            Long groupId = Long.parseLong(groupIdStr);
            UserDao.User currentUser = session.getUser();

            // 检查是否已经是群成员
            // 这里简化处理，实际应该检查群是否存在等

            if (groupDao.addGroupMember(groupId, currentUser.getId(), 0)) {
                session.sendMessage("[系统] 成功加入群组 " + groupId);
            } else {
                session.sendMessage("[系统] 加入群组失败");
            }

        } catch (NumberFormatException e) {
            session.sendMessage("[系统] 群组ID必须是数字");
        }
    }

    private void handleListGroups(ClientSession session) {
        UserDao.User currentUser = session.getUser();
        List<GroupDao.GroupInfo> groups = groupDao.getUserGroups(currentUser.getId());

        if (groups.isEmpty()) {
            session.sendMessage("[系统] 你还没有加入任何群组，使用 /creategroup 创建群组或 /joingroup 加入群组");
            return;
        }

        StringBuilder groupList = new StringBuilder("[系统] 我的群组:\n");
        for (GroupDao.GroupInfo group : groups) {
            String role = "";
            switch (group.getRole()) {
                case 2: role = "👑"; break; // 群主
                case 1: role = "⭐"; break; // 管理员
                default: role = "👤"; break; // 普通成员
            }
            groupList.append(String.format("%s %s (ID:%d) - %d人\n", role,
                    group.getGroupName(), group.getId(), group.getMemberCount()));
        }

        session.sendMessage(groupList.toString());
    }

    private void handleGroupMessage(ClientSession session, String groupIdStr, String message) {
        if (groupIdStr.isEmpty() || message.isEmpty()) {
            session.sendMessage("[系统] 用法: /groupmsg 群组ID 消息内容");
            return;
        }

        try {
            Long groupId = Long.parseLong(groupIdStr);
            // 这里应该检查用户是否在群组中

            // 获取群组成员
            List<GroupDao.GroupMember> members = groupDao.getGroupMembers(groupId);

            // 发送群消息
            String groupMessage = "[群聊][" + session.getUsername() + "]: " + message;
            for (GroupDao.GroupMember member : members) {
                // 查找在线的群成员
                for (ClientSession clientSession : sessionManager.getAllSessions()) {
                    if (member.getUserId().equals(clientSession.getUser().getId()) &&
                            clientSession.isActive()) {
                        clientSession.sendMessage(groupMessage);
                    }
                }
            }

            // 保存群消息到数据库
            messageDao.saveMessage(2, session.getUser().getId(), null, groupId, message, 1, null);

            session.sendMessage("[系统] 群消息发送成功");

        } catch (NumberFormatException e) {
            session.sendMessage("[系统] 群组ID必须是数字");
        }
    }


    private void listOnlineUsers(ClientSession session) {
        Collection<ClientSession> sessions = sessionManager.getAllSessions();
        StringBuilder userList = new StringBuilder("[系统] 在线用户 (" + sessions.size() + "): ");

        for (ClientSession s : sessions) {
            userList.append(s.getUsername()).append(", ");
        }

        if (sessions.size() > 0) {
            userList.setLength(userList.length() - 2); // 移除最后的逗号和空格
        }

        session.sendMessage(userList.toString());
    }

    //在线用户数：通过session.size()获取
    private void showStats(ClientSession session) {
        Collection<ClientSession> sessions = sessionManager.getAllSessions();
        session.sendMessage("[系统] 服务器状态 - 在线用户: " + sessions.size());
    }

    private void handlePrivateMessageCommand(ClientSession session, String param) {
        if (param.isEmpty()) {
            session.sendMessage("[系统] 用法: /msg 用户名 消息内容");
            return;
        }

        String[] msgParts = param.split(" ", 2);
        if (msgParts.length < 2) {
            session.sendMessage("[系统] 用法: /msg 用户名 消息内容");
            return;
        }

        String targetUser = msgParts[0];
        String privateMsg = msgParts[1];
        if (sendPrivateMessage(session.getUsername(), targetUser, privateMsg)) {
            log.info("{} 发送私聊给 {}: {}", session.getUsername(), targetUser, privateMsg);
        } else {
            session.sendMessage("[系统] 用户 " + targetUser + " 不在线或不存在");
        }
    }

    // 更新帮助信息
    private void showHelp(ClientSession session) {
        String help = "[系统] 可用命令:\n\n" +
                "基础命令:\n" +
                "/help          - 显示此帮助信息\n" +
                "/users         - 查看在线用户\n" +
                "/stats         - 查看服务器状态\n\n" +
                "私聊命令:\n" +
                "/msg 用户 消息  - 发送私聊消息\n" +
                "/whisper 用户 消息 - 发送私聊消息\n\n" +
                "好友命令:\n" +
                "/addfriend 用户 - 添加好友\n" +
                "/delfriend 用户 - 删除好友  \n" +
                "/friends        - 查看好友列表\n\n" +
                "群组命令:\n" +
                "/creategroup 名称 [描述] - 创建群组\n" +
                "/joingroup ID   - 加入群组\n" +
                "/groups         - 查看我的群组\n" +
                "/groupmsg ID 消息 - 发送群消息\n";

        session.sendMessage(help);
    }

}