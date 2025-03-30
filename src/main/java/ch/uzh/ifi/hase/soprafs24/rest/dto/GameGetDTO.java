package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.time.LocalDateTime;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;



public class GameGetDTO {
    private Long id;
    private List<User> users;
    private String boardBase; 
    private String host;
    private List<String> userOrder;
    private String gameStatus;
    private LocalDateTime startTime;
    private List<String> moves;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public List<User> getUsers() { return users; }
    public void setUsers(List<User> users) { this.users = users; }

    public String getBoardBase() { return boardBase; }
    public void setBoardBase(String boardBase) { this.boardBase = boardBase; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public List<String> getUserOrder() { return userOrder; }
    public void setUserOrder(List<String> userOrder) { this.userOrder = new ArrayList<>(userOrder); }

    public String getGameStatus() { return gameStatus; }
    public void setGameStatus(String gameStatus) { this.gameStatus = gameStatus; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public List<String> getMoves() { return moves; }
    public void setMoves(List<String> moves) { this.moves = moves; }
}
