package com.zipwhip.executors;

import com.zipwhip.concurrent.ConfiguredFactory;
import com.zipwhip.lifecycle.Destroyable;
import com.zipwhip.util.Factory;
import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Created by IntelliJ IDEA.
 * User: Russ
 * Date: 8/31/12
 * Time: 2:19 PM
 */
public class DebuggingExecutor extends ExecutorAdapterBase {

    public final static ConfiguredFactory<String, ExecutorService> NAMED_FACTORY = new ConfiguredFactory<String, ExecutorService>() {
        @Override
        public ExecutorService create(String name) {
            return new DebuggingExecutor(Executors.newSingleThreadExecutor(new NamedThreadFactory(name)));
        }
    };

    private final static Logger LOGGER = Logger.getLogger(DebuggingExecutor.class);

    protected final List<Runnable> runnableSet = Collections.synchronizedList(new LinkedList<Runnable>());

    public DebuggingExecutor(Executor executor) {
        super(executor);
    }

    @Override
    public synchronized void execute(final Runnable command) {
        final DebugDurationHelper helper = new DebugDurationHelper(String.format("%s:%s", DebuggingExecutor.this, command.toString()));

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(String.format("[%s %s queue=%s]", "enqueue", command, runnableSet));
        }
        runnableSet.add(command);

        super.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    runnableSet.remove(command);
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace(String.format("[%s %s queue=%s]", "run", command, runnableSet));
                        LOGGER.trace(helper.start());
                    }
                    command.run();
                } finally {
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace(helper.stop());
                        LOGGER.trace(String.format("[%s %s queue=%s]", "finish", command, runnableSet));
                    }
                }
            }
        });
    }

}
