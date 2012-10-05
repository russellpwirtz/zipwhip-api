package com.zipwhip.executors;

import com.zipwhip.util.Factory;

import java.util.concurrent.ExecutorService;

/**
 * Created with IntelliJ IDEA.
 * User: Michael
 * Date: 9/12/12
 * Time: 3:01 PM
 *
 *
 */
public interface CommonExecutorFactory extends Factory<ExecutorService> {

    ExecutorService create(CommonExecutorTypes type, String name);

}
