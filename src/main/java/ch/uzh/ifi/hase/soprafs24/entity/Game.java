package ch.uzh.ifi.hase.soprafs24.entity;

import ch.uzh.ifi.hase.soprafs24.constant.GameStatus;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Game {

    @GeneratedValue
    @Id
    private Long id;


    @ManyToMany
    @JoinTable(name = "game_user",
            joinColumns = @JoinColumn(name = "game_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private List<User> users = new ArrayList<User>();

    private String host;

    @ElementCollection
    private List<String> userOrder = new ArrayList<>();

    private GameStatus gameStatus = GameStatus.CREATED;

    @Column(name = "start_time")
    private LocalDateTime startTime;



    public List<User> getUsers() { return this.users; }
    public void setUsers(List<User> users) { this.users = users; }
    public void addUser(User user) { this.users.add(user); }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public List<String> getUserOrder() { return userOrder; }
    public void setUserOrder(List<String> userOrder) { this.userOrder = userOrder; }

    public GameStatus getGameStatus() { return gameStatus; }
    public void setGameStatus(GameStatus gameStatus) { this.gameStatus = gameStatus; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
