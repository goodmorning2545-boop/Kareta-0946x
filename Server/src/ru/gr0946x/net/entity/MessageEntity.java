package ru.gr0946x.net.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
public class MessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private UserEntity sender;

    @ManyToOne
    @JoinColumn(name = "receiver_id")
    private UserEntity receiver; // null = broadcast

    @Column(nullable = false)
    private String text;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Column(nullable = false)
    private boolean delivered = false;

    public MessageEntity() {}

    public MessageEntity(UserEntity sender, UserEntity receiver,
                         String text, LocalDateTime sentAt) {
        this.sender   = sender;
        this.receiver = receiver;
        this.text     = text;
        this.sentAt   = sentAt;
    }

    public Long getId() { return id; }
    public UserEntity getSender() { return sender; }
    public UserEntity getReceiver() { return receiver; }
    public String getText() { return text; }
    public LocalDateTime getSentAt() { return sentAt; }
    public boolean isDelivered() { return delivered; }
    public void setDelivered(boolean delivered) { this.delivered = delivered; }
}