package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class GameInvitationPostDTO {
    private Long gameId;
    private Long targetId;

    public Long getGameId() {
        return gameId;
    }

    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }
}
