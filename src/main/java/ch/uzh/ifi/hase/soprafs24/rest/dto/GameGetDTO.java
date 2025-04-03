package ch.uzh.ifi.hase.soprafs24.rest.dto;

import ch.uzh.ifi.hase.soprafs24.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;



public class GameGetDTO {
    private Long id;
    private List<User> users;
    private User host;
    private boolean isHostTurn;
    private GameStatus gameStatus;
    private LocalDateTime startTime;

    private String boardState;

    private char[][] board;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public List<User> getUsers() { return users; }
    public void setUsers(List<User> users) { this.users = users; }


    public User getHost() { return host; }
    public void setHost(User host) { this.host = host; }


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

    public String getBoardState() {
        return boardState;
    }
    
    public void setBoardState(String boardState) {
        this.boardState = boardState;
    }
    
    public boolean getIsHostTurn() {
        return isHostTurn;
    }
    
    public void setIsHostTurn(boolean hostTurn) {
        isHostTurn = hostTurn;
    }

}
