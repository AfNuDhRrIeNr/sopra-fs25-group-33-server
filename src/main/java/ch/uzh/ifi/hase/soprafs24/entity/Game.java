package ch.uzh.ifi.hase.soprafs24.entity;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "GAME")
public class Game {
    @Id
    @GenerateValue
    private Long id;

    @ElementCollection
    private List<String> users;

    private String boardBase;

    private String host;

    @ElementCollection
    private Queue<String> userOrder;

    private String gameStatus;

    private LocalDateTime startTime;

    @ElementCollection
    private List<String> moves;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public List<String> getUsers() { return users; }
    public void setUsers(List<String> users) { this.users = users; }   

    public String getBoardBase() { return boardBase; }
    public void setBoardBase(String boardBase) { this.boardBase = boardBase; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public Queue<String> getUserOrder() { return userOrder; }
    public void setUserOrder(Queue<String> userOrder) { this.userOrder = userOrder; }

    public String getGameStatus() { return gameStatus; }
    public void setGameStatus(String gameStatus) { this.gameStatus = gameStatus; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public List<String> getMoves() { return moves; }
    public void setMoves(List<String> moves) { this.moves = moves; }
}
