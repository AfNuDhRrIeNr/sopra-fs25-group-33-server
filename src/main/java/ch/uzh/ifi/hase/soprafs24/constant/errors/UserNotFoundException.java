package ch.uzh.ifi.hase.soprafs24.constant.errors;

import javassist.NotFoundException;

public class UserNotFoundException extends NotFoundException {
    public UserNotFoundException(String message) {
        super(message);
    }

    public UserNotFoundException(String message, Exception cause) {
        super(message, cause);
    }
}
