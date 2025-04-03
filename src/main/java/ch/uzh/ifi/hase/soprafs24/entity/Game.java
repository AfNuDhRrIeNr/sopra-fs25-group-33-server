package ch.uzh.ifi.hase.soprafs24.entity;

import ch.uzh.ifi.hase.soprafs24.constant.GameStatus;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @OneToOne
    private User host;

    @Column(name = "is_host_turn")
    private boolean isHostTurn = true;

    private GameStatus gameStatus = GameStatus.CREATED;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(length = 225) // 15x15=225 characters
    private String boardState;

    // Transient means this field won't be persisted directly
    @Transient
    private char[][] board;

    // Get the in-memory 2D array representation
    public char[][] getBoard() {
        if (board == null && boardState != null) {
            // Convert string to 2D array
            board = new char[15][15];
            int index = 0;
            for (int i = 0; i < 15; i++) {
                for (int j = 0; j < 15; j++) {
                    if (index < boardState.length()) {
                        // Empty spaces in the string represent null characters in the array
                        board[i][j] = boardState.charAt(index) == ' ' ? '\0' : boardState.charAt(index);
                    } else {
                        board[i][j] = '\0';
                    }
                    index++;
                }
            }
        } else if (board == null) {
            // Initialize new board if both are null
            board = new char[15][15];
            saveBoardState();
        }

        return board;
    }

    // Set the 2D array and update the string representation
    public void setBoard(char[][] newBoard) {
        this.board = newBoard;
        saveBoardState();
    }

    // Convert 2D array to string for storage
    private void saveBoardState() {
        if (board == null) {
            boardState = null;
            return;
        }

        StringBuilder sb = new StringBuilder(225);
        for (int i = 0; i < 15; i++) {
            for (int j = 0; j < 15; j++) {
                // Use space for empty cells
                sb.append(board[i][j] == '\0' ? ' ' : board[i][j]);
            }
        }
        boardState = sb.toString();
    }

    // Initialize an empty board
    public void initializeEmptyBoard() {
        board = new char[15][15];
        saveBoardState();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public List<User> getUsers() { return users; }
    public void setUsers(List<User> users) { this.users = users; }
    public void addUser(User user) { this.users.add(user); }  

    public User getHost() { return host; }
    public void setHost(User host) { this.host = host; }

    public GameStatus getGameStatus() { return gameStatus; }
    public void setGameStatus(GameStatus gameStatus) { this.gameStatus = gameStatus; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public boolean getIsHostTurn() {
        return isHostTurn;
    }

    public void setIsHostTurn(boolean hostTurn) {
        isHostTurn = hostTurn;
    }

    public String getBoardState() {
        return boardState;
    }
    
    public void setBoardState(String boardState) {
        this.boardState = boardState;
    }

}

