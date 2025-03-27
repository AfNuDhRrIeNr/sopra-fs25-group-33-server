package ch.uzh.ifi.hase.soprafs24.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.Queue;
import java.util.LinkedList;

@Entity
@Table(name = "GAME")
public class Game implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ElementCollection
    private List<String> users = new ArrayList<>();

    private String boardBase = "DEFAULT_BOARD";

    private String host;

    @ElementCollection
    private Queue<String> userOrder = new LinkedList<>();
 
    private String gameStatus = "WAITING";

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @ElementCollection
    private List<String> moves = new ArrayList<>();

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
