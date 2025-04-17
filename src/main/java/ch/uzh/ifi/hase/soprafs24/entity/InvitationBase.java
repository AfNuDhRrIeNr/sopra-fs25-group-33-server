package ch.uzh.ifi.hase.soprafs24.entity;

import ch.uzh.ifi.hase.soprafs24.enums.InvitationStatus;

import javax.persistence.*;
import java.time.LocalDateTime;
@MappedSuperclass
public abstract class InvitationBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    // Who is receiving the invitation
    @ManyToOne
    @JoinColumn(name = "sender_id")
    private User target;

    // Who sent the invitation
    @ManyToOne
    @JoinColumn(name = "target_id")
    private User sender;

    @Column(nullable = false)
    InvitationStatus status = InvitationStatus.PENDING;

    @Column(nullable = false)
    LocalDateTime timeStamp = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getTarget() {
        return target;
    }

    public void setTarget(User target) {
        this.target = target;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public InvitationStatus getStatus() {
        return status;
    }

    public void setStatus(InvitationStatus status) {
        this.status = status;
    }

    public LocalDateTime getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(LocalDateTime timeStamp) {
        this.timeStamp = timeStamp;
    }

}
