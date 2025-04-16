package ch.uzh.ifi.hase.soprafs24.rest.dto;

import ch.uzh.ifi.hase.soprafs24.enums.InvitationStatus;

import java.time.LocalDateTime;

public class GameInvitationsGetDTO {
    private Long id;

    private UserGetDTO sender;

    private UserGetDTO target;

    private GameGetDTO game;

    private InvitationStatus status;

    private LocalDateTime timeStamp;

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

    public GameGetDTO getGame() {
        return game;
    }

    public void setGame(GameGetDTO game) {
        this.game = game;
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
