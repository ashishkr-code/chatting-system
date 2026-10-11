package com.chat.models;

import jakarta.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne
    @JoinColumn(name = "receiver_id")
    private User receiver;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(columnDefinition = "LONGTEXT")
    private String reactions = "{}";

    @Column(name = "timestamp", insertable = false, updatable = false)
    private Timestamp timestamp;

    public Message() {}

    public Message(User sender, User receiver, String content) {
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
    }

    @Column(name = "reply_to_id")
    private Integer replyToId;

    @Column(name = "message_status")
    private String messageStatus = "SENT";

    public int getId() { return id; }
    public User getSender() { return sender; }
    public User getReceiver() { return receiver; }
    public String getContent() { return content; }
    public Timestamp getTimestamp() { return timestamp; }
    
    public String getReactions() { return reactions; }
    public void setReactions(String reactions) { this.reactions = reactions; }

    public Integer getReplyToId() { return replyToId; }
    public void setReplyToId(Integer replyToId) { this.replyToId = replyToId; }

    public String getMessageStatus() { return messageStatus; }
    public void setMessageStatus(String messageStatus) { this.messageStatus = messageStatus; }
}
