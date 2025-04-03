package ch.uzh.ifi.hase.soprafs24.rest.dto;

import ch.uzh.ifi.hase.soprafs24.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;



public class GameGetDTO {
    private Long id;
    private List<User> users;
    private String host;
    private List<String> userOrder;
    private GameStatus gameStatus;
    private LocalDateTime startTime;

    private char[][] board;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public List<User> getUsers() { return users; }
    public void setUsers(List<User> users) { this.users = users; }


    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public List<String> getUserOrder() { return userOrder; }
    public void setUserOrder(List<String> userOrder) { this.userOrder = new ArrayList<>(userOrder); }

    public GameStatus getGameStatus() { return gameStatus; }
    public void setGameStatus(GameStatus gameStatus) { this.gameStatus = gameStatus; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public char[][] getBoard() {
        return board;
    }

    public void setBoard(char[][] board) {
        this.board = board;
    }

}
