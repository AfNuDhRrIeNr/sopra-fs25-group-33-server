package ch.uzh.ifi.hase.soprafs24.constant.errors;

import javassist.NotFoundException;

public class GameInvitationNotFoundException extends NotFoundException {
    private static final long serialVersionUID = 1L;

    public GameInvitationNotFoundException() {
        super("Game invitation not found");
    }

    public GameInvitationNotFoundException(String message) {
        super(message);
    }

}
