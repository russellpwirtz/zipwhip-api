package com.zipwhip.concurrent;

import com.zipwhip.executors.DebuggingExecutor;
import com.zipwhip.executors.NamedThreadFactory;
import com.zipwhip.util.Factory;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created with IntelliJ IDEA.
 * User: Michael
 * Date: 9/11/12
 * Time: 3:42 PM
 *
 *
 */
public class ExecutorFactory implements Factory<Executor>, ConfiguredFactory<String, Executor> {

    public final static ConfiguredFactory<String, ExecutorService> NAMED_FACTORY = new ConfiguredFactory<String, ExecutorService>() {
        @Override
        public ExecutorService create(String name) {
            return Executors.newSingleThreadExecutor(new NamedThreadFactory(name));
        }
    };

    @Override
    public Executor create() {
        return create(null);
    }

    @Override
    public Executor create(String name) {
        return NAMED_FACTORY.create(null);
    }

    public static ExecutorService newInstance() {
        return newInstance(null);
    }

    public static ExecutorService newInstance(String name) {
        return NAMED_FACTORY.create(name);
    }
}
