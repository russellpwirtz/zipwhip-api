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

    final Executor executor;
    final List<Runnable> runnableSet = Collections.synchronizedList(new LinkedList<Runnable>());

    public DebuggingExecutor(Executor executor) {
        this.executor = executor;
    }

    @Override
    public synchronized void execute(final Runnable command) {
        final DebugDurationHelper helper = new DebugDurationHelper(String.format("%s:%s", DebuggingExecutor.this, command.toString()));

        runnableSet.add(command);
        LOGGER.debug(String.format("[%s event=\"%s\" item=\"%s\" queue=%s]", DebuggingExecutor.this, "enqueue", command, runnableSet));

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    runnableSet.remove(command);
                    LOGGER.debug(String.format("[%s event=\"%s\" item=\"%s\" queue=%s]", DebuggingExecutor.this, "run", command, runnableSet));
                    LOGGER.debug(helper.start());
                    command.run();
                } finally {
                    LOGGER.debug(helper.stop());
                    LOGGER.debug(String.format("[%s event=\"%s\" item=\"%s\" queue=%s]", DebuggingExecutor.this, "finish", command, runnableSet));
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
