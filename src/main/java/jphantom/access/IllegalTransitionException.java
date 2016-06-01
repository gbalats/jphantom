package jphantom.access;

public class IllegalTransitionException extends IllegalStateException
{
    protected static final long serialVersionUID = 837483459345L;

    public IllegalTransitionException(State state, Event event) {
        super("State: " + state + " Event: " + event);
    }
}
