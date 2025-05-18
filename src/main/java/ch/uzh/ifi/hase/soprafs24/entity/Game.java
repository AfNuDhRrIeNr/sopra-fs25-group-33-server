package ch.uzh.ifi.hase.soprafs24.entity;

import ch.uzh.ifi.hase.soprafs24.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs24.constant.LetterCount;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "GAME")
public class Game implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToMany(/*fetch = FetchType.EAGER - Not possible causes user duplication when sending gameInvitations*/)
    @JoinTable(name = "game_user",
            joinColumns = @JoinColumn(name = "game_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private List<User> users = new ArrayList<>();

    @OneToOne
    private User host;

    @Column(name = "is_host_turn")
    private boolean isHostTurn = true;

    private GameStatus gameStatus = GameStatus.CREATED;

    @Column(nullable = true)
    private LocalDateTime startTime;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "game_letter_bag", joinColumns = @JoinColumn(name = "game_id"))
    @MapKeyColumn(name = "letter")
    @Column(name = "count")
    private Map<Character, Integer> letterBag = new HashMap<>(LetterCount.INITIAL_LETTER_COUNTS);

    // Store board as a string - much simpler than bytes
    @Column(length = 225) // 15x15=225 characters
    private String boardState;

    // Transient means this field won't be persisted directly
    @Transient
    private String[][] board;

    private Long surrenderId;

    @ElementCollection(fetch = FetchType.EAGER)
    private Map<Long, Integer> playerScores = new HashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    private Map<Long, String> playerTiles = new HashMap<>();

    @Transient
    private Map<Long, List<String>> playerTilesList = new HashMap<>();

    public String[] getPlayerTiles(Long userId) {
        String tiles = playerTiles.get(userId);
        return tiles==null ? new String[]{}: tiles.split("");
    }

    public void setTilesForPlayer(Long userId, List<String> tiles) {
        playerTilesList.put(userId,tiles);
        String tilesAsString = "";
        for (String tile: tiles) {
            tilesAsString+= tile;
        }
        playerTiles.put(userId,tilesAsString);
    }

    // Get the in-memory 2D array representation
    public String[][] getBoard() {
        if (board == null && boardState != null) {
            // Convert string to 2D array
            board = new String[15][15];
            int index = 0;
            for (int i = 0; i < 15; i++) {
                for (int j = 0; j < 15; j++) {
                    char c = (index < boardState.length()) ? boardState.charAt(index) : ' ';
                    board[i][j] = (c == ' ') ? "" : String.valueOf(c);
                    index++;
                }
            }
        } else if (board == null) {
            // Initialize new board if both are null
            board = new String[15][15];
            for (int i = 0; i < 15; i++) {
                for (int j = 0; j < 15; j++) {
                    board[i][j] = "";
                }
            }
        }

        return board;
    }

    // Set the 2D array and update the string representation
    public void setBoard(String[][] newBoard) {
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
                sb.append(board[i][j].equals("") ? ' ' : board[i][j]);
            }
        }
        boardState = sb.toString();
    }

    // Initialize an empty board
    public void initializeEmptyBoard() {
        board = new String[15][15];
        for (int i = 0; i < 15; i++) {
            for (int j = 0; j < 15; j++) {
                board[i][j] = "";
            }
        }
        saveBoardState();
    }

    public void addScore(Long playerId, int points) {
        int currentScore = playerScores.getOrDefault(playerId, 0);
        playerScores.put(playerId, currentScore + points);
    }

    public int getPlayerScore(Long playerId) {
        return playerScores.getOrDefault(playerId, 0);
    }

    public Map<Long, Integer> getPlayerScores() {
        return Collections.unmodifiableMap(playerScores);
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

    public boolean isHostTurn() { return isHostTurn; }
    public void setHostTurn(boolean hostTurn) { this.isHostTurn = hostTurn; }

    public Map<Character, Integer> getLetterCounts() { return letterBag;}
    public void setLetterBag(Map<Character, Integer> letterBag) { this.letterBag = letterBag; }

    public List<Character> drawLetters(int count) {
        List<Character> drawnLetters = new ArrayList<>();
        Random random = new Random();
    
        for (int i = 0; i < count; i++) {
            List<Character> availableLetters = letterBag.entrySet().stream()
                .filter(entry -> entry.getValue() > 0) 
                .map(Map.Entry::getKey)
                .toList();
    
            if (availableLetters.isEmpty()) {
                break;
            }
            char selectedLetter = availableLetters.get(random.nextInt(availableLetters.size()));
            drawnLetters.add(selectedLetter);
    
            letterBag.put(selectedLetter, letterBag.get(selectedLetter) - 1);
        }
    
        return drawnLetters;
    }

    public int getRemainingLetterCount(char letter) {    
        return letterBag.getOrDefault(letter, 0);
    }

    public List<Character> exchangeTiles(List<Character> currentLetters) {
        // Usage:
        // List<Character> currentLetters = List.of('A', 'B', 'C');
        // List<Character> newLetters = game.exchangeLetters(currentLetters);
        List<Character> newLetters = drawLetters(currentLetters.size());

        for (char letter : currentLetters) {
            if(letter == ' ') continue; // Skip empty letters for example for new game
            letterBag.put(letter, letterBag.getOrDefault(letter, 0) + 1);
        }
        return newLetters;
    }

    public Long getSurrenderId() {
        return surrenderId;
    }

    public void setSurrenderId(Long surrenderId) {
        this.surrenderId = surrenderId;
    }
}

