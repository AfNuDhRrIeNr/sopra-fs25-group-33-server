package ch.uzh.ifi.hase.soprafs24.entity;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.BoardBase;
import ch.uzh.ifi.hase.soprafs24.entity.Moves;
import ch.uzh.ifi.hase.soprafs24.constant.GameStatus;
import javax.persistence.*;
import java.io.Serializable;
import ch.uzh.ifi.hase.soprafs24.entity.User;
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

    @ManyToMany
    @JoinTable(name = "game_user",
            joinColumns = @JoinColumn(name = "game_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private List<User> users = new ArrayList<>();

    private BoardBase boardBase;

    private String host;

    @ElementCollection
    private List<String> userOrder = new ArrayList<>();
 
    private GameStatus gameStatus = GameStatus.CREATED;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @ElementCollection
    private List<Moves> moves = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public List<User> getUsers() { return users; }
    public void setUsers(List<User> users) { this.users = users; }
    public void addUser(User user) { this.users.add(user); }  

    public BoardBase getBoardBase() { return boardBase; }
    public void setBoardBase(BoardBase boardBase) { this.boardBase = boardBase; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public List<String> getUserOrder() { return userOrder; } 
    public void setUserOrder(List<String> userOrder) { this.userOrder = userOrder; }

    public GameStatus getGameStatus() { return gameStatus; }
    public void setGameStatus(GameStatus gameStatus) { this.gameStatus = gameStatus; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public List<Moves> getMoves() { return moves; }
    public void setMoves(List<Moves> moves) { this.moves = moves; }
}