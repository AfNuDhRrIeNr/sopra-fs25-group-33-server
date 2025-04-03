package ch.uzh.ifi.hase.soprafs24.rest.dto;

import ch.uzh.ifi.hase.soprafs24.constant.MessageStatus;

public class MessageGameStateMessageDTO {

    private Long gameId;

    private MessageStatus messageStatus;
    private String message;
    private GameStateDTO gameState;

    public MessageGameStateMessageDTO(Long gameId, MessageStatus messageStatus, String message, GameStateDTO dto) {
        this.gameId = gameId;
        this.messageStatus = messageStatus;
        this.message = message;
        this.gameState = dto;
    }

    public Long getGameId() {
        return gameId;
    }

    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public GameStateDTO getGameState() {
        return gameState;
    }

    public void setGameState(GameStateDTO gameState) {
        this.gameState = gameState;
    }

    public MessageStatus getMessageStatus() {
        return messageStatus;
    }

    public void setMessageStatus(MessageStatus messageStatus) {
        this.messageStatus = messageStatus;
    }
}
