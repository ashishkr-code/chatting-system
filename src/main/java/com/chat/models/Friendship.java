package com.chat.models;

import jakarta.persistence.*;

@Entity
@Table(name = "friendships", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"requester_id", "receiver_id"})
})
public class Friendship {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Column(nullable = false)
    private String status; // "PENDING" or "ACCEPTED"

    public Friendship() {}
    
    public Friendship(User requester, User receiver, String status) {
        this.requester = requester;
        this.receiver = receiver;
        this.status = status;
    }

    public int getId() { return id; }
    public User getRequester() { return requester; }
    public void setRequester(User requester) { this.requester = requester; }
    public User getReceiver() { return receiver; }
    public void setReceiver(User receiver) { this.receiver = receiver; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
