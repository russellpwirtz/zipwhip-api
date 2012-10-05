package com.zipwhip.concurrent;

import com.zipwhip.executors.DebuggingExecutor;
import com.zipwhip.executors.NamedThreadFactory;
import com.zipwhip.util.Factory;

import java.util.HashMap;
import java.util.Map;
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
public class ExecutorFactory implements Factory<ExecutorService>, ConfiguredFactory<String, Executor> {

    private static Map<String, NamedThreadFactory> factories = new HashMap<String, NamedThreadFactory>();
    public static NamedThreadFactory getOrCreate(String name) {
        if (factories.containsKey(name)) {
            return factories.get(name);
        }
        factories.put(name, new NamedThreadFactory(name));
        return factories.get(name);
    }

    public final static ConfiguredFactory<String, ExecutorService> NAMED_FACTORY = new ConfiguredFactory<String, ExecutorService>() {
        @Override
        public ExecutorService create(String name) {
            return Executors.newSingleThreadExecutor(getOrCreate(name));
        }
    };

    @Override
    public ExecutorService create() {
        return create(null);
    }

    @Override
    public ExecutorService create(String name) {
        return NAMED_FACTORY.create(null);
    }

    public static ExecutorService newInstance() {
        return newInstance(null);
    }

    public static ExecutorService newInstance(String name) {
        return NAMED_FACTORY.create(name);
    }
}
