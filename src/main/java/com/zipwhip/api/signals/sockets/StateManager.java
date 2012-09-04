package com.zipwhip.api.signals.sockets;

import com.zipwhip.util.CollectionUtil;
import com.zipwhip.util.Directory;
import com.zipwhip.util.SetDirectory;

import java.util.Arrays;
import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: Michael
 * Date: 8/15/12
 * Time: 11:05 AM
 *
 * For trusted state transitioning. Thread safe.
 */
public class StateManager<T extends Enum> {

    private Directory<T, T> states = new SetDirectory<T, T>();
    private T state;
    private long stateId = 0;

    public StateManager(Directory<T, T> directory) {
        this();
        this.states = directory;
    }

    public StateManager() {

    }

    public synchronized void add(T source, T destination) {
        states.add(source, destination);
    }

    public synchronized void set(T state) {
        this.stateId ++;
        this.state = state;
    }

    public synchronized boolean transition(T state) {
        Collection<T> states = this.states.get(this.state);

        if (CollectionUtil.isNullOrEmpty(states)) {
            // there is no transition!
            return false;
        }

        // is this allowed?
        boolean allowed = states.contains(state);

        if (allowed) {
            this.set(state);
        }

        return allowed;
    }

    public synchronized void transitionOrThrow(T state) {
        T originalState = this.state;
        boolean allowed = transition(state);

        if (!allowed) {
            throw new IllegalStateException(String.format("Cannot transition from %s to %s", originalState, state));
        }
    }

    public synchronized void ensure(T state) {
        if (this.state != state) {
            throw new IllegalStateException(String.format("State needed to be %s BUT WAS %s instead.", this.state, state));
        }
    }

    public synchronized void ensureNot(Collection<T> states) {
        T currentState = this.get();
        for (T state : states) {
            if (state == currentState) {
                throw new IllegalStateException(String.format("The state was %s. Failed ensureNot() check.", state));
            }
        }
    }
    public synchronized void ensureNot(T... states) {
        ensureNot(Arrays.asList(states));
    }

    public T get() {
        return this.state;
    }

    public synchronized boolean or(T... states) {
        for (T state : states) {
            if (get() == state) {
                return true;
            }
        }
        return false;
    }

    public long getStateId() {
        return stateId;
    }
}
