package ch.uzh.ifi.hase.soprafs24.constant.errors;

public class InvalidGameStatusException extends IllegalArgumentException {
    public InvalidGameStatusException(String message) {
        super(message);
    }

    public InvalidGameStatusException(String message, Throwable cause) {
        super(message, cause);
    }
}
