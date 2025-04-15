package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class GameInvitationPostDTO {
    private Long gameId;

    private String targetUsername;

    public Long getGameId() {
        return gameId;
    }

    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }

    public String getTargetUsername() { return targetUsername; }

    public void setTargetUsername(String targetUsername) { this.targetUsername = targetUsername; }
}
