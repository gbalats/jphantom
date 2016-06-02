package org.clyze.jphantom.access;

import com.google.common.collect.ForwardingTable;

public abstract class StateMachine<S,E> extends ForwardingTable<S,E,S>
{
    protected final S initial;

    public StateMachine(S initialState) {
        this.initial = initialState;
    }
}
