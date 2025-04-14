package ch.uzh.ifi.hase.soprafs24.rest.dto;

import ch.uzh.ifi.hase.soprafs24.enums.InvitationStatus;

import java.time.LocalDateTime;

public class FriendRequestGetDTO {
    private Long id;

    private UserGetDTO sender;

    private UserGetDTO target;

    private InvitationStatus status;

    private LocalDateTime timeStamp;

    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserGetDTO getSender() {
        return sender;
    }

    public void setSender(UserGetDTO sender) {
        this.sender = sender;
    }

    public UserGetDTO getTarget() {
        return target;
    }

    public void setTarget(UserGetDTO target) {
        this.target = target;
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
