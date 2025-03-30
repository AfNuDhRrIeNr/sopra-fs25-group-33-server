package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.time.LocalDateTime;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.BoardBase;
import ch.uzh.ifi.hase.soprafs24.entity.Moves;
import ch.uzh.ifi.hase.soprafs24.constant.GameStatus;
import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;



public class GameGetDTO {
    private Long id;
    private List<User> users;
    private BoardBase boardBase; 
    private String host;
    private List<String> userOrder;
    private GameStatus gameStatus;
    private LocalDateTime startTime;
    private List<Moves> moves;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public List<User> getUsers() { return users; }
    public void setUsers(List<User> users) { this.users = users; }

    public BoardBase getBoardBase() { return boardBase; }
    public void setBoardBase(BoardBase boardBase) { this.boardBase = boardBase; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public List<String> getUserOrder() { return userOrder; }
    public void setUserOrder(List<String> userOrder) { this.userOrder = new ArrayList<>(userOrder); }

    public GameStatus getGameStatus() { return gameStatus; }
    public void setGameStatus(GameStatus gameStatus) { this.gameStatus = gameStatus; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public List<Moves> getMoves() { return moves; }
    public void setMoves(List<Moves> moves) { this.moves = moves; }
}
