package ch.uzh.ifi.hase.soprafs24.rest.dto;

import ch.uzh.ifi.hase.soprafs24.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;

import java.time.LocalDateTime;
import java.util.List;

public class GameGetDTO {
    private Long id;
    private List<UserGetDTO> users;
    private UserGetDTO host;
    private boolean isHostTurn;
    private GameStatus gameStatus;
    private LocalDateTime startTime;

    private String[][] board;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public List<UserGetDTO> getUsers() { return users; }
    public void setUsers(List<UserGetDTO> users) { this.users = users; }

    public UserGetDTO getHost() { return host; }
    public void setHost(UserGetDTO host) { this.host = host; }

    public GameStatus getGameStatus() { return gameStatus; }
    public void setGameStatus(GameStatus gameStatus) { this.gameStatus = gameStatus; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public String[][] getBoard() {
        return board;
    }

    public void setBoard(String[][] board) {
        this.board = board;
    }

    public boolean isHostTurn() {
        return isHostTurn;
    }

    public void setHostTurn(boolean hostTurn) {
        this.isHostTurn = hostTurn;
    }
}
