package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class FriendRequestPostDTO {
    private String targetUsername;
    private String message;

    public String getTargetUsername() {
        return targetUsername;
    }

    public void setTargetUsername(String targetUsername) {
        this.targetUsername = targetUsername;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
