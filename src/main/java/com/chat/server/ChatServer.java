package com.chat.server;

import com.chat.dao.FriendDAO;
import com.chat.dao.MessageDAO;
import com.chat.dao.UserDAO;
import com.chat.models.Message;
import com.chat.models.Packet;
import com.chat.models.User;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer extends WebSocketServer {
    
    private Map<String, WebSocket> activeUsers = new ConcurrentHashMap<>();
    private Map<WebSocket, String> socketToUser = new ConcurrentHashMap<>();
    private Gson gson = new Gson();

    public ChatServer(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("New browser connection: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String username = socketToUser.remove(conn);
        if (username != null) {
            activeUsers.remove(username);
            UserDAO.updateLastSeen(username); // Update last seen on disconnect
            System.out.println(username + " disconnected.");
            broadcastActiveUsers();
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        Packet packet = gson.fromJson(message, Packet.class);
        
        if ("REGISTER".equals(packet.getType())) {
            String[] parts = packet.getContent().split(":", 2);
            boolean success = false;
            if (parts.length == 2) success = UserDAO.registerUser(packet.getSender(), parts[0], parts[1]);
            Packet response = new Packet("REGISTER_RESPONSE", "SERVER", packet.getSender(), success ? "SUCCESS" : "FAIL");
            conn.send(gson.toJson(response));

        } else if ("LOGIN".equals(packet.getType())) {
            User user = UserDAO.loginUser(packet.getSender(), packet.getContent());
            if (user != null) {
                activeUsers.put(user.getUsername(), conn);
                socketToUser.put(conn, user.getUsername());
                
                Packet response = new Packet("LOGIN_RESPONSE", "SERVER", user.getUsername(), "SUCCESS");
                conn.send(gson.toJson(response));
                
                broadcastActiveUsers();
                sendGlobalHistory(conn);
                sendFriendsAndBlockedList(conn, user.getUsername());
                
                // Fetch info to send self profile pic
                Packet infoPacket = new Packet("USER_INFO", "SERVER", user.getUsername(), "");
                infoPacket.setProfilePic(user.getProfilePic());
                conn.send(gson.toJson(infoPacket));
            } else {
                Packet response = new Packet("LOGIN_RESPONSE", "SERVER", packet.getSender(), "FAIL");
                conn.send(gson.toJson(response));
            }
            
        } else if ("SEARCH_USERS".equals(packet.getType())) {
            List<String> results = UserDAO.searchUsers(packet.getContent(), packet.getSender());
            Packet response = new Packet("SEARCH_RESULTS", "SERVER", packet.getSender(), gson.toJson(results));
            conn.send(gson.toJson(response));

        } else if ("SEND_REQUEST".equals(packet.getType())) {
            String receiverUser = packet.getContent();
            String result = FriendDAO.sendRequest(packet.getSender(), receiverUser);
            if ("SUCCESS".equals(result)) {
                Packet notif = new Packet("NEW_REQUEST", packet.getSender(), receiverUser, "");
                sendMessageToUser(receiverUser, gson.toJson(notif));
            }
            
        } else if ("ACCEPT_REQUEST".equals(packet.getType())) {
            String requester = packet.getContent();
            boolean success = FriendDAO.acceptRequest(requester, packet.getSender());
            if (success) {
                Packet notif = new Packet("REQUEST_ACCEPTED", packet.getSender(), requester, "");
                sendMessageToUser(requester, gson.toJson(notif));
                sendFriendsAndBlockedList(conn, packet.getSender());
                WebSocket reqConn = activeUsers.get(requester);
                if (reqConn != null) sendFriendsAndBlockedList(reqConn, requester);
            }

        } else if ("BLOCK_USER".equals(packet.getType())) {
            String toBlock = packet.getContent();
            FriendDAO.blockUser(packet.getSender(), toBlock);
            sendFriendsAndBlockedList(conn, packet.getSender());

        } else if ("UNBLOCK_USER".equals(packet.getType())) {
            String toUnblock = packet.getContent();
            FriendDAO.unblockUser(packet.getSender(), toUnblock);
            sendFriendsAndBlockedList(conn, packet.getSender());

        } else if ("GET_FRIENDS".equals(packet.getType())) {
            sendFriendsAndBlockedList(conn, packet.getSender());

        } else if ("GET_PRIVATE_HISTORY".equals(packet.getType())) {
            sendPrivateHistory(conn, packet.getSender(), packet.getContent());

        } else if ("TEXT".equals(packet.getType())) {
            int msgId = saveMessageToDB(packet.getSender(), packet.getReceiver(), packet.getContent(), packet.getReplyToId());
            packet.setId(msgId);
            packet.setMessageStatus(activeUsers.containsKey(packet.getReceiver()) ? "DELIVERED" : "SENT");
            
            // Immediately update DB if it's delivered
            if ("DELIVERED".equals(packet.getMessageStatus())) {
                MessageDAO.updateMessageStatus(msgId, "DELIVERED");
            }
            
            String jsonStr = gson.toJson(packet);
            sendMessageToUser(packet.getReceiver(), jsonStr); 
            sendMessageToUser(packet.getSender(), jsonStr);

        } else if ("BROADCAST".equals(packet.getType())) {
            int msgId = saveMessageToDB(packet.getSender(), null, packet.getContent(), packet.getReplyToId());
            packet.setId(msgId);
            broadcast(gson.toJson(packet));
            
        } else if ("TYPING".equals(packet.getType()) || "STOP_TYPING".equals(packet.getType())) {
            sendMessageToUser(packet.getReceiver(), message);
            
        } else if ("DELETE_MESSAGE".equals(packet.getType())) {
            int msgId = Integer.parseInt(packet.getContent());
            MessageDAO.deleteMessage(msgId, packet.getSender());
            sendMessageToUser(packet.getSender(), message);
            sendMessageToUser(packet.getReceiver(), message);
            
        } else if ("REACT_MESSAGE".equals(packet.getType())) {
            int msgId = packet.getId();
            MessageDAO.addReaction(msgId, packet.getSender(), packet.getContent());
            sendMessageToUser(packet.getSender(), message);
            sendMessageToUser(packet.getReceiver(), message);
            
        } else if ("UPDATE_MESSAGE_STATUS".equals(packet.getType())) {
            int msgId = packet.getId();
            String newStatus = packet.getContent(); // e.g. "READ"
            MessageDAO.updateMessageStatus(msgId, newStatus);
            // Notify original sender that their message was read
            sendMessageToUser(packet.getReceiver(), message);

        } else if ("UPDATE_PROFILE_PIC".equals(packet.getType())) {
            UserDAO.updateProfilePic(packet.getSender(), packet.getContent());
            // Broadcast new pic
            broadcast(message);

        } else if ("GET_USER_INFO".equals(packet.getType())) {
            User u = UserDAO.getUserByUsername(packet.getContent());
            if (u != null) {
                Packet infoPacket = new Packet("USER_INFO", "SERVER", packet.getSender(), packet.getContent());
                infoPacket.setProfilePic(u.getProfilePic());
                infoPacket.setLastSeen(u.getLastSeen() != null ? u.getLastSeen().getTime() : 0);
                conn.send(gson.toJson(infoPacket));
            }
        }
        
        else if ("CALL_OFFER".equals(packet.getType()) || 
                   "CALL_ANSWER".equals(packet.getType()) || 
                   "ICE_CANDIDATE".equals(packet.getType())) {
            sendMessageToUser(packet.getReceiver(), message);
        }
    }

    private void sendFriendsAndBlockedList(WebSocket conn, String username) {
        List<String> friends = FriendDAO.getFriends(username);
        List<String> pending = FriendDAO.getPendingRequests(username);
        List<String> blocked = FriendDAO.getBlockedUsers(username);
        
        conn.send(gson.toJson(new Packet("FRIENDS_LIST", "SERVER", username, gson.toJson(friends))));
        conn.send(gson.toJson(new Packet("PENDING_REQUESTS", "SERVER", username, gson.toJson(pending))));
        conn.send(gson.toJson(new Packet("BLOCKED_USERS", "SERVER", username, gson.toJson(blocked))));
    }

    private void sendGlobalHistory(WebSocket conn) {
        List<Message> history = MessageDAO.getGlobalChatHistory();
        List<Packet> packets = new ArrayList<>();
        for (Message m : history) {
            Packet p = new Packet("BROADCAST", m.getSender().getUsername(), "ALL", m.getContent());
            p.setId(m.getId());
            p.setReactions(m.getReactions());
            p.setReplyToId(m.getReplyToId());
            packets.add(p);
        }
        conn.send(gson.toJson(new Packet("HISTORY_GLOBAL", "SERVER", "ALL", gson.toJson(packets))));
    }

    private void sendPrivateHistory(WebSocket conn, String user1, String user2) {
        List<Message> history = MessageDAO.getPrivateChatHistory(user1, user2);
        List<Packet> packets = new ArrayList<>();
        for (Message m : history) {
            Packet p = new Packet("TEXT", m.getSender().getUsername(), m.getReceiver().getUsername(), m.getContent());
            p.setId(m.getId());
            p.setReactions(m.getReactions());
            p.setReplyToId(m.getReplyToId());
            p.setMessageStatus(m.getMessageStatus());
            packets.add(p);
        }
        conn.send(gson.toJson(new Packet("HISTORY_PRIVATE", "SERVER", user1, gson.toJson(packets))));
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {}

    @Override
    public void onStart() {
        System.out.println("WebSocket Server successfully started on port: " + getPort());
        setConnectionLostTimeout(100);
    }

    private void sendMessageToUser(String username, String jsonMessage) {
        WebSocket conn = activeUsers.get(username);
        if (conn != null && conn.isOpen()) {
            conn.send(jsonMessage);
        }
    }

    private void broadcastActiveUsers() {
        String usersJson = gson.toJson(activeUsers.keySet());
        Packet packet = new Packet("USER_LIST", "SERVER", "ALL", usersJson);
        broadcast(gson.toJson(packet));
    }

    private int saveMessageToDB(String sender, String receiver, String content, Integer replyToId) {
        User senderUser = UserDAO.getUserByUsername(sender);
        User receiverUser = receiver != null && !receiver.equals("ALL") ? UserDAO.getUserByUsername(receiver) : null;
        if (senderUser != null) {
            Message msg = new Message(senderUser, receiverUser, content);
            msg.setReplyToId(replyToId);
            msg.setMessageStatus(receiverUser != null && activeUsers.containsKey(receiverUser.getUsername()) ? "DELIVERED" : "SENT");
            return MessageDAO.saveMessage(msg);
        }
        return -1;
    }

    public static void main(String[] args) {
        String portEnv = System.getenv("PORT");
        int port = (portEnv != null && !portEnv.isEmpty()) ? Integer.parseInt(portEnv) : 5000;
        ChatServer server = new ChatServer(port);
        server.start();
        System.out.println("WebSocket Server is running on port " + port);
    }
}
