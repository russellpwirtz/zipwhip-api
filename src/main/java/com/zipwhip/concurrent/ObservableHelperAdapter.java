package com.zipwhip.concurrent;

import com.zipwhip.events.ObservableHelper;
import com.zipwhip.events.Observer;

/**
 * Created with IntelliJ IDEA.
 * User: Michael
 * Date: 9/11/12
 * Time: 2:37 PM
 *
 * Subclass this to adapt the ObservableHelper
 */
public class ObservableHelperAdapter<T> extends ObservableHelper<T> {

    private final ObservableHelper<T> observableHelper;

    public ObservableHelperAdapter(ObservableHelper<T> observableHelper) {
        this.observableHelper = observableHelper;
        this.link(observableHelper);
    }

    @Override
    public void addObserver(Observer<T> observer) {
        observableHelper.addObserver(observer);
    }

    @Override
    public void removeObserver(Observer<T> observer) {
        observableHelper.removeObserver(observer);
    }

    @Override
    public void notifyObservers(Object sender, T result) {
        observableHelper.notifyObservers(sender, result);
    }

    @Override
    public void notifyObserver(Observer<T> observer, Object sender, T result) {
        observableHelper.notifyObserver(observer, sender, result);
    }

    @Override
    public void notify(Object sender, T item) {
        observableHelper.notify(sender, item);
    }

    @Override
    public String toString() {
        return String.format("[a: %s]", observableHelper.toString());
    }

    @Override
    public int hashCode() {
        return observableHelper.hashCode();
    }

    public ObservableHelper<T> getObservableHelper() {
        return observableHelper;
    }
}
