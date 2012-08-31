package com.zipwhip.executors;

/**
 * Created by IntelliJ IDEA.
 * User: Russ
 * Date: 8/31/12
 * Time: 2:19 PM
 */
public class DebugDurationHelper {

    private final long created = System.currentTimeMillis();
    private final String name;
    private long start;

    public DebugDurationHelper(String name) {
        this.name = name;
    }

    public String start() {
        start = System.currentTimeMillis();
        return String.format("<%s started=\"true\" delay=\"%s\">", name, start - created);
    }

    public String stop() {
        return String.format("</%s delay=\"%s\" runtime=\"%s\">", name, start - created, System.currentTimeMillis() - start);
    }

    public String enqueue() {
        return String.format("<%s enqueued=\"true\">", name);
    }
}
