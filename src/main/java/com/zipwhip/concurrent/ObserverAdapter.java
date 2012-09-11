package com.zipwhip.concurrent;

import com.zipwhip.events.Observer;

/**
* Created with IntelliJ IDEA.
* User: Michael
* Date: 9/10/12
* Time: 6:16 PM
* To change this template use File | Settings | File Templates.
*/
public class ObserverAdapter<T> implements Observer<T> {

    private final Observer<T> observer;

    public ObserverAdapter(Observer<T> observer) {
        this.observer = observer;

        if (this.observer == null) {
            throw new IllegalArgumentException("Observer cannot be null!");
        }
    }

    @Override
    public void notify(Object sender, T item) {
        observer.notify(sender, item);
    }

    public Observer<T> getObserver() {
        return observer;
    }
}
