package com.zipwhip.concurrent;

import com.zipwhip.util.StateManager;
import com.zipwhip.lifecycle.DestroyableBase;
import com.zipwhip.util.CollectionUtil;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;

/**
 * Created with IntelliJ IDEA.
 * User: Michael
 * Date: 8/15/12
 * Time: 9:29 AM
 * <p/>
 * Allows us to safely control threaded ensureAbleTo
 */
public class ThreadBoundary<T extends Enum> extends DestroyableBase {

    private static final Logger LOGGER = Logger.getLogger(ThreadBoundary.class);

    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private long timeout = 60;
    private TimeUnit timeUnit = TimeUnit.SECONDS;
    private Collection<T> forbiddenStates;
    private Collection<Lock> locks;
    private StateManager<T> stateManager;

    public ThreadBoundary(StateManager<T> stateManager, Collection<T> forbiddenStates, Collection<Lock> locks) {
        this();
        this.forbiddenStates = forbiddenStates;
        this.locks = locks;
        this.stateManager = stateManager;
    }

    public ThreadBoundary(StateManager<T> stateManager, Lock lock) {
        this();
        this.locks = Arrays.asList(lock);
        this.stateManager = stateManager;
    }

    public ThreadBoundary() {

    }

    public ThreadBoundary(Lock lock) {
        this();
        this.locks = Arrays.asList(lock);
    }

    public <V> Future<V> enqueue(final Callable<V> task) {
        return enqueue(new FutureTask<V>(task), false);
    }

    public <V> Future<V> enqueue(final Callable<V> task, boolean ignoreState) {
        return enqueue(new FutureTask<V>(task), ignoreState);
    }

    public synchronized <V> Future<V> enqueue(final FutureTask<V> task) {
        return enqueue(task, false);
    }

    public synchronized <V> Future<V> enqueue(final FutureTask<V> task, final boolean ignoreState) {
        if (this.isDestroyed()) {
            // we are destroyed, some terrible concurrency bug must have happened
            // a new wrapper is in charge and we were destroyed!
            // rather than muck with the state of our parent (or myself) let's just quit.
            throw new IllegalStateException("The wrapper was destroyed and we tried to enqueue more work");
        }

        if (!ignoreState) {
            // ensure that we're not destroyed or disconnecting. give the a good stack trace by checking
            // outside the runnable
            if (CollectionUtil.exists(forbiddenStates) && stateManager != null) {
                stateManager.ensureNot(forbiddenStates);
            }
        }

        // if someone changes the connection state while we enqueue, it doesn't matter. The reason is that
        // we double check inside the runnable right before execution.
        executor.execute(new Runnable() {

            @Override
            public void run() {
                // this execution _IS_NOT_ inside the synchronized(this) block.
                // even though the .execute() was called within the sync block, it has already ended.
                // we're on a different thread.

                // DONT LET ANYONE CHANGE THE CURRENT CONNECTION
                // ALWAYS GET THE CONNECTION LATCH FIRST
                try {
                    if (CollectionUtil.exists(locks)) {
                        for (Lock lock : locks) {
                            lock.lock();
                        }
                    }

                    // TODO: think about "what if they destroy themselves after isDestroyed() is checked!
                    synchronized (ThreadBoundary.this) {
                        // slower check for destroyed (within a sync block)
                        if (isDestroyed()) {
                            // shit we got destroyed between then and now!
                            // we need to just quit?
                            throw new IllegalStateException("The executor was destroyed before running this task");
                        }

                        if (!ignoreState) {
                            if (CollectionUtil.exists(forbiddenStates) && stateManager != null) {
                                // ensure that we're not destroyed or disconnecting.
                                stateManager.ensureNot(forbiddenStates);
                            }
                        }

                        // ok cool we're safe to run. nothing can change from anyone, anywhere.
                        // TODO: consider the deadlock with ThreadBoundary.this
                        task.run();
                    }

                } finally {
                    if (CollectionUtil.exists(locks)) {
                        for (Lock lock : locks) {
                            lock.unlock();
                        }
                    }
                }

            }
        });

        return task;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }

    public Collection<T> getForbiddenStates() {
        return forbiddenStates;
    }

    public void setForbiddenStates(Collection<T> forbiddenStates) {
        this.forbiddenStates = forbiddenStates;
    }

    public Collection<Lock> getLocks() {
        return locks;
    }

    public void setLocks(Collection<Lock> locks) {
        this.locks = locks;
    }

    public StateManager<T> getStateManager() {
        return stateManager;
    }

    public void setStateManager(StateManager<T> stateManager) {
        this.stateManager = stateManager;
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        List<Runnable> runnableList = this.executor.shutdownNow();
        if (!CollectionUtil.isNullOrEmpty(runnableList)) {
            LOGGER.warn("Cancelled " + runnableList.size() + " items because the ChannelStateWrapper was destroyed.");
        }
    }
}
