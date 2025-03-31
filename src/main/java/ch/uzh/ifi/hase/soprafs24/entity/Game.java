package ch.uzh.ifi.hase.soprafs24.entity;

import javax.persistence.*;
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
    private List<User> players = new ArrayList<User>();

    // Store board as a string - much simpler than bytes
    @Column(length = 225) // 15x15=225 characters
    private String boardState;

    // Transient means this field won't be persisted directly
    @Transient
    private char[][] board;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<User> getPlayers() {
        return players;
    }

    public void setPlayers(List<User> players) {
        this.players = players;
    }
    
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
}
