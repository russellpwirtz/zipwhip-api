package com.zipwhip.executors;

import com.zipwhip.concurrent.NamedThreadFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by IntelliJ IDEA.
 * User: Russ
 * Date: 9/2/12
 * Time: 5:07 PM
 */
public class SlowExecutor implements Executor {

    Executor executor = Executors.newSingleThreadExecutor(new NamedThreadFactory("SlowExecutor-"));

    @Override
    public void execute(final Runnable command) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }

                command.run();
            }
        });
    }
}
