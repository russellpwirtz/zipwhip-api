package com.zipwhip.concurrent;

import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.*;

/**
 * Created by IntelliJ IDEA.
 * User: Russ
 * Date: 9/2/12
 * Time: 1:34 PM
 */
public class TestUtil {

    public static <T> T awaitAndAssertSuccess(ObservableFuture<T> future) {
        assertNotNull("future was null!!", future);
        try {
            assertTrue("future didn't finish!", future.await(90, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        assertTrue("future wasn't done!", future.isDone());
        assertNull(String.format("Future failure was %s", future.getCause()), future.getCause());
        assertFalse("future was cancelled!", future.isCancelled());
        assertTrue("future wasn't successful!", future.isSuccess());

        return future.getResult();
    }
}
