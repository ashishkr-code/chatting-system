package com.chat.models;

import jakarta.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(length = 20)
    private String status = "OFFLINE";

    @Column(name = "created_at", insertable = false, updatable = false)
    private Timestamp createdAt;

    public User() {
        // Hibernate requires a no-arg constructor
    }

    public User(String username, String passwordHash, String fullName) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
    }

    @Column(name = "profile_pic", columnDefinition = "LONGTEXT")
    private String profilePic;

    @Column(name = "last_seen")
    private Timestamp lastSeen;

    // Getters and Setters
    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getFullName() { return fullName; }
    public String getStatus() { return status; }
    public Timestamp getCreatedAt() { return createdAt; }
    
    public String getProfilePic() { return profilePic; }
    public void setProfilePic(String profilePic) { this.profilePic = profilePic; }
    
    public Timestamp getLastSeen() { return lastSeen; }
    public void setLastSeen(Timestamp lastSeen) { this.lastSeen = lastSeen; }

    public void setStatus(String status) { this.status = status; }
}
