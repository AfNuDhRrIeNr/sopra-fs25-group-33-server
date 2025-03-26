package ch.uzh.ifi.hase.soprafs24.entity;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Internal User Representation
 * This class composes the internal representation of the user and defines how
 * the user is stored in the database.
 * Every variable will be mapped into a database field with the @Column
 * annotation
 * - nullable = false -> this cannot be left empty
 * - unique = true -> this value must be unqiue across the database -> composes
 * the primary key
 */
@Entity
public class User implements Serializable {

  @Id
  @GeneratedValue
  private Long id;

  @Column(nullable = false, unique = true)
  private String username;

  @Column(nullable = false)
  private String password;

  @Column(nullable = false, unique = true)
  private String token;

  @Column(nullable = false)
  private UserStatus status;

  @Column(nullable = false)
  private boolean isInGame = false;

    // Self-referencing many-to-many for friendship
  @ManyToMany
  private Set<User> friends = new HashSet<>();

  @OneToMany(mappedBy = "sender")
  private Set<FriendRequest> sentFriendRequests = new HashSet<>();

  @OneToMany(mappedBy = "target")
  private Set<FriendRequest> receivedFriendRequests = new HashSet<>();

  @OneToMany(mappedBy = "sender")
  private Set<GameInvitation> receivedGameInvitations = new HashSet<>();

  @OneToOne
  private Game bestGamePlayed;

  @Column(nullable = false)
  private LocalDateTime lastModified = LocalDateTime.now();



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

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public UserStatus getStatus() {
    return status;
  }

  public void setStatus(UserStatus status) {
    this.status = status;
  }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isInGame() {
        return isInGame;
    }

    public void setInGame(boolean inGame) {
        isInGame = inGame;
    }

    public Set<User> getFriends() {
        return friends;
    }

    public void setFriends(Set<User> friends) {
        this.friends = friends;
    }

    public void addFriend(User user) {
      this.friends.add(user);
    }

    public Set<FriendRequest> getSentRequests() {
        return sentFriendRequests;
    }

    public void addFriendRequestTSentRequests(FriendRequest request) {
        this.sentFriendRequests.add(request);
    }

    public Set<FriendRequest> getReceivedRequests() {
        return receivedFriendRequests;
    }

    public void addFriendRequestsToReceivedRequests(FriendRequest request) {
        this.receivedFriendRequests.add(request);
    }

    public Game getBestGamePlayed() {
        return bestGamePlayed;
    }

    public void setBestGamePlayed(Game bestGamePlayed) {
        this.bestGamePlayed = bestGamePlayed;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }
    public Set<GameInvitation> getReceivedGameInvitations() {
        return receivedGameInvitations;
    }

    public void addGameInvitation(GameInvitation gameInvitation) {
      this.receivedGameInvitations.add(gameInvitation);
    }
}
