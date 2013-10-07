package com.zipwhip.signals;

import com.zipwhip.concurrent.HoleRange;
import com.zipwhip.events.Observable;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Russ
 * Date: 10/4/13
 * Time: 10:40 AM
 */
public interface VersionRange {

    // Add a version to the list.
    boolean add(long version);

    // Remove and return current holes
    List<HoleRange> takeHoles();

    Observable<HoleRange> getHoleDetectedEvent();

    // Track the latest
    Long getHighestVersion();

}
