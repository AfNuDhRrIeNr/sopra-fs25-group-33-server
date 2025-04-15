package ch.uzh.ifi.hase.soprafs24.constant.errors;

import javassist.NotFoundException;

public class FriendRequestNotFoundException extends NotFoundException {
    public FriendRequestNotFoundException(String msg) {
        super(msg);
    }

    public FriendRequestNotFoundException(String msg, Exception e) {
        super(msg, e);
    }
}
