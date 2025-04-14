package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class FriendRequestPostDTO {
    private Long targetId;
    private String message;

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
