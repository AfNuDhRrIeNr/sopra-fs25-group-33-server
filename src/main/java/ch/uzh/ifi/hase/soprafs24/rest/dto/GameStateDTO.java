package ch.uzh.ifi.hase.soprafs24.rest.dto;

import ch.uzh.ifi.hase.soprafs24.constant.MoveType;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
public class GameStateDTO {

    private Long id;

    private MoveType type;

    private String token;
    private String[] userTiles;
    private String[][] board;
    private String action;
    private Long playerId;
    private Map<Long, Integer> playerScores;
    private Long surrenderedPlayerId;

    public boolean isValid() {
        if(this.id == null) throw new IllegalArgumentException("ID is missing");
        if(this.action == null) throw new IllegalArgumentException("Action is missing");
        if (this.token == null) throw new IllegalArgumentException("Token is missing");
        if (this.userTiles == null) throw new IllegalArgumentException("UserTiles is missing");
        if(this.board == null) throw new IllegalArgumentException("Board is missing");

        return true;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String[] getUserTiles() {
        return userTiles;
    }

    public void setUserTiles(String[] userTiles) {
        this.userTiles = userTiles;
    }

    public String[][] getBoard() {
        return board;
    }

    public void setBoard(String[][] board) {
        this.board = board;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public Map<Long, Integer> getPlayerScores() {
        return playerScores;
    }

    public void setPlayerScores(Map<Long, Integer> playerScores) {
        this.playerScores = playerScores;
    }

    public Long getSurrenderedPlayerId() {
        return surrenderedPlayerId;
    }

    public void setSurrenderedPlayerId(Long surrenderedPlayerId) {
        this.surrenderedPlayerId = surrenderedPlayerId;
    }
}
