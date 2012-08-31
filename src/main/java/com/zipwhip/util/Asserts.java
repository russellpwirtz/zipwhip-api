package com.zipwhip.util;

/**
 * Created by IntelliJ IDEA.
 * User: Russ
 * Date: 8/27/12
 * Time: 11:11 AM
 */
public class Asserts {

    public static void assertTrue(boolean test, String message) {
        if (!test) {
            throw new RuntimeException("Assert failed!: " + message);
        }
    }

}
