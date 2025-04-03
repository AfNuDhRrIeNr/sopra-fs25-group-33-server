package ch.uzh.ifi.hase.soprafs24.rest.dto;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import java.util.List;

public class GamePutDTO {
    private List<User> users;

    public List<User> getUsers() { return users; }
    public void setUsers(List<User> users) { this.users = users; }
}
