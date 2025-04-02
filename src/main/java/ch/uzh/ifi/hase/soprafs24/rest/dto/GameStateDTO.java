package ch.uzh.ifi.hase.soprafs24.rest.dto;

import ch.uzh.ifi.hase.soprafs24.constant.MoveType;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class GameStateDTO {

    private Long id;

    private MoveType type;

    private String token;
    private char[] userTiles;
    private char[][] board;

    public boolean isValid() {
        if(this.id == null) throw new IllegalArgumentException("ID is missing");
        if(this.type == null) throw new IllegalArgumentException("MoveType is missing");
        if (this.token == null) throw new IllegalArgumentException("Token is missing");
        if (this.userTiles == null) throw new IllegalArgumentException("UserTiles is missing");
        if(this.board == null) throw new IllegalArgumentException("Board is missing");

        return true;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public MoveType getType() {
        return type;
    }

    public void setType(MoveType type) {
        this.type = type;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public char[] getUserTiles() {
        return userTiles;
    }

    public void setUserTiles(char[] userTiles) {
        this.userTiles = userTiles;
    }

    public char[][] getBoard() {
        return board;
    }

    public void setBoard(char[][] board) {
        this.board = board;
    }
}
