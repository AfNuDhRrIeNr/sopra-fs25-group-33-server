package ch.uzh.ifi.hase.soprafs24.rest.dto;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;

public class UserGetDTO {

  private Long id;
  private String username;
  private UserStatus status;
  private String token;
  private boolean isInGame;

  private int highScore;

  private FriendDTO[] friends;

  public boolean isInGame() {
      return isInGame;
    }

  public void setInGame(boolean inGame) {
        isInGame = inGame;
  }

  public int getHighScore() { return highScore; }
    public void setHighScore(int highScore) { this.highScore = highScore; }

    public FriendDTO[] getFriends() {
        return friends;
    }

    public void setFriends(FriendDTO[] friends) {
        this.friends = friends;
    }

    public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public UserStatus getStatus() {
    return status;
  }

  public void setStatus(UserStatus status) {
    this.status = status;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }
}
