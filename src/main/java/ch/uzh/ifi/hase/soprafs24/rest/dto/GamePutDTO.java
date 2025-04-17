package ch.uzh.ifi.hase.soprafs24.rest.dto;
import ch.uzh.ifi.hase.soprafs24.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import java.util.List;

public class GamePutDTO {
    private GameStatus gameStatus;
    private List<Character> newTiles;

    public GameStatus getGameStatus() {
        return gameStatus;
    }

    public void setGameStatus(GameStatus gameStatus) {
        this.gameStatus = gameStatus;
    }
    public List<Character> getNewTiles() {
        return newTiles;
    }

    public void setNewTiles(List<Character> newTiles) {
        this.newTiles = newTiles;
    }
}
