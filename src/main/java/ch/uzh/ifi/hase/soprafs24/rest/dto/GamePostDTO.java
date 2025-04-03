package ch.uzh.ifi.hase.soprafs24.rest.dto;
import ch.uzh.ifi.hase.soprafs24.entity.User;
public class GamePostDTO {
    private User host;

    public User getHost() { return host; }

    public void setHost(User host) { this.host = host; }
}
