package com.zipwhip.executors;

import com.zipwhip.lifecycle.DestroyableBase;
import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Created by IntelliJ IDEA.
 * User: Russ
 * Date: 8/31/12
 * Time: 2:19 PM
 */
public class DebuggingExecutor extends DestroyableBase implements Executor {

    private final static Logger LOGGER = Logger.getLogger(DebuggingExecutor.class);

    public final Executor executor;
    protected Runnable currentItem;
    protected final List<Runnable> runnableSet = Collections.synchronizedList(new LinkedList<Runnable>());

    public DebuggingExecutor(Executor executor) {
        this.executor = executor;
    }

    @Override
    public synchronized void execute(final Runnable command) {
        final DebugDurationHelper helper = new DebugDurationHelper(String.format("%s:%s", DebuggingExecutor.this, command.toString()));

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(String.format("[%s event=\"%s\" item=\"%s\" queue=%s]", DebuggingExecutor.this, "enqueue", command, runnableSet));
        }
        runnableSet.add(command);

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    runnableSet.remove(command);
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace(String.format("[%s event=\"%s\" item=\"%s\" queue=%s]", DebuggingExecutor.this, "run", command, runnableSet));
                        LOGGER.trace(helper.start());
                    }
                    currentItem = command;
                    command.run();
                } finally {
                    currentItem = null;
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace(helper.stop());
                        LOGGER.trace(String.format("[%s event=\"%s\" item=\"%s\" queue=%s]", DebuggingExecutor.this, "finish", command, runnableSet));
                    }
                }
            }
        });
    }

    @Override
    public String toString() {
        return String.format("DebuggingExecutor(%s)", executor);
    }

    @Override
    protected void onDestroy() {
        runnableSet.clear();
    }
}
