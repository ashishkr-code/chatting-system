package com.chat.models;

public class Packet {
    private int id; // Added for message deletion & reactions
    private String type; 
    private String sender;
    private String receiver; 
    private String content; 
    private String reactions; // Added for reactions JSON

    public Packet(String type, String sender, String receiver, String content) {
        this.type = type;
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.reactions = "{}";
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getType() { return type; }
    public String getSender() { return sender; }
    public String getReceiver() { return receiver; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getReactions() { return reactions; }
    public void setReactions(String reactions) { this.reactions = reactions; }

    // New Fields
    private Integer replyToId;
    private String messageStatus;
    private String profilePic;
    private Long lastSeen;

    public Integer getReplyToId() { return replyToId; }
    public void setReplyToId(Integer replyToId) { this.replyToId = replyToId; }
    public String getMessageStatus() { return messageStatus; }
    public void setMessageStatus(String messageStatus) { this.messageStatus = messageStatus; }
    public String getProfilePic() { return profilePic; }
    public void setProfilePic(String profilePic) { this.profilePic = profilePic; }
    public Long getLastSeen() { return lastSeen; }
    public void setLastSeen(Long lastSeen) { this.lastSeen = lastSeen; }
}
