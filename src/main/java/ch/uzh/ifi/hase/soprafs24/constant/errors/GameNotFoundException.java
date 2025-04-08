package ch.uzh.ifi.hase.soprafs24.constant.errors;

import javassist.NotFoundException;

public class GameNotFoundException extends NotFoundException {
    public GameNotFoundException(String message) {
        super(message);
    }

    public GameNotFoundException(String message, Exception cause) {
        super(message, cause);
    }

}
